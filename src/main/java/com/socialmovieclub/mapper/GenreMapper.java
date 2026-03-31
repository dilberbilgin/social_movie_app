package com.socialmovieclub.mapper;


import com.socialmovieclub.dto.request.GenreCreateRequest;
import com.socialmovieclub.dto.request.TranslationRequest;
import com.socialmovieclub.dto.response.GenreResponse;
import com.socialmovieclub.entity.Genre;
import com.socialmovieclub.entity.GenreTranslation;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring" )
public interface GenreMapper {

    // 1. Request -> Entity (Kategoriyi oluştururken)
    Genre toEntity(GenreCreateRequest request);
    // 2. Child Mapping: MapStruct TranslationRequest listesini GenreTranslation'a çevirirken bunu kullanır
    GenreTranslation toEntity(TranslationRequest request);

    // 3. İlişkiyi Bağlama: Her bir çeviriye "Senin kategorin budur" diyoruz.
    @AfterMapping
    default void linkTranslations(@MappingTarget Genre genre) {
        if (genre.getTranslations() != null) {
            genre.getTranslations().forEach(t -> t.setGenre(genre));
        }
    }

    // 4. Entity -> Response (Tekil)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "description", ignore = true)
    GenreResponse toResponse(Genre genre, @Context String languageCode);

    List<GenreResponse> toResponseList(List<Genre> genres, @Context String languageCode);

    @AfterMapping
    default void setLanguageFields(@MappingTarget GenreResponse response, Genre genre, @Context String languageCode) {
        if (genre.getTranslations() != null) {
            genre.getTranslations().stream()
            // @Context ile gelen dili burada kullanıyoruz:
                    .filter(t ->t.getLanguageCode().equalsIgnoreCase(languageCode))
                    .findFirst()
                    .ifPresent(t -> {
                        // Ignore ettiğimiz alanları burada manuel dolduruyoruz:
                        response.setTitle(t.getTitle());
                        response.setDescription(t.getDescription());
                    });
        }
    }
}
