package com.socialmovieclub.service;

import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final MessageHelper messageHelper;

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Eğer anonim kullanıcıysa veya context boşsa direkt hata fırlatabiliriz
        if (username == null || username.equals("anonymousUser")) {
            throw new BusinessException(messageHelper.getMessage("auth.unauthorized"));
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));
    }

    // Seçenekli kullanıcı: Giriş yapmışsa User döner, yapmamışsa boş Optional döner (Örn: yorum listelerken)
    public Optional<User> getUserIfLoggedIn() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.equals("anonymousUser")) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}