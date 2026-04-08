package com.socialmovieclub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "weekly_winners")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WeeklyWinner extends BaseEntity {

    @NotNull(message = "Winner must be linked to a comment")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @NotNull(message = "Winner must be linked to a user")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Winner must be linked to a movie")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @PastOrPresent
    private LocalDate weekStartDate; // O haftanın başlangıç tarihi

    @NotNull
    private LocalDate weekEndDate;

    private Long finalLikeCount; // Kazandığı andaki beğeni sayısı (tarihsel veri için)
}