package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    // 'CreatedAt' yerine 'CreatedDate' (BaseEntity'deki değişken adın neyse o olmalı)
//    List<Comment> findByMovieIdAndParentIsNullAndDeletedFalseOrderByCreatedDateDesc(UUID movieId);

    // List yerine Page dönüyoruz
    Page<Comment> findByMovieIdAndParentIsNullAndDeletedFalseOrderByCreatedDateDesc(UUID movieId, Pageable pageable);
}