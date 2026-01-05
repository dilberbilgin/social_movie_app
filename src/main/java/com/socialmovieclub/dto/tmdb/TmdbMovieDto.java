package com.socialmovieclub.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TmdbMovieDto {
    private Long id; // TMDB'nin kendi sayısal ID'si

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("imdb_id") // TMDB detay sorgusunda bu alan gelir
    private String imdbId;

    @JsonProperty("overview")
    private String overview; // Film açıklaması (Biz bunu Translation'a koyacağız)

    private String title; // Film başlığı (Biz bunu Translation'a koyacağız)

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("genre_ids")
    private List<Long> genreIds; // TMDB'nin kategori ID listesi

    private List<TmdbGenreDto> genres; // Bazen genre_ids yerine bu dolu gelir
}