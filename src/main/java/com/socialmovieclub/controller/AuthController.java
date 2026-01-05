package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.LoginRequest;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import com.socialmovieclub.dto.response.JwtResponse;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.service.AuthService;
import com.socialmovieclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService; // Login için
    private final UserService userService; // Kayıt için (Eskisinden çekiyoruz)

    @PostMapping("/signup")
    public RestResponse<UserResponse> signup(@Valid @RequestBody UserRegistrationRequest request) {
        return userService.register(request); // İşi UserService'e pasladık
    }

    @PostMapping("/login")
    public ResponseEntity<RestResponse<JwtResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request)); // İşi AuthService'e pasladık
    }
}