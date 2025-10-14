package com.N9.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Authentication WebSocket Handler (MVP Version for Group Project)
 * 
 * Simple echo handler that:
 * - Accepts WebSocket connections
 * - Logs incoming messages
 * - Echoes back messages for testing
 * 
 * This is a simplified version suitable for educational purposes.
 * Production implementation would include full authentication logic.
 * 
 * @version 1.0.0 (MVP)
 */
@Component
public class AuthenticationHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    
    public AuthenticationHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
        
        // Send welcome message
        String welcomeMsg = String.format(
            "{\"type\":\"SYSTEM_MESSAGE\",\"message\":\"Connected to Gateway (Session: %s)\"}",
            session.getId()
        );
        session.sendMessage(new TextMessage(welcomeMsg));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            System.out.println("Received from " + session.getId() + ": " + payload);
            
            // Simple echo response for testing
            String echoResponse = String.format(
                "{\"type\":\"ECHO_RESPONSE\",\"received\":%s,\"timestamp\":%d}",
                payload,
                System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(echoResponse));
            
            // Log for debugging
            System.out.println("Sent to " + session.getId() + ": " + echoResponse);
            
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            e.printStackTrace();
            
            // Send error response
            String errorResponse = String.format(
                "{\"type\":\"ERROR\",\"message\":\"%s\"}",
                e.getMessage()
            );
            session.sendMessage(new TextMessage(errorResponse));
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket connection closed: " + session.getId() + " - " + status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error for " + session.getId() + ": " + exception.getMessage());
        exception.printStackTrace();
    }
}
