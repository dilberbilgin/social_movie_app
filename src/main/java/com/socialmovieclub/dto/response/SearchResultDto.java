package com.socialmovieclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {
    private String id;          // UUID (User/Local Movie) veya Long (TMDB ID)
    private String title;       // Film adı veya Username
    private String subTitle;    // "2024 • Action" veya "Software Developer"
    private String imageUrl;    // Poster URL veya Profile Image URL
    private String type;        // "MOVIE", "USER", "GROUP", "HASHTAG"

    // Frontend'de tıklandığında detaylara gitmek için ek bilgi
    private Map<String, Object> metadata;
}