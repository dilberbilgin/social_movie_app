package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.ActivityResponse;
import com.socialmovieclub.dto.response.CustomPageResponse;
import com.socialmovieclub.service.FeedService;
import com.socialmovieclub.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final SecurityService securityService;

    /**
     * Kullanıcının takip ettiği kişilerin aktivitelerini döner.
     * Sayfalama (Pageable) desteği ile performanslı çalışır.
     */
    @GetMapping
    public RestResponse<CustomPageResponse<ActivityResponse>> getMyFeed(
            @PageableDefault(size = 10, sort = "createdDate") Pageable pageable) {
        UUID currentUserId = securityService.getCurrentUserId();
        return feedService.getFollowedFeed(currentUserId, pageable);
    }
}