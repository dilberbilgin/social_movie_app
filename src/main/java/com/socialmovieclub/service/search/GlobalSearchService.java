package com.socialmovieclub.service.search;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.GlobalSearchResponse;
import com.socialmovieclub.dto.response.SearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {
    // Spring, SearchProvider interface'ini implement eden tüm class'ları buraya enjekte eder.
    private final List<SearchProvider> providers;

    public RestResponse<GlobalSearchResponse> executeSearch(String query, String lang) {
        // Hızlı kontrol
        if (query == null || query.trim().length() < 2) {
            return RestResponse.success(GlobalSearchResponse.builder()
                    .movies(List.of()).users(List.of()).topResults(List.of()).build());
        }

        // Tüm provider'ları tetikle ve sonuçları topla
        Map<String, List<SearchResultDto>> allResults = providers.stream()
                .collect(Collectors.toMap(
                        SearchProvider::getType,
                        p -> p.search(query, lang, 10) // Her kategoriden max 10 tane
                ));

        return RestResponse.success(GlobalSearchResponse.builder()
                .movies(allResults.getOrDefault("MOVIE", List.of()))
                .users(allResults.getOrDefault("USER", List.of()))
                .topResults(combineForTopResults(allResults))
                .build());
    }

    private List<SearchResultDto> combineForTopResults(Map<String, List<SearchResultDto>> all) {
        // "Keşfet" mantığı: Karışık en iyi sonuçlar
        List<SearchResultDto> top = new ArrayList<>();
        all.values().forEach(list -> top.addAll(list.stream().limit(3).toList()));
        return top;
    }
}