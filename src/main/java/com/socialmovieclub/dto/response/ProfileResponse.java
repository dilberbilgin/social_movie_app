package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String bio; // Entity'ye ekleyeceğiz
    private String profilePictureUrl; // İleride eklenecek

    // İstatistikler
    private long movieCount;
    private long followerCount;
    private long followingCount;

    // Kullanıcının son aktiviteleri (Örn: Son verdiği 10 puan)
    private Page<RatingResponse> recentRatings;

    // Özel: Eğer giriş yapmış kullanıcı bu profile bakıyorsa, onu takip ediyor mu?
    @JsonProperty("isFollowing")
    private boolean isFollowing;
}