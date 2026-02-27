package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowMapper {

    @Mapping(source = "follower.id", target = "id")
    @Mapping(source = "follower.username", target = "username")
    @Mapping(target = "following", ignore = true)
    UserResponse toFollowerResponse(Follow follow);

    @Mapping(source = "following.id", target = "id")
    @Mapping(source = "following.username", target = "username")
    @Mapping(target = "following", ignore = true)
    UserResponse toFollowingResponse(Follow follow);
}
