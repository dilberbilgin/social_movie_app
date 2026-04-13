package com.socialmovieclub.service;

import com.socialmovieclub.core.context.UserContextHolder;
import com.socialmovieclub.dto.response.MovieWatchProvidersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchProviderService {
    private final TmdbService tmdbService;


    // Burada ayrıca cache yapmaya gerek yok, MovieService seviyesinde yapıyoruz.
    // Ama doğrudan bu servisi başka yerden çağıracaksan eklenebilir.
    public MovieWatchProvidersResponse getProviders(Long tmdbId, String contentType) {
        String currentRegion = UserContextHolder.getContext().getRegion();
        return tmdbService.getWatchProviders(tmdbId, contentType, currentRegion);
    }
}