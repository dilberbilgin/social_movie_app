package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieResponse {
    private UUID id;            // Bizim sistemdeki ID (Aramada null gelir)
    private Long tmdbId;        // TMDB'deki ID (Arama ve Import için kritik)
    private String originalTitle;
    private Integer releaseYear;
    private String director;
//    private Double imdbRating;
// --- YENİ EKLENEN ALANLAR ---
    private Double tmdbRating;      // TMDB'den gelen genel puan
    private String imdbId;          // IMDb linki için ID
    private String imdbUrl;
    private Double clubRating;      // Senin sitemizdeki ortalama
    private Integer clubVoteCount;  // Kaç kişi oy verdi?
    private Integer userScore; // Kullanıcının bu filme verdiği özel puan (eğer varsa)
    // ----------------------------
    private String posterUrl;

    // Kullanıcının Accept-Language header'ına göre dolacak alanlar
    private String title;
    private String description;

    private List<GenreResponse> genres;

    private long likeCount;
    private long dislikeCount;
    private Boolean userReaction; // null: tepki yok, true: liked, false: disliked
}


