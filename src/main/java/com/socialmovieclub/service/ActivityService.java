package com.socialmovieclub.service;

import com.socialmovieclub.entity.Activity;
import com.socialmovieclub.enums.ActivityType;
import com.socialmovieclub.repository.ActivityRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    /**
     * Aktivite kaydı oluşturur.
     * @Async kullanarak ana işlemin (beğeni/yorum) yavaşlamasını engelliyoruz.
     * (Not: @Async için ana uygulama sınıfında @EnableAsync eklemelisin)
     */
    @Async
    @Transactional
    public void createActivity(UUID userId, ActivityType type, UUID targetId, String content, String targetImage, String targetTitle) {
        userRepository.findById(userId).ifPresent(user -> {
            Activity activity = Activity.builder()
                    .user(user)
                    .type(type)
                    .targetId(targetId)
                    .content(content)
                    .targetImage(targetImage)
                    .targetTitle(targetTitle)
                    .build();
            activityRepository.save(activity);
        });
    }
}