package com.socialmovieclub.service.notification.strategy;

import com.socialmovieclub.enums.NotificationType;

public interface NotificationStrategy {
    NotificationType getType(); // Stratejinin hangi tipte olduğunu belirler
    String buildMessage(String actorName, String targetTitle, String lang);

    // Email başlığı için metot
    String buildEmailSubject(String actorName, String lang);
}
