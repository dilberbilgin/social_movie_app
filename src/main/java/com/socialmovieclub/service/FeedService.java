package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.entity.Rating;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.RatingMapper;
import com.socialmovieclub.repository.RatingRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;
    private final UserRepository userRepository;

    // ŞU AN: Database Pull Stratejisi
    // return databaseFeedStrategy.getFeed(...);

    // GELECEKTE: Sadece burayı değiştirerek Redis Stratejisine geçebilirsin
    // return redisFeedStrategy.getFeed(...);

    //Redis veya Push modeli maliyetlidir. Sunucu kirası ve RAM kullanımı çok daha yüksektir. 100k kullanıcıya kadar şu anki Pageable ve Indexing yapımız yeterlo.
    // 1 milyona yaklasan projeler icin de bir butce olacagi icin Redis mimarisine gecilebilir.

    public RestResponse<Page<RatingResponse>> getHomeFeed(String lang, int page, int size) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Rating> ratings = ratingRepository.findFeed(currentUser.getId(), pageable);

        // Dil desteğini ekleyerek mapliyoruz
        Page<RatingResponse> response = ratings.map(rating -> {
            RatingResponse dto = ratingMapper.toResponse(rating);
            dto.setMovieTitle(getMovieTitleByLang(rating, lang));
            return dto;
        });

        return RestResponse.success(response, "Feed fetched");
    }

    private String getMovieTitleByLang(Rating rating, String lang) {
        if (rating.getMovie().getTranslations() == null) return rating.getMovie().getOriginalTitle();
        return rating.getMovie().getTranslations().stream()
                .filter(t -> t.getLanguageCode().equalsIgnoreCase(lang))
                .map(t -> t.getTitle())
                .findFirst()
                .orElse(rating.getMovie().getOriginalTitle());
    }
}