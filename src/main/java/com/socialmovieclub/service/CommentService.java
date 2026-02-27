package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.CommentRequest;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.entity.Comment;
import com.socialmovieclub.entity.CommentLike;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.CommentMapper;
import com.socialmovieclub.repository.CommentLikeRepository;
import com.socialmovieclub.repository.CommentRepository;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final MessageHelper messageHelper; // Eksik olan parça eklendi
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public RestResponse<CommentResponse> createComment(CommentRequest request) {
        // 1. Giriş yapmış kullanıcıyı bul
        User user = getCurrentUser();
        // 2. Filmi kontrol et
        Movie movie = getMovieOrThrow(request.getMovieId());

        // 3. Entity oluştur
        Comment comment = commentMapper.toEntity(request);
        comment.setUser(user);
        comment.setMovie(movie);

        // 4. Eğer bir alt yorumsa parent'ı set et
        if (request.getParentId() != null) {
            handleParentComment(comment, request.getParentId());
        }

        Comment savedComment = commentRepository.save(comment);
        String successMsg = messageHelper.getMessage("comment.create.success");
        return RestResponse.success(commentMapper.toResponse(savedComment), successMsg);
    }



//    public RestResponse<Page<CommentResponse>> getMovieComments(UUID movieId, Pageable pageable) {
//        // 1. Ana yorumları (top-level) sayfalı olarak çek
//        Page<Comment> commentPage = commentRepository.findByMovieIdAndParentIsNullAndDeletedFalseOrderByCreatedDateDesc(movieId, pageable);
//
//        // 2. Page nesnesini DTO'ya map et
//        Page<CommentResponse> response = commentPage.map(commentMapper::toResponse);
//
//        return RestResponse.success(response);
//    }

    public RestResponse<Page<CommentResponse>> getMovieComments(UUID movieId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByMovieIdAndParentIsNullAndDeletedFalseOrderByCreatedDateDesc(movieId, pageable);

        Page<CommentResponse> response = commentPage.map(comment -> {
            CommentResponse dto = commentMapper.toResponse(comment);
            // Recursive olarak bu yorumu ve tüm alt yanıtlarını zenginleştir
            enrichWithLikesRecursive(dto);
            return dto;
        });

        return RestResponse.success(response);
    }

    @Transactional
    public RestResponse<Void> deleteComment(UUID commentId) {
        User currentUser = getCurrentUser();
        Comment comment = getCommentOrThrow(commentId); // Yeni helper kullanıldı

        // Güvenlik: Nesne odaklı kontrol
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(messageHelper.getMessage("auth.access.denied"));
        }
        // Soft Delete
        comment.setDeleted(true);
        // "Bu yorum silindi" mesajını da dilden bağımsız hale getirebiliriz:
        comment.softDelete(messageHelper.getMessage("comment.content.deleted"));
        commentRepository.save(comment);

        return RestResponse.success(null, messageHelper.getMessage("comment.delete.success"));
    }

    // --- Private Helper Methods (Clean Code & Reusability) ---

    private Comment getCommentOrThrow(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("comment.not.found")));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));
    }

    private Movie getMovieOrThrow(UUID movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));
    }

    private void handleParentComment(Comment child, UUID parentId) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("comment.parent.not.found")));

        // Instagram Mantığı: Sadece 2 seviyeli derinlik (Threaded değil, Nested)
        // Eğer parent'ın da bir parent'ı varsa, bu cevabı ana parent'a bağla.
        // Böylece yorumlar çok fazla sağa kaymaz.
        if (parent.getParent() != null) {
            child.setParent(parent.getParent());
        } else {
            child.setParent(parent);
        }
    }

    // Hem yorumun kendisini hem de tüm alt yanıtlarını zenginleştirir
    private void enrichWithLikesRecursive(CommentResponse dto) {
        // 1. Mevcut yorumu zenginleştir

        dto.setLikeCount(commentLikeRepository.countByCommentIdAndIsLikedTrue(dto.getId()));
        dto.setDislikeCount(commentLikeRepository.countByCommentIdAndIsLikedFalse(dto.getId()));

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username != null && !username.equals("anonymousUser")) {
                userRepository.findByUsername(username).ifPresent(user -> {
                    commentLikeRepository.findByUserIdAndCommentId(user.getId(), dto.getId())
                            .ifPresent(like -> dto.setUserReaction(like.isLiked()));
                });
            }
        } catch (Exception e) { /* Login değilse geç */ }

        // 2. Eğer alt yanıtlar (replies) varsa, onları da tek tek zenginleştir
        if (dto.getReplies() != null && !dto.getReplies().isEmpty()) {
            dto.getReplies().forEach(this::enrichWithLikesRecursive);
        }
    }

    @Transactional
    public RestResponse<Void> handleCommentReaction(UUID commentId, boolean isLike) {
        User currentUser = getCurrentUser();
        Comment comment = getCommentOrThrow(commentId);

        Optional<CommentLike> existingLike = commentLikeRepository.findByUserIdAndCommentId(currentUser.getId(), commentId);

        String messageKey;

        if (existingLike.isPresent()) {
            CommentLike like = existingLike.get();
            if (like.isLiked() == isLike) {
                commentLikeRepository.delete(like);
                messageKey = "comment.reaction.removed";
            } else {
                like.setLiked(isLike);
                commentLikeRepository.save(like);
                messageKey = isLike ? "comment.liked" : "comment.disliked";
            }
        } else {
            CommentLike newLike = new CommentLike();
            newLike.setUser(currentUser);
            newLike.setComment(comment);
            newLike.setLiked(isLike);
            commentLikeRepository.save(newLike);
            messageKey = isLike ? "comment.liked" : "comment.disliked";
        }

        return RestResponse.success(null, messageHelper.getMessage(messageKey));
    }
}
