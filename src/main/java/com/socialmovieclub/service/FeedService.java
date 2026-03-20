package com.socialmovieclub.service;

import com.socialmovieclub.core.constant.CacheConstants;
import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.ActivityResponse;
import com.socialmovieclub.dto.response.CustomPageResponse;
import com.socialmovieclub.entity.Activity;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.ActivityType;
import com.socialmovieclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final ActivityRepository activityRepository;
    private final FollowRepository followRepository;
    private final SecurityService securityService;
    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.FEED_CACHE,
            key = "#currentUserId + '-' + #pageable.pageNumber", // Artık hata vermez
            unless = "#result.success == false")
    public RestResponse<CustomPageResponse<ActivityResponse>> getFollowedFeed(UUID currentUserId, Pageable pageable) {
        User currentUser = securityService.getCurrentUser();

        // 1. Takip edilenlerin ID listesini al
        // Not: Burada Pageable olmayan, tüm listeyi dönen bir repo metodu daha efektif olabilir
        List<UUID> followedUserIds = followRepository.findAllByFollowerId(currentUser.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(Collectors.toList());

        // Eğer kimseyi takip etmiyorsa boş sayfa dön
        if (followedUserIds.isEmpty()) {
            return RestResponse.success(new CustomPageResponse<>()); // Boş liste dön
        }

        // 2. Bu ID'lerin aktivitelerini çek(Henüz zenginleştirilmemiş ham veriler)
        Page<Activity> activities = activityRepository.findByUserIdInOrderByCreatedDateDesc(followedUserIds, pageable);


//        // 3. Map to Response (İleride bunu MapStruct ile yapabiliriz)
//        Page<ActivityResponse> response = activities.map(this::mapToActivityResponse);
//
//        return RestResponse.success(response);

        // 3. PERFORMANS ANAHTARI: Tüm Film ID'lerini topla (Batching)
        // Set kullanarak mükerrer ID'leri eliyoruz (Hafıza tasarrufu)
        Set<UUID> movieIds = activities.getContent().stream()
                .filter(a -> isMovieRelated(a.getType())) // Sadece film ile ilgili olanları seç
                .map(Activity::getTargetId)
                .collect(Collectors.toSet());
        // 4. Tek bir toplu sorgu ile tüm filmleri getir ve Map'e at (Hızlı erişim için)
        // Map yapısı: {ID -> Movie Nesnesi} şeklindedir, böylece döngüde O(1) hızında ulaşırız.
        Map<UUID, Movie> movieMap = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, m -> m));

        // 5. Build kullanarak Response oluştur
        Page<ActivityResponse> response = activities.map(activity -> {
            Movie relatedMovie = movieMap.get(activity.getTargetId());
            return mapToResponse(activity, relatedMovie);

        });
        return RestResponse.success(CustomPageResponse.of(response));
    }

    private boolean isMovieRelated(ActivityType type) {
        return List.of(ActivityType.MOVIE_LIKE,
                ActivityType.MOVIE_RATE,
                ActivityType.COMMENT_CREATE,
                ActivityType.COMMENT_LIKE).contains(type);
    }

    private ActivityResponse mapToResponse(Activity activity, Movie movie) {
        // BUILDER KULLANIMI: Okunabilirlik ve Değişmezlik (Immutability) sağlar.
        // Zincirleme (Fluent) yapısı sayesinde setId, setUsername yazmaktan kurtuluruz.
        return ActivityResponse.builder()
                .id(activity.getId())
                .userId(activity.getUser().getId())
                .username(activity.getUser().getUsername())
                .userAvatar(activity.getUser().getProfilePictureUrl())
                .type(activity.getType())
                .targetId(activity.getTargetId())
                .createdDate(activity.getCreatedDate())
                .content(activity.getContent())
                // Eğer film varsa bilgilerini ekle, yoksa boş bırak
                .targetTitle(movie != null ? movie.getOriginalTitle() : null)
                .targetImage(movie != null ? movie.getPosterUrl() : activity.getTargetImage())
                .build();
    }

//    private ActivityResponse mapToActivityResponse(Activity activity) {
//        ActivityResponse res = new ActivityResponse();
//        res.setId(activity.getId());
//        res.setUserId(activity.getUser().getId());
//        res.setUsername(activity.getUser().getUsername());
//        res.setType(activity.getType());
//        res.setTargetId(activity.getTargetId());
////        res.setContent(activity.getContent());
//        res.setCreatedDate(activity.getCreatedDate());
//        // ÖNEMLİ: Veritabanındaki gerçek içeriği ve resmi buraya aktar
//        res.setContent(activity.getContent());
//        res.setTargetImage(activity.getTargetImage());
//
//        // INSTAGRAM-STYLE ENRICHMENT (Veriyi Zenginleştirme)
//        switch (activity.getType()) {
//            case MOVIE_LIKE, MOVIE_RATE -> {
//                movieRepository.findById(activity.getTargetId()).ifPresent(m -> {
//                    res.setTargetTitle(m.getOriginalTitle());
//                    res.setTargetImage(m.getPosterUrl());
//                });
//            }
//            case FOLLOW_USER -> {
//                userRepository.findById(activity.getTargetId()).ifPresent(u -> {
//                    res.setTargetTitle(u.getUsername());
//                     res.setTargetImage(u.getProfilePictureUrl());
//                });
//            }
//            case COMMENT_CREATE, COMMENT_LIKE -> {
////                // Yorum bir filme ait olduğu için filmin adını da gösterebiliriz
//////                res.setTargetTitle("a comment");
//////                res.setTargetTitle(activity.getContent());
////
////                // Eğer Activity nesnesinde targetImage doluysa onu kullan,
////                // yoksa targetId (Film ID) üzerinden posteri çek:
////                if (activity.getTargetImage() == null) {
////                    movieRepository.findById(activity.getTargetId()).ifPresent(m -> {
////                        res.setTargetImage(m.getPosterUrl());
////                    });
//
//                // 1. Veritabanından filmi bulup adını targetTitle olarak set et
//                movieRepository.findById(activity.getTargetId()).ifPresent(m -> {
//                    res.setTargetTitle(m.getOriginalTitle()); // "{title}" artık film adı olacak
//
//                    // Eğer aktivite kaydedilirken poster gelmediyse buradan tamamla
//                    if (activity.getTargetImage() == null) {
//                        res.setTargetImage(m.getPosterUrl());
//                    }
//                });
//
//            }
//        }
//        return res;
//
//
//    }

}