package com.socialmovieclub.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class RatingResponse {
    private UUID id;
    private Integer score;
    private String username;
    private UUID movieId;
}