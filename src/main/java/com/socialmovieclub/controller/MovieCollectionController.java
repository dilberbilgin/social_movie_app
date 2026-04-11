package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.MovieCollectionRequest;
import com.socialmovieclub.dto.response.MovieCollectionResponse;
import com.socialmovieclub.service.MovieCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class MovieCollectionController {

    private final MovieCollectionService movieCollectionService;

    @PostMapping
    public RestResponse<MovieCollectionResponse> create(@RequestBody MovieCollectionRequest request) {
        return movieCollectionService.createCollection(request);
    }

    @GetMapping("/my-collections")
    public RestResponse<List<MovieCollectionResponse>> getMyCollections() {
        return movieCollectionService.getMyCollections();
    }

    @PostMapping("/{collectionId}/movies/{tmdbId}")
    public RestResponse<Void> addMovie(
            @PathVariable UUID collectionId,
            @PathVariable Long tmdbId,
            @RequestParam(required = false, defaultValue = "MOVIE") String contentType,
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        // TMDB ID kullanarak ekleme (Eğer yoksa otomatik import eder)
        return movieCollectionService.addMovieToCollection(collectionId, tmdbId, contentType, lang);
    }

    @GetMapping("/{id}")
    public RestResponse<MovieCollectionResponse> getCollectionDetail(
            @PathVariable UUID id,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return movieCollectionService.getCollectionDetail(id, lang);
    }

    @GetMapping("/user/{username}")
    public RestResponse<List<MovieCollectionResponse>> getUserCollections(
            @PathVariable String username,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return movieCollectionService.getUserCollectionsByUsername(username, lang);
    }
}