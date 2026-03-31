package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>, JpaSpecificationExecutor<Movie> { // Burası UUID oldu
    boolean existsByOriginalTitleAndReleaseYear(String originalTitle, Integer releaseYear);

    @EntityGraph(attributePaths = {"genres", "translations"})
    Page<Movie> findAll(Specification<Movie> spec, Pageable pageable);

    // List yerine Page dönecek
    @EntityGraph(attributePaths = {"genres", "translations"})
    Page<Movie> findAll(Pageable pageable);

    boolean existsByTmdbId(Long tmdbId);

    // En yüksek puan alan ilk 10 filmi getir
    List<Movie> findTop10ByOrderByClubRatingDesc();

    // En çok oylanan (trend olan) ilk 10 filmi getir
    List<Movie> findTop10ByOrderByClubVoteCountDesc();

    Optional<Movie> findByTmdbId(Long tmdbId);

    // Kullanıcının etkileşime girmediklerini getir
    @Query("SELECT m FROM Movie m WHERE m.id NOT IN " +
            "(SELECT r.movie.id FROM Rating r WHERE r.user.id = :userId) " +
            "AND m.id NOT IN " +
            "(SELECT l.movie.id FROM MovieLike l WHERE l.user.id = :userId) " +
            "ORDER BY m.clubRating DESC")
    Page<Movie> findSuggestedMoviesForUser(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"genres", "translations"})
    List<Movie> findByOriginalTitleContainingIgnoreCase(String title);
}


//N+1 Problemi (Performans): Movie listesi dönerken (Örn: toResponseList), her film için genres listesine erişmeye çalışırsa Hibernate her film için ayrı bir SQL atabilir.
//Çözüm: Repository katmanında @EntityGraph veya JOIN FETCH kullanarak kategorileri tek seferde çekeceğiz.