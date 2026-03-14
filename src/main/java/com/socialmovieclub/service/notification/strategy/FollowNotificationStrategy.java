package com.socialmovieclub.service.notification.strategy;

import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowNotificationStrategy implements NotificationStrategy {
    private final MessageHelper messageHelper;

    @Override
    public NotificationType getType() { return NotificationType.FOLLOW; }

    @Override
    public String buildMessage(String actorName, String targetTitle) {
        return messageHelper.getMessage("notification.follow", actorName);
    }
}