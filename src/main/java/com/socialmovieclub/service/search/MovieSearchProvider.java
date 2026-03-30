package com.socialmovieclub.service.search;

import com.socialmovieclub.dto.response.SearchResultDto;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.service.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
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
                .map(m -> SearchResultDto.builder()
                        .id(m.getId().toString())
                        .title(m.getOriginalTitle())
                        .subTitle(m.getReleaseYear() + " • Movie")
                        .imageUrl(m.getPosterUrl())
                        .type(getType())
                        .build())
                .collect(Collectors.toList());

        // 2. Hibrit Yapı: Eğer local sonuç azsa TMDB Servisindeki metodunu kullan
        if (results.size() < limit) {
            int remaining = limit - results.size();
            // Mevcut tmdbService.searchMovies metodunu çağırıyoruz
            var tmdbData = tmdbService.searchMovies(query, lang).getData();

            if (tmdbData != null) {
                tmdbData.stream()
                        .limit(remaining)
                        .forEach(t -> results.add(SearchResultDto.builder()
                                .id(String.valueOf(t.getTmdbId()))
                                .title(t.getTitle())
                                .subTitle("TMDB • Movie")
                                .imageUrl(t.getPosterUrl())
                                .type(getType())
                                .build()));
            }
        }
        return results;
    }
}