package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.CommentRequest;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public RestResponse<CommentResponse> addComment(@Valid @RequestBody CommentRequest request) {
        return commentService.createComment(request);
    }

    @GetMapping("/movie/{movieId}")
    public RestResponse<List<CommentResponse>> getComments(@PathVariable UUID movieId) {
        return commentService.getMovieComments(movieId);
    }

    // CommentController.java içine eklenecek
    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteComment(@PathVariable UUID id) {
        return commentService.deleteComment(id);
    }
}