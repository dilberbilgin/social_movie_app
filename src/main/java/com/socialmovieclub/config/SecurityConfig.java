package com.socialmovieclub.config;

import com.socialmovieclub.security.jwt.AuthEntryPointJwt;
import com.socialmovieclub.security.jwt.AuthTokenFilter;
import com.socialmovieclub.security.jwt.JwtUtils;
import com.socialmovieclub.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize kullanmamızı sağlar
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final JwtUtils jwtUtils;


    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. Herkese Açık Kapılar
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. İçerik Görüntüleme (GET istekleri genelde açık)
                        .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/discover").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/suggestions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/genres/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/movie/**").permitAll()
                        .requestMatchers("/api/tmdb/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follows/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/profile/**").permitAll() // Profilleri herkes görebilsin
                        .requestMatchers("/ws-notifications/**").permitAll() // WebSocket bağlantısına izin ver
                        .requestMatchers("/api/notifications/**").authenticated()

                        // 3. Kimlik Doğrulama Gerektiren Aksiyonlar
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated() // Hem add hem like/dislike kapsar
                        .requestMatchers(HttpMethod.POST, "/api/comments/*/like", "/api/comments/*/dislike").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/movies/*/like", "/api/movies/*/dislike").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/ratings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/follows/**").authenticated()

                        // PROFİL GÜNCELLEME (PUT)
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile/**").authenticated()

                        // 4. Geri Kalan Her Şey (EN SONDA VE SADECE BİR KEZ OLMALI)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}