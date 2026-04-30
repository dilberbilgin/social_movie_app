package com.socialmovieclub.core.interceptor;

import com.socialmovieclub.core.context.UserContext;
import com.socialmovieclub.core.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. Manuel seçim (Frontend X-Region)
        String region = request.getHeader("X-Region"); // Frontend manuel gönderebilir

        // 2. Coğrafi Headerlar (Cloudflare vb. varsa)
        if (region == null || region.isBlank()) {
            region = request.getHeader("CF-IPCountry");
        }
//        // 2. Altyapı bilgisi (Cloudflare/Nginx) - Canlı ortamda en güvenilir IP verisi
//        if (region == null || region.isEmpty()) {
//            region = request.getHeader("CF-IPCountry");
//        }

        // 3. Accept-Language içindeki Ülke Kodu (Örn: tr-TR -> TR)
        if (region == null || region.isBlank()) {
            Locale locale = request.getLocale();
            if (locale != null && locale.getCountry() != null && !locale.getCountry().isBlank()) {
                region = locale.getCountry();
            }
        }

        // 3. Tarayıcı Locale bilgisi (Accept-Language içinden çekilen ülke)
//        if (region == null || region.isEmpty()) {
//            region = request.getLocale().getCountry();
//        }


        // 3. O da yoksa IP tabanlı bir kütüphane kullan (Opsiyonel: MaxMind GeoIP kütüphanesi)

        // 4. Fallback: Eğer hala bulunamadıysa (Lokal testler veya eksik header)
        if (region == null || region.isBlank()) {
            region = "TR";
        }
//        // 4. Fallback (Varsayılan)
//        if (region == null || region.isEmpty()) {
//            region = "TR";
//        }

        UserContext context = new UserContext();
        context.setRegion(region.toUpperCase());
        context.setLanguage(request.getLocale().getLanguage());
        context.setLocale(request.getLocale());

        UserContextHolder.setContext(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear(); // Bellek sızıntısını önlemek için temizle
        //İnterceptor'larda ThreadLocal kullanırken bu temizliği yapmazsan, özellikle yoğun trafik altında sunucu bir süre sonra şişebilir.
    }
}