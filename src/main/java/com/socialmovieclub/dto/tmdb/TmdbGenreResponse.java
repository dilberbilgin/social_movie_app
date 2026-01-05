package com.socialmovieclub.dto.tmdb;

import lombok.Data;

import java.util.List;

// TMDB kategorileri liste olarak döndüğü için bir de Wrapper (Sarmalayıcı) lazım
@Data
public class TmdbGenreResponse {
    private List<TmdbGenreDto> genres;
}
