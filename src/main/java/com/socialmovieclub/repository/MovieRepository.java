package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
//public interface MovieRepository extends JpaRepository<Movie, Long> {
//
//    // Aynı isim ve yılda film var mı?
//    boolean existsByOriginalTitleAndReleaseYear(String originalTitle, Integer releaseYear);
//}

//JpaSpecificationExecutor ekleyerek dinamik sorgu yeteneği kazandırıyoruz.
public interface MovieRepository extends JpaRepository<Movie, UUID>, JpaSpecificationExecutor<Movie> { // Burası UUID oldu
    boolean existsByOriginalTitleAndReleaseYear(String originalTitle, Integer releaseYear);

//    @EntityGraph(attributePaths = {"genres", "translations"})
//    List<Movie> findAll(); // Tüm filmleri kategorileri ve çevirileriyle TEK sorguda getirir

    // Sayfalamalı ve EntityGraph'lı ana sorgu
    @EntityGraph(attributePaths = {"genres", "translations"})
    Page<Movie> findAll(Specification<Movie> spec, Pageable pageable);

    // List yerine Page dönecek şekilde güncelliyoruz
    @EntityGraph(attributePaths = {"genres", "translations"})
    Page<Movie> findAll(Pageable pageable);


    boolean existsByTmdbId(Long tmdbId);

    // En yüksek puan alan ilk 10 filmi getir
    List<Movie> findTop10ByOrderByClubRatingDesc();

    // En çok oylanan (trend olan) ilk 10 filmi getir
    List<Movie> findTop10ByOrderByClubVoteCountDesc();
}


//N+1 Problemi (Performans): Movie listesi dönerken (Örn: toResponseList), her film için genres listesine erişmeye çalışırsan Hibernate her film için ayrı bir SQL atabilir.
//Çözüm: Repository katmanında @EntityGraph veya JOIN FETCH kullanarak kategorileri tek seferde çekeceğiz.