package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.CommentRequest;
import com.socialmovieclub.dto.response.CommentResponse;
import com.socialmovieclub.entity.Comment;
import com.socialmovieclub.entity.CommentLike;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.ActivityType;
import com.socialmovieclub.enums.NotificationType;
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

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final MessageHelper messageHelper; // Eksik olan parça eklendi
    private final CommentLikeRepository commentLikeRepository;
    private final ActivityService activityService;
    private final SecurityService securityService;
    private final NotificationService notificationService;

    @Transactional
    public RestResponse<CommentResponse> createComment(CommentRequest request) {
        User user = securityService.getCurrentUser();
        Movie movie = getMovieOrThrow(request.getMovieId());

        Comment comment = commentMapper.toEntity(request);
        comment.setUser(user);
        comment.setMovie(movie);

        // 1. Önce Parent ilişkisini kur
        if (request.getParentId() != null) {
            handleParentComment(comment, request.getParentId());
        }

        // 2. Yorumu Kaydet (ID ve diğer alanlar kesinleşsin)
        Comment savedComment = commentRepository.save(comment);

        // 3. EĞER BİR YANITSA BİLDİRİM GÖNDER
        if (savedComment.getParent() != null) {
            Comment parentComment = savedComment.getParent();

            // Kendi yorumuna yanıt veriyorsa bildirim gitmesin
            if (!parentComment.getUser().getId().equals(user.getId())) {
                String originalCommentPreview = parentComment.getContent().length() > 20
                        ? parentComment.getContent().substring(0, 17) + "..."
                        : parentComment.getContent();

                notificationService.createNotification(
                        parentComment.getUser(),
                        user,
                        NotificationType.COMMENT_REPLY,
                        movie.getId(),
                      //  movie.getOriginalTitle() // Stratejiye targetTitle olarak gidecek
                        // Burada movie.getOriginalTitle() yerine asıl yorumun içeriğini gönderiyoruz
                        parentComment.getContent().length() > 30 ? parentComment.getContent().substring(0, 27) + "..." : parentComment.getContent()
                );
            }
        }

        // AKTİVİTE KAYDI: Kullanıcı yorum yaptı
        // İçerik olarak yorumun ilk 50 karakterini saklıyoruz (Feed'de önizleme için)
        String preview = comment.getContent().length() > 50
                ? comment.getContent().substring(0, 47) + "..."
                : comment.getContent();
        activityService.createActivity(user.getId(), ActivityType.COMMENT_CREATE, movie.getId(), preview,movie.getPosterUrl());

        String successMsg = messageHelper.getMessage("comment.create.success");
        return RestResponse.success(commentMapper.toResponse(savedComment), successMsg);
        //return RestResponse.success(commentMapper.toResponse(savedComment), messageHelper.getMessage("comment.create.success"));
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
    public RestResponse<Void> handleCommentReaction(UUID commentId, boolean isLike) {
        User currentUser = securityService.getCurrentUser();
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

            // --- BİLDİRİM VE AKTİVİTE: İlk kez beğenildiğinde ---
            if (isLike) {
                // Bildirim Gönder (Yorum sahibine)
                notificationService.createNotification(
                        comment.getUser(),
                        currentUser,
                        NotificationType.COMMENT_LIKE,
                        comment.getMovie().getId(),
                       // comment.getContent()
                        comment.getContent().length() > 30 ? comment.getContent().substring(0, 27) + "..." : comment.getContent()
                );
                // Aktivite Oluştur (Takipçilere)
                String commentPreview = comment.getContent().length() > 30 ? comment.getContent().substring(0, 27) + "..." : comment.getContent();
                activityService.createActivity(currentUser.getId(), ActivityType.COMMENT_LIKE, comment.getMovie().getId(), commentPreview, comment.getMovie().getPosterUrl());
            }

            /// --- KRİTİK NOKTA: AKTİVİTE KAYDI ---
            // Sadece yeni bir beğeni atıldığında kayıt oluşturuyoruz
//            if (isLike) {
//                String content = comment.getUser().getUsername() + "'s comment";
//                // user nesnesi yerine id gönderiyoruz
//                activityService.createActivity(currentUser.getId(), ActivityType.COMMENT_LIKE, commentId, content);
//            }
            // --- KRİTİK NOKTA: AKTİVİTE KAYDI ---
//            if (isLike) {
//                // Sabit metin yerine yorumun ilk 30 karakterini önizleme olarak alalım
//                String commentPreview = comment.getContent().length() > 30
//                        ? comment.getContent().substring(0, 27) + "..."
//                        : comment.getContent();
//
//                // "deneme2 'Harika bir film!' yorumunu beğendi" şeklinde görünmesi için:
//                activityService.createActivity(
//                        currentUser.getId(),
//                        ActivityType.COMMENT_LIKE,
//                        comment.getMovie().getId(), // targetId olarak Film ID'si gönderiyoruz ki tıklayınca filme gitsin
//                        commentPreview,
//                        comment.getMovie().getPosterUrl()
//                );
//            }
        }

        return RestResponse.success(null, messageHelper.getMessage(messageKey));
    }

    @Transactional
    public RestResponse<Void> deleteComment(UUID commentId) {
//        User currentUser = getCurrentUser();
        User currentUser = securityService.getCurrentUser();
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

//    private User getCurrentUser() {
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//        return userRepository.findByUsername(username)
//                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));
//    }

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
        // 1. Beğeni sayılarını her zaman set et (Login olmasa da görünür)
        dto.setLikeCount(commentLikeRepository.countByCommentIdAndIsLikedTrue(dto.getId()));
        dto.setDislikeCount(commentLikeRepository.countByCommentIdAndIsLikedFalse(dto.getId()));

        // 2. Kullanıcı giriş yapmışsa reaksiyonunu set et
        securityService.getUserIfLoggedIn().ifPresent(user -> {
            commentLikeRepository.findByUserIdAndCommentId(user.getId(), dto.getId())
                    .ifPresent(like -> dto.setUserReaction(like.isLiked()));
        });

        // 3. Alt yorumları gez
        if (dto.getReplies() != null && !dto.getReplies().isEmpty()) {
            dto.getReplies().forEach(this::enrichWithLikesRecursive);
        }
    }

}

