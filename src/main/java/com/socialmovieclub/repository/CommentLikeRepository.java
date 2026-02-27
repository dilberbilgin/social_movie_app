package com.socialmovieclub.repository;

import com.socialmovieclub.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    // Belirli bir kullanıcının belirli bir yoruma tepkisini bul (Toggle için)
    Optional<CommentLike> findByUserIdAndCommentId(UUID userId, UUID commentId);

    // Yorumun toplam Like ve Dislike sayılarını saymak için
    long countByCommentIdAndIsLikedTrue(UUID commentId);
    long countByCommentIdAndIsLikedFalse(UUID commentId);
}