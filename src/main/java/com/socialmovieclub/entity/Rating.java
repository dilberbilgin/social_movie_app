package com.socialmovieclub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ratings", uniqueConstraints = {
        // Aynı kullanıcı aynı filme birden fazla kez puan veremez (Database Level)
        @UniqueConstraint(columnNames = {"user_id", "movie_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Rating extends BaseEntity {

    @Column(nullable = false)
    private Integer score; // 1 ile 10 arası

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
}