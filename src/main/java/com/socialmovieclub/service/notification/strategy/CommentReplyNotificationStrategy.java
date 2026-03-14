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
    public String buildMessage(String actorName, String targetTitle) {
        // notification.comment_reply={0} senin yorumuna yanıt verdi: "{1}"
        return messageHelper.getMessage("notification.comment_reply", actorName, targetTitle);
    }
}