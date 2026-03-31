package com.socialmovieclub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GlobalSearchResponse {
    // Sonuçları kategorize edilmiş şekilde döner
    private List<SearchResultDto> movies;
    private List<SearchResultDto> users;
    private List<SearchResultDto> topResults; // En alakalı karışık sonuçlar
}