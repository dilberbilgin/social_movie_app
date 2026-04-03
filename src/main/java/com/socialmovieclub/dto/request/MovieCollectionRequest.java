package com.socialmovieclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovieCollectionRequest(
        @NotBlank(message = "{collection.name.required}")
        String name,

        @Size(max = 500)
        String description,

        boolean isPublic
) {}