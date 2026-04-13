package com.socialmovieclub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.dto.response.MovieWatchProvidersResponse;
import com.socialmovieclub.dto.response.WatchProviderDto;
import com.socialmovieclub.dto.tmdb.TmdbGenreDto;
import com.socialmovieclub.dto.tmdb.TmdbGenreResponse;
import com.socialmovieclub.dto.tmdb.TmdbMovieDto;
import com.socialmovieclub.dto.tmdb.TmdbSearchResponse;
import com.socialmovieclub.entity.Genre;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.MovieTranslation;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.MovieMapper;
import com.socialmovieclub.repository.GenreRepository;
import com.socialmovieclub.repository.MovieRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

import static com.socialmovieclub.core.result.RestResponse.success;

@Service
@RequiredArgsConstructor
public class TmdbService {

    private final RestTemplate restTemplate;
    private final GenreRepository genreRepository;
    private final MessageHelper messageHelper;
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final ObjectMapper objectMapper;

    private final List<String> supportedLanguages = List.of("tr", "en");

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Transactional
    public RestResponse<Void> syncGenres(String lang) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/genre/movie/list")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en") // Tür isimlerini standart İngilizce çekiyoruz
                    .toUriString();
            TmdbGenreResponse response = restTemplate.getForObject(url, TmdbGenreResponse.class);

            if (response != null && response.getGenres() != null) {
                for (TmdbGenreDto tmdbGenre : response.getGenres()) {
                    String genreName = tmdbGenre.getName().toUpperCase();

                    // 1. Önce bu tmdbId zaten var mı diye bak (Daha önceki sync'ler için)
                    if (genreRepository.existsByTmdbId(tmdbGenre.getId())) {
                        continue; // Varsa geç
                    }

                    // 2. tmdbId yok ama BU İSİMDE bir kategori var mı? ( manuel eklediklerim için)
                    // GenreRepository'de Optional<Genre> findByName(String name) olmalı.
                    Optional<Genre> existingGenre = genreRepository.findByName(genreName);

                    if (existingGenre.isPresent()) {
                        // İsim aynı ama tmdbId boş kalmış, onu güncelle!
                        Genre genre = existingGenre.get();
                        genre.setTmdbId(tmdbGenre.getId());
                        genreRepository.save(genre);
                    } else {
                        // Hiç yoksa yeni oluştur
                        Genre newGenre = new Genre();
                        newGenre.setName(genreName);
                        newGenre.setTmdbId(tmdbGenre.getId());
                        genreRepository.save(newGenre);
                    }
                }
            }
            return success(null, messageHelper.getMessage("tmdb.sync.success"));
        } catch (Exception e) {
            e.printStackTrace(); // Konsola hatanın detayını yazar (Debug için çok önemli!)
            throw new BusinessException(messageHelper.getMessage("tmdb.sync.error"));
        }
    }

    @Transactional
    public Movie importMovieEntity(Long tmdbId, String contentType, String currentLang) {
        // 1. Kontrol: Bu film zaten var mı? (Çift kontrol)
        Optional<Movie> existingMovie = movieRepository.findByTmdbId(tmdbId);
        if (existingMovie.isPresent()) {
            return existingMovie.get();
        }

        // 2. Filmin ana bilgilerini al
        String typePath = "TV".equalsIgnoreCase(contentType) ? "/tv/" : "/movie/";
        String mainUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + typePath + tmdbId)
//        String mainUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId)
                .queryParam("api_key", apiKey)
                .toUriString();

        TmdbMovieDto mainData = restTemplate.getForObject(mainUrl, TmdbMovieDto.class);
        if (mainData == null) throw new BusinessException(messageHelper.getMessage("tmdb.movie.not.found"));

        // 3. Movie Entity oluştur ve sabit verileri doldur
        Movie movie = new Movie();
        movie.setTmdbId(mainData.getId());
        movie.setImdbId(mainData.getImdbId());

        // Dizi ise 'name', film ise 'original_title' (TMDB farkı)
        String originalTitle = mainData.getOriginalTitle() != null ? mainData.getOriginalTitle() : mainData.getOriginalName();
        movie.setOriginalTitle(originalTitle);
       // movie.setOriginalTitle(mainData.getOriginalTitle());

        movie.setTmdbRating(mainData.getVoteAverage());
        movie.setPosterUrl(mainData.getPosterPath());
        movie.setContentType(contentType.toUpperCase()); // Mutlaka kaydet!

