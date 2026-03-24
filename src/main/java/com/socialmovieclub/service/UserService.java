package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.UserProfileUpdateRequest;
import com.socialmovieclub.dto.response.ActivityResponse;
import com.socialmovieclub.dto.response.ProfileResponse;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.dto.response.UserResponse;
import com.socialmovieclub.dto.request.UserRegistrationRequest;
import com.socialmovieclub.entity.Activity;
import com.socialmovieclub.entity.Rating;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.Role;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.RatingMapper;
import com.socialmovieclub.mapper.UserMapper;
import com.socialmovieclub.repository.ActivityRepository;
import com.socialmovieclub.repository.FollowRepository;
import com.socialmovieclub.repository.RatingRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.socialmovieclub.core.result.RestResponse.success;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MessageHelper messageHelper;
    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;
    private final FollowRepository followRepository;
    private final SecurityService securityService;
    private final ActivityRepository activityRepository;

    @Transactional
    public RestResponse<UserResponse> register(UserRegistrationRequest registrationRequest) {

        // 1. Önce kontrol et (Duplicate check)
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            throw new BusinessException(messageHelper.getMessage("user.username.already.exists"));
        }
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new BusinessException(messageHelper.getMessage("user.email.already.exists"));
        }
        // 1. Request'i Entity'ye dönüştür
        User user = userMapper.toEntity(registrationRequest);
        // 2. Şifreyi güvenli hale getir
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Set.of(Role.USER));
        // 3. Veritabanına kaydet
        User savedUser = userRepository.save(user);
        // 4. Kaydedilen veriyi Response DTO'suna çevirip dön
        UserResponse responseData = userMapper.toResponse(savedUser);
        String successMsg = messageHelper.getMessage("user.register.success");

        return success(responseData, successMsg);
    }

    public RestResponse<UserResponse> getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));
        return  RestResponse.success(userMapper.toResponse(user), "User fetched");
    }

    public RestResponse<ProfileResponse> getUserProfile(String username, String lang, Pageable pageable) {
        // 1. Kullanıcıyı bul
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));

        // 2. İstatistikleri DB'den direkt COUNT sorgusuyla al (PERFORMANS İÇİN)
        long movieCount = ratingRepository.countByUserId(user.getId()); // Repository'e ekleyeceğiz
        long followerCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());

        // 3. Sayfalı Rating verisini çek (findTop10... yerine findAllByUserId... + Pageable)
        //Sayfalı Rating verisini çek (Grid görünümü için hala lazım olabilir)
        Page<Rating> ratingsPage = ratingRepository.findAllByUserIdOrderByCreatedDateDesc(user.getId(), pageable);
        // 4. Page.map() ile DTO ve Dil dönüşümü
        Page<RatingResponse> recentRatingResponses = ratingsPage.map(rating -> ratingMapper.toResponse(rating, lang));

        //  TÜM Aktiviteleri çek (Feed görünümü için)
        Page<Activity> activities = activityRepository.findByUserIdOrderByCreatedDateDesc(user.getId(), pageable);

        //  Map işlemi (DİKKAT: Burada FeedService'deki mantığı kullanıyoruz)
        Page<ActivityResponse> activityResponses = activities.map(this::mapActivityToResponse);

        //        Page<RatingResponse> recentRatingResponses = ratingsPage.map(rating -> {
//            RatingResponse res = ratingMapper.toResponse(rating);
//            String translatedTitle = rating.getMovie().getTranslations().stream()
//                    .filter(t -> t.getLanguageCode().equalsIgnoreCase(lang))
//                    .findFirst()
//                    .map(t -> t.getTitle())
//                    .orElse(rating.getMovie().getOriginalTitle());
//            res.setMovieTitle(translatedTitle);
//            return res;
//        });

        // 5. Response oluştur
        ProfileResponse profile = ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .movieCount(movieCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .recentRatings(recentRatingResponses) // Bu artık Page tipinde!
                .recentActivities(activityResponses)
                .isFollowing(checkIfCurrentUserFollows(user.getId()))
                .build();

        return success(profile);
    }

    // Yardımcı Metot: Activity -> ActivityResponse dönüşümü
    private ActivityResponse mapActivityToResponse(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .userId(activity.getUser().getId())
                .username(activity.getUser().getUsername())
                .userAvatar(activity.getUser().getProfilePictureUrl())
                .type(activity.getType())
                .targetId(activity.getTargetId())
                .targetImage(activity.getTargetImage())
                .targetTitle(activity.getTargetTitle() != null ? activity.getTargetTitle() : "Movie") // Eğer null ise "Movie" yaz
                .content(activity.getContent())
                .createdDate(activity.getCreatedDate())
                .build();
    }

    public RestResponse<Page<UserResponse>> searchUsers(String query, Pageable pageable) {
        if (query == null || query.trim().length() < 2) {
            return success(Page.empty(), "Query too short");
        }
        Page<User> users = userRepository.findByUsernameContainingIgnoreCase(query, pageable);
        return success(users.map(userMapper::toResponse), "Search completed");
    }

    private boolean checkIfCurrentUserFollows(UUID targetUserId) {
        return securityService.getUserIfLoggedIn()
                .map(currentUser -> followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId))
                .orElse(false);
    }

    @Transactional
    public RestResponse<UserResponse> updateProfile(String username, UserProfileUpdateRequest request) {
        User currentUser = securityService.getCurrentUser();

        if (!currentUser.getUsername().equals(username)) {
            throw new BusinessException(messageHelper.getMessage("user.unauthorized.update"));
        }

        userMapper.updateEntityFromRequest(request, currentUser);
        User updatedUser = userRepository.save(currentUser);
        return success(userMapper.toResponse(updatedUser), messageHelper.getMessage("user.profile.update.success"));
    }

    public RestResponse<List<UserResponse>> getSuggestedUsers(int limit) {
        int finalLimit = Math.min(limit, 20);

        List<User> users = securityService.getUserIfLoggedIn()
                .map(user -> userRepository.findSuggestedUsers(user.getId(), finalLimit))
                .orElseGet(() -> userRepository.findAll().stream().limit(finalLimit).toList());

        return success(users.stream().map(userMapper::toResponse).toList(), "Suggestions fetched");
    }
}