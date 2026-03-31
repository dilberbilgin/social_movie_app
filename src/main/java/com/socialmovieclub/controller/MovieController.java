package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.MovieCreateRequest;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.socialmovieclub.core.result.RestResponse.success;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    public RestResponse<MovieResponse> createMovie(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            @Valid @RequestBody MovieCreateRequest request) {

        return movieService.createMovie(request, lang);
    }

@GetMapping
public RestResponse<Page<MovieResponse>> getAllMovies(
        @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
        Pageable pageable) { // Spring bunu otomatik çözer
    return movieService.getAllMovies(lang, pageable);
}

    // En yüksek puanlılar
    @GetMapping("/top-rated")
    public RestResponse<List<MovieResponse>> getTopRated(@RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return movieService.getTopRatedMovies(lang);
    }

    // En çok oylananlar (Trendler)
    @GetMapping("/trending")
    public RestResponse<List<MovieResponse>> getTrending(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return movieService.getTrendingMovies(lang);
    }

//    @GetMapping("/{id}")
//    public RestResponse<MovieResponse> getMovieDetail(
//            @PathVariable UUID id,
//            @RequestParam(required = false) Long tmdbId, // TMDB ID'yi de parametre olarak alabilmeliyiz
//            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
//
//        return movieService.getMovieDetail(id, tmdbId, lang);
//    }

    @GetMapping("/{id}")
    public RestResponse<MovieResponse> getMovieDetail(
            @PathVariable String id, // String olmalı. kendi db'imizde veri kalmayinca aramada tmdb den geliyor. long ve uuid olunca string yapiyoruz burda.
            @RequestParam(required = false) Long tmdbId,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            Pageable pageable) {

        // İş mantığına göndermeden önce basit kontrol
        UUID movieUuid = null;
        if (id != null && !id.equals("0")) {
            try { movieUuid = UUID.fromString(id); } catch (Exception e) { /* null kalabilir */ }
        }
        return movieService.getMovieDetail(movieUuid, tmdbId, lang);
    }

    @GetMapping("/search")
    public RestResponse<Page<MovieResponse>> searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) UUID genreId,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            Pageable pageable
    ) {
        return movieService.searchMovies(title, genreId, lang, pageable);
    }

    @GetMapping("/discover")
    public RestResponse<Page<MovieResponse>> discover(
            @RequestParam(required = false) UUID genreId,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            Pageable pageable) {
        return movieService.getDiscoverMovies(genreId, lang, pageable);
    }

    @GetMapping("/suggestions")
    public RestResponse<Page<MovieResponse>> getSuggestions(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            Pageable pageable) {
        return movieService.getSuggestedMovies(lang, pageable);
    }
}