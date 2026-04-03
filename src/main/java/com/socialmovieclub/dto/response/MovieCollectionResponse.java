package com.socialmovieclub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MovieCollectionResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean isPublic;
    private int movieCount;
    private String ownerUsername;
    // Listenin kapak fotoğrafı (İçindeki ilk filmin posteri olabilir)
    private String coverImageUrl;
    // Koleksiyona ait filmler
    private java.util.List<MovieResponse> movies;
    private List<MovieCollectionResponse> collections;
}
