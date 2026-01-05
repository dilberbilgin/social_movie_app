package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
//    private Long id;
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}