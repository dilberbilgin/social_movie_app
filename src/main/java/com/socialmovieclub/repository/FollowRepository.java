package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Follow;
import com.socialmovieclub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    // Takipçi sayısını bul
    long countByFollowingId(UUID userId);

    // Takip edilen sayısını bul
    long countByFollowerId(UUID userId);

    // Takip kontrolü
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

// Bu sorgu veritabanı seviyesinde kontrol sağlar
    boolean existsByFollowerAndFollowing(User follower, User following);

    // Takipten çıkmak için kaydı bul
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    //Takipcileri getir
    Page<Follow> findAllByFollowingId(UUID userId, Pageable pageable);

    //Takip edilenleri getir
    Page<Follow> findAllByFollowerId(UUID userId, Pageable pageable);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :currentUserId AND f.following.id IN :targetIds")
    List<UUID> findFollowedIds(@Param("currentUserId") UUID currentUserId, @Param("targetIds") List<UUID> targetIds);
}