package com.socialmovieclub.config;

import com.socialmovieclub.entity.Language;
import com.socialmovieclub.repository.GenreRepository;
import com.socialmovieclub.repository.LanguageRepository;
import com.socialmovieclub.service.TmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final GenreRepository genreRepository;
    private final LanguageRepository languageRepository; // Yeni ekledik
    private final TmdbService tmdbService;

    @Override
    public void run(String... args) {
        // 1. Önce desteklenen dilleri kontrol et/oluştur
        initializeLanguages();

        // 2. Eğer kategoriler boşsa, sistemdeki tüm diller için senkronize et
        if (genreRepository.count() == 0) {
            log.info("Genres table is empty. Starting auto-sync for all active languages...");

            languageRepository.findAll().forEach(lang -> {
                log.info("Syncing genres for: {}", lang.getCode());
                tmdbService.syncGenres(lang.getCode());
            });

            log.info("Auto-sync completed successfully!");
        }
    }

    private void initializeLanguages() {
        if (languageRepository.count() == 0) {
            saveLanguage("en", "English");
            saveLanguage("tr", "Turkish");
            saveLanguage("pt", "Portuguese");
            saveLanguage("de", "German");
            // İleride buraya ekleme yapmak yerine bir Admin paneli üzerinden DB'ye dil eklenebilir.
        }
    }

    private void saveLanguage(String code, String name) {
        Language lang = new Language();
        lang.setCode(code);
        lang.setName(name);
        languageRepository.save(lang);
    }

// @Scheduled(cron = "0 0 0 1 * *") // Her ayın başında bir kez çalışır
//    public void scheduledGenreSync() {
//        log.info("Starting monthly genre synchronization...");
//        tmdbService.syncGenres("en");
//    }

}



