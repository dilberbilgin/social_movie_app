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
    private final CommentRepository commentRepository;
    private final MovieLikeRepository movieLikeRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.FEED_CACHE,
            key = "#currentUserId + '-' + #pageable.pageNumber", // Artık hata vermez
            unless = "#result.success == false")

    public RestResponse<CustomPageResponse<ActivityResponse>> getFollowedFeed(UUID currentUserId, Pageable pageable) {
        User currentUser = securityService.getCurrentUser();

        // 1. Takip edilenleri al
        List<UUID> followedUserIds = followRepository.findAllByFollowerId(currentUser.getId(), Pageable.unpaged())
                .getContent().stream().map(f -> f.getFollowing().getId()).collect(Collectors.toList());

        if (followedUserIds.isEmpty()) return RestResponse.success(new CustomPageResponse<>());

        // 2. Aktiviteleri çek
        Page<Activity> activities = activityRepository.findByUserIdInOrderByCreatedDateDesc(followedUserIds, pageable);

        // 3. Filmleri toplu çek
        Set<UUID> movieIds = activities.getContent().stream()
                .filter(a -> isMovieRelated(a.getType()))
                .map(Activity::getTargetId)
                .collect(Collectors.toSet());

        Map<UUID, Movie> movieMap = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, m -> m));

        // 4. MAP TO RESPONSE (Zenginleştirilmiş)
        Page<ActivityResponse> response = activities.map(activity -> {
            Movie movie = movieMap.get(activity.getTargetId());
            ActivityResponse res = mapToResponse(activity, movie);

            if (movie != null) {
                // 1. Toplam Beğeni Sayısı (MovieLikeRepository kullanarak)
                res.setLikeCount(movieLikeRepository.countByMovieIdAndIsLikedTrue(movie.getId()));

                // 2. Toplam Yorum Sayısı (CommentRepository kullanarak)
                res.setCommentCount(commentRepository.countByMovieIdAndDeletedFalse(movie.getId()));

                // 3. Giriş yapan kullanıcı bu filmi beğendi mi? (Kırmızı kalp için)
                securityService.getUserIfLoggedIn().ifPresent(user -> {
                    movieLikeRepository.findByUserIdAndMovieId(user.getId(), movie.getId())
                            .ifPresent(like -> res.setUserReaction(like.isLiked()));
                });
            }
            return res;
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
}