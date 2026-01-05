package com.socialmovieclub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movie extends BaseEntity {

    @Column(name = "original_title", nullable = false)
    private String originalTitle; // Örn: "Inception" (Asla değişmez)

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "director")
    private String director;

//    @Column(name = "imdb_rating")
//    private Double imdbRating; // api ucretli

    // IMDb linki oluşturmak için ID (Örn: tt0111161)
    @Column(name = "imdb_id")
    private String imdbId;

    // --- PROFESYONEL CLUB RATING ALANLARI ---
    /**
     * NEDEN BURADALAR?
     * Her film listelendiğinde (Örn: Anasayfada 20 film) DB'ye gidip binlerce satır arasından
     * ortalama hesaplatmak sistemi yorar. Bunun yerine, sonucu burada 'hazır' tutuyoruz.
     */
    @Column(name = "club_rating")
    private Double clubRating = 0.0; // Sitemizdeki kullanıcıların ortalaması

    @Column(name = "club_vote_count")
    private Integer clubVoteCount = 0; // Toplam oy sayısı

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId; // TMDB'den gelen sayısal ID'yi burada iz olarak tutacağız.

    @Column(name = "tmdb_rating")
    private Double tmdbRating;


    // İlişki: Bir filmin birden fazla dilde (TR, EN) karşılığı olabilir.
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<MovieTranslation> translations = new ArrayList<>();
    private Set<MovieTranslation> translations = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "movie_genres", //Veritabaninda olusacak tablonun adi
            joinColumns = @JoinColumn(name = "movie_id"), // Bu tablodaki film ID kolonu
            inverseJoinColumns = @JoinColumn(name = "genre_id") //Bu tablodaki kategori ID kolonu
    )
    private Set<Genre> genres = new HashSet<>();
    //Best practice Set kullanmaktir(List yerine). Bir filmin veya kategorinin ayni dilde iki tane cevirisi olamaz.
    // Set, veri tabani seviyesinde mukerrer kaydi onlemeye yardimci olur ve
    //Hibernate'in bazı performans sorunlarını (özellikle birden fazla liste olduğundaki "MultipleBagFetchException") engeller.

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();


    // Rating ilişkisi: Veritabanında Movie tablosu ile Rating tablosunu birbirine bağlar.
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings = new ArrayList<>();
}