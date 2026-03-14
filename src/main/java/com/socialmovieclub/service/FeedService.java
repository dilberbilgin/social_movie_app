package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.ActivityResponse;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.entity.Activity;
import com.socialmovieclub.entity.Follow;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final ActivityRepository activityRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final CommentRepository commentRepository;

    public RestResponse<Page<ActivityResponse>> getFollowedFeed(Pageable pageable) {
        User currentUser = getCurrentUser();

        // 1. Takip edilen kullanıcıların ID'lerini bul
        // Not: Burada Pageable olmayan, tüm listeyi dönen bir repo metodu daha efektif olabilir
        List<UUID> followedUserIds = followRepository.findAllByFollowerId(currentUser.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(Collectors.toList());

        // Eğer kimseyi takip etmiyorsa boş sayfa dön
        if (followedUserIds.isEmpty()) {
            return RestResponse.success(Page.empty());
        }

        // 2. Bu ID'lerin aktivitelerini çek
        Page<Activity> activities = activityRepository.findByUserIdInOrderByCreatedDateDesc(followedUserIds, pageable);

        // 3. Map to Response (İleride bunu MapStruct ile yapabiliriz)
        Page<ActivityResponse> response = activities.map(this::mapToActivityResponse);

        return RestResponse.success(response);
    }

    private ActivityResponse mapToActivityResponse(Activity activity) {
        ActivityResponse res = new ActivityResponse();
        res.setId(activity.getId());
        res.setUserId(activity.getUser().getId());
        res.setUsername(activity.getUser().getUsername());
        res.setType(activity.getType());
        res.setTargetId(activity.getTargetId());
//        res.setContent(activity.getContent());
        res.setCreatedDate(activity.getCreatedDate());
        // ÖNEMLİ: Veritabanındaki gerçek içeriği ve resmi buraya aktar
        res.setContent(activity.getContent());
        res.setTargetImage(activity.getTargetImage());

        // INSTAGRAM-STYLE ENRICHMENT (Veriyi Zenginleştirme)
        switch (activity.getType()) {
            case MOVIE_LIKE, MOVIE_RATE -> {
                movieRepository.findById(activity.getTargetId()).ifPresent(m -> {
                    res.setTargetTitle(m.getOriginalTitle());
                    res.setTargetImage(m.getPosterUrl());
                });
            }
            case FOLLOW_USER -> {
                userRepository.findById(activity.getTargetId()).ifPresent(u -> {
                    res.setTargetTitle(u.getUsername());
                     res.setTargetImage(u.getProfilePictureUrl());
                });
            }
            case COMMENT_CREATE, COMMENT_LIKE -> {
//                // Yorum bir filme ait olduğu için filmin adını da gösterebiliriz
////                res.setTargetTitle("a comment");
////                res.setTargetTitle(activity.getContent());
//
//                // Eğer Activity nesnesinde targetImage doluysa onu kullan,
//                // yoksa targetId (Film ID) üzerinden posteri çek:
//                if (activity.getTargetImage() == null) {
//                    movieRepository.findById(activity.getTargetId()).ifPresent(m -> {
//                        res.setTargetImage(m.getPosterUrl());
//                    });

                // 1. Veritabanından filmi bulup adını targetTitle olarak set et
                movieRepository.findById(activity.getTargetId()).ifPresent(m -> {
                    res.setTargetTitle(m.getOriginalTitle()); // "{title}" artık film adı olacak

                    // Eğer aktivite kaydedilirken poster gelmediyse buradan tamamla
                    if (activity.getTargetImage() == null) {
                        res.setTargetImage(m.getPosterUrl());
                    }
                });

            }
        }
        return res;


    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
}