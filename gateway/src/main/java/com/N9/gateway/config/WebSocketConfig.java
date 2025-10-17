package com.N9.gateway.config;

import com.N9.gateway.websocket.GatewayWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GatewayWebSocketHandler gatewayWebSocketHandler;

    // Spring sẽ tự động "tiêm" (inject) handler vào đây
    public WebSocketConfig(GatewayWebSocketHandler gatewayWebSocketHandler) {
        this.gatewayWebSocketHandler = gatewayWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Frontend sẽ kết nối đến địa chỉ ws://<gateway_ip>:<port>/ws
        registry.addHandler(gatewayWebSocketHandler, "/ws")
                .setAllowedOrigins("*"); // Cho phép tất cả các nguồn (tiện cho dev)
    }
}