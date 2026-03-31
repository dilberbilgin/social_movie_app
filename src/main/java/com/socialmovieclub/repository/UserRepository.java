package com.socialmovieclub.repository;

import com.socialmovieclub.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public interface UserRepository extends JpaRepository<User, UUID> {

    // Veritabanında kullanıcı adına göre arama yapmak için
    Optional<User> findByUsername(String username);

    //Kullanici adina gore arama(Case-insensitive)
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    //SOLID ve performans açısından Sayfalamaya (Page) ihtiyacımız olmayan, sadece hızlıca ilk birkaç sonucu getiren bir yapı
    List<User> findByUsernameContainingIgnoreCase(String username);

    // Veritabanında e-postaya göre arama yapmak için
    Optional<User> findByEmail(String email);

    //SELECT count(id) > 0 FROM users WHERE username = 'user1';
    // Bu kullanıcı adı zaten var mı kontrolü için
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Rastgele 5 kullanıcı getir (Kendisi hariç ve takip etmedikleri arasından)
    // Bu sorgu: "Beni takip etmeyen ve benim takip etmediğim kişileri getir" mantığıdır.
    @Query(value = "SELECT * FROM users u WHERE u.id != :currentUserId " +
            "AND u.id NOT IN (SELECT f.following_id FROM follows f WHERE f.follower_id = :currentUserId) " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<User> findSuggestedUsers(@Param("currentUserId") UUID currentUserId, @Param("limit") int limit);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :currentUserId AND f.following.id IN :targetIds")
    List<UUID> findFollowedIdsFromList(@Param("currentUserId") UUID currentUserId, @Param("targetIds") List<UUID> targetIds);
    }

    //todo : incele