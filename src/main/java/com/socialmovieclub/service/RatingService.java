package com.socialmovieclub.service;

import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import com.socialmovieclub.dto.request.RatingRequest;
import com.socialmovieclub.dto.response.RatingResponse;
import com.socialmovieclub.entity.Movie;
import com.socialmovieclub.entity.Rating;
import com.socialmovieclub.entity.User;
import com.socialmovieclub.exception.BusinessException;
import com.socialmovieclub.mapper.RatingMapper;
import com.socialmovieclub.repository.MovieRepository;
import com.socialmovieclub.repository.RatingRepository;
import com.socialmovieclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final RatingMapper ratingMapper;
    private final MessageHelper messageHelper;

    @Transactional // İşlem bütünlüğü: Puan kaydedilirken elektrik kesilirse veya hata olursa her şeyi geri alır.
    public RestResponse<RatingResponse> rateMovie(RatingRequest request) {
        // 1. GÜVENLİK: İstemi yapan kişinin kim olduğunu JWT Token içinden çekiyoruz.
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("user.not.found")));

        // 2. DOĞRULAMA: Puan verilmek istenen film gerçekten veritabanımızda var mı?
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new BusinessException(messageHelper.getMessage("movie.not.found")));

        // 3. UPSERT MANTIĞI (Update or Insert):
        // Kullanıcı daha önce bu filme puan vermiş mi?
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(user.getId(), movie.getId());

        boolean isNew = existingRating.isEmpty(); // Yeni mi yoksa güncelleme mi buradan anlıyoruz.
        // Boşsa yeni kayıttır (isNew = true).

        // Eğer varsa mevcut olanı getir, yoksa yeni bir Rating nesnesi oluştur.
        Rating rating = existingRating.orElse(new Rating());

        rating.setScore(request.getScore()); // Yeni puanı ayarla (1-10 arası).
        rating.setUser(user);
        rating.setMovie(movie);

        // Puanı ratings tablosuna kaydet.
        ratingRepository.save(rating);

        /**
         * 4. MATEMATİKSEL TETİKLEME (Denormalizasyon):
         * Puan değiştiği an, Movie tablosundaki o filmin genel ortalaması da eskimiş olur.
         * Bu yüzden hemen gidip o filmin ortalamasını yeniden hesaplamamız lazım.
         */
        updateMovieRatingStats(movie);

        // Yeni kayıt mı yoksa güncelleme mi olduğuna göre kullanıcıya farklı mesaj dönüyoruz.
        String msgKey = isNew ? "rating.success" : "movie.rating.updated";
        return RestResponse.success(ratingMapper.toResponse(rating), messageHelper.getMessage(msgKey));
    }

    // Bu metod arka planda çalışır ve filmin istatistiklerini günceller.
    private void updateMovieRatingStats(Movie movie) {
        // Repository'deki @Query'yi çağırır.
        // Veritabanı bize o anki en güncel ortalamayı (AVG) ve sayıyı (COUNT) döner.
        Object result = ratingRepository.getRatingStatsByMovieId(movie.getId());
        Object[] stats = (Object[]) ((Object[]) result)[0];

        Double avg = (Double) stats[0]; // SQL'den gelen AVG sonucu
        Long count = (Long) stats[1]; // SQL'den gelen COUNT sonucu

        // Film nesnesini güncelle.
        movie.setClubRating(avg != null ? avg : 0.0);
        movie.setClubVoteCount(count.intValue());

        // Movie tablosuna bu güncel bilgileri yaz.
        // Artık GET Movie isteği atan biri hesaplama yapmadan direkt bu güncel değeri görecek.
        movieRepository.save(movie);
    }
}