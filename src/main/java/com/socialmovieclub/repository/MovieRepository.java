package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
//public interface MovieRepository extends JpaRepository<Movie, Long> {
//
//    // Aynı isim ve yılda film var mı?
//    boolean existsByOriginalTitleAndReleaseYear(String originalTitle, Integer releaseYear);
//}

public interface MovieRepository extends JpaRepository<Movie, UUID> { // Burası UUID oldu
    boolean existsByOriginalTitleAndReleaseYear(String originalTitle, Integer releaseYear);

    @EntityGraph(attributePaths = {"genres", "translations"})
    List<Movie> findAll(); // Tüm filmleri kategorileri ve çevirileriyle TEK sorguda getirir

    boolean existsByTmdbId(Long tmdbId);
}


//N+1 Problemi (Performans): Movie listesi dönerken (Örn: toResponseList), her film için genres listesine erişmeye çalışırsan Hibernate her film için ayrı bir SQL atabilir.
//Çözüm: Repository katmanında @EntityGraph veya JOIN FETCH kullanarak kategorileri tek seferde çekeceğiz.