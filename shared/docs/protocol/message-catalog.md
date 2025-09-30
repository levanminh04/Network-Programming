# Message Protocol Catalog - Complete Reference

## 🎯 **OVERVIEW**

This document provides a comprehensive catalog of all message types, their purposes, payload structures, and usage examples. It serves as the **Single Source of Truth** for protocol implementation across Frontend, Gateway, and Core modules.

### **Message Flow Architecture**
```
Frontend (React) ↔ Gateway (Spring WS) ↔ Core (TCP Server)
        JSON                JSON/TCP              Internal
```

### **Message Structure Standard**
```json
{
    "type": "DOMAIN.ACTION[.MODIFIER]",
    "correlationId": "uuid-v4-string",
    "timestamp": 1736412000000,
    "userId": "user-123",
    "sessionId": "session-abc",
    "matchId": "match-456",
    "version": "1.0.0",
    "payload": { ... },
    "error": { ... }
}
```

---

## 🔐 **AUTHENTICATION DOMAIN (AUTH)**

### **AUTH.LOGIN_REQUEST**
**Purpose**: Authenticate user with credentials  
**Direction**: Client → Server  
**Requires Auth**: No  

**Payload Schema**:
```json
{
    "username": "string (3-50 chars, alphanumeric + underscore/dash)",
    "password": "string (6-100 chars)",
    "clientVersion": "string (optional)",
    "rememberMe": "boolean (optional, default: false)"
}
```

**Example**:
```json
{
    "type": "AUTH.LOGIN_REQUEST",
    "correlationId": "auth-12345-67890",
    "timestamp": 1736412000000,
    "payload": {
        "username": "player_one",
        "password": "securePassword123",
        "clientVersion": "1.0.0",
        "rememberMe": true
    }
}
```

**Responses**: `AUTH.LOGIN_SUCCESS` | `AUTH.LOGIN_FAILURE`

---

### **AUTH.LOGIN_SUCCESS**
**Purpose**: Successful authentication response  
**Direction**: Server → Client  
**Requires Auth**: No (this establishes auth)  

**Payload Schema**:
```json
{
    "userId": "string (unique identifier)",
    "username": "string",
    "displayName": "string (optional)",
    "token": "string (JWT token)",
    "expiresAt": "number (epoch milliseconds)",
    "refreshToken": "string (optional)",
    "score": "number (user's current score)",
    "gamesPlayed": "number",
    "gamesWon": "number",
    "rank": "string (Bronze/Silver/Gold/Platinum/Diamond)",
    "lastLogin": "number (epoch milliseconds)"
}
```

**Example**:
```json
{
    "type": "AUTH.LOGIN_SUCCESS",
    "correlationId": "auth-12345-67890",
    "timestamp": 1736412001000,
    "payload": {
        "userId": "user-123",
        "username": "player_one",
        "displayName": "Player One",
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresAt": 1736415600000,
        "score": 1250.5,
        "gamesPlayed": 47,
        "gamesWon": 23,
        "rank": "Gold",
        "lastLogin": 1736400000000
    }
}
```

---

### **AUTH.LOGIN_FAILURE**
**Purpose**: Failed authentication response  
**Direction**: Server → Client  
**Requires Auth**: No  

**Payload Schema**:
```json
{
    "reason": "string (INVALID_CREDENTIALS|ACCOUNT_LOCKED|SERVER_ERROR)",
    "message": "string (human-readable error)",
    "retryAfter": "number (optional, seconds until retry allowed)",
    "lockoutExpires": "number (optional, epoch milliseconds)"
}
```

**Example**:
```json
{
    "type": "AUTH.LOGIN_FAILURE",
    "correlationId": "auth-12345-67890",
    "timestamp": 1736412001000,
    "payload": {
        "reason": "INVALID_CREDENTIALS",
        "message": "Invalid username or password",
        "retryAfter": 5
    }
}
```

---

## 🏛️ **LOBBY DOMAIN**