//        if (mainData.getReleaseDate() != null && mainData.getReleaseDate().length() >= 4) {
//            movie.setReleaseYear(Integer.parseInt(mainData.getReleaseDate().substring(0, 4)));
//        }
        // Tarih Kontrolü: Dizilerde first_air_date, Filmlerde release_date gelir
        String date = mainData.getReleaseDate() != null ? mainData.getReleaseDate() : mainData.getFirstAirDate();
        if (date != null && date.length() >= 4) {
            movie.setReleaseYear(Integer.parseInt(date.substring(0, 4)));
        }

        // 4. Kategorileri eşleştir
        if (mainData.getGenres() != null) {
            List<Long> genreIds = mainData.getGenres().stream().map(TmdbGenreDto::getId).toList();
            for (Long gId : genreIds) {
                genreRepository.findByTmdbId(gId).ifPresent(movie.getGenres()::add);
            }
        }

        // 5. Desteklenen her dil için çevirileri topla
        for (String langCode : supportedLanguages) {
            try {
                //String langUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId)
                String langUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + typePath + tmdbId)
                        .queryParam("api_key", apiKey)
                        .queryParam("language", langCode)
                        .toUriString();

                TmdbMovieDto langData = restTemplate.getForObject(langUrl, TmdbMovieDto.class);

                if (langData != null) {
                    MovieTranslation translation = new MovieTranslation();
                    translation.setLanguageCode(langCode);

                    // Dizi ise 'name', film ise 'title' kullan
                    String title = langData.getTitle() != null ? langData.getTitle() : langData.getName();
                    translation.setTitle(title);

//                    translation.setTitle(langData.getTitle());
                    translation.setDescription(langData.getOverview());
                    translation.setMovie(movie);
                    movie.getTranslations().add(translation);
                }
            } catch (Exception e) {
                System.err.println(langCode + " dili için çeviri hatası: " + e.getMessage());
            }
        }

        // 6. Kaydet ve Entity dön
        return movieRepository.save(movie);
    }

    @Transactional
    public RestResponse<MovieResponse> importMovie(Long tmdbId, String contentType, String currentLang) {
        Movie savedMovie = importMovieEntity(tmdbId, contentType, currentLang);
        return RestResponse.success(movieMapper.toResponse(savedMovie, currentLang),
                messageHelper.getMessage("movie.import.success"));
    }

    public RestResponse<List<MovieResponse>> searchMovies(String query, String lang) {
        try {
//            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search/movie")
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search/multi")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", query)
                    .queryParam("language", lang)
                    .build()
                    .toUriString();

            // 2. TMDB'den sonuçları al
            TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return success(List.of(), messageHelper.getMessage("tmdb.search.no.result"));
            }

