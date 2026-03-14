package com.socialmovieclub.dto.response;

import com.socialmovieclub.enums.ActivityType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ActivityResponse {
    private UUID id;
    private UUID userId;
    private String username;
    private String userAvatar;
    private ActivityType type;

    // Instagram Tarzı Zengin Veri
    private UUID targetId;      // FilmId veya CommentId
    private String targetTitle;  // Film Adı (OriginalTitle)
    private String targetImage;  // Film Posteri veya Profil Resmi
    private String content;      // Yorumun içeriği veya "X filmini beğendi" mesajı

    private LocalDateTime createdDate;
}