package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.MovieCollectionRequest;
import com.socialmovieclub.dto.response.MovieCollectionResponse;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.MovieCollection;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.enums.ActivityType;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.MovieCollectionMapper;
import com.socialmovieclub.repository.FollowRepository;
import com.socialmovieclub.repository.MovieCollectionRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieCollectionService {

    private final MovieCollectionRepository movieCollectionRepository;
    private final MovieService movieService;
    private final SecurityService securityService;
    private final ActivityService activityService;
    private final MessageHelper messageHelper;
    private final MovieCollectionMapper movieCollectionMapper;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public RestResponse<MovieCollectionResponse> createCollection(MovieCollectionRequest request) {
        User user = securityService.getCurrentUser();

        MovieCollection collection = MovieCollection.builder()
                .name(request.name())
                .description(request.description())
                .isPublic(request.isPublic())
                .user(user)
                .build();

        MovieCollection saved = movieCollectionRepository.save(collection);

        // Sadece Public ise Aktivite oluştur
        if (saved.isPublic()) {
            activityService.createActivity(user.getId(), ActivityType.COLLECTION_CREATE,
                    saved.getId(), saved.getName(), null, saved.getName());
        }

//        return RestResponse.success(mapToResponse(saved), messageHelper.getMessage("collection.create.success"));
        return RestResponse.success(movieCollectionMapper.toResponse(saved, "en"),
                messageHelper.getMessage("collection.create.success"));
    }

    @Transactional
    public RestResponse<Void> addMovieToCollection(UUID collectionId, Long tmdbId) {
        User user = securityService.getCurrentUser();
        MovieCollection collection = movieCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("collection.not.found")));

        // 1. Yetki Kontrolü
        if (!collection.getUser().getId().equals(user.getId())) {
            throw new BusinessException(messageHelper.getMessage("auth.access.denied"));
        }

        // 2. Film DB'de yoksa import et, varsa getir
        Movie movie = movieService.ensureMovieExists(tmdbId, "en");

        // 3. Mükerrer Kayıt Kontrolü (Business Rule)
        if (collection.getMovies().contains(movie)) {
            // Çok dilli hata mesajı: "Bu film zaten bu listede mevcut."
            throw new BusinessException(messageHelper.getMessage("collection.movie.already.exists", movie.getOriginalTitle()));
        }

        collection.addMovie(movie);
        movieCollectionRepository.save(collection);

        return RestResponse.success(null, messageHelper.getMessage("collection.movie.added"));
    }

    // Helper: Entity -> DTO (MapStruct da kullanabilirsin)
    private MovieCollectionResponse mapToResponse(MovieCollection collection) {
        return MovieCollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .isPublic(collection.isPublic())
                .movieCount(collection.getMovies() != null ? collection.getMovies().size() : 0)
                .ownerUsername(collection.getUser().getUsername())
                .build();
    }

//    public RestResponse<List<MovieCollectionResponse>> getMyCollections() {
//        UUID userId = securityService.getCurrentUserId();
//        List<MovieCollection> collections = movieCollectionRepository.findAllByUserIdOrderByCreatedDateDesc(userId);
//
//        // MapStruct kullanmıyorsan manuel map'leme (veya Mapper'a taşıyabilirsin)
//        List<MovieCollectionResponse> response = collections.stream()
//                .map(this::mapToResponse)
//                .toList();
//
//        return RestResponse.success(response);
//    }

    @Transactional
    public RestResponse<List<MovieCollectionResponse>> getMyCollections() {
        UUID userId = securityService.getCurrentUserId();
        List<MovieCollection> collections = movieCollectionRepository.findAllByUserIdOrderByCreatedDateDesc(userId);

        // Profesyonel dönüşüm
        List<MovieCollectionResponse> response = collections.stream()
                .map(col -> movieCollectionMapper.toResponse(col, "en")) // veya aktif dil
                .toList();

        return RestResponse.success(response);
    }

    public RestResponse<MovieCollectionResponse> getCollectionDetail(UUID id, String lang) {
        MovieCollection collection = movieCollectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("collection.not.found")));

        MovieCollectionResponse response = movieCollectionMapper.toResponse(collection, lang);

        // Zenginleştirme işlemi
        if (response.getMovies() != null) {
            response.getMovies().forEach(movieDto -> {
                // MovieService'deki public yaptığımız metodu çağırıyoruz
                movieService.enrichMovieWithLikes(movieDto);
            });
        }

        return RestResponse.success(response);
    }

//    @Transactional(readOnly = true)
//    public RestResponse<List<MovieCollectionResponse>> getUserCollectionsByUsername(String username, String lang) {
//        // 1. Hedef kullanıcıyı bul
//
//        User targetUser = userRepository.findByUsername(username)
//                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));
//
//        User currentUser = securityService.getCurrentUser();
//        List<MovieCollection> collections;
//
//        // 2. Mantık: Eğer bakan kişi profilin sahibiyse tümü, değilse sadece public olanlar
//        if (currentUser != null && currentUser.getId().equals(targetUser.getId())) {
//            collections = movieCollectionRepository.findAllByUserIdOrderByCreatedDateDesc(targetUser.getId());
//        } else {
//            collections = movieCollectionRepository.findAllByUserIdAndIsPublicTrue(targetUser.getId());
//        }
//
//        List<MovieCollectionResponse> response = collections.stream()
//                .map(col -> movieCollectionMapper.toResponse(col, lang))
//                .toList();
//
//        return RestResponse.success(response);
//    }

    @Transactional(readOnly = true)
    public RestResponse<List<MovieCollectionResponse>> getUserCollectionsByUsername(String username, String lang) {
        // 1. Sayfasına bakılan kullanıcı (Test 5)
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));

        // 2. Bakmakta olan kullanıcı (Test 2)
        User currentUser = securityService.getCurrentUser();

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());

        List<MovieCollection> collections;

        if (isOwnProfile) {
            // Kendi profilimse tümünü getir
            collections = movieCollectionRepository.findAllByUserIdOrderByCreatedDateDesc(targetUser.getId());
        } else if (isFollowing) {
            // Takip ediyorsam Test 5'in sadece public olanlarını getir
            // BURASI KRİTİK: findAllByUserIdAndIsPublicTrue(targetUser.getId()) olmalı!
            collections = movieCollectionRepository.findAllByUserIdAndIsPublicTrue(targetUser.getId());
        } else {
            // Takip etmiyorsam boş liste
            return RestResponse.success(List.of());
        }

        List<MovieCollectionResponse> response = collections.stream()
                .map(col -> movieCollectionMapper.toResponse(col, lang))
                .toList();

        return RestResponse.success(response);
    }


}