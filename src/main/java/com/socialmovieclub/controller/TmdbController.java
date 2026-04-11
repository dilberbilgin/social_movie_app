package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.socialmovieclub.core.result.RestResponse.success;

@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbController {

    private final TmdbService tmdbService;

    @PostMapping("/sync-genres")
    public RestResponse<Void> syncGenres(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        // Kontrolcü sadece dili alır ve servise paslar
        return tmdbService.syncGenres(lang);
    }

    @PostMapping("/import/{tmdbId}")
    public RestResponse<MovieResponse> importMovie(
            @PathVariable Long tmdbId,
            @RequestParam(defaultValue = "MOVIE") String contentType,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return tmdbService.importMovie(tmdbId, contentType, lang);
    }

    @GetMapping("/search")
    public RestResponse<List<MovieResponse>> search(
            @RequestParam String query,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return tmdbService.searchMovies(query, lang);
    }
}