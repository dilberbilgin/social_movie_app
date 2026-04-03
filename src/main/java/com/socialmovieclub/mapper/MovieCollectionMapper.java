package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.response.MovieCollectionResponse;
import com.socialmovieclub.entity.MovieCollection;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovieCollectionMapper {

//    @Mapping(target = "movieCount", expression = "java(collection.getMovies() != null ? collection.getMovies().size() : 0)")
//    @Mapping(target = "ownerUsername", source = "user.username")
//    @Mapping(target = "coverImageUrl", expression = "java(collection.getMovies() != null && !collection.getMovies().isEmpty() ? collection.getMovies().iterator().next().getPosterUrl() : null)")
//    MovieCollectionResponse toResponse(MovieCollection collection);
//}

    @Mapping(target = "movieCount", expression = "java(collection.getMovies() != null ? collection.getMovies().size() : 0)")
    @Mapping(target = "ownerUsername", source = "collection.user.username")
    @Mapping(target = "coverImageUrl", expression = "java(collection.getMovies() != null && !collection.getMovies().isEmpty() ? collection.getMovies().iterator().next().getPosterUrl() : null)")
    // Koleksiyon içindeki filmleri de map'lemek istersen (opsiyonel)
    @Mapping(target = "movies", source = "collection.movies")
    MovieCollectionResponse toResponse(MovieCollection collection, @Context String lang);
}