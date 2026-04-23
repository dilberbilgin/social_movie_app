package com.socialmovieclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyWinnerResponse {
    private UUID id;
    private UUID commentId;
    private String commentContent;
    private String username;
    private String profilePictureUrl;

    // YÖNLENDİRME İÇİN KRİTİK EKLER
    private UUID movieId;   // frontend'deki review.movieId karşılığı
    private Long tmdbId; // frontend'deki review.tmdbId karşılığı


    private String movieTitle;
    private String moviePosterUrl;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Long finalLikeCount;

    private Double clubRating;
    private Integer clubVoteCount;

    private Long movieLikeCount;
    private Long movieDislikeCount;
    private Boolean userReaction; // Giriş yapan kullanıcının bu filme tepkisi

}