package com.socialmovieclub.service;

import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.NotificationResponse;
import com.socialmovieclub.entity.Notification;
import com.socialmovieclub.entity.User; // EKSİK OLAN BUYDU
import com.socialmovieclub.enums.NotificationType;
import com.socialmovieclub.mapper.NotificationMapper;
import com.socialmovieclub.repository.NotificationRepository;
import com.socialmovieclub.service.notification.strategy.NotificationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final List<NotificationStrategy> strategies;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final MessageHelper messageHelper;

    // Uygulama ayağa kalktığında bir kez dolacak
    private Map<NotificationType, NotificationStrategy> strategyMap;

    // Stratejileri hızlıca bulmak için Map yapısı
//    private Map<NotificationType, NotificationStrategy> getStrategyMap() {
//        return strategies.stream().collect(Collectors.toMap(NotificationStrategy::getType, s -> s));
//    }

    @PostConstruct
    public void init() {
        // Uygulama ayağa kalkarken stratejileri Map'e atar (Performans!)
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotificationStrategy::getType, s -> s));
    }

//    @Async
    public void createNotification(User recipient, User actor, NotificationType type,
                                   UUID targetId, UUID subTargetId, String targetTitle) {
        if (recipient.getId().equals(actor.getId())) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .targetId(targetId)
                .subTargetId(subTargetId)
                .content(targetTitle)
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // 1. WebSocket ile anlık bildirim
        sendRealTimeNotification(savedNotification);

        // 2. Email Bildirimi (Arka Planda)
        sendEmailNotification(savedNotification);
    }

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private void sendEmailNotification(Notification n) {
        User recipient = n.getRecipient();
        String lang = recipient.getPreferredLanguage();

        NotificationStrategy strategy = strategyMap.get(n.getType());
        if (strategy != null) {
            String subject = strategy.buildEmailSubject(n.getActor().getUsername(), lang);
            String body = strategy.buildMessage(n.getActor().getUsername(), n.getContent(), lang);

            // Link oluşturma (Frontend URL'ine yönlendirme)
            String actionUrl = frontendUrl + "/notifications";
            String buttonText = messageHelper.getMessage("mail.button.view", lang);

            emailService.sendHtmlMail(
                    recipient.getEmail(),
                    subject,
                    subject, // Title olarak da subject'i gönderiyoruz
                    body,
                    actionUrl,
                    buttonText
            );
        }
    }

    private void sendRealTimeNotification(Notification notification) {
        // 1. Entity'yi Response DTO'ya çeviriyoruz
        NotificationResponse response = notificationMapper.toResponse(notification);

        // 2. Mesajı strateji ile oluşturuyoruz
        NotificationStrategy strategy = strategyMap.get(notification.getType());
        if (strategy != null) {
            String msg = strategy.buildMessage(notification.getActor().getUsername(), notification.getContent(), notification.getRecipient().getPreferredLanguage());
            response.setMessage(msg);
        }

        // 3. WebSocket üzerinden sadece alıcıya (recipient) gönderiyoruz
        // Hedef kanal: /user/{username}/queue/notifications
        messagingTemplate.convertAndSendToUser(
                notification.getRecipient().getUsername(),
                "/queue/notifications",
                response
        );
    }

    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {

        return notificationRepository.findByRecipientOrderByCreatedDateDesc(user, pageable)
                .map(this::convertToResponse);
    }

    @Transactional
    public void markAsRead(UUID notificationId, User currentUser) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            // GÜVENLİK: Bildirim gerçekten bu kullanıcıya mı ait?
            if (n.getRecipient().getId().equals(currentUser.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    private NotificationResponse convertToResponse(Notification n) {
        NotificationResponse res = notificationMapper.toResponse(n);
        NotificationStrategy strategy = strategyMap.get(n.getType());
        if (strategy != null) {
            res.setMessage(strategy.buildMessage(n.getActor().getUsername(), n.getContent(), n.getRecipient().getPreferredLanguage()));
        }
        return res;
    }

    public void markAllAsRead(User currentUser) {
        notificationRepository.markAllAsReadForUser(currentUser);
    }
}