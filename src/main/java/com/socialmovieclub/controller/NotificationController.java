package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.NotificationResponse;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.service.NotificationService;
import com.socialmovieclub.service.SecurityService;
import com.socialmovieclub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityService securityService;

    @GetMapping
    public RestResponse<Page<NotificationResponse>> getMyNotifications(Pageable pageable) {
        User currentUser = securityService.getCurrentUser();
        return RestResponse.success(notificationService.getMyNotifications(currentUser, pageable));
    }

    //TODO : BURAYA BAKILCAK
    @PatchMapping("/{id}/read")
    public RestResponse<Void> markAsRead(@PathVariable UUID id) {
        // 1. Önce o anki kullanıcıyı alıyoruz
        User currentUser = securityService.getCurrentUser();

        // 2. Servise hem bildirimin ID'sini hem de kullanıcıyı gönderiyoruz
        notificationService.markAsRead(id, currentUser);

        return RestResponse.success(null);
    }

    @PatchMapping("/read-all")
    public RestResponse<Void> markAllAsRead() {
        User currentUser = securityService.getCurrentUser();
        notificationService.markAllAsRead(currentUser);
        return RestResponse.success(null);
    }
}