# Integration Guide - Developer Implementation Handbook

## 🎯 **QUICK START**

This guide provides step-by-step integration instructions for each system component using the shared protocol library.

### **Prerequisites**
```xml
<!-- Add to your module's pom.xml -->
<dependency>
    <groupId>com.n9</groupId>
    <artifactId>shared</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## 🌐 **FRONTEND INTEGRATION (React)**

### **1. WebSocket Connection Setup**

```javascript
// src/services/websocket.js
import { MessageType } from '../protocol/mt';

class GameWebSocketService {
    constructor() {
        this.ws = null;
        this.messageHandlers = new Map();
        this.correlationCallbacks = new Map();
        this.isConnected = false;
    }

    connect(token) {
        const wsUrl = `ws://localhost:8080/game?token=${token}`;
        this.ws = new WebSocket(wsUrl);
        
        this.ws.onopen = () => {
            console.log('Connected to game server');
            this.isConnected = true;
        };

        this.ws.onmessage = (event) => {
            this.handleMessage(JSON.parse(event.data));
        };

        this.ws.onclose = () => {
            console.log('Disconnected from game server');
            this.isConnected = false;
            this.scheduleReconnect();
        };
    }

    sendMessage(type, payload = {}) {
        if (!this.isConnected) {
            throw new Error('WebSocket not connected');
        }

        const message = {
            type,
            correlationId: this.generateCorrelationId(),
            timestamp: Date.now(),
            payload
        };

        this.ws.send(JSON.stringify(message));
        return message.correlationId;
    }

    // Promise-based message sending with response
    async sendMessageWithResponse(type, payload = {}, timeoutMs = 5000) {
        const correlationId = this.sendMessage(type, payload);
        
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                this.correlationCallbacks.delete(correlationId);
                reject(new Error('Message timeout'));
            }, timeoutMs);

            this.correlationCallbacks.set(correlationId, (response) => {
                clearTimeout(timeout);
                resolve(response);
            });
        });
    }

    handleMessage(message) {
        // Handle correlation-based responses
        if (message.correlationId && this.correlationCallbacks.has(message.correlationId)) {
            const callback = this.correlationCallbacks.get(message.correlationId);
            this.correlationCallbacks.delete(message.correlationId);
            callback(message);
            return;
        }

        // Handle broadcast messages
        const handlers = this.messageHandlers.get(message.type) || [];
        handlers.forEach(handler => handler(message));
    }

    onMessage(type, handler) {
        if (!this.messageHandlers.has(type)) {
            this.messageHandlers.set(type, []);
        }
        this.messageHandlers.get(type).push(handler);
    }

    generateCorrelationId() {
        return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }
}

export default new GameWebSocketService();
```

### **2. Authentication Integration**

```javascript
// src/services/auth.js
import wsService from './websocket';

export class AuthService {
    async login(username, password) {
        try {
            const response = await wsService.sendMessageWithResponse(
                'AUTH.LOGIN_REQUEST',
                { username, password, clientVersion: '1.0.0' }
            );

            if (response.type === 'AUTH.LOGIN_SUCCESS') {
                const { token, userId, ...userInfo } = response.payload;
                localStorage.setItem('token', token);
                localStorage.setItem('userId', userId);
                localStorage.setItem('userInfo', JSON.stringify(userInfo));
                return { success: true, userInfo: { userId, ...userInfo } };
            } else {
                return { success: false, error: response.payload.message };
            }
        } catch (error) {
            return { success: false, error: 'Connection failed' };
        }
    }

    async logout() {
        const correlationId = wsService.sendMessage('AUTH.LOGOUT_REQUEST');
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('userInfo');
        wsService.disconnect();
    }

    getStoredToken() {
        return localStorage.getItem('token');
    }

    getCurrentUser() {
        const userInfo = localStorage.getItem('userInfo');
        return userInfo ? JSON.parse(userInfo) : null;
    }
}
```

### **3. Game State Management (React Hook)**

```javascript
// src/hooks/useGameState.js
import { useState, useEffect, useCallback } from 'react';
import wsService from '../services/websocket';

