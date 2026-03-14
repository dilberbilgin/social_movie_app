package com.socialmovieclub.service.notification.strategy;

import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeNotificationStrategy implements NotificationStrategy {

    private final MessageHelper messageHelper;

    @Override
    public com.socialmovieclub.enums.NotificationType getType() {
        return NotificationType.COMMENT_LIKE;
    }

    @Override
    public String buildMessage(String actorName, String targetTitle) {
        return messageHelper.getMessage("notification.comment_like", actorName, targetTitle);
    }
}