package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    // Belirli bir kullanıcının belirli bir filme verdiği puanı bul
    Optional<Rating> findByUserIdAndMovieId(UUID userId, UUID movieId);

    // Filmin ortalamasını ve toplam oy sayısını tek seferde hesaplamak için
    @Query("SELECT AVG(r.score), COUNT(r) FROM Rating r WHERE r.movie.id = :movieId")
    Object[] getRatingStatsByMovieId(@Param("movieId") UUID movieId);


    // Kullanıcının verdiği tüm puanları, en son verilenden başlayarak getir
    @EntityGraph(attributePaths = {"movie"}) // Performans için: Filmi de tek seferde getir
//    List<Rating> findAllByUserIdOrderByCreatedDateDesc(UUID userId);
    // List yerine Page dönmek, bize "daha veri var mı?" bilgisini de verir.
    Page<Rating> findAllByUserIdOrderByCreatedDateDesc(UUID userId, Pageable pageable);

    void deleteByUserIdAndMovieId(UUID id, UUID movieId);

    long countByUserId(UUID userId);
    List<Rating> findTop10ByUserIdOrderByCreatedDateDesc(UUID userId);

    // Feed sorgusu (Pull Modeli): Takip ettiğim kişilerin puanlamalarını getir
    @Query("SELECT r FROM Rating r WHERE r.user.id IN " +
            "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :currentUserId) " +
            "ORDER BY r.createdDate DESC")
    Page<Rating> findFeed(@Param("currentUserId") UUID currentUserId, Pageable pageable);
}