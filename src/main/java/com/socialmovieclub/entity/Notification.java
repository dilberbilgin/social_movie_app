package com.socialmovieclub.entity;

import com.socialmovieclub.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient; // Bildirimi alan (Hedef)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor; // Eylemi yapan (Tetikleyici)

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private UUID targetId; // Film ID, Yorum ID vb.

    private UUID subTargetId; // Örn: Yorum ID

    private String content; // Opsiyonel: Yorum önizlemesi gibi

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;
}