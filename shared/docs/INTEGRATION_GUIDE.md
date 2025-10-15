# 🔗 SHARED MODULE - INTEGRATION GUIDE

**Version**: 1.0.0 (MVP)  
**Last Updated**: 2025-01-05  
**Audience**: Frontend & Backend Developers  

---

## 📋 **TABLE OF CONTENTS**

1. [Overview](#1-overview)
2. [Backend Integration (Java/Spring Boot)](#2-backend-integration-javaspring-boot)
3. [Frontend Integration (React/JavaScript)](#3-frontend-integration-reactjavascript)
4. [Message Protocol Usage](#4-message-protocol-usage)
5. [Validation & Error Handling](#5-validation--error-handling)
6. [Testing Integration](#6-testing-integration)
7. [Best Practices](#7-best-practices)
8. [Troubleshooting](#8-troubleshooting)

---

## **1. OVERVIEW**

### **1.1. Purpose of Shared Module**

The `shared` module provides:
- **Common DTOs** for data exchange between Frontend ↔ Gateway ↔ Core
- **Message Protocol** definitions (MessageEnvelope, MessageType)
- **Enumerations** (CardSuit, CardRank, GameState, ErrorCode)
- **Constants** (GameConstants, ProtocolConstants, ValidationConstants)
- **Utilities** (JsonUtils, ValidationUtils, IdUtils)

### **1.2. Module Independence**

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Frontend   │     │   Gateway   │     │    Core     │
│  (React)    │     │ (Spring WS) │     │  (TCP/Java) │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │                   │                   │
       └───────────────────┴───────────────────┘
                           │
                    ┌──────▼──────┐
                    │   SHARED    │
                    │   MODULE    │
                    └─────────────┘
```

**Key Principle**: Shared module has ZERO dependencies on other modules.

---

## **2. BACKEND INTEGRATION (Java/Spring Boot)**

### **2.1. Maven Dependency**

#### **In Core Module (pom.xml)**

```xml
<dependencies>
    <!-- Shared Module -->
    <dependency>
        <groupId>com.N9</groupId>
        <artifactId>shared</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Other dependencies -->
</dependencies>
```

#### **In Gateway Module (pom.xml)**

```xml
<dependencies>
    <!-- Shared Module -->
    <dependency>
        <groupId>com.N9</groupId>
        <artifactId>shared</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
</dependencies>
```

---

### **2.2. Import DTOs**

```java
// Authentication DTOs
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.model.dto.auth.LoginSuccessDto;
import com.n9.shared.model.dto.auth.RegisterRequestDto;

// Game DTOs
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.model.dto.game.GameResultDto;

// Protocol
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.shared.protocol.MessageFactory;

// Enums
import com.n9.shared.model.enums.CardRank;
import com.n9.shared.model.enums.CardSuit;
import com.n9.shared.model.enums.GameState;
import com.n9.shared.model.enums.PlayerStatus;

// Constants
import com.n9.shared.constants.GameConstants;
import com.n9.shared.constants.ProtocolConstants;

// Utilities
import com.n9.shared.util.JsonUtils;
import com.n9.shared.util.ValidationUtils;
import com.n9.shared.util.IdUtils;
```

---

### **2.3. Example: Login Handler (Core)**

```java
package com.n9.core.handler;

import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.model.dto.auth.LoginSuccessDto;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.JsonUtils;
import com.n9.shared.util.ValidationUtils;

public class LoginHandler {
    
    public MessageEnvelope handleLogin(MessageEnvelope request) {
        // 1. Extract payload
        LoginRequestDto loginRequest = JsonUtils.convertPayload(
            request.getPayload(), 
            LoginRequestDto.class
        );
        
        // 2. Validate
        ValidationUtils.ValidationResult validation = 
            ValidationUtils.validate(loginRequest);
        
        if (!validation.isValid()) {
            return MessageFactory.createError(
                request.getCorrelationId(),
                new ErrorInfo(
                    ErrorCode.VALIDATION_INVALID_INPUT,
                    validation.getErrorMessage()
                )
            );
        }
        
        // 3. Authenticate
        User user = userService.authenticate(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        );
        
        if (user == null) {
            return MessageFactory.createError(
                request.getCorrelationId(),
                new ErrorInfo(
                    ErrorCode.AUTH_INVALID_CREDENTIALS,
                    "Invalid username or password"
                )
            );
        }
        
        // 4. Create response
        LoginSuccessDto response = new LoginSuccessDto.Builder()
            .userId(user.getId())
            .username(user.getUsername())
            .displayName(user.getDisplayName())
            .gamesPlayed(user.getGamesPlayed())
            .gamesWon(user.getGamesWon())
            .lastLogin(System.currentTimeMillis())
            .build();
        
        return MessageFactory.createResponse(
            MessageType.AUTH_LOGIN_SUCCESS,
            request.getCorrelationId(),
            response
        );
    }
}
```

---

### **2.4. Example: WebSocket Handler (Gateway)**

```java
package com.n9.gateway.websocket;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.shared.util.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class GameWebSocketHandler extends TextWebSocketHandler {
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            // 1. Parse incoming message
            String payload = message.getPayload();
            MessageEnvelope envelope = JsonUtils.fromJson(payload, MessageEnvelope.class)
                .orElseThrow(() -> new IllegalArgumentException("Invalid message format"));
            
            // 2. Route based on message type
            String messageType = envelope.getType();
            
            switch (messageType) {
                case MessageType.AUTH_LOGIN_REQUEST:
                    handleLoginRequest(session, envelope);
                    break;
                    
                case MessageType.MATCH_REQUEST:
                    handleMatchRequest(session, envelope);
                    break;
                    
                case MessageType.GAME_PLAY_CARD:
                    handlePlayCard(session, envelope);
                    break;
                    
                // ... other cases
                
                default:
                    sendError(session, envelope.getCorrelationId(), 
                        "Unknown message type: " + messageType);
            }
            
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendSystemError(session, e.getMessage());
        }
    }
    
    private void handleLoginRequest(WebSocketSession session, MessageEnvelope envelope) {
        // Forward to Core server via TCP
        MessageEnvelope response = coreClient.sendAndReceive(envelope);
        
        // Send response back to frontend
        sendMessage(session, response);
    }
    
    private void sendMessage(WebSocketSession session, MessageEnvelope envelope) {
        try {
            String json = JsonUtils.toJson(envelope);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Error sending message", e);
        }
    }
}
```

---

### **2.5. Example: Using Constants**

```java
import com.n9.shared.constants.GameConstants;
import com.n9.shared.constants.ProtocolConstants;

public class GameService {
    
    public Game createGame(String player1Id, String player2Id) {
        Game game = new Game();
        game.setPlayer1Id(player1Id);
        game.setPlayer2Id(player2Id);
        game.setTotalRounds(GameConstants.TOTAL_ROUNDS);  // 3
        game.setRoundTimeoutSeconds(GameConstants.ROUND_TIMEOUT_SECONDS);  // 10
        game.setStatus(GameState.WAITING_FOR_PLAYERS);
        
        return gameRepository.save(game);
    }
    
    public RoundDto startRound(String matchId, int roundNumber) {
        long deadline = System.currentTimeMillis() + 
            GameConstants.ROUND_TIMEOUT_MS;  // +10000ms
        
        RoundDto round = new RoundDto();
        round.setMatchId(matchId);
        round.setRoundNumber(roundNumber);
        round.setDeadlineEpochMs(deadline);
        round.setDurationMs(GameConstants.ROUND_TIMEOUT_MS);
        
        return round;
    }
}
```

---

### **2.6. Example: Using Enums**

```java
import com.n9.shared.model.enums.CardRank;
import com.n9.shared.model.enums.CardSuit;

public class CardService {
    
    public List<Card> generateDeck() {
        List<Card> deck = new ArrayList<>();
        int id = 1;
        
        for (CardSuit suit : CardSuit.values()) {
            for (CardRank rank : CardRank.values()) {
                Card card = new Card();
                card.setId(id++);
                card.setSuit(suit);
                card.setRank(rank);
                card.setValue(rank.getValue());
                deck.add(card);
            }
        }
        
        return deck;  // 4 suits × 9 ranks = 36 cards
    }
    
    public String getCardDisplay(Card card) {
        return card.getRank().getVietnameseName() + " " + 
               card.getSuit().getSymbol();  // "Bảy ♥"
    }
}
```

---

## **3. FRONTEND INTEGRATION (React/JavaScript)**

### **3.1. Install Shared Module (Conceptual)**

Since Java DTOs can't be directly used in JavaScript, we need **Type Definitions**.

#### **Option 1: Manual TypeScript Definitions**

Create `src/types/shared.ts`:

```typescript
// Message Envelope
export interface MessageEnvelope<T = any> {
  type: string;
  correlationId: string;
  timestamp: number;
  userId?: string;
  sessionId?: string;
  matchId?: string;
  version: string;
  payload?: T;
  error?: ErrorInfo;
}

// Auth DTOs
export interface LoginRequestDto {
  username: string;
  password: string;
  clientVersion?: string;
  rememberMe?: boolean;
}

export interface LoginSuccessDto {
  userId: string;
  username: string;
  displayName: string;
  token?: string;
  expiresAt?: number;
  gamesPlayed: number;
  gamesWon: number;
  lastLogin: number;
}

// Game DTOs
export interface PlayCardRequestDto {
  matchId: string;
  roundNumber: number;
  cardId: number;
  requestId: string;
}

export interface RoundRevealDto {
  matchId: string;
  roundNumber: number;
  player1Card: CardDto;
  player2Card: CardDto;
  player1IsAutoPicked: boolean;
  player2IsAutoPicked: boolean;
  roundWinnerId: string | null;
  player1RoundScore: number;
  player2RoundScore: number;
  player1TotalScore: number;
  player2TotalScore: number;
}

export interface CardDto {
  id: number;
  rank: string;
  suit: string;
  value: number;
}

// Enums
export enum CardSuit {
  HEARTS = 'H',
  DIAMONDS = 'D',
  CLUBS = 'C',
  SPADES = 'S'
}

export enum CardRank {
  ACE = 'A',
  TWO = '2',
  THREE = '3',
  FOUR = '4',
  FIVE = '5',
  SIX = '6',
  SEVEN = '7',
  EIGHT = '8',
  NINE = '9'
}

export enum PlayerStatus {
  ONLINE = 'online',
  IDLE = 'idle',
  IN_QUEUE = 'in_queue',
  IN_GAME = 'in_game',
  OFFLINE = 'offline'
}

// Constants
export const GameConstants = {
  DECK_SIZE: 36,
  TOTAL_ROUNDS: 3,
  ROUND_TIMEOUT_SECONDS: 10,
  ROUND_TIMEOUT_MS: 10000,
  MIN_CARD_VALUE: 1,
  MAX_CARD_VALUE: 9
};

export const ProtocolConstants = {
  PROTOCOL_VERSION: '1.0.0',
  HEARTBEAT_INTERVAL_SECONDS: 30,
  MAX_MESSAGE_SIZE_BYTES: 1048576
};

// Message Types
export const MessageType = {
  // Auth
  AUTH_LOGIN_REQUEST: 'AUTH.LOGIN_REQUEST',
  AUTH_LOGIN_OK: 'AUTH.LOGIN_OK',
  AUTH_LOGIN_FAIL: 'AUTH.LOGIN_FAIL',
  AUTH_REGISTER_REQUEST: 'AUTH.REGISTER_REQUEST',
  AUTH_LOGOUT_REQUEST: 'AUTH.LOGOUT_REQUEST',
  
  // Lobby
  LOBBY_SNAPSHOT: 'LOBBY.SNAPSHOT',
  LOBBY_UPDATE: 'LOBBY.UPDATE',
  
  // Match
  MATCH_REQUEST: 'MATCH.REQUEST',
  MATCH_CANCEL: 'MATCH.CANCEL',
  MATCH_FOUND: 'MATCH.FOUND',
  MATCH_START: 'MATCH.START',
  MATCH_RESULT: 'MATCH.RESULT',
  MATCH_OPPONENT_LEFT: 'MATCH.OPPONENT_LEFT',
  
  // Game
  GAME_ROUND_START: 'GAME.ROUND_START',
  GAME_PLAY_CARD: 'GAME.PLAY_CARD',
  GAME_PICK_ACK: 'GAME.PICK_ACK',
  GAME_PICK_NACK: 'GAME.PICK_NACK',
  GAME_OPPONENT_STATUS: 'GAME.OPPONENT_STATUS',
  GAME_ROUND_REVEAL: 'GAME.ROUND_REVEAL',
  
  // Leaderboard
  LEADERBOARD_REQUEST: 'LEADERBOARD.REQUEST',
  LEADERBOARD_RESPONSE: 'LEADERBOARD.RESPONSE',
  
  // System
  SYS_PING: 'SYS.PING',
  SYS_PONG: 'SYS.PONG',
  SYS_ERROR: 'SYS.ERROR'
};
```

---

### **3.2. WebSocket Client Service**

```typescript
// src/services/websocket.ts
import { MessageEnvelope, MessageType, ProtocolConstants } from '@/types/shared';

class WebSocketService {
  private ws: WebSocket | null = null;
  private correlationMap = new Map<string, (response: MessageEnvelope) => void>();
  
  connect(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(url);
      
      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.startHeartbeat();
        resolve();
      };
      
      this.ws.onmessage = (event) => {
        const envelope: MessageEnvelope = JSON.parse(event.data);
        this.handleMessage(envelope);
      };
      
      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        reject(error);
      };
      
      this.ws.onclose = () => {
        console.log('WebSocket closed');
        this.stopHeartbeat();
      };
    });
  }
  
  send<T>(type: string, payload: T): Promise<MessageEnvelope> {
    return new Promise((resolve, reject) => {
      const correlationId = this.generateCorrelationId();
      
      const envelope: MessageEnvelope = {
        type,
        correlationId,
        timestamp: Date.now(),
        version: ProtocolConstants.PROTOCOL_VERSION,
        payload
      };
      
      // Store callback for response
      this.correlationMap.set(correlationId, resolve);
      
      // Set timeout
      setTimeout(() => {
        if (this.correlationMap.has(correlationId)) {
          this.correlationMap.delete(correlationId);
          reject(new Error('Request timeout'));
        }
      }, 30000);
      
      // Send message
      this.ws?.send(JSON.stringify(envelope));
    });
  }
  
  private handleMessage(envelope: MessageEnvelope): void {
    // Check if response to a request
    if (this.correlationMap.has(envelope.correlationId)) {
      const callback = this.correlationMap.get(envelope.correlationId)!;
      this.correlationMap.delete(envelope.correlationId);
      callback(envelope);
      return;
    }
    
    // Handle server-initiated messages
    this.handleServerMessage(envelope);
  }
  
  private handleServerMessage(envelope: MessageEnvelope): void {
    switch (envelope.type) {
      case MessageType.LOBBY_UPDATE:
        // Dispatch to Redux store
        store.dispatch(lobbyActions.updateLobby(envelope.payload));
        break;
        
      case MessageType.GAME_ROUND_START:
        store.dispatch(gameActions.startRound(envelope.payload));
        break;
        
      case MessageType.GAME_ROUND_REVEAL:
        store.dispatch(gameActions.revealCards(envelope.payload));
        break;
        
      // ... other cases
    }
  }
  
  private heartbeatInterval: NodeJS.Timeout | null = null;
  
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      this.send(MessageType.SYS_PING, {
        nonce: Math.random().toString(36),
        clientTimestamp: Date.now()
      });
    }, ProtocolConstants.HEARTBEAT_INTERVAL_SECONDS * 1000);
  }
  
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
  }
  
  private generateCorrelationId(): string {
    return `cor-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

export const wsService = new WebSocketService();
```

---

### **3.3. Example: Login Component**

```typescript
// src/components/auth/Login.tsx
import React, { useState } from 'react';
import { wsService } from '@/services/websocket';
import { MessageType, LoginRequestDto, LoginSuccessDto } from '@/types/shared';

export const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  
  const handleLogin = async () => {
    setLoading(true);
    
    try {
      const request: LoginRequestDto = {
        username,
        password,
        clientVersion: '1.0.0',
        rememberMe: true
      };
      
      const response = await wsService.send<LoginRequestDto>(
        MessageType.AUTH_LOGIN_REQUEST,
        request
      );
      
      if (response.type === MessageType.AUTH_LOGIN_OK) {
        const payload = response.payload as LoginSuccessDto;
        
        // Save user to Redux store
        dispatch(authActions.loginSuccess(payload));
        
        // Navigate to lobby
        navigate('/lobby');
      } else if (response.type === MessageType.AUTH_LOGIN_FAIL) {
        alert('Login failed: ' + response.error?.message);
      }
      
    } catch (error) {
      console.error('Login error:', error);
      alert('Network error');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="login-form">
      <input 
        type="text" 
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="Username"
      />
      <input 
        type="password" 
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Password"
      />
      <button onClick={handleLogin} disabled={loading}>
        {loading ? 'Logging in...' : 'Login'}
      </button>
    </div>
  );
};
```

---

### **3.4. Example: Play Card Component**

```typescript
// src/components/game/GameBoard.tsx
import React from 'react';
import { wsService } from '@/services/websocket';
import { MessageType, PlayCardRequestDto, GameConstants } from '@/types/shared';
import { useSelector } from 'react-redux';

export const GameBoard: React.FC = () => {
  const { matchId, roundNumber, availableCards } = useSelector(state => state.game);
  
  const handleCardClick = async (cardId: number) => {
    const request: PlayCardRequestDto = {
      matchId,
      roundNumber,
      cardId,
      requestId: `req-${Date.now()}`
    };
    
    try {
      const response = await wsService.send(
        MessageType.GAME_PLAY_CARD,
        request
      );
      
      if (response.type === MessageType.GAME_PICK_ACK) {
        console.log('Card accepted:', response.payload);
        // UI will update via GAME_ROUND_REVEAL message
      } else if (response.type === MessageType.GAME_PICK_NACK) {
        alert('Card rejected: ' + response.payload.message);
      }
      
    } catch (error) {
      console.error('Play card error:', error);
    }
  };
  
  return (
    <div className="game-board">
      <div className="available-cards">
        {availableCards.map(card => (
          <div 
            key={card.id} 
            className="card"
            onClick={() => handleCardClick(card.id)}
          >
            {card.rank} {card.suit}
          </div>
        ))}
      </div>
      
      <div className="round-info">
        Round {roundNumber} / {GameConstants.TOTAL_ROUNDS}
      </div>
    </div>
  );
};
```

---

## **4. MESSAGE PROTOCOL USAGE**

### **4.1. Creating Messages (Backend)**

```java
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.protocol.MessageType;

// Request
MessageEnvelope request = MessageFactory.createRequest(
    MessageType.MATCH_REQUEST,
    matchRequestDto
);

// Response
MessageEnvelope response = MessageFactory.createResponse(
    MessageType.MATCH_FOUND,
    request.getCorrelationId(),
    matchFoundDto
);

// Error
MessageEnvelope error = MessageFactory.createError(
    request.getCorrelationId(),
    new ErrorInfo(ErrorCode.GAME_CARD_NOT_AVAILABLE, "Card already taken")
);
```

### **4.2. Creating Messages (Frontend)**

```typescript
const createMessage = <T>(type: string, payload: T): MessageEnvelope<T> => {
  return {
    type,
    correlationId: generateId(),
    timestamp: Date.now(),
    version: ProtocolConstants.PROTOCOL_VERSION,
    payload
  };
};

const loginMessage = createMessage(MessageType.AUTH_LOGIN_REQUEST, {
  username: 'player01',
  password: 'secret'
});
```

---

## **5. VALIDATION & ERROR HANDLING**

### **5.1. Backend Validation**

```java
import com.n9.shared.util.ValidationUtils;

public MessageEnvelope handleRequest(MessageEnvelope request) {
    // Extract DTO
    LoginRequestDto dto = JsonUtils.convertPayload(
        request.getPayload(), 
        LoginRequestDto.class
    );
    
    // Validate
    ValidationUtils.ValidationResult result = ValidationUtils.validate(dto);
    
    if (!result.isValid()) {
        return MessageFactory.createError(
            request.getCorrelationId(),
            new ErrorInfo(
                ErrorCode.VALIDATION_INVALID_INPUT,
                result.getErrorMessage(),
                result.getFieldErrors()
            )
        );
    }
    
    // Process request...
}
```

### **5.2. Frontend Validation**

```typescript
const validateLoginRequest = (data: LoginRequestDto): string | null => {
  if (!data.username || data.username.length < 3) {
    return 'Username must be at least 3 characters';
  }
  
  if (!data.password || data.password.length < 6) {
    return 'Password must be at least 6 characters';
  }
  
  if (!/^[a-zA-Z0-9_-]+$/.test(data.username)) {
    return 'Username can only contain letters, numbers, underscore and dash';
  }
  
  return null;
};
```

---

## **6. TESTING INTEGRATION**

### **6.1. Backend Unit Test**

```java
import org.junit.jupiter.api.Test;
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.util.JsonUtils;
import com.n9.shared.util.ValidationUtils;

class LoginRequestDtoTest {
    
    @Test
    void testValidLoginRequest() {
        LoginRequestDto dto = new LoginRequestDto("player01", "secret123");
        
        ValidationUtils.ValidationResult result = ValidationUtils.validate(dto);
        
        assertTrue(result.isValid());
    }
    
    @Test
    void testInvalidUsername() {
        LoginRequestDto dto = new LoginRequestDto("ab", "secret123");  // Too short
        
        ValidationUtils.ValidationResult result = ValidationUtils.validate(dto);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("between 3 and 50"));
    }
    
    @Test
    void testSerialization() {
        LoginRequestDto dto = new LoginRequestDto("player01", "secret123");
        
        String json = JsonUtils.toJson(dto);
        LoginRequestDto deserialized = JsonUtils.fromJson(json, LoginRequestDto.class).orElseThrow();
        
        assertEquals(dto.getUsername(), deserialized.getUsername());
    }
}
```

### **6.2. Frontend Unit Test**

```typescript
// __tests__/types/shared.test.ts
import { LoginRequestDto, MessageType } from '@/types/shared';

describe('Shared Types', () => {
  test('MessageType constants are defined', () => {
    expect(MessageType.AUTH_LOGIN_REQUEST).toBe('AUTH.LOGIN_REQUEST');
    expect(MessageType.GAME_PLAY_CARD).toBe('GAME.PLAY_CARD');
  });
  
  test('LoginRequestDto structure', () => {
    const dto: LoginRequestDto = {
      username: 'player01',
      password: 'secret123',
      clientVersion: '1.0.0',
      rememberMe: true
    };
    
    expect(dto.username).toBe('player01');
    expect(dto.rememberMe).toBe(true);
  });
});
```

---

## **7. BEST PRACTICES**

### **7.1. DO's**

✅ **Always validate DTOs** before processing  
✅ **Use MessageFactory** for creating envelopes  
✅ **Use Constants** instead of magic numbers  
✅ **Use Enums** for type-safe state values  
✅ **Include correlationId** in all requests  
✅ **Handle errors gracefully** with ErrorInfo  
✅ **Version your messages** with protocol version  
✅ **Test serialization/deserialization** for all DTOs  

### **7.2. DON'Ts**

❌ **Don't hardcode message types** - use MessageType constants  
❌ **Don't skip validation** - always validate inputs  
❌ **Don't modify shared DTOs** in module-specific code  
❌ **Don't create duplicate DTOs** - reuse shared definitions  
❌ **Don't ignore correlationId** - use it for request/response matching  
❌ **Don't send sensitive data** in logs (use toString() with [HIDDEN])  

---

## **8. TROUBLESHOOTING**

### **8.1. Common Issues**

#### **Issue 1: JSON Deserialization Fails**

**Symptom**: `JsonProcessingException` or `Cannot deserialize`

**Solution**:
```java
// Ensure DTOs have default constructor
public class MyDto {
    public MyDto() {}  // Required for Jackson
    
    public MyDto(String field1, String field2) {
        this.field1 = field1;
        this.field2 = field2;
    }
}
```

#### **Issue 2: Validation Always Fails**

**Symptom**: ValidationUtils returns false for valid data

**Solution**:
```java
// Check @NotBlank vs @NotNull
@NotBlank  // For strings - checks non-null AND non-empty
@NotNull   // For objects - checks non-null only
```

#### **Issue 3: Frontend Types Mismatch**

**Symptom**: TypeScript errors when using DTOs

**Solution**:
- Ensure TypeScript definitions match Java DTOs exactly
- Use optional properties (`?`) for optional fields
- Check enum values match backend

#### **Issue 4: Message Not Routed**

**Symptom**: Message received but not handled

**Solution**:
```java
// Check message type exactly matches
if (MessageType.AUTH_LOGIN_REQUEST.equals(envelope.getType())) {
    // Handle login
}
```

---

## **9. MIGRATION GUIDE**

### **9.1. Updating Shared Module**

When shared module is updated:

1. **Backend**:
   ```bash
   mvn clean install -pl shared
   mvn clean install -pl core
   mvn clean install -pl gateway
   ```

2. **Frontend**:
   - Update TypeScript definitions in `src/types/shared.ts`
   - Run type checking: `npm run type-check`
   - Update tests if needed

3. **Test compatibility**:
   - Run integration tests
   - Verify message serialization
   - Check protocol version compatibility

---

## ✅ **INTEGRATION CHECKLIST**

### **Backend**
- [ ] Maven dependency added to pom.xml
- [ ] DTOs imported correctly
- [ ] Validation implemented with ValidationUtils
- [ ] MessageFactory used for creating envelopes
- [ ] Constants used instead of magic numbers
- [ ] Error handling with ErrorInfo
- [ ] Unit tests written for all handlers

### **Frontend**
- [ ] TypeScript definitions created
- [ ] WebSocket service implemented
- [ ] Message routing configured
- [ ] Redux integration for state management
- [ ] Validation implemented client-side
- [ ] Error handling UI implemented
- [ ] Components use shared types

---

**For detailed message examples, see** `MESSAGE_CATALOG.md`.  
**For DTO schemas, see** `DTO_SCHEMAS.md`.
