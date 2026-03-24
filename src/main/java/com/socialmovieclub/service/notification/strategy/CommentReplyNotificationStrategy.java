package com.socialmovieclub.service.notification.strategy;

import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReplyNotificationStrategy implements NotificationStrategy {
    private final MessageHelper messageHelper;

    @Override
    public NotificationType getType() {
        return NotificationType.COMMENT_REPLY; // Burayı güncelledik
    }

    @Override
    public String buildMessage(String actorName, String targetTitle, String lang) {
        // notification.comment_reply={0} senin yorumuna yanıt verdi: "{1}"
        return messageHelper.getMessage("notification.comment_reply", lang, actorName, targetTitle);
    }

    @Override
    public String buildEmailSubject(String actorName, String lang) {
        return messageHelper.getMessage("mail.subject.comment_reply", lang, actorName);
    }
}