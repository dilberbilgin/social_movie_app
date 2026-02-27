package com.socialmovieclub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "movie_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "movie_id"}) // Bir kullanıcı bir filme sadece 1 tepki verebilir
})
@Getter
@Setter
public class MovieLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private boolean isLiked; // true = Like, false = Dislike
}

