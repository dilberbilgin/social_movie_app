package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.LoginRequest;
import com.socialmovieclub.dto.response.JwtResponse;
import com.socialmovieclub.mapper.UserMapper;
import com.socialmovieclub.repository.UserRepository;
import com.socialmovieclub.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    public RestResponse<JwtResponse> login(LoginRequest request) {
        // 1. Kullanıcıyı doğrula (AuthenticationManager arkada UserDetailsService'i kullanır)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // 2. SecurityContext'e set et (Bu thread için kullanıcıyı "Giriş yapmış" sayar)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. JWT üret
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return RestResponse.success(new JwtResponse(jwt, userDetails.getUsername(), roles), "Login successful");
    }
}