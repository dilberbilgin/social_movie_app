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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    private final List<String> supportedLanguages = List.of("tr", "en");

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

    @Transactional
    public RestResponse<MovieResponse> importMovie(Long tmdbId, String currentLang) {
        // 1. Kontrol: Bu film zaten var mı?
        if (movieRepository.existsByTmdbId(tmdbId)) {
            throw new BusinessException(messageHelper.getMessage("movie.already.exists"));
        }

        // 2. Filmin ana bilgilerini al (Genel bilgiler için tek bir istek yeterli)
        String mainUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId)
                .queryParam("api_key", apiKey)
                .toUriString();

        TmdbMovieDto mainData = restTemplate.getForObject(mainUrl, TmdbMovieDto.class);
        if (mainData == null) throw new BusinessException(messageHelper.getMessage("tmdb.movie.not.found"));

        // 3. Movie Entity oluştur ve sabit verileri doldur
        Movie movie = new Movie();
        movie.setTmdbId(mainData.getId());
        movie.setImdbId(mainData.getImdbId());
        movie.setOriginalTitle(mainData.getOriginalTitle());
        movie.setTmdbRating(mainData.getVoteAverage());
        movie.setPosterUrl(mainData.getPosterPath());
        if (mainData.getReleaseDate() != null && mainData.getReleaseDate().length() >= 4) {
            movie.setReleaseYear(Integer.parseInt(mainData.getReleaseDate().substring(0, 4)));
        }

        // 4. Kategorileri eşleştir
        List<Long> genreIds = mainData.getGenres().stream().map(TmdbGenreDto::getId).toList();
        for (Long gId : genreIds) {
            genreRepository.findByTmdbId(gId).ifPresent(movie.getGenres()::add);
        }

        // 5. KRİTİK NOKTA: Desteklenen her dil için çevirileri topla
        for (String langCode : supportedLanguages) {
            try {
                String langUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId)
                        .queryParam("api_key", apiKey)
                        .queryParam("language", langCode)
                        .toUriString();

                TmdbMovieDto langData = restTemplate.getForObject(langUrl, TmdbMovieDto.class);

                if (langData != null) {
                    MovieTranslation translation = new MovieTranslation();
                    translation.setLanguageCode(langCode);
                    translation.setTitle(langData.getTitle());
                    translation.setDescription(langData.getOverview());
                    translation.setMovie(movie);
                    movie.getTranslations().add(translation);
                }
            } catch (Exception e) {
                // Bir dilde hata alınırsa diğerlerini durdurma, logla geç.
                System.out.println(langCode + " dili için çeviri bulunamadı.");
            }
        }

        // 6. Kaydet ve kullanıcının o anki diline göre response dön
        Movie savedMovie = movieRepository.save(movie);
        return success(movieMapper.toResponse(savedMovie, currentLang), messageHelper.getMessage("movie.import.success"));
    }

//    public RestResponse<MovieResponse> importMovie(Long tmdbId, String lang) {
//        //1. Daha once kaydedilmis mi?
//        if (movieRepository.existsByTmdbId(tmdbId)) {
//            throw new BusinessException(messageHelper.getMessage("movie.already.exists"));
//        }
//
//        // 2. TMDB'den detaylari cek
//        // language=en yerine language=%s ekledik
////        String url = String.format("%s/movie/%d?api_key=%s&language=%s", baseUrl, tmdbId, apiKey, lang);
//        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + tmdbId)
//                .queryParam("api_key", apiKey)
//                .queryParam("language", lang)
//                .toUriString();
//        TmdbMovieDto tmdbMovie = restTemplate.getForObject(url, TmdbMovieDto.class);
//
//        if (tmdbMovie == null) {
//            throw new BusinessException(messageHelper.getMessage("tmdb.movie.not.found"));
//        }
//
//        // 3. DOnusturme
//        Movie movie = new Movie();
//        movie.setImdbId(tmdbMovie.getImdbId()); // Entity'deki imdbId alanına set ediyoruz
//        movie.setOriginalTitle(tmdbMovie.getOriginalTitle());
//        movie.setTmdbId(tmdbMovie.getId());
//        movie.setTmdbRating(tmdbMovie.getVoteAverage());
//
//        if (tmdbMovie.getReleaseDate() != null && tmdbMovie.getReleaseDate().length() >= 4) {
//            movie.setReleaseYear(Integer.parseInt(tmdbMovie.getReleaseDate().substring(0, 4)));
//        }
//
//        //postPath'i tam URL'e cevir
//        movie.setPosterUrl(tmdbMovie.getPosterPath());
//
//        //4. Kategorileri Eslestir
//        // genre_ids veya genres listesinden ID'leri topla
//
//        List<Long> genreIds = tmdbMovie.getGenreIds() != null ?
//                tmdbMovie.getGenreIds() :
//                tmdbMovie.getGenres().stream().map(TmdbGenreDto::getId).toList();
//
//        for (Long tempGenreId : genreIds) {
//            // movie.getGenres() Set olduğu için direkt içine ekleyebiliriz
//            genreRepository.findByTmdbId(tempGenreId).ifPresent(movie.getGenres()::add);
//        }
//
//        // 5. Çeviriyi ekle (Burada 'en' yerine 'lang' kullanıyoruz!)
//        MovieTranslation translation = new MovieTranslation();
//        translation.setLanguageCode(lang); // Kullanıcı hangi dilde import ediyorsa o kod
//        translation.setTitle(tmdbMovie.getTitle());
//        translation.setDescription(tmdbMovie.getOverview());
//        translation.setMovie(movie);
//
//        movie.getTranslations().add(translation);
//
//        // 6. kaydet ve response don
//        Movie savedMovie = movieRepository.save(movie);
//        return  success(movieMapper.toResponse(savedMovie, lang), messageHelper.getMessage("movie.import.success"));
//    }

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

    // TmdbService.java içine ekle

    public RestResponse<Page<MovieResponse>> discoverMoviesFromTmdb(UUID genreId, String lang, int tmdbPage, Pageable pageable) {
        try {
            // Tür eşleşmesi için TMDB ID'sini bulalım
            Long tmdbGenreId = null;
            if (genreId != null) {
                tmdbGenreId = genreRepository.findById(genreId)
                        .map(Genre::getTmdbId)
                        .orElse(null);
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/discover/movie")
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

            // Spring Page nesnesi oluşturup dönüyoruz (Frontend Infinite Scroll bozulmasın diye)
//            Page<MovieResponse> pageResult = new PageImpl<>(movies, PageRequest.of(tmdbPage - 1, 20), response.getTotalResults());

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


