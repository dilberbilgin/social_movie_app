package com.socialmovieclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TranslationRequest {

    @NotBlank(message = "{translation.languageCode.required}")
    private String languageCode;

    @NotBlank(message = "{translation.title.required}")
    private String title;

    private String description;
}