### **LOBBY.JOIN_REQUEST**
**Purpose**: Join the game lobby  
**Direction**: Client → Server  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "preferredGameMode": "string (optional: QUICK|RANKED|CUSTOM)",
    "inviteCode": "string (optional, for private lobbies)"
}
```

**Example**:
```json
{
    "type": "LOBBY.JOIN_REQUEST",
    "correlationId": "lobby-join-123",
    "timestamp": 1736412005000,
    "userId": "user-123",
    "payload": {
        "preferredGameMode": "QUICK"
    }
}
```

---

### **LOBBY.PLAYER_LIST_UPDATE**
**Purpose**: Broadcast current lobby players  
**Direction**: Server → Client (Broadcast)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "players": [
        {
            "userId": "string",
            "username": "string",
            "displayName": "string",
            "score": "number",
            "rank": "string",
            "status": "string (ONLINE|IN_GAME|AWAY)",
            "isInvitable": "boolean"
        }
    ],
    "totalOnline": "number",
    "totalInGame": "number"
}
```

**Example**:
```json
{
    "type": "LOBBY.PLAYER_LIST_UPDATE",
    "correlationId": "broadcast-456",
    "timestamp": 1736412010000,
    "payload": {
        "players": [
            {
                "userId": "user-123",
                "username": "player_one",
                "displayName": "Player One",
                "score": 1250.5,
                "rank": "Gold",
                "status": "ONLINE",
                "isInvitable": true
            },
            {
                "userId": "user-456",
                "username": "player_two",
                "displayName": "Player Two",
                "score": 987.0,
                "rank": "Silver",
                "status": "IN_GAME",
                "isInvitable": false
            }
        ],
        "totalOnline": 15,
        "totalInGame": 8
    }
}
```

---

### **LOBBY.MATCH_REQUEST**
**Purpose**: Request to find a match  
**Direction**: Client → Server  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "gameMode": "string (QUICK|RANKED|CUSTOM)",
    "targetOpponent": "string (optional, specific user ID to challenge)",
    "timeoutSeconds": "number (optional, max wait time)"
}
```

**Example**:
```json
{
    "type": "LOBBY.MATCH_REQUEST",
    "correlationId": "match-req-789",
    "timestamp": 1736412015000,
    "userId": "user-123",
    "payload": {
        "gameMode": "QUICK",
        "timeoutSeconds": 30
    }
}
```

---

## 🎮 **GAME DOMAIN**

### **GAME.MATCH_FOUND**
**Purpose**: Notify players that a match has been found  
**Direction**: Server → Client (to both players)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string (unique match identifier)",
    "opponent": {
        "userId": "string",
        "username": "string",
        "displayName": "string",
        "score": "number",
        "rank": "string"
    },
    "gameMode": "string",
    "estimatedStartTime": "number (epoch milliseconds)",
    "rules": {
        "totalRounds": "number (default: 3)",
        "timePerRound": "number (seconds, default: 10)",
        "deckType": "string (STANDARD_52)"
    }
}
```

**Example**:
```json
{
    "type": "GAME.MATCH_FOUND",
    "correlationId": "match-found-101",
    "timestamp": 1736412020000,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "opponent": {
            "userId": "user-456",
            "username": "player_two",
            "displayName": "Player Two",
            "score": 987.0,
            "rank": "Silver"
        },
        "gameMode": "QUICK",
        "estimatedStartTime": 1736412025000,
        "rules": {
            "totalRounds": 3,
            "timePerRound": 10,
            "deckType": "STANDARD_52"
        }
    }
}
```

---

### **GAME.START**
**Purpose**: Game session officially begins  
**Direction**: Server → Client (to both players)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "playerPosition": "string (PLAYER_1|PLAYER_2)",
    "currentRound": "number (starting at 1)",
    "gameState": "string (IN_PROGRESS)",
    "deckInfo": {
        "totalCards": "number (52)",
        "cardsPerPlayer": "number (52 - same deck)"
    },
    "timeSettings": {
        "roundTimeLimit": "number (seconds)",
        "totalGameTimeLimit": "number (seconds, optional)"
    }
}
```

**Example**:
```json
{
    "type": "GAME.START",
    "correlationId": "game-start-202",
    "timestamp": 1736412025000,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "playerPosition": "PLAYER_1",
        "currentRound": 1,
        "gameState": "IN_PROGRESS",
        "deckInfo": {
            "totalCards": 52,
            "cardsPerPlayer": 52
        },
        "timeSettings": {
            "roundTimeLimit": 10
        }
    }
}
```

---

### **GAME.ROUND_START**
**Purpose**: New round begins, players can select cards  
**Direction**: Server → Client (to both players)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "roundNumber": "number (1, 2, or 3)",
    "deadline": "number (epoch milliseconds when round ends)",
    "availableCards": [
        {
            "index": "number (0-51)",
            "rank": "string",
            "suit": "string",
            "value": "number",
            "displayName": "string"
        }
    ],
    "roundScore": {
        "player1": "number",
        "player2": "number"
    }
}
```

