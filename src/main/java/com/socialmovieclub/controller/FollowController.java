package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.FollowRequest;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.socialmovieclub.core.result.RestResponse.success;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/follow")
    public RestResponse<Void> follow(@RequestBody FollowRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        followService.followUser(request.getFollowingId(), username);
        return success(null, "Followed successfully");
    }

    @PostMapping("/unfollow")
    public RestResponse<Void> unfollow(@RequestBody FollowRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Unfollow isteği geldi: " + username + " -> Target ID: " + request.getFollowingId());
        followService.unfollowUser(request.getFollowingId());
        return success(null, "Unfollowed successfully");
    }

    @GetMapping("/{userId}/followers")
    public RestResponse<Page<UserResponse>> getFollowers(
            @PathVariable UUID userId,
            @org.springframework.data.web.PageableDefault(size = 20) Pageable pageable) {
        return success(followService.getFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/following")
    public RestResponse<Page<UserResponse>> getFollowing(
            @PathVariable UUID userId,
            @org.springframework.data.web.PageableDefault(size = 20) Pageable pageable) {
        return success(followService.getFollowing(userId, pageable));
    }


}
