package com.socialmovieclub.service;

import com.socialmovieclub.core.constant.CacheConstants;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.entity.Follow;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.ActivityType;
import com.socialmovieclub.enums.NotificationType;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.FollowMapper;
import com.socialmovieclub.repository.FollowRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MessageHelper messageHelper;
    private final FollowMapper followMapper;
    private final ActivityService activityService;
    private final SecurityService securityService;
    private final NotificationService notificationService;

    @Transactional
    @CacheEvict(value = CacheConstants.FEED_CACHE, allEntries = true)
    public void followUser(UUID followingId, String currentUsername) {
        User follower = securityService.getCurrentUser();

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new BusinessException("Target user not found"));
        //todo : buradaki mesajlari solide uygun duzenle

        // 1. Kendi kendini takip edemezsin
        if (follower.getId().equals(following.getId())) {
            throw new BusinessException(messageHelper.getMessage("follow.self"));
        }

        // 2. Zaten takip ediyor mu? (Daha önce varsa hata verme, sessizce kal)
        if (followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) {
            return;
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);

        // --- BİLDİRİM TETİKLEME ---
        notificationService.createNotification(
                following,           // Alıcı: Takip edilen kişi
                follower,            // Aktör: Takip eden kişi
                NotificationType.FOLLOW,
                follower.getId(),    // Tıklayınca takip edenin profiline gitsi
                null,
                follower.getUsername()
        );

        // AKTİVİTE KAYDI: X kullanıcısı Y kullanıcısını takip etti
        activityService.createActivity(follower.getId(), ActivityType.FOLLOW_USER, followingId, following.getUsername(), follower.getProfilePictureUrl(), following.getUsername());
    }

    public Page<UserResponse> getFollowers(UUID userId, Pageable pageable) {
        Optional<User> currentUser = securityService.getUserIfLoggedIn();

        // 1. Sayfalanmış Follow kayıtlarını çek
        Page<Follow> followersPage = followRepository.findAllByFollowingId(userId, pageable);
        if (followersPage.isEmpty()) return Page.empty();

        // 2. Sadece BU sayfadaki kullanıcıların ID'lerini topla (Maksimum 'size' kadar, örn: 20)
        List<UUID> followerIds = followersPage.getContent().stream()
                .map(f -> f.getFollower().getId())
                .toList();

        // Toplu kontrol
        Set<UUID> followedUserIds = currentUser
                .map(user -> new HashSet<>(followRepository.findFollowedIds(user.getId(), followerIds)))
                .orElse(new HashSet<>());

        return followersPage.map(follow -> {
            UserResponse res = followMapper.toFollowerResponse(follow);
            res.setFollowing(followedUserIds.contains(res.getId()));
            return res;
        });
    }

    public Page<UserResponse> getFollowing(UUID userId, Pageable pageable) {
        Optional<User> currentUser = securityService.getUserIfLoggedIn();

        Page<Follow> followingPage = followRepository.findAllByFollowerId(userId, pageable);
        if (followingPage.isEmpty())
            return Page.empty();

        List<UUID> followingIds = followingPage.getContent().stream()
                .map(f -> f.getFollowing().getId())
                .toList();

        Set<UUID> followedUserIds = currentUser
                .map(user -> new HashSet<>(followRepository.findFollowedIds(user.getId(), followingIds)))
                .orElse(new HashSet<>());

        return followingPage.map(follow -> {
            UserResponse res = followMapper.toFollowingResponse(follow);
            res.setFollowing(followedUserIds.contains(res.getId()));
            return res;
        });
    }

    @Transactional
    @CacheEvict(value = CacheConstants.FEED_CACHE, allEntries = true)
    public void unfollowUser(UUID followingId) {
        User follower = securityService.getCurrentUser();

        followRepository.findByFollowerIdAndFollowingId(follower.getId(), followingId)
                .ifPresent(followRepository::delete);
    }
}
