package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.MovieCreateRequest;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.entity.Genre;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.Rating;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.MovieMapper;
import com.socialmovieclub.repository.GenreRepository;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.RatingRepository;
import com.socialmovieclub.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static com.socialmovieclub.core.result.RestResponse.success;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MessageHelper messageHelper;
    private final GenreRepository genreRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;



    @Transactional
    public RestResponse<MovieResponse> createMovie(MovieCreateRequest request, String lang) {

        // 1. İş Kuralı Kontrolü (Dinamik ve Çok Dilli)
        if (movieRepository.existsByOriginalTitleAndReleaseYear(request.getOriginalTitle(), request.getReleaseYear())) {
            // "movie.already.exists" anahtarını ve parametre olarak film adını gönderiyoruz
            String errorMessage = messageHelper.getMessage("movie.already.exists", request.getOriginalTitle());
            throw new BusinessException(errorMessage);
        }
        // 2. MapStruct ile kaydet
        Movie movie = movieMapper.toEntity(request);

        //3. KATEGORILERI BAGLAMA
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            // ID listesini kullanarak veritabanından tüm Genre nesnelerini tek seferde çekiyoruz
            List<Genre> foundGenres = genreRepository.findAllById(request.getGenreIds());
            // Eğer veritabanında bulunan kategori sayısı, istenen ID sayısından azsa hata fırlatabiliriz (opsiyonel ama güvenli)
            if (foundGenres.size() != request.getGenreIds().size()) {
                throw new BusinessException(messageHelper.getMessage("genre.not.found"));
            }

            movie.setGenres(new HashSet<>(foundGenres));
        }

        // 4. Kaydet
        Movie savedMovie = movieRepository.save(movie);

        // 5. Response'a çevir (Artık genres dolu olduğu için Mapper onları da çevirecek)
        MovieResponse responseData = movieMapper.toResponse(savedMovie, lang);

        // Başarı mesajı
        String successMsg = messageHelper.getMessage("movie.create.success");

        // 2. Kaydedilen filmi, isteği atan kişinin dilinde geri dön
        return success(responseData, successMsg);
    }

    public RestResponse<List<MovieResponse>> getAllMovies(String lang) {
        List<Movie> movies = movieRepository.findAll();

        // 1. Listeyi dile göre çevirip Response DTO listesine dönüştür
        List<MovieResponse> responseData = movieMapper.toResponseList(movies, lang);

        // 2. Bunu RestResponse zarfına koyup dön
        return success(responseData);
    }

    public RestResponse<List<MovieResponse>> getTopRatedMovies(String lang) {
        List<Movie> movies = movieRepository.findTop10ByOrderByClubRatingDesc();
        return success(movieMapper.toResponseList(movies, lang), messageHelper.getMessage("common.success"));
    }

    public RestResponse<List<MovieResponse>> getTrendingMovies(String lang) {
        // Repository'de yazdığımız "En çok oylanan ilk 10" sorgusunu çağırıyoruz
        List<Movie> movies = movieRepository.findTop10ByOrderByClubVoteCountDesc();
        return success(movieMapper.toResponseList(movies, lang), messageHelper.getMessage("common.success"));
    }



//    public RestResponse<MovieResponse> getMovieById(UUID id, String lang) {
//        Movie movie = movieRepository.findById(id)
//                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));
//
//        return success(movieMapper.toResponse(movie, lang));
//    }

    public RestResponse<MovieResponse> getMovieById(UUID id, String lang) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        MovieResponse response = movieMapper.toResponse(movie, lang);

        // Giriş yapmış kullanıcının puanını kontrol et
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username != null && !username.equals("anonymousUser")) {
                // Kullanıcıyı bul ve eğer varsa puanını response'a ekle
                userRepository.findByUsername(username).ifPresent(user -> {
                    ratingRepository.findByUserIdAndMovieId(user.getId(), id)
                            .ifPresent(rating -> response.setUserScore(rating.getScore()));
                });
            }
        } catch (Exception e) {
            // Kullanıcı giriş yapmamışsa hata almamak için sessizce devam et
        }

        return success(response);
    }
}