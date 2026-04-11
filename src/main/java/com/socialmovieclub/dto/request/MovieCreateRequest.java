package com.socialmovieclub.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MovieCreateRequest {
    @NotBlank(message = "{movie.originalTitle.required}")
    private String originalTitle;

    @Min(value = 1888, message = "{movie.releaseYear.invalid}")
    private Integer releaseYear;

    private String director;
    private Double imdbRating;
    private String posterUrl;

    // Çoklu dil çevirileri
    @NotEmpty(message = "{movie.translations.required}")
    @Valid
    private List<TranslationRequest> translations;

    private List<UUID> genreIds;

    @NotBlank(message = "{movie.contentType.required}")
    private String contentType; // "MOVIE" veya "TV"
}