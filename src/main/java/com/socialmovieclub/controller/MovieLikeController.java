package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.service.MovieLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/movies/{movieId}")
@RequiredArgsConstructor
public class MovieLikeController {
    private final MovieLikeService movieLikeService;

    @PostMapping("/like")
    public RestResponse<UUID> likeMovie(
            @PathVariable UUID movieId,
            @RequestParam(required = false) Long tmdbId,
            @RequestParam(required = false, defaultValue = "MOVIE") String contentType,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return movieLikeService.handleMovieReaction(movieId, tmdbId, true, contentType,  lang);
    }

    @PostMapping("/dislike")
    public RestResponse<UUID> dislikeMovie(
            @PathVariable UUID movieId,
            @RequestParam(required = false) Long tmdbId,
            @RequestParam(required = false, defaultValue = "MOVIE") String contentType,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return movieLikeService.handleMovieReaction(movieId, tmdbId, false, contentType, lang);
    }
}