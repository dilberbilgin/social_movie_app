package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.CommentRequest;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

//    @GetMapping("/movie/{movieId}")
//    public RestResponse<List<CommentResponse>> getComments(@PathVariable UUID movieId) {
//        return commentService.getMovieComments(movieId);
//    }

    @GetMapping("/movie/{movieId}")
    public RestResponse<Page<CommentResponse>> getComments(
            @PathVariable UUID movieId,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return commentService.getMovieComments(movieId, pageable);
    }

    // CommentController.java içine eklenecek
    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteComment(@PathVariable UUID id) {
        return commentService.deleteComment(id);
    }

    // --- Etkileşim (Like/Dislike) Endpointleri ---

    @PostMapping("/{id}/like")
    public RestResponse<Void> likeComment(@PathVariable UUID id) {
        // Servis katmanı MessageHelper kullanarak "Yorum beğenildi" vs. mesajını dönecek
        return commentService.handleCommentReaction(id, true);
    }

    @PostMapping("/{id}/dislike")
    public RestResponse<Void> dislikeComment(@PathVariable UUID id) {
        // isLike = false göndererek dislike mantığını işletiyoruz
        return commentService.handleCommentReaction(id, false);
    }


}