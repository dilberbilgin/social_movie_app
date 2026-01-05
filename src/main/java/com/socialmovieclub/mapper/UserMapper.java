package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Request -> Entity
    User toEntity(UserRegistrationRequest request);

    // Entity -> Response
    UserResponse toResponse(User user);
}