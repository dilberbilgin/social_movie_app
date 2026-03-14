package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.Rating;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.RatingMapper;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.RatingRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final RatingMapper ratingMapper;
    private final MessageHelper messageHelper;
    private final SecurityService securityService;

    @Transactional
    public RestResponse<RatingResponse> rateMovie(RatingRequest request) {
        // 1. GÜVENLİK: İstemi yapan kullanıcıyı bul
        User user = securityService.getCurrentUser();
        // 2. DOĞRULAMA: Film var mı?
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        // 3. UPSERT: Mevcut puanı güncelle veya yeni oluştur
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(user.getId(), movie.getId());
        boolean isNew = existingRating.isEmpty();
        Rating rating = existingRating.orElse(new Rating());

        rating.setScore(request.getScore());
        rating.setUser(user);
        rating.setMovie(movie);

        ratingRepository.save(rating);

        // 4. İSTATİSTİK GÜNCELLEME: Filmin genel ortalamasını tazele
        updateMovieRatingStats(movie);

        // 5. RESPONSE HAZIRLAMA
        RatingResponse response = ratingMapper.toResponse(rating);

        // Aktif dili alıp başlığı setliyoruz
        String currentLang = LocaleContextHolder.getLocale().getLanguage();
        response.setMovieTitle(getMovieTitleByLang(movie, currentLang));

        response.setNewClubRating(movie.getClubRating());
        response.setNewClubVoteCount(movie.getClubVoteCount());

        String msgKey = isNew ? "rating.success" : "movie.rating.updated";
        return RestResponse.success(response, messageHelper.getMessage(msgKey));
    }

    public RestResponse<Page<RatingResponse>> getUserRatings(String lang, Pageable pageable ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));

        Page<Rating> ratings = ratingRepository.findAllByUserIdOrderByCreatedDateDesc(user.getId(), pageable);

        Page<RatingResponse> responsePage = ratings.map(rating -> {
            RatingResponse dto = ratingMapper.toResponse(rating);
            // Her film için ilgili dildeki başlığı bul
            dto.setMovieTitle(getMovieTitleByLang(rating.getMovie(), lang));
            return dto;
        });

        return RestResponse.success(responsePage, messageHelper.getMessage("common.success"));
    }

    private void updateMovieRatingStats(Movie movie) {
        Object result = ratingRepository.getRatingStatsByMovieId(movie.getId());
        Object[] stats = (Object[]) ((Object[]) result)[0];

        Double avg = (Double) stats[0];
        Long count = (Long) stats[1];

        movie.setClubRating(avg != null ? avg : 0.0);
        movie.setClubVoteCount(count.intValue());

        movieRepository.save(movie);
    }

    /**
     * Yardımcı Metod: Filmin translations seti içinden talep edilen dildeki başlığı döner.
     */
    private String getMovieTitleByLang(Movie movie, String lang) {
        if (movie.getTranslations() == null) return movie.getOriginalTitle();

        return movie.getTranslations().stream()
                .filter(t -> t.getLanguageCode().equalsIgnoreCase(lang))
                .map(t -> t.getTitle())
                .findFirst()
                .orElse(movie.getOriginalTitle());
    }

    @Transactional
    public RestResponse<Void> deleteRating(UUID movieId) {
        // 1. Kullanıcıyı ve Filmi doğrula
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        // 2. Puanı sil
        ratingRepository.deleteByUserIdAndMovieId(user.getId(), movieId);

        // 3. MATEMATİKSEL TETİKLEME: Puan silindiği için filmin ortalamasını yeniden hesapla
        updateMovieRatingStats(movie);

        return RestResponse.success(null, messageHelper.getMessage("rating.deleted.success"));
    }
}