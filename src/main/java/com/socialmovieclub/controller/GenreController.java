package com.socialmovieclub.controller;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.request.GenreCreateRequest;
import com.socialmovieclub.dto.response.GenreResponse;
import com.socialmovieclub.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @PostMapping
    public RestResponse<GenreResponse> createGenre(
            @Valid @RequestBody GenreCreateRequest request,
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return genreService.createGenre(request, lang);
    }

    @GetMapping
    public RestResponse<List<GenreResponse>> getAllGenres(
            @RequestHeader(name = "Accept-Language", defaultValue = "en") String lang) {
        return genreService.getAllGenres(lang);
    }
}
