package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true) // Fazladan veri gelirse patlamasın
public class MovieResponse implements Serializable {

    private static final long serialVersionUID = 1L; // Versiyon kontrolü için önerilir

    private UUID id;            // Bizim sistemdeki ID (Aramada null gelir)
    private Long tmdbId;        // TMDB'deki ID (Arama ve Import için
    private String originalTitle;
    private Integer releaseYear;
    private String director;
//    private Double imdbRating;
// --- YENİ EKLENEN ALANLAR ---
    private Double tmdbRating;      // TMDB'den gelen genel puan
    private String imdbId;          // IMDb linki için ID
    private String imdbUrl;
    private Double clubRating;      // SocialMovieClub ortalama
    private Integer clubVoteCount;  // Kaç kişi oy verdi?
    private Integer userScore; // Kullanıcının bu filme verdiği özel puan (eğer varsa)

    private String posterUrl;

    // Kullanıcının Accept-Language header'ına göre dolacak alanlar
    private String title;
    private String description;

    private List<GenreResponse> genres;

    private long likeCount;
    private long dislikeCount;
    private Boolean userReaction; // null: tepki yok, true: liked, false: disliked
    private long commentCount;

    private String contentType; // Frontend'e "MOVIE" mi "TV" mi olduğunu söyleyecek.

    private MovieWatchProvidersResponse watchProviders;
}


