package com.socialmovieclub.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CommentResponse {
    private UUID id;
    private String content;
    private String username; // Yorumu yapan kişi
    private LocalDateTime createdDate;
    private List<CommentResponse> replies; // Alt yorumlar
}