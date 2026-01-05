package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.CommentRequest;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.entity.Comment;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.CommentMapper;
import com.socialmovieclub.repository.CommentRepository;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Transactional
    public RestResponse<CommentResponse> createComment(CommentRequest request) {
        // 1. Giriş yapmış kullanıcıyı bul
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));

        // 2. Filmi kontrol et
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        // 3. Entity oluştur
        Comment comment = commentMapper.toEntity(request);
        comment.setUser(user);
        comment.setMovie(movie);

        // 4. Eğer bir alt yorumsa parent'ı set et
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(messageHelper.getMessage("comment.parent.not.found")));
            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);
        String successMsg = messageHelper.getMessage("comment.create.success");

        return RestResponse.success(commentMapper.toResponse(savedComment), successMsg);
    }

    public RestResponse<List<CommentResponse>> getMovieComments(UUID movieId) {
        List<Comment> comments = commentRepository.findByMovieIdAndParentIsNullAndDeletedFalseOrderByCreatedDateDesc(movieId);
        List<CommentResponse> response = comments.stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
        return RestResponse.success(response);
    }

    @Transactional
    public RestResponse<Void> deleteComment(UUID commentId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("comment.not.found")));

        // Güvenlik Kontrolü: Yorumu sadece sahibi silebilir
        if (!comment.getUser().getUsername().equals(currentUsername)) {
            throw new BusinessException(messageHelper.getMessage("auth.access.denied"));
        }

        // Soft Delete
        comment.setDeleted(true);
        // "Bu yorum silindi" mesajını da dilden bağımsız hale getirebiliriz:
        comment.setContent(messageHelper.getMessage("comment.content.deleted"));

        commentRepository.save(comment);
        return RestResponse.success(null, messageHelper.getMessage("comment.delete.success"));
    }
}