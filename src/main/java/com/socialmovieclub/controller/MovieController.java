package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.MovieCreateRequest;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public RestResponse<List<MovieResponse>> getAllMovies(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {

//        List<MovieResponse> data = movieService.getAllMovies(lang);
//        return success(data);

        // Service zaten RestResponse<List<MovieResponse>> dönüyor.
        return movieService.getAllMovies(lang);
    }
}