//            // 3. TMDB DTO listesini MovieResponse listesine çevir
//            // Stream kullanarak her bir TmdbMovieDto'yu MovieResponse'a çeviriyoruz .//todo: stream
//            List<MovieResponse> searchResults = response.getResults().stream()
//                    .map(dto -> movieMapper.toResponseFromTmdb(dto, lang))
//                    .toList();

            // FİLTRELEME: Sadece movie ve tv olanları al, person (oyuncu) olanları at.
            List<MovieResponse> searchResults = response.getResults().stream()
                    .filter(dto -> "movie".equals(dto.getMediaType()) || "tv".equals(dto.getMediaType()))
                    .map(dto -> movieMapper.toResponseFromTmdb(dto, lang))
                    .toList();

            return success(searchResults, messageHelper.getMessage("tmdb.search.success"));

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(messageHelper.getMessage("tmdb.search.error"));
        }
    }

    public RestResponse<Page<MovieResponse>> discoverMoviesFromTmdb(String contentType, UUID genreId, String lang, int tmdbPage, Pageable pageable) {
        try {
            // Tür eşleşmesi için TMDB ID'sini bulalım
            Long tmdbGenreId = null;
            if (genreId != null) {
                tmdbGenreId = genreRepository.findById(genreId)
                        .map(Genre::getTmdbId)
                        .orElse(null);
            }

            // contentType "movie" veya "tv" gelmeli
            String path = "/discover/" + (contentType.equalsIgnoreCase("TV") ? "tv" : "movie");

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
//            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/discover/movie")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", lang)
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("page", tmdbPage);

            if (tmdbGenreId != null) {
                builder.queryParam("with_genres", tmdbGenreId);
            }

            TmdbSearchResponse response = restTemplate.getForObject(builder.toUriString(), TmdbSearchResponse.class);

            if (response == null || response.getResults() == null) {
                return success(Page.empty());
            }

            // TMDB'den gelenleri bizim MovieResponse formatına çevir
            List<MovieResponse> movies = response.getResults().stream()
                    .map(dto -> {
                        MovieResponse res = movieMapper.toResponseFromTmdb(dto, lang);
                        // Bu filmler henüz DB'de olmadığı için Like/Comment sayıları 0 olacak
                        res.setLikeCount(0L);
                        res.setCommentCount(0L);
                        res.setClubRating(0.0);
                        return res;
                    })
                    .toList();

            Page<MovieResponse> pageResult = new PageImpl<>(
                    movies,
                    pageable, // Methoda Pageable pageable parametresini de ekleyip buraya paslayın
                    5000      // Sabit yüksek değer veya response.getTotalResults() + totalLocalElements
            );
            return success(pageResult);

        } catch (Exception e) {
            return success(Page.empty());
        }
    }

    public List<MovieResponse> getTrendingFromTmdb(String lang, int page) {
        try {
//            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/trending/movie/day")
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/trending/all/day")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", lang)
                    .queryParam("page", page)
                    .toUriString();

            TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);

            if (response == null || response.getResults() == null) {
                return List.of();
            }

            return response.getResults().stream()
                    .filter(dto -> "movie".equals(dto.getMediaType()) || "tv".equals(dto.getMediaType()))
                    .map(dto -> movieMapper.toResponseFromTmdb(dto, lang))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    public RestResponse<List<MovieResponse>> searchAll(String query, String lang) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search/multi") // multi-search endpoint
                    .queryParam("api_key", apiKey)
                    .queryParam("query", query)
                    .queryParam("language", lang)
                    .toUriString();

            TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);

            if (response == null || response.getResults() == null) {
                return success(List.of());
            }

            // Sadece film ve dizileri filtrele (kişileri/aktörleri şimdilik eleyebiliriz)
            List<MovieResponse> searchResults = response.getResults().stream()
                    .filter(dto -> "movie".equals(dto.getMediaType()) || "tv".equals(dto.getMediaType()))
                    .map(dto -> movieMapper.toResponseFromTmdb(dto, lang))
                    .toList();

            return success(searchResults);
        } catch (Exception e) {
            throw new BusinessException("Arama sırasında bir hata oluştu");
        }
    }

    public MovieWatchProvidersResponse getWatchProviders(Long tmdbId, String contentType, String region) {
        String typePath = "TV".equalsIgnoreCase(contentType) ? "/tv/" : "/movie/";
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + typePath + tmdbId + "/watch/providers")
                .queryParam("api_key", apiKey)
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> results = (Map<String, Object>) response.get("results");
            Map<String, Object> regionData = (Map<String, Object>) results.get(region.toUpperCase());

            if (regionData == null) return new MovieWatchProvidersResponse();

            MovieWatchProvidersResponse providerResponse = new MovieWatchProvidersResponse();
            providerResponse.setFlatrate(parseProviders(regionData.get("flatrate")));
            providerResponse.setRent(parseProviders(regionData.get("rent")));
            providerResponse.setBuy(parseProviders(regionData.get("buy")));

            return providerResponse;
        } catch (Exception e) {
            return new MovieWatchProvidersResponse();
        }
    }

    private List<WatchProviderDto> parseProviders(Object data) {
        if (data == null) return new ArrayList<>();
        // Jackson ile Map'i List<WatchProviderDto>'ya çevirme mantığı
        return objectMapper.convertValue(data, new TypeReference<List<WatchProviderDto>>() {});
    }
}

//todo: tekrar incele


