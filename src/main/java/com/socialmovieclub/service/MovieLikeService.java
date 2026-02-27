package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.MovieLike;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.repository.MovieLikeRepository;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Transactional
    public RestResponse<Void> handleMovieReaction(UUID movieId, boolean isLike) {
        User user = getCurrentUser();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        Optional<MovieLike> existing = movieLikeRepository.findByUserIdAndMovieId(user.getId(), movie.getId());
        String messageKey;

        if (existing.isPresent()) {
            MovieLike like = existing.get();
            if (like.isLiked() == isLike) {
                movieLikeRepository.delete(like);
                messageKey = "movie.reaction.removed";
            } else {
                like.setLiked(isLike);
                movieLikeRepository.save(like);
                messageKey = isLike ? "movie.liked" : "movie.disliked";
            }
        } else {
            MovieLike newLike = new MovieLike();
            newLike.setUser(user);
            newLike.setMovie(movie);
            newLike.setLiked(isLike);
            movieLikeRepository.save(newLike);
            messageKey = isLike ? "movie.liked" : "movie.disliked";
        }

        return RestResponse.success(null, messageHelper.getMessage(messageKey));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));
    }
}