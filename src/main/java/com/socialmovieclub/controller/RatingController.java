package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public RestResponse<RatingResponse> addOrUpdateRating(@Valid @RequestBody RatingRequest request) {
        return ratingService.rateMovie(request);
    }
}