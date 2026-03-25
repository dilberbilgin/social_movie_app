package com.socialmovieclub.repository;

import com.socialmovieclub.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    // Takip edilen kullanıcıların ID listesine göre aktiviteleri getir (Zaman sırasıyla)
    // BaseEntity kullandığımız için createdDate üzerinden sıralayabiliyoruz.
    Page<Activity> findByUserIdInOrderByCreatedDateDesc(List<UUID> userIds, Pageable pageable);

    // Belirli bir kullanıcının kendi aktivitelerini getir (Profil sayfası için gerekebilir)
    Page<Activity> findByUserIdOrderByCreatedDateDesc(UUID userId, Pageable pageable);

//    // Sadece takip ettiğim kişilerin aktivitelerini getir (Instagram/Twitter mantığı)
//    @Query("SELECT a FROM Activity a WHERE a.user.id IN " +
//            "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :currentUserId) " +
//            "ORDER BY a.createdDate DESC")
//    Page<Activity> findFollowingActivities(UUID currentUserId, Pageable pageable);
}