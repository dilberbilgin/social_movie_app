package com.socialmovieclub.service;

import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.entity.Follow;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.FollowMapper;
import com.socialmovieclub.repository.FollowRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MessageHelper messageHelper;
    private final FollowMapper followMapper;

    @Transactional
    public void followUser(UUID followingId, String currentUsername) {
        User follower = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException("User not found"));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new BusinessException("Target user not found"));

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
    }

    @Transactional
    public void unfollowUser(UUID followingId, String currentUsername) {
        User follower = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException("User not found" + currentUsername));

        followRepository.findByFollowerIdAndFollowingId(follower.getId(), followingId)
                .ifPresent(followRepository::delete);
    }

//    public void unfollowUser(UUID followingId, String currentUsername) {
//        // 1. İşlemi yapan kullanıcıyı bul
//        User follower = userRepository.findByUsername(currentUsername)
//                .orElseThrow(() -> new BusinessException("User not found: " + currentUsername));
//
//        // 2. Takip kaydını bul
//        Follow follow = followRepository.findByFollowerIdAndFollowingId(follower.getId(), followingId)
//                .orElseThrow(() -> new BusinessException("Follow relationship not found"));
//
//        // 3. Sil
//        followRepository.delete(follow);
//    }

//    public List<UserResponse> getFollowers(UUID userId) {
//        return followRepository.findAllByFollowingId(userId).stream()
//                .map(followMapper::toFollowerResponse)
//                .toList();
//    }

    public Page<UserResponse> getFollowers(UUID userId, Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        // 1. Sayfalanmış Follow kayıtlarını çek
        Page<Follow> followersPage = followRepository.findAllByFollowingId(userId, pageable);

        if (followersPage.isEmpty()) return Page.empty();

        // 2. Sadece BU sayfadaki kullanıcıların ID'lerini topla (Maksimum 'size' kadar, örn: 20)
        List<UUID> followerIds = followersPage.getContent().stream()
                .map(f -> f.getFollower().getId())
                .toList();

        // 3. Toplu kontrol (Bulk check)
        Set<UUID> followedUserIds = new HashSet<>();
        if (currentUser != null) {
            followedUserIds = new HashSet<>(followRepository.findFollowedIds(currentUser.getId(), followerIds));
        }

        // 4. Page nesnesini dönüştür (Page.map() hem veriyi çevirir hem meta-datayı korur)
        Set<UUID> finalFollowedUserIds = followedUserIds;
        return followersPage.map(follow -> {
            UserResponse res = followMapper.toFollowerResponse(follow);
            res.setFollowing(finalFollowedUserIds.contains(res.getId()));
            return res;
        });
    }

    public Page<UserResponse> getFollowing(UUID userId, Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        Page<Follow> followingPage = followRepository.findAllByFollowerId(userId, pageable);
        if (followingPage.isEmpty()) return Page.empty();

        List<UUID> followingIds = followingPage.getContent().stream()
                .map(f -> f.getFollowing().getId())
                .toList();

        Set<UUID> followedUserIds = new HashSet<>();
        if (currentUser != null) {
            followedUserIds = new HashSet<>(followRepository.findFollowedIds(currentUser.getId(), followingIds));
        }

        Set<UUID> finalFollowedUserIds = followedUserIds;
        return followingPage.map(follow -> {
            UserResponse res = followMapper.toFollowingResponse(follow);
            res.setFollowing(finalFollowedUserIds.contains(res.getId()));
            return res;
        });
    }

}
