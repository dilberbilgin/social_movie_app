package com.socialmovieclub.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TmdbSearchResponse {

    private int page;

    private List<TmdbMovieDto> results; // Arama sonuçları bu listenin içine dolacak

    @JsonProperty("total_results")
    private int totalResults;

    @JsonProperty("total_pages")
    private int totalPages;
}