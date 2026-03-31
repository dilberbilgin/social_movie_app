package com.socialmovieclub.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class RatingResponse {
    private UUID id;
    private Integer score;
    private String username;
    private UUID movieId;

    // profil sayfasında film kartını direkt bas
    private String movieTitle;
    private String posterUrl;
    private Integer releaseYear;

    private Double newClubRating;      // Güncel ortalama
    private Integer newClubVoteCount;  // Güncel oy sayısı
}