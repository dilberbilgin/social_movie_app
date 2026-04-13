package com.socialmovieclub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class WatchProviderDto {
    @JsonProperty("provider_name") // TMDB'den gelen isimle eşle
    private String providerName;

    @JsonProperty("logo_path") // TMDB'den gelen isimle eşle
    private String logoPath;

    @JsonProperty("display_priority")
    private Integer displayPriority;

    @JsonProperty("provider_id")
    private String providerId;
}

