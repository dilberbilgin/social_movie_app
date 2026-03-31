package com.socialmovieclub.repository;

import com.socialmovieclub.entity.MovieTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.function.ToDoubleBiFunction;

@Repository
public interface MovieTranslationRepository extends JpaRepository<MovieTranslation, UUID> {

    // Artık movieId bir UUID olduğu için, Spring Data JPA bunu otomatik eşleştirecektir.
    Optional<MovieTranslation> findByMovieIdAndLanguageCode(UUID movieId, String languageCode);
}

