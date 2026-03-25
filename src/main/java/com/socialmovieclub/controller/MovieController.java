package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.MovieCreateRequest;
import com.socialmovieclub.dto.response.MovieResponse;
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

//    @GetMapping
//    public ResponseEntity<RestResponse<List<MovieResponse>>> getAllMovies(
//            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
//
//        List<MovieResponse> response = movieService.getAllMovies(lang);
//        return ResponseEntity.ok(RestResponse.success(response));
//    }

//    @GetMapping
//    public RestResponse<List<MovieResponse>> getAllMovies(
//            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
//
////        List<MovieResponse> data = movieService.getAllMovies(lang);
////        return success(data);
//
//        // Service zaten RestResponse<List<MovieResponse>> dönüyor.
//        return movieService.getAllMovies(lang);
//    }

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

    @GetMapping("/{id}")
    public RestResponse<MovieResponse> getMovieDetail(
            @PathVariable UUID id, // Eğer ID tipin UUID ise
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {

        return movieService.getMovieById(id, lang);
    }

//    @GetMapping("/search")
//    public RestResponse<List<MovieResponse>> searchMovies(
//            @RequestParam(required = false) String title,
//            @RequestParam(required = false) UUID genreId,
//            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang
//    ) {
//        return movieService.searchMovies(title, genreId, lang);
//    }

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
}