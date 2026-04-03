package com.socialmovieclub.repository;

import com.socialmovieclub.entity.MovieCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieCollectionRepository extends JpaRepository<MovieCollection, UUID> {
    // Kullanıcının kendi oluşturduğu listeler
    List<MovieCollection> findAllByUserIdOrderByCreatedDateDesc(UUID userId);

    // Başka birinin profilinde sadece "public" olan listeleri görmek için
    List<MovieCollection> findAllByUserIdAndIsPublicTrue(UUID userId);


    @Query("SELECT c FROM MovieCollection c LEFT JOIN FETCH c.movies WHERE c.id = :id")
    Optional<MovieCollection> findByIdWithMovies(@Param("id") UUID id);
}