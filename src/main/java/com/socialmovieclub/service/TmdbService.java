package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.MovieResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.socialmovieclub.core.result.RestResponse.success;

@Service
@RequiredArgsConstructor
public class TmdbService {

    private final RestTemplate restTemplate;
    private final GenreRepository genreRepository;
    private final MessageHelper messageHelper;
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Transactional
    public RestResponse<Void> syncGenres(String lang) {
        try {
//            String url = String.format("%s/genre/movie/list?api_key=%s&language=en", baseUrl, apiKey);
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

                    // 2. tmdbId yok ama BU İSİMDE bir kategori var mı? (Senin manuel eklediklerin için)
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
            // Başarı mesajını buradan dönüyoruz
            return success(null, messageHelper.getMessage("tmdb.sync.success"));
        } catch (Exception e) {
            e.printStackTrace(); // Konsola hatanın detayını yazar (Debug için çok önemli!)
            throw new BusinessException(messageHelper.getMessage("tmdb.sync.error"));
        }
    }

    public RestResponse<MovieResponse> importMovie(Long tmdbId, String lang) {
        //1. Daha once kaydedilmis mi?
        if (movieRepository.existsByTmdbId(tmdbId)) {
            throw new BusinessException(messageHelper.getMessage("movie.already.exist"));
        }

        // 2. TMDB'den detaylari cek
        // language=en yerine language=%s ekledik
//        String url = String.format("%s/movie/%d?api_key=%s&language=%s", baseUrl, tmdbId, apiKey, lang);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId)
                .queryParam("api_key", apiKey)
                .queryParam("language", lang)
                .toUriString();
        TmdbMovieDto tmdbMovie = restTemplate.getForObject(url, TmdbMovieDto.class);

        if (tmdbMovie == null) {
            throw new BusinessException(messageHelper.getMessage("tmdb.movie.not.found"));
        }

        // 3. DOnusturme
        Movie movie = new Movie();
        movie.setImdbId(tmdbMovie.getImdbId()); // Entity'deki imdbId alanına set ediyoruz
        movie.setOriginalTitle(tmdbMovie.getOriginalTitle());
        movie.setTmdbId(tmdbMovie.getId());
        movie.setTmdbRating(tmdbMovie.getVoteAverage());

        if (tmdbMovie.getReleaseDate() != null && tmdbMovie.getReleaseDate().length() >= 4) {
            movie.setReleaseYear(Integer.parseInt(tmdbMovie.getReleaseDate().substring(0, 4)));
        }

        //postPath'i tam URL'e cevir
        movie.setPosterUrl(tmdbMovie.getPosterPath());

        //4. Kategorileri Eslestir
        // genre_ids veya genres listesinden ID'leri topla

        List<Long> genreIds = tmdbMovie.getGenreIds() != null ?
                tmdbMovie.getGenreIds() :
                tmdbMovie.getGenres().stream().map(TmdbGenreDto::getId).toList();

        for (Long tempGenreId : genreIds) {
            // movie.getGenres() Set olduğu için direkt içine ekleyebiliriz
            genreRepository.findByTmdbId(tempGenreId).ifPresent(movie.getGenres()::add);
        }

        // 5. Çeviriyi ekle (Burada 'en' yerine 'lang' kullanıyoruz!)
        MovieTranslation translation = new MovieTranslation();
        translation.setLanguageCode(lang); // Kullanıcı hangi dilde import ediyorsa o kod
        translation.setTitle(tmdbMovie.getTitle());
        translation.setDescription(tmdbMovie.getOverview());
        translation.setMovie(movie);

        movie.getTranslations().add(translation);

        // 6. kaydet ve response don
        Movie savedMovie = movieRepository.save(movie);
        return  success(movieMapper.toResponse(savedMovie, lang), messageHelper.getMessage("movie.import.success"));
    }

    public RestResponse<List<MovieResponse>> searchMovies(String query, String lang) {
        try {
            // 1. URL Oluştur (Arama için)
//            String url = String.format("%s/search/movie?api_key=%s&query=%s&language=%s",
//                    baseUrl, apiKey, query, lang);
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search/movie")
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

            // 3. TMDB DTO listesini MovieResponse listesine çevir
            // Stream kullanarak her bir TmdbMovieDto'yu MovieResponse'a çeviriyoruz
            List<MovieResponse> searchResults = response.getResults().stream()
                    .map(dto -> movieMapper.toResponseFromTmdb(dto, lang))
                    .toList();

            return success(searchResults, messageHelper.getMessage("tmdb.search.success"));

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(messageHelper.getMessage("tmdb.search.error"));
        }

//    } catch (Exception e) {
//        log.error("TMDB search error for query: {}", query, e); // log kullanıyorsan
//        throw new BusinessException(messageHelper.getMessage("tmdb.search.error"));
//    }
    }
}


//importMOvie icinde
/*
TMDB'den ham veriyi çektik.

Bizim veritabanımızdaki kategorilerle (Genre) bağ kurduk.

Tarihi kesip sadece yılı aldık.

İngilizce çeviriyi "ana açıklama" olarak ekledik.

Her şeyi veritabanına kaydettik.

Kaydettiğimiz bu tertemiz veriyi, kullanıcının formatına (DTO) çevirip geri yolladık.
* */


