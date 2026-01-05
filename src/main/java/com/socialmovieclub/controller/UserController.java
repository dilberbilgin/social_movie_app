package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.LoginRequest;
import com.socialmovieclub.dto.response.JwtResponse;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import com.socialmovieclub.service.AuthService;
import com.socialmovieclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // İleride buraya /me (profilim), /update vb. gelecek.
}