**Example**:
```json
{
    "type": "GAME.ROUND_START",
    "correlationId": "round-start-303",
    "timestamp": 1736412030000,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "roundNumber": 1,
        "deadline": 1736412040000,
        "availableCards": [
            {
                "index": 0,
                "rank": "A",
                "suit": "HEARTS",
                "value": 1,
                "displayName": "A♥"
            },
            {
                "index": 1,
                "rank": "2",
                "suit": "HEARTS", 
                "value": 2,
                "displayName": "2♥"
            }
            // ... all 52 cards (first round) or remaining cards
        ],
        "roundScore": {
            "player1": 0,
            "player2": 0
        }
    }
}
```

---

### **GAME.CARD_PLAY_REQUEST**
**Purpose**: Player selects a card to play  
**Direction**: Client → Server  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "roundNumber": "number",
    "cardIndex": "number (0-51, index in deck)",
    "timestamp": "number (when player made selection)"
}
```

**Example**:
```json
{
    "type": "GAME.CARD_PLAY_REQUEST",
    "correlationId": "card-play-404",
    "timestamp": 1736412035000,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "roundNumber": 1,
        "cardIndex": 25,
        "timestamp": 1736412035000
    }
}
```

---

### **GAME.CARD_PLAY_ACK**
**Purpose**: Server acknowledges card selection  
**Direction**: Server → Client (to the player who played)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "roundNumber": "number",
    "playedCard": {
        "index": "number",
        "rank": "string",
        "suit": "string",
        "value": "number",
        "displayName": "string"
    },
    "waitingForOpponent": "boolean"
}
```

**Example**:
```json
{
    "type": "GAME.CARD_PLAY_ACK",
    "correlationId": "card-play-404",
    "timestamp": 1736412035100,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "roundNumber": 1,
        "playedCard": {
            "index": 25,
            "rank": "K",
            "suit": "CLUBS",
            "value": 13,
            "displayName": "K♣"
        },
        "waitingForOpponent": true
    }
}
```

---

### **GAME.OPPONENT_READY**
**Purpose**: Notify player that opponent has made their selection  
**Direction**: Server → Client (to the waiting player)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "roundNumber": "number",
    "message": "string (optional notification message)"
}
```

**Example**:
```json
{
    "type": "GAME.OPPONENT_READY",
    "correlationId": "opponent-ready-505",
    "timestamp": 1736412036000,
    "userId": "user-456",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "roundNumber": 1,
        "message": "Opponent has selected their card"
    }
}
```

---

### **GAME.ROUND_REVEAL**
**Purpose**: Reveal both players' cards simultaneously  
**Direction**: Server → Client (to both players)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "roundNumber": "number",
    "player1Card": {
        "index": "number",
        "rank": "string",
        "suit": "string",
        "value": "number",
        "displayName": "string"
    },
    "player2Card": {
        "index": "number", 
        "rank": "string",
        "suit": "string",
        "value": "number",
        "displayName": "string"
    },
    "roundWinner": "string (PLAYER_1|PLAYER_2|TIE)",
    "player1RoundScore": "number",
    "player2RoundScore": "number",
    "player1TotalScore": "number",
    "player2TotalScore": "number"
}
```

**Example**:
```json
{
    "type": "GAME.ROUND_REVEAL",
    "correlationId": "round-reveal-606",
    "timestamp": 1736412040000,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "roundNumber": 1,
        "player1Card": {
            "index": 25,
            "rank": "K",
            "suit": "CLUBS",
            "value": 13,
            "displayName": "K♣"
        },
        "player2Card": {
            "index": 38,
            "rank": "Q",
            "suit": "SPADES",
            "value": 12,
            "displayName": "Q♠"
        },
        "roundWinner": "PLAYER_1",
        "player1RoundScore": 13,
        "player2RoundScore": 12,
        "player1TotalScore": 13,
        "player2TotalScore": 12
    }
}
```