export function useGameState() {
    const [gameState, setGameState] = useState({
        currentMatch: null,
        gamePhase: 'LOBBY', // LOBBY, MATCHMAKING, IN_GAME, GAME_OVER
        playerCards: [],
        opponentInfo: null,
        roundNumber: 0,
        scores: { player1: 0, player2: 0 },
        timeRemaining: 0,
        isMyTurn: false
    });

    const [error, setError] = useState(null);

    // Handle game messages
    useEffect(() => {
        wsService.onMessage('GAME.MATCH_FOUND', (message) => {
            setGameState(prev => ({
                ...prev,
                currentMatch: message.payload.matchId,
                opponentInfo: message.payload.opponent,
                gamePhase: 'MATCH_FOUND'
            }));
        });

        wsService.onMessage('GAME.START', (message) => {
            setGameState(prev => ({
                ...prev,
                gamePhase: 'IN_GAME',
                playerPosition: message.payload.playerPosition,
                roundNumber: 0
            }));
        });

        wsService.onMessage('GAME.ROUND_START', (message) => {
            setGameState(prev => ({
                ...prev,
                roundNumber: message.payload.roundNumber,
                playerCards: message.payload.availableCards,
                scores: message.payload.roundScore,
                timeRemaining: Math.floor((message.payload.deadline - Date.now()) / 1000),
                isMyTurn: true
            }));
        });

        wsService.onMessage('GAME.ROUND_REVEAL', (message) => {
            setGameState(prev => ({
                ...prev,
                scores: {
                    player1: message.payload.player1TotalScore,
                    player2: message.payload.player2TotalScore
                },
                lastRoundResult: {
                    player1Card: message.payload.player1Card,
                    player2Card: message.payload.player2Card,
                    winner: message.payload.roundWinner
                },
                isMyTurn: false
            }));
        });

        wsService.onMessage('GAME.END', (message) => {
            setGameState(prev => ({
                ...prev,
                gamePhase: 'GAME_OVER',
                finalResult: message.payload,
                isMyTurn: false
            }));
        });

        wsService.onMessage('SYSTEM.ERROR', (message) => {
            setError(message.payload.message);
        });
    }, []);

    // Game actions
    const joinLobby = useCallback(() => {
        wsService.sendMessage('LOBBY.JOIN_REQUEST', { preferredGameMode: 'QUICK' });
        setGameState(prev => ({ ...prev, gamePhase: 'LOBBY' }));
    }, []);

    const requestMatch = useCallback(() => {
        wsService.sendMessage('LOBBY.MATCH_REQUEST', { gameMode: 'QUICK' });
        setGameState(prev => ({ ...prev, gamePhase: 'MATCHMAKING' }));
    }, []);

    const playCard = useCallback((cardIndex) => {
        if (!gameState.isMyTurn || !gameState.currentMatch) return;

        wsService.sendMessage('GAME.CARD_PLAY_REQUEST', {
            matchId: gameState.currentMatch,
            roundNumber: gameState.roundNumber,
            cardIndex,
            timestamp: Date.now()
        });

        setGameState(prev => ({ ...prev, isMyTurn: false }));
    }, [gameState.isMyTurn, gameState.currentMatch, gameState.roundNumber]);

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    return {
        gameState,
        error,
        actions: {
            joinLobby,
            requestMatch,
            playCard,
            clearError
        }
    };
}
```

---

## 🚪 **GATEWAY INTEGRATION (Spring Boot)**

### **1. Message Processing Service**

```java
// src/main/java/com/n9/gateway/service/MessageProcessingService.java
package com.n9.gateway.service;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class MessageProcessingService {
    
    private final ObjectMapper objectMapper;
    private final AuthenticationService authService;
    private final CoreServerCommunicator coreComm;
    
    public MessageProcessingService(ObjectMapper objectMapper,
                                  AuthenticationService authService,
                                  CoreServerCommunicator coreComm) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.coreComm = coreComm;
    }
    
    public MessageEnvelope processMessage(MessageEnvelope envelope, 
                                        WebSocketSession session) {
        try {
            switch (envelope.getType()) {
                case MessageType.AUTH_LOGIN_REQUEST:
                    return handleLogin(envelope, session);
                    
                case MessageType.LOBBY_JOIN_REQUEST:
                    return handleLobbyJoin(envelope, session);
                    
                case MessageType.LOBBY_MATCH_REQUEST:
                    return handleMatchRequest(envelope, session);
                    
                case MessageType.GAME_CARD_PLAY_REQUEST:
                    return handleCardPlay(envelope, session);
                    
                default:
                    return createErrorResponse(envelope, "UNSUPPORTED_MESSAGE_TYPE", 
                                             "Message type not supported: " + envelope.getType());
            }
        } catch (Exception e) {
            return createErrorResponse(envelope, "PROCESSING_ERROR", 
                                     "Failed to process message: " + e.getMessage());
        }
    }
    
    private MessageEnvelope handleLogin(MessageEnvelope envelope, WebSocketSession session) {
        try {
            LoginRequestDto loginRequest = objectMapper.convertValue(
                envelope.getPayload(), LoginRequestDto.class);
            
            // Validate input
            if (!loginRequest.isValid()) {
                return createErrorResponse(envelope, "INVALID_INPUT", 
                                         "Invalid login credentials format");
            }
            
            // Authenticate with database
            AuthResult authResult = authService.authenticate(
                loginRequest.getUsername(), loginRequest.getPassword());
            
            if (authResult.isSuccess()) {
                // Store user session
                session.getAttributes().put("userId", authResult.getUserId());
                session.getAttributes().put("authenticated", true);
                
                return MessageEnvelope.builder()
                    .type(MessageType.AUTH_LOGIN_SUCCESS)
                    .correlationId(envelope.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .userId(authResult.getUserId())
                    .sessionId(session.getId())
                    .payload(authResult.toSuccessDto())
                    .build();
            } else {
                return MessageEnvelope.builder()
                    .type(MessageType.AUTH_LOGIN_FAILURE)
                    .correlationId(envelope.getCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .payload(authResult.toFailureDto())
                    .build();
            }
            
        } catch (Exception e) {
            return createErrorResponse(envelope, "AUTH_ERROR", 
                                     "Authentication failed: " + e.getMessage());
        }
    }
    
    private MessageEnvelope handleLobbyJoin(MessageEnvelope envelope, WebSocketSession session) {
        // Forward to Core server via TCP
        return coreComm.forwardToCore(envelope, session);
    }
    
    private MessageEnvelope handleMatchRequest(MessageEnvelope envelope, WebSocketSession session) {
        // Validate user is authenticated
        if (!isAuthenticated(session)) {
            return createErrorResponse(envelope, "AUTHENTICATION_REQUIRED", 
                                     "Must be logged in to request matches");
        }
        
        // Forward to Core server for matchmaking
        return coreComm.forwardToCore(envelope, session);
    }
    
    private MessageEnvelope handleCardPlay(MessageEnvelope envelope, WebSocketSession session) {
        // Validate user is authenticated and in a game
        if (!isAuthenticated(session)) {
            return createErrorResponse(envelope, "AUTHENTICATION_REQUIRED", 
                                     "Must be logged in to play cards");
        }
        
        // Forward to Core server for game logic
        return coreComm.forwardToCore(envelope, session);
    }
    
    private boolean isAuthenticated(WebSocketSession session) {
        return Boolean.TRUE.equals(session.getAttributes().get("authenticated"));
    }
    
    private MessageEnvelope createErrorResponse(MessageEnvelope original, 
                                              String errorCode, String message) {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_ERROR)
            .correlationId(original.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(original.getUserId())
            .sessionId(original.getSessionId())
            .error(ErrorInfo.builder()
                .code(errorCode)
                .message(message)
                .retryable(false)
                .build())
            .build();
    }
}
```

### **2. Core Server Communication**

```java
// src/main/java/com/n9/gateway/service/CoreServerCommunicator.java
package com.n9.gateway.service;

import com.n9.shared.protocol.MessageEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Service
public class CoreServerCommunicator {
    
    private final ObjectMapper objectMapper;
    private Socket coreSocket;
    private DataOutputStream out;
    private DataInputStream in;
    
    public CoreServerCommunicator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        connectToCore();
    }
    
    private void connectToCore() {
        try {
            coreSocket = new Socket("localhost", 9090);
            out = new DataOutputStream(coreSocket.getOutputStream());
            in = new DataInputStream(coreSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to Core server", e);
        }
    }
    
    public MessageEnvelope forwardToCore(MessageEnvelope envelope, WebSocketSession session) {
        try {
            // Add session context
            envelope = envelope.toBuilder()
                .sessionId(session.getId())
                .build();
            
            // Serialize message
            String jsonMessage = objectMapper.writeValueAsString(envelope);
            byte[] messageBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
            
            // Send length-prefixed message
            out.writeInt(messageBytes.length);
            out.write(messageBytes);
            out.flush();
            
            // Read response
            int responseLength = in.readInt();
            byte[] responseBytes = new byte[responseLength];
            in.readFully(responseBytes);
            
            String responseJson = new String(responseBytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(responseJson, MessageEnvelope.class);
            
        } catch (IOException e) {
            return createErrorResponse(envelope, "CORE_COMMUNICATION_ERROR", 
                                     "Failed to communicate with game server");
        }
    }
    
    private MessageEnvelope createErrorResponse(MessageEnvelope original, 
                                              String errorCode, String message) {
        return MessageEnvelope.builder()
            .type("SYSTEM.ERROR")
            .correlationId(original.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .error(ErrorInfo.builder()
                .code(errorCode)
                .message(message)
                .retryable(true)
                .build())
            .build();
    }
}
```

---

## 🖥️ **CORE SERVER INTEGRATION (Java)**

### **1. TCP Message Handler**

```java
// src/main/java/com/n9/core/network/CoreServerListener.java
package com.n9.core.network;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoreServerListener {
    
    private final ObjectMapper objectMapper;
    private final GameService gameService;
    private final LobbyService lobbyService;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    
    public CoreServerListener(ObjectMapper objectMapper, 
                            GameService gameService,
                            LobbyService lobbyService) {
        this.objectMapper = objectMapper;
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        this.executor = Executors.newCachedThreadPool();
    }
    
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Core server listening on port " + port);
        
        while (!serverSocket.isClosed()) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> handleConnection(clientSocket));
        }
    }
    
    private void handleConnection(Socket clientSocket) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            
            while (!clientSocket.isClosed()) {
                // Read length-prefixed message
                int messageLength = in.readInt();
                byte[] messageBytes = new byte[messageLength];
                in.readFully(messageBytes);
                
                String messageJson = new String(messageBytes, StandardCharsets.UTF_8);
                MessageEnvelope envelope = objectMapper.readValue(messageJson, MessageEnvelope.class);
                
                // Process message
                MessageEnvelope response = processMessage(envelope);
                
                // Send response
                String responseJson = objectMapper.writeValueAsString(response);
                byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
                out.writeInt(responseBytes.length);
                out.write(responseBytes);
                out.flush();
            }
            
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
    
    private MessageEnvelope processMessage(MessageEnvelope envelope) {
        try {
            switch (envelope.getType()) {
                case MessageType.LOBBY_JOIN_REQUEST:
                    return lobbyService.handleJoinRequest(envelope);
                    
                case MessageType.LOBBY_MATCH_REQUEST:
                    return lobbyService.handleMatchRequest(envelope);
                    
                case MessageType.GAME_CARD_PLAY_REQUEST:
                    return gameService.handleCardPlay(envelope);
                    
                default:
                    return createErrorResponse(envelope, "UNSUPPORTED_MESSAGE", 
                                             "Core server doesn't handle: " + envelope.getType());
            }
        } catch (Exception e) {
            return createErrorResponse(envelope, "PROCESSING_ERROR", 
                                     "Failed to process: " + e.getMessage());
        }
    }
}
```

### **2. Game Logic Service**

```java
// src/main/java/com/n9/core/service/GameService.java
package com.n9.core.service;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.shared.model.dto.game.CardPlayRequestDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.model.Card;
import com.n9.shared.model.enums.GameState;

