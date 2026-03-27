package com.socialmovieclub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket mesaj trafiğini aktif eder
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Sunucudan istemciye (Frontend) gidecek mesajlar için önek
        // Kullanıcıya özel mesajlar için /user kullanılır
        config.enableSimpleBroker("/topic", "/queue", "/user");

        // İstemciden sunucuya gelecek mesajlar için uygulama öneki
        config.setApplicationDestinationPrefixes("/app");

        // Kullanıcıya özel (private) kanallar için gerekli
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*"); // .withSockJS() kısmını sildik
    }
}