//package com.socialmovieclub.service;
//
//import com.socialmovieclub.core.result.RestResponse;
//import com.socialmovieclub.core.utils.MessageHelper;
//import com.socialmovieclub.dto.request.CommentRequest;
//import com.socialmovieclub.dto.response.CommentResponse;
//import com.socialmovieclub.entity.Comment;
//import com.socialmovieclub.entity.CommentLike;
//import com.socialmovieclub.entity.Movie;
//import com.socialmovieclub.entity.User;
//import com.socialmovieclub.enums.ActivityType;
//import com.socialmovieclub.enums.NotificationType;
//import com.socialmovieclub.exception.BusinessException;
//import com.socialmovieclub.mapper.CommentMapper;
//import com.socialmovieclub.repository.CommentLikeRepository;
//import com.socialmovieclub.repository.CommentRepository;
//import com.socialmovieclub.repository.MovieRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class CommentService {
//
//    private final CommentRepository commentRepository;
//    private final MovieRepository movieRepository;
//    private final CommentMapper commentMapper;
//    private final MessageHelper messageHelper;
//    private final CommentLikeRepository commentLikeRepository;
//    private final ActivityService activityService;
//    private final SecurityService securityService;
//    private final NotificationService notificationService;
//
//    @Transactional
//    public RestResponse<CommentResponse> createComment(CommentRequest request) {
//        // 1. Kullanıcı ve Film Bulma
//        User user = securityService.getCurrentUser();
//        Movie movie = getMovieOrThrow(request.getMovieId());
//
//        // 2. Yorum Nesnesi Hazırlama
//        Comment comment = commentMapper.toEntity(request);
//        comment.setUser(user);
//        comment.setMovie(movie);
//
//        // 3. Eğer bir yanıtsa (Reply) parent ilişkisini kur
//        if (request.getParentId() != null) {
//            handleParentComment(comment, request.getParentId());
//        }
//
//        // 4. Veritabanına Kaydet
//        Comment savedComment = commentRepository.save(comment);
//
//        // 5. BİLDİRİM: Eğer bu bir yanıtsa, asıl yorum sahibine bildir
//        if (savedComment.getParent() != null) {
//            Comment parentComment = savedComment.getParent();
//            // Kişi kendi yorumuna yanıt vermiyorsa bildirim gönder
//            if (!parentComment.getUser().getId().equals(user.getId())) {
//                notificationService.createNotification(
//                        parentComment.getUser(),
//                        user,
//                        NotificationType.COMMENT_REPLY,
//                        movie.getId(),
//                        movie.getOriginalTitle()
//                );
//            }
//        }
//
//        // 6. AKTİVİTE: Feed'de görünmesi için (Takipçiler için)
//        String preview = comment.getContent().length() > 50
//                ? comment.getContent().substring(0, 47) + "..."
//                : comment.getContent();
//        activityService.createActivity(user.getId(), ActivityType.COMMENT_CREATE, movie.getId(), preview, movie.getPosterUrl());
//
//        return RestResponse.success(commentMapper.toResponse(savedComment), messageHelper.getMessage("comment.create.success"));
//    }
//
//    @Transactional(readOnly = true)
//    public RestResponse<Page<CommentResponse>> getMovieComments(UUID movieId, Pageable pageable) {
//        // Ana yorumları çek ve her birini (varsa yanıtlarıyla birlikte) zenginleştir
//        Page<CommentResponse> response = commentRepository
//                .findByMovieIdAndParentIsNullAndDeletedFalseOrderByCreatedDateDesc(movieId, pageable)
//                .map(comment -> {
//                    CommentResponse dto = commentMapper.toResponse(comment);
//                    enrichWithLikesRecursive(dto);
//                    return dto;
//                });
//
//        return RestResponse.success(response);
//    }
//
//    @Transactional
//    public RestResponse<Void> handleCommentReaction(UUID commentId, boolean isLike) {
//        User currentUser = securityService.getCurrentUser();
//        Comment comment = getCommentOrThrow(commentId);
//
//        Optional<CommentLike> existingLike = commentLikeRepository.findByUserIdAndCommentId(currentUser.getId(), commentId);
//        String messageKey;
//
//        if (existingLike.isPresent()) {
//            CommentLike like = existingLike.get();
//            if (like.isLiked() == isLike) {
//                commentLikeRepository.delete(like);
//                messageKey = "comment.reaction.removed";
//            } else {
//                like.setLiked(isLike);
//                commentLikeRepository.save(like);
//                messageKey = isLike ? "comment.liked" : "comment.disliked";
//            }
//        } else {
//            CommentLike newLike = new CommentLike();
//            newLike.setUser(currentUser);
//            newLike.setComment(comment);
//            newLike.setLiked(isLike);
//            commentLikeRepository.save(newLike);
//            messageKey = isLike ? "comment.liked" : "comment.disliked";
//
//            // Bildirim Gönder (Beğenilince yorum sahibine)
//            if (isLike) {
//                notificationService.createNotification(
//                        comment.getUser(),
//                        currentUser,
//                        NotificationType.COMMENT_LIKE,
//                        comment.getMovie().getId(),
//                        comment.getContent()
//                );
//            }
//        }
//
//        return RestResponse.success(null, messageHelper.getMessage(messageKey));
//    }
//
//    @Transactional
//    public RestResponse<Void> deleteComment(UUID commentId) {
//        User currentUser = securityService.getCurrentUser();
//        Comment comment = getCommentOrThrow(commentId);
//
//        if (!comment.getUser().getId().equals(currentUser.getId())) {
//            throw new BusinessException(messageHelper.getMessage("auth.access.denied"));
//        }
//
//        comment.setDeleted(true);
//        comment.softDelete(messageHelper.getMessage("comment.content.deleted"));
//        commentRepository.save(comment);
//
//        return RestResponse.success(null, messageHelper.getMessage("comment.delete.success"));
//    }
//
//    // --- Helper Methods ---
//
//    private void handleParentComment(Comment child, UUID parentId) {
//        Comment parent = getCommentOrThrow(parentId);
//        // Instagram stili: Sadece 2 derinlik olsun diye parent'ın parent'ı varsa en üste bağla
//        child.setParent(parent.getParent() != null ? parent.getParent() : parent);
//    }
//
//    private void enrichWithLikesRecursive(CommentResponse dto) {
//        dto.setLikeCount(commentLikeRepository.countByCommentIdAndIsLikedTrue(dto.getId()));
//        dto.setDislikeCount(commentLikeRepository.countByCommentIdAndIsLikedFalse(dto.getId()));
//
//        securityService.getUserIfLoggedIn().ifPresent(user -> {
//            commentLikeRepository.findByUserIdAndCommentId(user.getId(), dto.getId())
//                    .ifPresent(like -> dto.setUserReaction(like.isLiked()));
//        });
//
//        if (dto.getReplies() != null) {
//            dto.getReplies().forEach(this::enrichWithLikesRecursive);
//        }
//    }
//
//    private Comment getCommentOrThrow(UUID commentId) {
//        return commentRepository.findById(commentId)
//                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("comment.not.found")));
//    }
//
//    private Movie getMovieOrThrow(UUID movieId) {
//        return movieRepository.findById(movieId)
//                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));
//    }
//}
