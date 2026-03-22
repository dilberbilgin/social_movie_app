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
import com.socialmovieclub.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
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

    private final MovieLikeRepository movieLikeRepository;
    private final CommentRepository commentRepository;
    private final SecurityService securityService;



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

        enrichMovieListWithLikes(responseData);

        // 2. Bunu RestResponse zarfına koyup dön
        return success(responseData);
    }

    public RestResponse<List<MovieResponse>> getTopRatedMovies(String lang) {
        List<Movie> movies = movieRepository.findTop10ByOrderByClubRatingDesc();
        List<MovieResponse> responseData = movieMapper.toResponseList(movies, lang);
        enrichMovieListWithLikes(responseData);
        return success(responseData, messageHelper.getMessage("common.success"));
    }

    public RestResponse<List<MovieResponse>> getTrendingMovies(String lang) {
        // Repository'de yazdığımız "En çok oylanan ilk 10" sorgusunu çağırıyoruz
        List<Movie> movies = movieRepository.findTop10ByOrderByClubVoteCountDesc();
        List<MovieResponse> responseData = movieMapper.toResponseList(movies, lang);
        enrichMovieListWithLikes(responseData);
        return success(responseData, messageHelper.getMessage("common.success"));
    }


    public RestResponse<MovieResponse> getMovieById(UUID id, String lang) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        MovieResponse response = movieMapper.toResponse(movie, lang);

        // LIKE/DISLIKE Zenginleştirmesi
        enrichMovieWithLikes(response);

        // Giriş yapmış kullanıcının puanını kontrol et
        // Giriş yapmış kullanıcının puanını Optional ile güvenli set et
        securityService.getUserIfLoggedIn().ifPresent(user -> {
            ratingRepository.findByUserIdAndMovieId(user.getId(), id)
                    .ifPresent(rating -> response.setUserScore(rating.getScore()));
        });

        return success(response);
    }

    private void enrichMovieWithLikes(MovieResponse dto) {
        dto.setLikeCount(movieLikeRepository.countByMovieIdAndIsLikedTrue(dto.getId()));
        dto.setDislikeCount(movieLikeRepository.countByMovieIdAndIsLikedFalse(dto.getId()));
        dto.setCommentCount(commentRepository.countByMovieIdAndDeletedFalse(dto.getId()));


//        try {
//            String username = SecurityContextHolder.getContext().getAuthentication().getName();
//            if (username != null && !username.equals("anonymousUser")) {
//                userRepository.findByUsername(username).ifPresent(user -> {
//                    movieLikeRepository.findByUserIdAndMovieId(user.getId(), dto.getId())
//                            .ifPresent(like -> dto.setUserReaction(like.isLiked()));
//                });
//            }
//        } catch (Exception e) { /* Login değilse geç */ }
        securityService.getUserIfLoggedIn().ifPresent(user -> {
            movieLikeRepository.findByUserIdAndMovieId(user.getId(), dto.getId())
                    .ifPresent(like -> dto.setUserReaction(like.isLiked()));
        });
    }

    private void enrichMovieListWithLikes(List<MovieResponse> dtos) {
        dtos.forEach(this::enrichMovieWithLikes);
    }

    public RestResponse<List<MovieResponse>> searchMovies(String title, UUID genreId, String lang) {
        // Dinamik sorgu oluşturucu
        Specification<Movie> spec = Specification.where(null);

        if (title != null && !title.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("originalTitle")), "%" + title.toLowerCase() + "%"));
        }

        if (genreId != null) {
            // Many-to-Many ilişkide genre üzerinden filtreleme
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("genres").get("id"), genreId));
        }

        List<Movie> movies = movieRepository.findAll(spec);
        List<MovieResponse> responseData = movieMapper.toResponseList(movies, lang);
        enrichMovieListWithLikes(responseData);
        return success(responseData);
    }


}