package com.socialmovieclub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Sadece /api ile başlayan endpointleri dışarı aç
                .allowedOrigins("http://localhost:3000") // Sadece bizim frontend'e izin ver
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // İzin verilen metodlar
                .allowedHeaders("*") // Tüm başlıklara (Authorization dahil) izin ver
                .allowCredentials(true); // JWT ve Cookie işlemleri için gerekli
    }
}