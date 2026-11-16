package com.eduquest.api.config;

import com.eduquest.api.realtime.ChatSocketHandler;
import com.eduquest.api.realtime.TokenHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatSocketHandler chatSocketHandler;
    private final TokenHandshakeInterceptor handshakeInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    public WebSocketConfig(ChatSocketHandler chatSocketHandler, TokenHandshakeInterceptor handshakeInterceptor) {
        this.chatSocketHandler = chatSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        registry.addHandler(chatSocketHandler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(origins);
    }
}
