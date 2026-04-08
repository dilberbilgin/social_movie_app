package com.socialmovieclub.service;

import com.socialmovieclub.core.constant.CacheConstants;
import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.StringUtil;
import com.socialmovieclub.dto.response.WeeklyWinnerResponse;
import com.socialmovieclub.entity.Comment;
import com.socialmovieclub.entity.WeeklyWinner;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.WeeklyWinnerMapper;
import com.socialmovieclub.repository.CommentRepository;
import com.socialmovieclub.repository.WeeklyWinnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyWinnerService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CommentRepository commentRepository;
    private final WeeklyWinnerRepository winnerRepository;
    private final WeeklyWinnerMapper weeklyWinnerMapper;

    /**
     * Her Pazar gecesi 23:59'da çalışarak haftanın en çok beğeni alan yorumunu belirler.
     * Aynı yazarın üst üste iki hafta kazanmasını engelleyen mantığı içerir.
     */
    @Scheduled(cron = "0 59 23 * * SUN")
    @Transactional
    public void determineWeeklyWinner() {
        log.info("Starting weekly winner determination process...");

        // Merkezi sabitlerden key'i alıyoruz
        String rankKey = CacheConstants.WEEKLY_COMMENT_RANKING;

        // 1. Redis Sorted Set'ten en yüksek puanlı ilk 10 yorumu getiriyoruz
        Set<String> topCommentIds = redisTemplate.opsForZSet().reverseRange(rankKey, 0, 9);

        if (topCommentIds == null || topCommentIds.isEmpty()) {
            log.warn("No interactions found in Redis ranking for this week. Process skipped.");
            return;
        }

        // 2. Bir önceki haftanın kazanan kullanıcısını buluyoruz (Üst üste kazanma kontrolü için)
        UUID lastWinnerUserId = winnerRepository.findTopByOrderByWeekEndDateDesc()
                .map(w -> w.getUser().getId())
                .orElse(null);

        String winnerCommentId = null;

        // 3. İlk 10 aday arasından, geçen haftanın kazananı olmayan ilk kişiyi seçiyoruz
        for (String id : topCommentIds) {
            Comment comment = commentRepository.findById(UUID.fromString(id)).orElse(null);

            if (comment != null) {
                // Eğer yazar geçen haftaki yazar değilse, yeni kazanan budur
                if (lastWinnerUserId == null || !comment.getUser().getId().equals(lastWinnerUserId)) {
                    winnerCommentId = id;
                    log.info("New winner identified: User {} for comment {}", comment.getUser().getUsername(), id);
                    break;
                } else {
                    log.info("User {} was last week's winner, checking next candidate...", comment.getUser().getUsername());
                }
            }
        }

        // 4. Kazanan bulunduysa DB'ye kaydet
        if (winnerCommentId != null) {
            saveWinner(UUID.fromString(winnerCommentId), rankKey);
        }

        // 5. Redis'teki haftalık datayı temizle (Yeni hafta sıfırdan başlar)
        redisTemplate.delete(rankKey);
        log.info("Weekly winner process completed and Redis ranking cleared.");
    }

    /**
     * Kazanan yorumu ve puanını kalıcı olarak DB'ye işler.
     */
    private void saveWinner(UUID commentId, String rankKey) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("Winner comment not found in database"));

        // Kazandığı andaki puanı Redis'ten çekiyoruz
        Double score = redisTemplate.opsForZSet().score(rankKey, commentId.toString());

        WeeklyWinner winner = WeeklyWinner.builder()
                .comment(comment)
                .user(comment.getUser())
                .movie(comment.getMovie())
                .weekStartDate(LocalDate.now().minusDays(6))
                .weekEndDate(LocalDate.now())
                .finalLikeCount(score != null ? score.longValue() : 0L)
                .build();

        winnerRepository.save(winner);
        log.info("WeeklyWinner record persisted for user: {}", winner.getUser().getUsername());
    }

    /**
     * Frontend için son kazananı DTO olarak döner.
     */
    @Transactional(readOnly = true)
    public RestResponse<WeeklyWinnerResponse> getLastWinner() {
        return winnerRepository.findTopByOrderByWeekEndDateDesc()
                .map(weeklyWinnerMapper::toResponse)
                .map(RestResponse::success)
                .orElseThrow(() -> new BusinessException("No winner record found."));
    }

    @Transactional(readOnly = true)
    public RestResponse<List<WeeklyWinnerResponse>> getLatestWinners(int limit) {
        // Son belirlenen kazananlardan 'limit' kadarını getir (Örn: Son 4 kazanan)
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("weekEndDate").descending());
        Page<WeeklyWinner> winners = winnerRepository.findAll(pageRequest);

        List<WeeklyWinnerResponse> response = winners.getContent().stream()
                .map(weeklyWinnerMapper::toResponse)
                .toList();

        return RestResponse.success(response);
    }

    /**
     * Henüz haftalık kazanan belirlenmemiş olsa bile,
     * Redis'teki güncel skorlara göre o haftanın en popüler yorumlarını getirir.
     * Bu, siteyi "anlık" ve "canlı" gösteren asıl kurgudur.
     */
    @Transactional(readOnly = true)
    public RestResponse<List<WeeklyWinnerResponse>> getTrendingReviewsThisWeek(int limit) {
        String rankKey = CacheConstants.WEEKLY_COMMENT_RANKING;

        // 1. Redis'ten en yüksek skorlu ilk 'limit' kadar ID'yi al
        Set<String> topCommentIds = redisTemplate.opsForZSet().reverseRange(rankKey, 0, limit - 1);

        if (topCommentIds == null || topCommentIds.isEmpty()) {
            // Eğer Redis boşsa, son belirlenen resmi kazananları dön (Fallback)
            return getLatestWinners(limit);
        }

        // 2. Bu ID'leri DB'den çekip DTO'ya dönüştür
        List<WeeklyWinnerResponse> trending = topCommentIds.stream()
                .map(id -> commentRepository.findById(UUID.fromString(id)).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(comment -> {
                    Double score = redisTemplate.opsForZSet().score(rankKey, comment.getId().toString());
                    return WeeklyWinnerResponse.builder()
                            .commentId(comment.getId())
                            // Yorumu burada kesiyoruz ki frontend'de grid bozulmasın (Örn: 100 karakter)
                            .commentContent(StringUtil.truncate(comment.getContent(), 100))
                            .username(comment.getUser().getUsername())
                            .profilePictureUrl(comment.getUser().getProfilePictureUrl())
                            .movieTitle(comment.getMovie().getOriginalTitle())
                            .moviePosterUrl(comment.getMovie().getPosterUrl())
                            .finalLikeCount(score != null ? score.longValue() : 0L)
                            .weekEndDate(LocalDate.now())
                            .movieId(comment.getMovie().getId()) // Movie UUID
                            .tmdbId(comment.getMovie().getTmdbId()) // TMDB ID
                            .build();
//                .map(comment -> {
//                    Double score = redisTemplate.opsForZSet().score(rankKey, comment.getId().toString());
//                    // Burada WeeklyWinner entity'si yok, manuel veya özel bir mapper ile DTO oluşturuyoruz
//                    return WeeklyWinnerResponse.builder()
//                            .commentId(comment.getId())
//                            .commentContent(comment.getContent())
//                            .username(comment.getUser().getUsername())
//                            .profilePictureUrl(comment.getUser().getProfilePictureUrl())
//                            .movieTitle(comment.getMovie().getOriginalTitle())
//                            .moviePosterUrl(comment.getMovie().getPosterUrl())
//                            .finalLikeCount(score != null ? score.longValue() : 0L)
//                            .weekEndDate(LocalDate.now()) // Mevcut hafta
//                            .build();
                })
                .toList();

        return RestResponse.success(trending);
    }

    @Transactional(readOnly = true)
    public RestResponse<Page<WeeklyWinnerResponse>> getAllWinners(Pageable pageable) {
        Page<WeeklyWinner> winners = winnerRepository.findAllByOrderByWeekEndDateDesc(pageable);

        // MapStruct listeleri otomatik mapleyebilir (Mapper'a List<WeeklyWinnerResponse> metodunu eklemelisin)
        Page<WeeklyWinnerResponse> response = winners.map(weeklyWinnerMapper::toResponse);

        return RestResponse.success(response);
    }


}