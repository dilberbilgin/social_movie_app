package com.socialmovieclub.service.search;

import com.socialmovieclub.core.utils.StringUtil;
import com.socialmovieclub.dto.response.SearchResultDto;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MovieSearchProvider implements SearchProvider {
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    @Override
    public String getType() { return "MOVIE"; }

    @Override
    public List<SearchResultDto> search(String query, String lang, int limit) {
        // 1. Local Veritabanı Araması
        List<Movie> localMovies = movieRepository.findByOriginalTitleContainingIgnoreCase(query);

        List<SearchResultDto> results = localMovies.stream()
                .limit(limit)
                .map(m -> {
                    // Dil seçimine göre doğru açıklamayı buluyoruz
                    String description = m.getTranslations().stream()
                            .filter(t -> t.getLanguageCode().equalsIgnoreCase(lang))
                            .findFirst()
                            .map(t -> t.getDescription()) // BaseTranslation içindeki description
                            .orElse(""); // Eğer o dilde çeviri yoksa boş dön
                    return SearchResultDto.builder()
                            .id(m.getId().toString())
                            .title(m.getOriginalTitle())
                            .description(StringUtil.truncate(description, 100))
                            .subTitle(m.getReleaseYear() + " • Movie")
                            .imageUrl(m.getPosterUrl())
                            .type(getType())
                            .build();
                })
                .collect(Collectors.toList());

        // 2. Hibrit Yapı: Eğer local sonuç azsa TMDB Servisindeki metodunu kullan
        if (results.size() < limit) {
            int remaining = limit - results.size();
            // Mevcut tmdbService.searchMovies metodunu çağırıyoruz
         //   var tmdbData = tmdbService.searchMovies(query, lang).getData();
            var tmdbData = tmdbService.searchAll(query, lang).getData();

            if (tmdbData != null) {
                tmdbData.stream()
                        .limit(remaining)
                        .forEach(t -> results.add(SearchResultDto.builder()
                                .id(String.valueOf(t.getTmdbId()))
                                .title(t.getTitle())
                                .description(StringUtil.truncate(t.getDescription(), 100))
//                                .subTitle(t.getReleaseYear() + " TMDB • Movie")
                                .subTitle(t.getReleaseYear() + " TMDB • " + (t.getContentType().equalsIgnoreCase("TV") ? "Series" : "Movie"))
                                .imageUrl(t.getPosterUrl())
//                                .type(getType())
                                .type(getType()) // GlobalSearchResponse'da "MOVIE" (veya dilersen CONTENT) altında toplanır
                                .metadata(Map.of("contentType", t.getContentType(), "isTmdb", true))
                                .build()));
            }
        }
        return results;
    }
}