package com.socialmovieclub.entity;

import com.socialmovieclub.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt ile şifrelenmiş tutulacak

    @Column(unique = true, nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    private boolean active = true;

    // Rollerimizi bir Set olarak tutuyoruz (Hibernate performansı ve veri tekilliği için)
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private String bio;
    private String profilePictureUrl;
}