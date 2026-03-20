package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.socialmovieclub.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationResponse {
    private UUID id;
    private String actorUsername;
    private String actorAvatar;
    private String message;
    private NotificationType type;
    private UUID targetId;
    private UUID subTargetId;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdDate;
}