---

### **GAME.END**
**Purpose**: Game session ends with final results  
**Direction**: Server → Client (to both players)  
**Requires Auth**: Yes  

**Payload Schema**:
```json
{
    "matchId": "string",
    "gameResult": "string (PLAYER_1_WINS|PLAYER_2_WINS|TIE)",
    "finalScores": {
        "player1": "number",
        "player2": "number"
    },
    "gameStatistics": {
        "duration": "number (seconds)",
        "totalRounds": "number",
        "averageRoundTime": "number (seconds)"
    },
    "scoreChanges": {
        "player1": {
            "oldScore": "number",
            "newScore": "number",
            "change": "number (+/- points gained/lost)"
        },
        "player2": {
            "oldScore": "number", 
            "newScore": "number",
            "change": "number"
        }
    },
    "achievements": [
        {
            "playerId": "string",
            "achievementId": "string",
            "name": "string",
            "description": "string"
        }
    ]
}
```

**Example**:
```json
{
    "type": "GAME.END",
    "correlationId": "game-end-707",
    "timestamp": 1736412100000,
    "userId": "user-123",
    "matchId": "match-abc-def",
    "payload": {
        "matchId": "match-abc-def",
        "gameResult": "PLAYER_1_WINS",
        "finalScores": {
            "player1": 26,
            "player2": 24
        },
        "gameStatistics": {
            "duration": 75,
            "totalRounds": 3,
            "averageRoundTime": 8.5
        },
        "scoreChanges": {
            "player1": {
                "oldScore": 1250.5,
                "newScore": 1275.5,
                "change": 25.0
            },
            "player2": {
                "oldScore": 987.0,
                "newScore": 977.0,
                "change": -10.0
            }
        },
        "achievements": [
            {
                "playerId": "user-123",
                "achievementId": "perfect_game",
                "name": "Perfect Game",
                "description": "Won all 3 rounds"
            }
        ]
    }
}
```

---

## 🛠️ **SYSTEM DOMAIN**

### **SYSTEM.WELCOME**
**Purpose**: Welcome message after connection  
**Direction**: Server → Client  
**Requires Auth**: No  

**Payload Schema**:
```json
{
    "message": "string",
    "serverVersion": "string",
    "protocolVersion": "string",
    "serverTime": "number (epoch milliseconds)",
    "features": ["string array of supported features"],
    "maintenanceScheduled": "number (optional, epoch milliseconds)"
}
```

---

### **SYSTEM.ERROR**
**Purpose**: Generic error notification  
**Direction**: Server → Client  
**Requires Auth**: No  

**Payload Schema**:
```json
{
    "code": "string (error code from ErrorCode enum)",
    "message": "string (human-readable error)",
    "details": "object (optional additional error context)",
    "retryable": "boolean",
    "retryAfter": "number (optional, seconds)"
}
```

---

### **SYSTEM.HEARTBEAT**
**Purpose**: Keep connection alive  
**Direction**: Client ↔ Server (bidirectional)  
**Requires Auth**: Optional  

**Payload Schema**:
```json
{
    "timestamp": "number (sender's timestamp)",
    "sessionInfo": "object (optional connection stats)"
}
```

---

## 📋 **MESSAGE VALIDATION RULES**

### **Required Fields (All Messages)**
- `type`: Must be valid MessageType constant
- `correlationId`: Must be valid UUID or unique string
- `timestamp`: Must be valid epoch milliseconds

### **Authentication Context**
- Messages requiring auth must include valid `userId`
- Game messages must include valid `matchId`
- Session-specific messages must include `sessionId`

### **Size Limits**
- Total message size: 64KB maximum
- Payload size: 32KB maximum  
- String fields: 1000 characters maximum (unless specified)

### **Rate Limits (per minute)**
- AUTH messages: 5 requests
- LOBBY messages: 10 requests
- GAME messages: 60 requests
- SYSTEM messages: 120 requests

This catalog provides complete protocol specification for reliable implementation across all system components.