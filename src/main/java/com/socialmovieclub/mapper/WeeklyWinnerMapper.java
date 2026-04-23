package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.response.WeeklyWinnerResponse;
import com.socialmovieclub.entity.WeeklyWinner;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeeklyWinnerMapper {

    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "commentId", source = "comment.id")
    @Mapping(target = "commentContent", source = "comment.content")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePictureUrl", source = "user.profilePictureUrl")
    @Mapping(target = "movieTitle", source = "movie.originalTitle")
    @Mapping(target = "moviePosterUrl", source = "movie.posterUrl")
    @Mapping(target = "clubRating", source = "movie.clubRating")
    @Mapping(target = "clubVoteCount", source = "movie.clubVoteCount")
    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "tmdbId", source = "movie.tmdbId")

    @Mapping(target = "movieLikeCount", ignore = true)
    @Mapping(target = "movieDislikeCount", ignore = true)
    @Mapping(target = "userReaction", ignore = true)
    WeeklyWinnerResponse toResponse(WeeklyWinner entity);
}