package com.socialmovieclub.entity;

import com.socialmovieclub.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Aktiviteyi yapan kişi

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(nullable = false)
    private UUID targetId; // FilmId, CommentId veya FollowedUserId

    private String content; // Opsiyonel: Yorumun ilk 50 karakteri veya puan değeri gibi
    private String targetImage; // Yeni: Poster veya profil resmi URL'i için
}