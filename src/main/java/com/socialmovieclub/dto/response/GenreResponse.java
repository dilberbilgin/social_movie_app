package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenreResponse {

    private UUID id;
    private String name;
    private String title;
    private String description;
}
