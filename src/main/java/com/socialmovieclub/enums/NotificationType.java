package com.socialmovieclub.enums;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;


public enum NotificationType {
    COMMENT_LIKE,
    COMMENT_CREATE,  // Filme yapılan ana yorum
    COMMENT_REPLY,   // Yoruma yapılan yanıt
    FOLLOW
}
