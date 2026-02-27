package com.socialmovieclub.repository;

import com.socialmovieclub.entity.MovieLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MovieLikeRepository extends JpaRepository<MovieLike, UUID> {
    Optional<MovieLike> findByUserIdAndMovieId(UUID userId, UUID movieId);
    long countByMovieIdAndIsLikedTrue(UUID movieId);
    long countByMovieIdAndIsLikedFalse(UUID movieId);
}