@Service
public class GameService {
    
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    public MessageEnvelope handleCardPlay(MessageEnvelope envelope) {
        try {
            CardPlayRequestDto request = objectMapper.convertValue(
                envelope.getPayload(), CardPlayRequestDto.class);
            
            GameSession session = activeSessions.get(request.getMatchId());
            if (session == null) {
                return createErrorResponse(envelope, "GAME_NOT_FOUND", 
                                         "No active game found");
            }
            
            // Process card play
            GameMoveResult result = session.playCard(envelope.getUserId(), 
                                                   request.getCardIndex());
            
            if (result.isError()) {
                return createErrorResponse(envelope, result.getErrorCode(), 
                                         result.getErrorMessage());
            }
            
            // Send acknowledgment
            MessageEnvelope ack = MessageEnvelope.builder()
                .type(MessageType.GAME_CARD_PLAY_ACK)
                .correlationId(envelope.getCorrelationId())
                .timestamp(System.currentTimeMillis())
                .userId(envelope.getUserId())
                .matchId(request.getMatchId())
                .payload(result.getAckPayload())
                .build();
            
            // Check if round is complete
            if (session.isRoundComplete()) {
                // Send round reveal to both players
                RoundRevealDto reveal = session.generateRoundReveal();
                
                MessageEnvelope revealMessage = MessageEnvelope.builder()
                    .type(MessageType.GAME_ROUND_REVEAL)
                    .correlationId(generateCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .matchId(request.getMatchId())
                    .payload(reveal)
                    .build();
                
                // Broadcast to both players (implement broadcast mechanism)
                broadcastToMatch(request.getMatchId(), revealMessage);
                
                // Check if game is complete
                if (session.isGameComplete()) {
                    handleGameEnd(session);
                } else {
                    // Start next round after delay
                    scheduleNextRound(session);
                }
            }
            
            return ack;
            
        } catch (Exception e) {
            return createErrorResponse(envelope, "GAME_ERROR", 
                                     "Failed to process card play: " + e.getMessage());
        }
    }
    
    private void broadcastToMatch(String matchId, MessageEnvelope message) {
        // Implementation depends on your session management
        // This would typically send the message back to Gateway
        // which then broadcasts to WebSocket clients
    }
    
    private void scheduleNextRound(GameSession session) {
        // Schedule next round start (e.g., 3 seconds delay)
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
            .execute(() -> startNextRound(session));
    }
    
    private void startNextRound(GameSession session) {
        MessageEnvelope roundStart = MessageEnvelope.builder()
            .type(MessageType.GAME_ROUND_START)
            .correlationId(generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .matchId(session.getMatchId())
            .payload(session.generateRoundStartPayload())
            .build();
            
        broadcastToMatch(session.getMatchId(), roundStart);
    }
}
```

---

## 🧪 **TESTING INTEGRATION**

### **1. Protocol Testing Utilities**

```java
// src/test/java/com/n9/shared/protocol/MessageTestUtils.java
package com.n9.shared.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

public class MessageTestUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static MessageEnvelope createTestMessage(String type, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId("test-" + System.currentTimeMillis())
            .timestamp(System.currentTimeMillis())
            .userId("test-user")
            .payload(payload)
            .build();
    }
    
    public static void assertValidMessage(MessageEnvelope envelope) {
        assertNotNull(envelope.getType(), "Message type is required");
        assertNotNull(envelope.getCorrelationId(), "Correlation ID is required");
        assertTrue(envelope.getTimestamp() > 0, "Timestamp must be positive");
    }
    
    public static void assertMessageSerialization(MessageEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            MessageEnvelope deserialized = objectMapper.readValue(json, MessageEnvelope.class);
            
            assertEquals(envelope.getType(), deserialized.getType());
            assertEquals(envelope.getCorrelationId(), deserialized.getCorrelationId());
            assertEquals(envelope.getTimestamp(), deserialized.getTimestamp());
            
        } catch (Exception e) {
            fail("Message serialization failed: " + e.getMessage());
        }
    }
}
```

### **2. End-to-End Integration Test**

```java
// src/test/java/integration/GameFlowIntegrationTest.java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class GameFlowIntegrationTest {
    
    @Autowired
    private TestWebSocketClient client1;
    
    @Autowired  
    private TestWebSocketClient client2;
    
    @Test
    @Order(1)
    public void testCompleteGameFlow() throws Exception {
        // 1. Both players login
        client1.login("player1", "password");
        client2.login("player2", "password");
        
        // 2. Join lobby
        client1.sendMessage(MessageType.LOBBY_JOIN_REQUEST, Map.of());
        client2.sendMessage(MessageType.LOBBY_JOIN_REQUEST, Map.of());
        
        // 3. Request match
        client1.sendMessage(MessageType.LOBBY_MATCH_REQUEST, 
                           Map.of("gameMode", "QUICK"));
        client2.sendMessage(MessageType.LOBBY_MATCH_REQUEST, 
                           Map.of("gameMode", "QUICK"));
        
        // 4. Verify match found
        MessageEnvelope matchFound1 = client1.waitForMessage(MessageType.GAME_MATCH_FOUND);
        MessageEnvelope matchFound2 = client2.waitForMessage(MessageType.GAME_MATCH_FOUND);
        
        String matchId = (String) matchFound1.getPayload().get("matchId");
        assertEquals(matchId, matchFound2.getPayload().get("matchId"));
        
        // 5. Verify game start
        client1.waitForMessage(MessageType.GAME_START);
        client2.waitForMessage(MessageType.GAME_START);
        
        // 6. Play 3 rounds
        for (int round = 1; round <= 3; round++) {
            MessageEnvelope roundStart1 = client1.waitForMessage(MessageType.GAME_ROUND_START);
            MessageEnvelope roundStart2 = client2.waitForMessage(MessageType.GAME_ROUND_START);
            
            // Both players select cards
            client1.sendMessage(MessageType.GAME_CARD_PLAY_REQUEST, Map.of(
                "matchId", matchId,
                "roundNumber", round,
                "cardIndex", round * 10  // Different card each round
            ));
            
            client2.sendMessage(MessageType.GAME_CARD_PLAY_REQUEST, Map.of(
                "matchId", matchId,
                "roundNumber", round,
                "cardIndex", round * 10 + 5  // Different card each round
            ));
            
            // Verify round reveal
            MessageEnvelope reveal1 = client1.waitForMessage(MessageType.GAME_ROUND_REVEAL);
            MessageEnvelope reveal2 = client2.waitForMessage(MessageType.GAME_ROUND_REVEAL);
            
            assertEquals(reveal1.getPayload(), reveal2.getPayload());
        }
        
        // 7. Verify game end
        MessageEnvelope gameEnd1 = client1.waitForMessage(MessageType.GAME_END);
        MessageEnvelope gameEnd2 = client2.waitForMessage(MessageType.GAME_END);
        
        assertNotNull(gameEnd1.getPayload().get("gameResult"));
        assertEquals(gameEnd1.getPayload(), gameEnd2.getPayload());
    }
}
```

This integration guide provides complete implementation examples for all system components using the shared protocol library. Each component can be developed and tested independently while maintaining protocol consistency.