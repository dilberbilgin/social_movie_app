package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.entity.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "movieId", source = "movie.id")
    RatingResponse toResponse(Rating rating);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "movie", ignore = true)
    Rating toEntity(RatingRequest request);
}