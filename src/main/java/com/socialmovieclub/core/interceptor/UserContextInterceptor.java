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
        String region = request.getHeader("X-Region"); // Frontend manuel gönderebilir

        // Eğer Header yoksa Cloudflare gibi servislerden veya Accept-Language'den çıkarım yap
        if (region == null || region.isEmpty()) {
            // Örn: PT, TR vb. (Basit mantık: Locale'den çekiyoruz)
            region = request.getLocale().getCountry();
        }

        // Eğer hala boşsa (localhost vb.) varsayılan ata
        if (region == null || region.isEmpty()) {
            region = "PT";
        }

        UserContext context = new UserContext();
        context.setRegion(region.toUpperCase());
        context.setLanguage(request.getLocale().getLanguage());
        context.setLocale(request.getLocale());

        UserContextHolder.setContext(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear(); // Bellek sızıntısını önlemek için temizle (CRITICAL)
    }
}