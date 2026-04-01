package com.socialmovieclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor  // Bunu ekle
@AllArgsConstructor // Bunu ekle
public class GlobalSearchResponse {
    // Sonuçları kategorize edilmiş şekilde döner
    private List<SearchResultDto> movies;
    private List<SearchResultDto> users;
    private List<SearchResultDto> topResults; // En alakalı karışık sonuçlar
}