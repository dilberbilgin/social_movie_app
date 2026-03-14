package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.ActivityResponse;
import com.socialmovieclub.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * Kullanıcının takip ettiği kişilerin aktivitelerini döner.
     * Sayfalama (Pageable) desteği ile performanslı çalışır.
     */
    @GetMapping
    public RestResponse<Page<ActivityResponse>> getMyFeed(
            @PageableDefault(size = 10, sort = "createdDate") Pageable pageable) {
        return feedService.getFollowedFeed(pageable);
    }
}