package com.socialmovieclub.mapper;

import com.socialmovieclub.dto.request.CommentRequest;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.entity.Comment;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CommentMapper {

//    @Mapping(target = "username", source = "user.username")
//        // MapStruct burada replies listesini görürse, otomatik olarak bu metodun
//        // kendisini (recursive) çağırarak tüm alt yorumları da dönüştürür.
//    CommentResponse toResponse(Comment comment);
//
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "user", ignore = true)
//    @Mapping(target = "movie", ignore = true)
//    @Mapping(target = "parent", ignore = true)
//    @Mapping(target = "replies", ignore = true)
//    Comment toEntity(CommentRequest request);

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "likeCount", ignore = true)     // Service dolduracak
    @Mapping(target = "dislikeCount", ignore = true)  // Service dolduracak
    @Mapping(target = "userReaction", ignore = true)  // Service dolduracak
    CommentResponse toResponse(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Comment toEntity(CommentRequest request);



}