package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.entity.MovieTranslation;
import com.socialmovieclub.entity.Rating;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "posterUrl", source = "movie.posterUrl", qualifiedByName = "posterUrlMapper")
    @Mapping(target = "releaseYear", source = "movie.releaseYear")
    @Mapping(target = "movieTitle", ignore = true)
    RatingResponse toResponse(Rating rating);

    // Yeni dil destekli yerler için (UserService için)
    RatingResponse toResponse(Rating rating, @Context String lang);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "movie", ignore = true)
    Rating toEntity(RatingRequest request);


    @Named("posterUrlMapper")
    default String handlePosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isEmpty()) {
            return "https://via.placeholder.com/500x750?text=No+Poster+Available";
        }
        if (posterPath.startsWith("http")) {
            return posterPath;
        }
        // Başına TMDB URL'ini ekleyerek tam URL haline getiriyoruz
        return "https://image.tmdb.org/t/p/w500" + (posterPath.startsWith("/") ? "" : "/") + posterPath;
    }

    @AfterMapping
    default void setMovieTitle(@MappingTarget RatingResponse response, Rating rating, @Context String lang) {
        if (rating.getMovie() == null || rating.getMovie().getTranslations() == null) {
            return;
        }

        String title = rating.getMovie().getTranslations().stream()
                .filter(t -> t.getLanguageCode().equalsIgnoreCase(lang))
                .findFirst()
                .map(MovieTranslation::getTitle)
                .orElse(rating.getMovie().getOriginalTitle());

        response.setMovieTitle(title);
    }
}