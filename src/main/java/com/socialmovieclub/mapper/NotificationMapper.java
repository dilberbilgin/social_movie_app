package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.response.NotificationResponse;
import com.socialmovieclub.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "actorUsername", source = "actor.username")
    @Mapping(target = "actorAvatar", source = "actor.profilePictureUrl")
    @Mapping(target = "message", ignore = true) // Strategy ile set edeceğiz
    NotificationResponse toResponse(Notification notification);
}