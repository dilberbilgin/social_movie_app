package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Notification;
import com.socialmovieclub.entity.User; // EKSİK OLAN BUYDU
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Param eklemek iyidir
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientOrderByCreatedDateDesc(User recipient, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient = :user AND n.isRead = false")
    long countUnreadNotifications(@Param("user") User user);
//todo :  countUnreadNotifications eklenebilir!

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :user AND n.isRead = false")
    void markAllAsReadForUser(@Param("user") User user);
}