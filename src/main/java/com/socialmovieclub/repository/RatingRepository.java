package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    // Belirli bir kullanıcının belirli bir filme verdiği puanı bul
    Optional<Rating> findByUserIdAndMovieId(UUID userId, UUID movieId);

    // Filmin ortalamasını ve toplam oy sayısını tek seferde hesaplamak için
    @Query("SELECT AVG(r.score), COUNT(r) FROM Rating r WHERE r.movie.id = :movieId")
    Object[] getRatingStatsByMovieId(@Param("movieId") UUID movieId);
}