package com.socialmovieclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieWatchProvidersResponse {
    private List<WatchProviderDto> flatrate = new ArrayList<>(); // Abonelik (Netflix vb.)
    private List<WatchProviderDto> rent = new ArrayList<>();     // Kiralama
    private List<WatchProviderDto> buy = new ArrayList<>();      // Satın Alma
}