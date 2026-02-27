package com.socialmovieclub.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class FollowRequest {
    private UUID followingId; // Takip edilecek kişinin ID'si

}
