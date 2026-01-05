package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Genre;
import com.socialmovieclub.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenreRepository extends JpaRepository<Genre, UUID> {

    // Spring Data JPA bu isimden otomatik olarak:
    // "SELECT count(id) > 0 FROM genres WHERE name = ?" sorgusunu üretir.
    boolean existsByName(String name);

    Optional<Genre> findByTmdbId(Long tmdbId);

    boolean existsByTmdbId(Long tmdbId);

    Optional<Genre> findByName(String name);
}
