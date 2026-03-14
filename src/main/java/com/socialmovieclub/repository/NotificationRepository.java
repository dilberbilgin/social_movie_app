package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Notification;
import com.socialmovieclub.entity.User; // EKSİK OLAN BUYDU
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Param eklemek iyidir

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientOrderByCreatedDateDesc(User recipient, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient = :user AND n.isRead = false")
    long countUnreadNotifications(@Param("user") User user);
}