package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.GenreCreateRequest;
import com.socialmovieclub.dto.response.GenreResponse;
import com.socialmovieclub.entity.Genre;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.GenreMapper;
import com.socialmovieclub.repository.GenreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.socialmovieclub.core.result.RestResponse.success;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;
    private final MessageHelper messageHelper;

    @Transactional
    public RestResponse<GenreResponse> createGenre(GenreCreateRequest request, String lang) {
        if (genreRepository.existsByName(request.getName())) {
            throw new BusinessException(messageHelper.getMessage("genre.already.exists"));
        }

        Genre genre = genreMapper.toEntity(request);
        Genre savedGenre = genreRepository.save(genre);
        GenreResponse responseData = genreMapper.toResponse(savedGenre, lang );

        return success(responseData, messageHelper.getMessage("genre.create.success"));
    }

    public RestResponse<List<GenreResponse>> getAllGenres(String lang) {
        List<Genre> genres = genreRepository.findAll();
        List<GenreResponse> responseData = genreMapper.toResponseList(genres, lang );

        return success(responseData);
    }


}
