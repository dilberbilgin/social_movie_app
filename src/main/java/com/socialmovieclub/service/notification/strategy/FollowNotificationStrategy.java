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
    public String buildMessage(String actorName, String targetTitle, String lang) {
        return messageHelper.getMessage("notification.follow", lang, actorName);
    }

    @Override
    public String buildEmailSubject(String actorName, String lang) {
        return messageHelper.getMessage("mail.subject.follow", lang, actorName);
    }
}