package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.LoginRequest;
import com.socialmovieclub.dto.request.UserProfileUpdateRequest;
import com.socialmovieclub.dto.response.JwtResponse;
import com.socialmovieclub.dto.response.ProfileResponse;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import com.socialmovieclub.service.AuthService;
import com.socialmovieclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.socialmovieclub.core.result.RestResponse.success;

@RestController
@RequestMapping("/api/users") // Sadece giriş yapmış kullanıcılar (SecurityConfig ile kilitlenecek)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/{id}")
    public RestResponse<UserResponse> getUserById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    // Profil Sayfası: Puanlamalar için sayfalama desteği eklendi
    @GetMapping("/profile/{username}")
    public RestResponse<ProfileResponse> getUserProfile(
            @PathVariable String username,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            @PageableDefault(size = 10, sort = "createdDate") Pageable pageable) {
        return userService.getUserProfile(username, lang, pageable);
    }

    @PutMapping("/profile/{username}")
    public RestResponse<UserResponse> updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return userService.updateProfile(username, request);
    }

    @GetMapping("/search")
    public RestResponse<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            @PageableDefault(size = 10) Pageable pageable) {
        return userService.searchUsers(query, pageable);
    }

    @GetMapping("/suggestions")
    public RestResponse<List<UserResponse>> getSuggestedUsers(
            @RequestParam(defaultValue = "5") int limit) {
        return userService.getSuggestedUsers(limit);
    }
}