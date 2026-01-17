package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public RestResponse<RatingResponse> addOrUpdateRating(@Valid @RequestBody RatingRequest request) {
        return ratingService.rateMovie(request);
    }

    // Giriş yapmış kullanıcının kendi puanlarını görmesi
    @GetMapping("/me")
    public RestResponse<List<RatingResponse>> getMyRatings(@RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return ratingService.getUserRatings(lang);
    }

    @DeleteMapping("/{movieId}")
    public RestResponse<Void> deleteRating(@PathVariable UUID movieId) {
        return ratingService.deleteRating(movieId);
    }
}