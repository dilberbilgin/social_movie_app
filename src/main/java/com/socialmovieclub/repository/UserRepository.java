package com.socialmovieclub.repository;

import com.socialmovieclub.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Veritabanında kullanıcı adına göre arama yapmak için
    Optional<User> findByUsername(String username);

    // Veritabanında e-postaya göre arama yapmak için
    Optional<User> findByEmail(String email);

    //SELECT count(id) > 0 FROM users WHERE username = 'user1';
    // Bu kullanıcı adı zaten var mı kontrolü için
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}