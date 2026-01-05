package com.socialmovieclub.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GenreCreateRequest {

    @NotBlank(message = "{genre.name.required}")
    private String name; // Örn: "ACTION"

    @NotEmpty(message = "{genre.translations.required}")
    @Valid
    private List<TranslationRequest> translations;
}