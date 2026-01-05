package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.Role;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.UserMapper;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static com.socialmovieclub.core.result.RestResponse.success;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MessageHelper messageHelper;


    @Transactional
    public RestResponse<UserResponse> register(UserRegistrationRequest registrationRequest) {

        // 1. Önce kontrol et (Duplicate check)
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            throw new BusinessException(messageHelper.getMessage("user.username.already.exists"));
        }
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BusinessException(messageHelper.getMessage("user.email.already.exists"));
        }
        // 1. Request'i Entity'ye dönüştür
        User user = userMapper.toEntity(registrationRequest);
        // 2. Şifreyi güvenli hale getir
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Set.of(Role.USER));
        // 3. Veritabanına kaydet
        User savedUser = userRepository.save(user);
        // 4. Kaydedilen veriyi Response DTO'suna çevirip dön
        UserResponse responseData = userMapper.toResponse(savedUser);
        String successMsg = messageHelper.getMessage("user.register.success");

        return success(responseData, successMsg);
    }

    public RestResponse<UserResponse> getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));
        return  RestResponse.success(userMapper.toResponse(user), "User fetched");
    }
}