package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.response.WeeklyWinnerResponse;
import com.socialmovieclub.entity.WeeklyWinner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeeklyWinnerMapper {

    @Mapping(target = "commentId", source = "comment.id")
    @Mapping(target = "commentContent", source = "comment.content")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePictureUrl", source = "user.profilePictureUrl")
    @Mapping(target = "movieTitle", source = "movie.originalTitle")
    @Mapping(target = "moviePosterUrl", source = "movie.posterUrl")
    WeeklyWinnerResponse toResponse(WeeklyWinner entity);
}