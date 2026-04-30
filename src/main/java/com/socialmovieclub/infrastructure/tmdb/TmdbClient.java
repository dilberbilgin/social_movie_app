
package com.socialmovieclub.infrastructure.tmdb;

import com.socialmovieclub.dto.tmdb.TmdbMovieDto;
import com.socialmovieclub.dto.tmdb.TmdbSearchResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TmdbClient {
    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    private String buildUrl(String path, String lang, Map<String, String> extraParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
                .queryParam("api_key", apiKey);

        if (lang != null) builder.queryParam("language", lang);
        if (extraParams != null) extraParams.forEach(builder::queryParam);

        return builder.toUriString();
    }

    // 1. WATCH PROVIDERS
    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackWatchProviders")
    public Map<String, Object> fetchWatchProviders(Long tmdbId, String contentType) {
        String typePath = "TV".equalsIgnoreCase(contentType) ? "/tv/" : "/movie/";
        String url = buildUrl(typePath + tmdbId + "/watch/providers", null, null);
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> fallbackWatchProviders(Long tmdbId, String contentType, Throwable t) {
        log.error("TMDB Watch Provider hatası (ID: {}): {}", tmdbId, t.getMessage());
        return Collections.emptyMap();
    }

    // 2. MOVIE/TV DETAILS
    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackMovieDetails")
    public TmdbMovieDto fetchMovieDetails(Long tmdbId, String contentType, String lang) {
        String path = ("TV".equalsIgnoreCase(contentType) ? "/tv/" : "/movie/") + tmdbId;
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("append_to_response", "credits");
        return restTemplate.getForObject(buildUrl(path, lang, extraParams), TmdbMovieDto.class);
    }

    public TmdbMovieDto fallbackMovieDetails(Long tmdbId, String contentType, String lang, Throwable t) {
        log.error("TMDB Detay hatası (ID: {}): {}", tmdbId, t.getMessage());
        return null; // Servis katmanında null kontrolü yapılmalı
    }

    // 3. SEARCH (BY TYPE & MULTI)
    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackSearch")
    public TmdbSearchResponse searchByType(String type, String query, String lang, Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("query", query);
        if (page != null) params.put("page", page.toString());
        return restTemplate.getForObject(buildUrl("/search/" + type, lang, params), TmdbSearchResponse.class);
    }

    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackSearchMulti")
    public TmdbSearchResponse searchMulti(String query, String lang, Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("query", query);
        if (page != null) params.put("page", page.toString());
        return restTemplate.getForObject(buildUrl("/search/multi", lang, params), TmdbSearchResponse.class);
    }

    // Arama için Fallback metotları
    public TmdbSearchResponse fallbackSearch(String type, String query, String lang, Integer page, Throwable t) {
        log.error("TMDB Arama hatası: {}", t.getMessage());
        return new TmdbSearchResponse();
    }

    public TmdbSearchResponse fallbackSearchMulti(String query, String lang, Integer page, Throwable t) {
        log.error("TMDB Multi-Arama hatası: {}", t.getMessage());
        return new TmdbSearchResponse();
    }
}









//package com.socialmovieclub.infrastructure.tmdb;
//
//import com.socialmovieclub.dto.tmdb.TmdbMovieDto;
//import com.socialmovieclub.dto.tmdb.TmdbSearchResponse;
//import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j; // Log için gerekli
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TmdbClient {
//    private final RestTemplate restTemplate;
//
//    @Value("${tmdb.api.key}")
//    private String apiKey;
//
//    @Value("${tmdb.api.base-url}")
//    private String baseUrl;
//
//    // Ortak URL oluşturma metodu (Kod tekrarını önler, yeni diller eklemeyi kolaylaştırır)
//    private String buildUrl(String path, String lang, Map<String, String> extraParams) {
//        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
//                .queryParam("api_key", apiKey);
//
//        if (lang != null) builder.queryParam("language", lang);
//        if (extraParams != null) extraParams.forEach(builder::queryParam);
//
//        return builder.toUriString();
//    }
//
//
//
//    // 1. WATCH PROVIDERS
//    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackWatchProviders")
//    public Map<String, Object> fetchWatchProviders(Long tmdbId, String contentType) {
//        String typePath = "TV".equalsIgnoreCase(contentType) ? "/tv/" : "/movie/";
//        String url = buildUrl(typePath + tmdbId + "/watch/providers", null, null);
//        return restTemplate.getForObject(url, Map.class);
//    }
//
//    // Parametreler asıl metotla aynı olmalı + Throwable
//    public Map<String, Object> fallbackWatchProviders(Long tmdbId, String contentType, Throwable t) {
//        log.error("TMDB Watch Provider hatası (ID: {}): {}", tmdbId, t.getMessage());
//        return Collections.emptyMap();
//    }
//
//    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackGeneric")
//    public TmdbMovieDto fetchMovieDetails(Long tmdbId, String contentType, String lang) {
//        String path = ("TV".equalsIgnoreCase(contentType) ? "/tv/" : "/movie/") + tmdbId;
//
//        // extraParams kullanarak append_to_response ekliyoruz
//        Map<String, String> extraParams = new HashMap<>();
//        extraParams.put("append_to_response", "credits");
//
//        return restTemplate.getForObject(buildUrl(path, lang, extraParams), TmdbMovieDto.class);
//    }
//
//
//    // 2. SEARCH
//    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackSearch")
//    public TmdbSearchResponse searchByType(String type, String query, String lang, Integer page) {
//        Map<String, String> params = new HashMap<>();
//        params.put("query", query);
//        if (page != null) params.put("page", page.toString());
//        return restTemplate.getForObject(buildUrl("/search/" + type, lang, params), TmdbSearchResponse.class);
//    }
//
//    // Parametreler asıl metotla aynı olmalı + Throwable
//    public TmdbSearchResponse fallbackSearch(String type, String query, String lang, Integer page, Throwable t) {
//        log.error("TMDB Arama hatası: {}", t.getMessage());
//        return new TmdbSearchResponse();
//    }
//
//    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackGeneric")
//    public TmdbSearchResponse searchMulti(String query, String lang, Integer page) {
//        Map<String, String> params = new HashMap<>();
//        params.put("query", query);
//        if (page != null) params.put("page", page.toString());
//
//        return restTemplate.getForObject(buildUrl("/search/multi", lang, params), TmdbSearchResponse.class);
//    }
//
//    // Genel Hata Yakalayıcı (Circuit Breaker açıkken veya hata anında çalışır)
////    public <T> T fallbackGeneric(Throwable t) {
////        log.error("TMDB servis hatası: {}", t.getMessage());
////        return null; // Veya boş bir DTO nesnesi
////    }
//    public Map<String, Object> fallbackGeneric(Long tmdbId, String contentType, Throwable t) {
//        log.error("TMDB Watch Provider hatası (ID: {}): {}", tmdbId, t.getMessage());
//        return Collections.emptyMap();
//    }
//
////    // TmdbClient içine eklenecek genel arama metodu
////    @CircuitBreaker(name = "tmdbClient", fallbackMethod = "fallbackGeneric")
////    public TmdbSearchResponse searchByType(String type, String query, String lang, Integer page) {
////        Map<String, String> params = new HashMap<>();
////        params.put("query", query);
////        if (page != null) params.put("page", page.toString());
////
////        // type: "multi", "movie", "tv", "person" veya "collection" olabilir
////        String path = "/search/" + type;
////        return restTemplate.getForObject(buildUrl(path, lang, params), TmdbSearchResponse.class);
////    }
//
////    public TmdbSearchResponse fallbackSearch(String query, String lang, Integer page, Throwable t) {
////        log.error("TMDB Arama hatası: {}", t.getMessage());
////        return new TmdbSearchResponse(); // Boş sonuç dön
////    }
//}