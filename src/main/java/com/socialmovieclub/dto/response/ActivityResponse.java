package com.socialmovieclub.dto.response;

import com.socialmovieclub.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    private long likeCount;      // Aktivitenin beğeni sayısı
    private long commentCount;   // Aktiviteye yapılan yorum sayısı
    private Boolean userReaction; // Giriş yapan kullanıcı beğendi mi?
}