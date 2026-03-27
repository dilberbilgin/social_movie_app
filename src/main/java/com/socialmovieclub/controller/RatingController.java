package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public RestResponse<RatingResponse> addOrUpdateRating(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
            @Valid @RequestBody RatingRequest request) {
        return ratingService.rateMovie(request, lang);
    }

    // Giriş yapmış kullanıcının kendi puanlarını görmesi
    @GetMapping("/me")
    public RestResponse<Page<RatingResponse>> getMyRatings(@RequestHeader(name = "Accept-Language", defaultValue = "en") String lang,
                                                           @org.springframework.data.web.PageableDefault(size = 20, sort = "createdDate") Pageable pageable) {
        return ratingService.getUserRatings(lang, pageable);
    }

    @DeleteMapping("/{movieId}")
    public RestResponse<Void> deleteRating(@PathVariable UUID movieId) {
        return ratingService.deleteRating(movieId);
    }
}