package com.socialmovieclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CommentRequest {

    @NotBlank(message = "{comment.content.required}")
    private String content;

    @NotNull(message = "{movie.id.required}")
    private UUID movieId;

    private UUID parentId; // Opsiyonel: Alt yorum ise doldurulur
}