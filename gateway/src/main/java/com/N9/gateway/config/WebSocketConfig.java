package com.n9.gateway.config;

import com.n9.gateway.handler.GameWebSocketHandler;
import com.n9.gateway.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;

/**
 * WebSocket Configuration for Game Gateway
 * 
 * Configures WebSocket endpoints, handlers, and security interceptors
 * for real-time communication between frontend clients and the gateway.
 * 
 * @author N9 Team
 * @version 1.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${websocket.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${websocket.endpoint}")
    private String websocketEndpoint;

    private final GameWebSocketHandler gameWebSocketHandler;
    private final AuthenticationInterceptor authenticationInterceptor;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler,
                          AuthenticationInterceptor authenticationInterceptor) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, websocketEndpoint)
                .addInterceptors(
                    new HttpSessionHandshakeInterceptor(),
                    authenticationInterceptor
                )
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .withSockJS()
                .setHeartbeatTime(25000)  // 25 seconds heartbeat
                .setDisconnectDelay(5000); // 5 seconds disconnect delay
    }

    /**
     * Configure WebSocket message size limits
     */
    @Bean
    public WebSocketMessageBrokerStats webSocketMessageBrokerStats() {
        return new WebSocketMessageBrokerStats();
    }
}