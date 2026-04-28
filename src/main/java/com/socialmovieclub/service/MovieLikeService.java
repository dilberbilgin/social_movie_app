package com.socialmovieclub.service;

import com.socialmovieclub.core.constant.CacheConstants;
import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.MovieLike;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.ActivityType;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.repository.MovieLikeRepository;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieLikeService {
    private final MovieLikeRepository movieLikeRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final MessageHelper messageHelper;
    private final ActivityService activityService;
    private final SecurityService securityService;
    private final MovieService movieService;


    @Transactional
    @CacheEvict(value = {CacheConstants.FEED_CACHE, "movieDetails"}, allEntries = true)
    public RestResponse<UUID> handleMovieReaction(UUID movieId, Long tmdbId, boolean isLike, String contentType, String lang) {
        User user = securityService.getCurrentUser();

        // Filmi belirle (Varsa getir, yoksa TMDB'den çek/kaydet)
        Movie movie = (tmdbId != null)
                ? movieService.ensureMovieExists(tmdbId, contentType, lang)
                : movieRepository.findById(movieId).orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        Optional<MovieLike> existing = movieLikeRepository.findByUserIdAndMovieId(user.getId(), movie.getId());
        String messageKey;

        // 1. REAKSİYON İŞLEME (Like/Dislike/Remove)
        if (existing.isPresent()) {
            MovieLike like = existing.get();
            if (like.isLiked() == isLike) {
                movieLikeRepository.delete(like); // Aynı şeye tekrar bastı -> Kaldır
                messageKey = "movie.reaction.removed";
            } else {
                like.setLiked(isLike); // Like'tı Dislike oldu (veya tersi)
                movieLikeRepository.save(like);
                messageKey = isLike ? "movie.liked" : "movie.disliked";
            }
        } else {
            MovieLike newLike = new MovieLike(); // İlk kez reaksiyon veriyor
            newLike.setUser(user);
            newLike.setMovie(movie);
            newLike.setLiked(isLike);
            movieLikeRepository.save(newLike);
            messageKey = isLike ? "movie.liked" : "movie.disliked";
        }

        // 2. AKTİVİTE KAYDI (Sadece ilk kez beğenince veya beğenmeme durumundan beğeniye geçince)
        if (isLike && (existing.isEmpty() || !existing.get().isLiked())) {
            activityService.createActivity(user.getId(), ActivityType.MOVIE_LIKE,
                    movie.getId(), movie.getOriginalTitle(), movie.getPosterUrl(), movie.getOriginalTitle());
        }

        return RestResponse.success(movie.getId(), messageHelper.getMessage(messageKey));
    }
//    @Transactional
//    @CacheEvict(value = {CacheConstants.FEED_CACHE, "movieDetails"}, allEntries = true) // Cache'i temizler, sayı güncellenir. Not: allEntries = true yerine key = "#movieId" kullanarak sadece o filmin cache'ini silmek performansı daha da artırır.
//    public RestResponse<UUID> handleMovieReaction(UUID movieId, Long tmdbId, boolean isLike, String contentType,  String lang) {
//        User user = securityService.getCurrentUser();
//        Movie movie;
//        if (tmdbId != null) {
//            // Otomatik import veya mevcut olanı getir
//            movie = movieService.ensureMovieExists(tmdbId, contentType, lang);
//        } else {
//            // Sadece yerel DB'de ara
//            movie = movieRepository.findById(movieId)
//                    .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));
//        }
//      UUID finalMovieId = movie.getId();
//
//        Optional<MovieLike> existing = movieLikeRepository.findByUserIdAndMovieId(user.getId(), movie.getId());
//        String messageKey;
//
//        if (existing.isPresent()) {
//            MovieLike like = existing.get();
//            if (like.isLiked() == isLike) {
//                movieLikeRepository.delete(like);
//                messageKey = "movie.reaction.removed";
//            } else {
//                like.setLiked(isLike);
//                movieLikeRepository.save(like);
//                messageKey = isLike ? "movie.liked" : "movie.disliked";
//
//                // AKTİVİTE KAYDI: Kullanıcı fikrini değiştirdi (beğendi)
//                if (isLike) {
//                    activityService.createActivity(user.getId(), ActivityType.MOVIE_LIKE, movie.getId(), movie.getOriginalTitle(), movie.getPosterUrl(), movie.getOriginalTitle());
//                }
//            }
//        } else {
//            MovieLike newLike = new MovieLike();
//            newLike.setUser(user);
//            newLike.setMovie(movie);
//            newLike.setLiked(isLike);
//            movieLikeRepository.save(newLike);
//            messageKey = isLike ? "movie.liked" : "movie.disliked";
//
//            // AKTİVİTE KAYDI: İlk kez beğendi
//            if (isLike) {
//                activityService.createActivity(user.getId(), ActivityType.MOVIE_LIKE, movie.getId(), movie.getOriginalTitle(), movie.getPosterUrl(), movie.getOriginalTitle());
//            }
//        }
//
//        return RestResponse.success(finalMovieId, messageHelper.getMessage(messageKey));
//    }
}