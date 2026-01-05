package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.MovieCreateRequest;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.entity.Genre;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.MovieMapper;
import com.socialmovieclub.repository.GenreRepository;
import com.socialmovieclub.repository.MovieRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.socialmovieclub.core.result.RestResponse.success;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MessageHelper messageHelper;
    private final GenreRepository genreRepository;



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
}