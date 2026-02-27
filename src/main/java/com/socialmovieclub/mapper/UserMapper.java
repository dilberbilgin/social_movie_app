package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.request.UserProfileUpdateRequest;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Request -> Entity
    User toEntity(UserRegistrationRequest request);

    // Entity -> Response
//    @Mapping(target = "isFollowing", ignore = true)
    UserResponse toResponse(User user);

    //Mevcut entity'i request'teki verilerle gunceller(null olmayanlar)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UserProfileUpdateRequest request, @MappingTarget User user);
}