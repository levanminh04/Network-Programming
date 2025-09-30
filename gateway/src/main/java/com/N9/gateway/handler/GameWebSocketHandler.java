package com.n9.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n9.gateway.service.CoreBridgeService;
import com.n9.gateway.service.SessionManager;
import com.n9.gateway.service.MessageValidator;
import com.n9.gateway.service.RateLimitingService;
import com.n9.gateway.model.GameSession;
import com.n9.gateway.util.GameLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main WebSocket Handler for Game Communication
 * 
 * Handles WebSocket connections, message routing, and session management
 * between frontend clients and the core game server.
 * 
 * Key Responsibilities:
 * - Establish and manage WebSocket connections
 * - Route messages between client and core server
 * - Handle authentication and session management
 * - Implement rate limiting and security measures
 * - Manage connection lifecycle and error handling
 * 
 * @author N9 Team
 * @version 1.0
 */
@Component
public class GameWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    
    private final ObjectMapper objectMapper;
    private final CoreBridgeService coreBridgeService;
    private final SessionManager sessionManager;
    private final MessageValidator messageValidator;
    private final RateLimitingService rateLimitingService;
    private final GameLogger gameLogger;
    
    // Track active connections by session ID
    private final ConcurrentHashMap<String, WebSocketSession> activeConnections = new ConcurrentHashMap<>();
    
    // Heartbeat scheduler
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);

    public GameWebSocketHandler(ObjectMapper objectMapper,
                               CoreBridgeService coreBridgeService,
                               SessionManager sessionManager,
                               MessageValidator messageValidator,
                               RateLimitingService rateLimitingService,
                               GameLogger gameLogger) {
        this.objectMapper = objectMapper;
        this.coreBridgeService = coreBridgeService;
        this.sessionManager = sessionManager;
        this.messageValidator = messageValidator;
        this.rateLimitingService = rateLimitingService;
        this.gameLogger = gameLogger;
        
        // Start heartbeat monitoring
        startHeartbeatMonitoring();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String clientIp = getClientIpAddress(session);
        
        logger.info("New WebSocket connection established: sessionId={}, clientIp={}", 
                   sessionId, clientIp);
        
        // Store active connection
        activeConnections.put(sessionId, session);
        
        // Create game session
        GameSession gameSession = sessionManager.createSession(sessionId, session);
        
        // Send welcome message
        sendWelcomeMessage(session);
        
        gameLogger.logConnection("CONNECTED", sessionId, clientIp);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        long startTime = System.currentTimeMillis();
        String sessionId = session.getId();
        String clientIp = getClientIpAddress(session);
        
        try {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                
                // Validate message
                var validationResult = messageValidator.validateMessage(payload);
                if (!validationResult.isValid()) {
                    sendErrorMessage(session, "VALIDATION_ERROR", validationResult.getErrorMessage());
                    return;
                }
                
                // Parse message
                var gameMessage = objectMapper.readTree(payload);
                String messageType = gameMessage.get("type").asText();
                
                // Rate limiting check
                if (!rateLimitingService.isAllowed(clientIp, messageType)) {
                    sendErrorMessage(session, "RATE_LIMIT_EXCEEDED", "Too many requests");
                    return;
                }
                
                // Route message to core server
                String coreResponse = coreBridgeService.forwardMessage(sessionId, payload);
                
                // Send response back to client
                if (coreResponse != null && !coreResponse.isEmpty()) {
                    session.sendMessage(new TextMessage(coreResponse));
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                gameLogger.logMessage("INBOUND", messageType, null, sessionId, processingTime);
                
            } else if (message instanceof PongMessage) {
                // Handle pong response
                sessionManager.updateLastActivity(sessionId);
                logger.debug("Received pong from session: {}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("Error handling message for session: {}", sessionId, e);
            sendErrorMessage(session, "PROCESSING_ERROR", "Failed to process message");
            gameLogger.logError("MESSAGE_HANDLING", e, sessionId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("Transport error for session: {}", sessionId, exception);
        
        gameLogger.logError("TRANSPORT_ERROR", (Exception) exception, sessionId);
        
        // Clean up session
        cleanupSession(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        String clientIp = getClientIpAddress(session);
        
        logger.info("WebSocket connection closed: sessionId={}, status={}, reason={}", 
                   sessionId, closeStatus.getCode(), closeStatus.getReason());
        
        // Clean up resources
        cleanupSession(sessionId);
        
        gameLogger.logConnection("DISCONNECTED", sessionId, clientIp);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Send welcome message to newly connected client
     */
    private void sendWelcomeMessage(WebSocketSession session) {
        try {
            var welcomeMessage = objectMapper.createObjectNode();
            welcomeMessage.put("type", "SYSTEM.WELCOME");
            welcomeMessage.put("timestamp", System.currentTimeMillis());
            welcomeMessage.put("sessionId", session.getId());
            
            var payload = objectMapper.createObjectNode();
            payload.put("message", "Connected to Card Game Gateway");
            payload.put("version", "1.0.0");
            welcomeMessage.set("payload", payload);
            
            session.sendMessage(new TextMessage(welcomeMessage.toString()));
        } catch (IOException e) {
            logger.error("Failed to send welcome message", e);
        }
    }

    /**
     * Send error message to client
     */
    private void sendErrorMessage(WebSocketSession session, String errorCode, String errorMessage) {
        try {
            var errorResponse = objectMapper.createObjectNode();
            errorResponse.put("type", "SYSTEM.ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            var payload = objectMapper.createObjectNode();
            payload.put("code", errorCode);
            payload.put("message", errorMessage);
            errorResponse.set("payload", payload);
            
            session.sendMessage(new TextMessage(errorResponse.toString()));
        } catch (IOException e) {
            logger.error("Failed to send error message", e);
        }
    }

    /**
     * Clean up session resources
     */
    private void cleanupSession(String sessionId) {
        // Remove from active connections
        activeConnections.remove(sessionId);
        
        // Clean up game session
        sessionManager.removeSession(sessionId);
        
        // Notify core server about disconnection
        try {
            coreBridgeService.notifyDisconnection(sessionId);
        } catch (Exception e) {
            logger.error("Failed to notify core about disconnection: {}", sessionId, e);
        }
    }

    /**
     * Get client IP address from WebSocket session
     */
    private String getClientIpAddress(WebSocketSession session) {
        try {
            return session.getRemoteAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Start heartbeat monitoring to detect dead connections
     */
    private void startHeartbeatMonitoring() {
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats, 30, 30, TimeUnit.SECONDS);
        heartbeatScheduler.scheduleAtFixedRate(this::checkStaleConnections, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Send ping messages to all active connections
     */
    private void sendHeartbeats() {
        activeConnections.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new PingMessage());
                }
            } catch (IOException e) {
                logger.debug("Failed to send heartbeat to session: {}", session.getId());
            }
        });
    }

    /**
     * Check and remove stale connections
     */
    private void checkStaleConnections() {
        var staleSessions = sessionManager.getStaleSessionIds(60000); // 60 seconds timeout
        staleSessions.forEach(sessionId -> {
            var session = activeConnections.get(sessionId);
            if (session != null) {
                try {
                    session.close(CloseStatus.GOING_AWAY);
                } catch (IOException e) {
                    logger.debug("Failed to close stale session: {}", sessionId);
                }
            }
        });
    }
}