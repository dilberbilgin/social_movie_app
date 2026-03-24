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
    public String buildMessage(String actorName, String targetTitle, String lang) {
        // notification.comment_like={0} senin "{1}" yorumunu beğendi.
        return messageHelper.getMessage("notification.comment_like", lang, actorName, targetTitle);
    }

    @Override
    public String buildEmailSubject(String actorName, String lang) {
        // mail.subject.comment_like=Yorumun beğenildi!
        return messageHelper.getMessage("mail.subject.comment_like", lang);
    }
}