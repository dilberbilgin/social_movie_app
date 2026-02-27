package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.service.MovieLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/movies/{movieId}")
@RequiredArgsConstructor
public class MovieLikeController {
    private final MovieLikeService movieLikeService;

    @PostMapping("/like")
    public RestResponse<Void> likeMovie(@PathVariable UUID movieId) {
        return movieLikeService.handleMovieReaction(movieId, true);
    }

    @PostMapping("/dislike")
    public RestResponse<Void> dislikeMovie(@PathVariable UUID movieId) {
        return movieLikeService.handleMovieReaction(movieId, false);
    }
}