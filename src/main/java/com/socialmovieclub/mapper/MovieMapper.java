package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.request.MovieCreateRequest;
import com.socialmovieclub.dto.request.TranslationRequest;
import com.socialmovieclub.dto.response.MovieResponse;
import com.socialmovieclub.dto.tmdb.TmdbMovieDto;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.MovieTranslation;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring" , uses = {GenreMapper.class})
public interface MovieMapper {

    // Request -> Entity
    Movie toEntity(MovieCreateRequest request);

    MovieTranslation toEntity(TranslationRequest request);

    @AfterMapping
    default void linkTranslations(@MappingTarget Movie movie) {
        if (movie.getTranslations() != null) {
            movie.getTranslations().forEach(t -> t.setMovie(movie));
        }
    }

    // Tekil dönüşüm
    // Entity -> Response
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "genres", source = "genres")
    @Mapping(target = "posterUrl", source = "posterUrl", qualifiedByName = "posterUrlMapper")
    @Mapping(target = "tmdbRating", source = "tmdbRating") // Entity'deki tmdbRating'i eşle
    @Mapping(target = "clubRating", source = "clubRating") // Entity'deki clubRating'i eşle
    @Mapping(target = "clubVoteCount", source = "clubVoteCount")
    @Mapping(target = "imdbUrl", source = "imdbId", qualifiedByName = "imdbUrlGenerator")
    // Entity'deki genres -> Response'daki genres
    MovieResponse toResponse(Movie movie, @Context String languageCode);

    // ÇOKLU DÖNÜŞÜM: MapStruct otomatik olarak yukarıdaki metodu kullanır
    List<MovieResponse> toResponseList(List<Movie> movies, @Context String languageCode);

    // Her dönüşümden sonra bu metod çalışacak ve doğru dili seçecek
    @AfterMapping
    default void setLanguageFields(@MappingTarget MovieResponse response, Movie movie, @Context String languageCode) {

        if (movie.getTranslations() == null || movie.getTranslations().isEmpty()) {
            return;
        }

        // 1. ADIM: Kullanıcının istediği dili ara (Örn: "pt" - Portekizce)
        MovieTranslation translation = movie.getTranslations().stream()
                .filter(t -> t.getLanguageCode().equalsIgnoreCase(languageCode))
                .findFirst()
                // 2. ADIM: Bulamazsan varsayılan dili ("en") ara
                .orElseGet(() -> movie.getTranslations().stream()
                        .filter(t -> t.getLanguageCode().equalsIgnoreCase("en"))
                        .findFirst()
                        // 3. ADIM: O da yoksa listenin en başındaki ilk çeviriyi getir (Hiç yoktan iyidir)
                        .orElse(movie.getTranslations().iterator().next())
                );

        // Seçilen (veya fallback edilen) veriyi set et
        response.setTitle(translation.getTitle());
        response.setDescription(translation.getDescription());
    }


    // TMDB DTO -> Response
    @Mapping(target = "id", ignore = true) // DB'de olmadığı için UUID'si yok
    @Mapping(target = "tmdbId", source = "dto.id") // TMDB'nin id'sini bizim tmdbId alanına eşle
    @Mapping(target = "tmdbRating", source = "dto.voteAverage") // TMDB'den gelen puanı eşle
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "description", source = "dto.overview")
    @Mapping(target = "releaseYear", expression = "java(extractYear(dto.getReleaseDate()))")
//    @Mapping(target = "posterUrl", expression = "java(dto.getPosterPath() != null ? \"https://image.tmdb.org/t/p/w500\" + dto.getPosterPath() : null)")
    @Mapping(target = "posterUrl", source = "dto.posterPath", qualifiedByName = "posterUrlMapper")
    @Mapping(target = "originalTitle", source = "dto.originalTitle")
    @Mapping(target = "genres", ignore = true)
    @Mapping(target = "clubRating", ignore = true) // TMDB aramasında henüz bizim puanımız olamaz
    @Mapping(target = "clubVoteCount", ignore = true)
    @Mapping(target = "imdbUrl", source = "dto.imdbId", qualifiedByName = "imdbUrlGenerator")
    MovieResponse toResponseFromTmdb(TmdbMovieDto dto, String lang);

    @Named("imdbUrlGenerator")
    default String generateImdbUrl(String imdbId) {
        if (imdbId == null || imdbId.isEmpty() || imdbId.startsWith("http")) return null;
        return "https://www.imdb.com/title/" + imdbId;
    }

    // Yılı güvenli bir şekilde çekmek için default metod
    default Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (Exception e) {
            return null;
        }
    }

    @Named("posterUrlMapper")
    default String handlePosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isEmpty()) {
            return "https://via.placeholder.com/500x750?text=No+Poster+Available";
        }
        // Eğer zaten tam bir URL ise (http ile başlıyorsa) dokunma
        if (posterPath.startsWith("http")) {
            return posterPath;
        }
        // TMDB path'i ise (/p96dm... gibi) prefix ekle
        return "https://image.tmdb.org/t/p/w500" + (posterPath.startsWith("/") ? "" : "/") + posterPath;
    }
}