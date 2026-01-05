package com.socialmovieclub.dto.tmdb;

import lombok.Data;

import java.util.List;

@Data
public class TmdbGenreDto {
    private Long id;   // TMDB'nin verdiği 28, 12 gibi ID'ler
    private String name; // TMDB'nin verdiği "Action" gibi isimler
}