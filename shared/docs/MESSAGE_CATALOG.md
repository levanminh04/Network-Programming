# 📬 MESSAGE CATALOG - Complete Reference

**Version**: 1.0.0 (MVP)  
**Last Updated**: 2025-01-05  
**Purpose**: Comprehensive catalog of all message types with JSON examples  

---

## 📋 **TABLE OF CONTENTS**

1. [Authentication Messages](#1-authentication-messages)
2. [Lobby Messages](#2-lobby-messages)
3. [Match Messages](#3-match-messages)
4. [Game Messages](#4-game-messages)
5. [Leaderboard Messages](#5-leaderboard-messages)
6. [System Messages](#6-system-messages)
7. [Error Handling](#7-error-handling)
8. [Message Flow Diagrams](#8-message-flow-diagrams)

---

## **1. AUTHENTICATION MESSAGES**

### **1.1. AUTH.LOGIN_REQUEST**

**Direction**: Frontend → Gateway → Core  
**Purpose**: User authentication with credentials  
**DTO**: `LoginRequestDto`

```json
{
  "type": "AUTH.LOGIN_REQUEST",
  "correlationId": "cor-12345-abc",
  "timestamp": 1736412000000,
  "version": "1.0.0",
  "payload": {
    "username": "player01",
    "password": "secret123",
    "clientVersion": "1.0.0",
    "rememberMe": true
  }
}
```

---

### **1.2. AUTH.LOGIN_OK**

**Direction**: Core → Gateway → Frontend  
**Purpose**: Successful authentication response  
**DTO**: `LoginSuccessDto`

```json
{
  "type": "AUTH.LOGIN_OK",
  "correlationId": "cor-12345-abc",
  "timestamp": 1736412001500,
  "userId": "user-abc123",
  "version": "1.0.0",
  "payload": {
    "userId": "user-abc123",
    "username": "player01",
    "displayName": "Player One",
    "token": "jwt-token-here",
    "expiresAt": 1736498400000,
    "gamesPlayed": 10,
    "gamesWon": 6,
    "lastLogin": 1736412001500
  }
}
```

---

### **1.3. AUTH.LOGIN_FAIL**

**Direction**: Core → Gateway → Frontend  
**Purpose**: Authentication failure  
**DTO**: `LoginFailureDto`

```json
{
  "type": "AUTH.LOGIN_FAIL",
  "correlationId": "cor-12345-abc",
  "timestamp": 1736412001500,
  "version": "1.0.0",
  "payload": {
    "errorCode": "AUTH_INVALID_CREDENTIALS",
    "message": "Invalid username or password",
    "attemptNumber": 1,
    "maxAttempts": 5,
    "lockoutAfter": 5
  }
}
```

---

### **1.4. AUTH.REGISTER_REQUEST**

**Direction**: Frontend → Gateway → Core  
**Purpose**: New user registration  
**DTO**: `RegisterRequestDto`

```json
{
  "type": "AUTH.REGISTER_REQUEST",
  "correlationId": "cor-reg-001",
  "timestamp": 1736412005000,
  "version": "1.0.0",
  "payload": {
    "username": "newplayer",
    "email": "newplayer@example.com",
    "password": "securepass123",
    "displayName": "New Player"
  }
}
```

---

### **1.5. AUTH.REGISTER_SUCCESS**

**Direction**: Core → Gateway → Frontend  
**Purpose**: Registration successful  
**DTO**: `RegisterResponseDto`

```json
{
  "type": "AUTH.REGISTER_SUCCESS",
  "correlationId": "cor-reg-001",
  "timestamp": 1736412006000,
  "version": "1.0.0",
  "payload": {
    "userId": "user-xyz789",
    "username": "newplayer",
    "message": "Registration successful. You can now login.",
    "success": true
  }
}
```

---

### **1.6. AUTH.LOGOUT_REQUEST**

**Direction**: Frontend → Gateway → Core  
**Purpose**: User logout  
**DTO**: `LogoutRequestDto`

```json
{
  "type": "AUTH.LOGOUT_REQUEST",
  "correlationId": "cor-logout-001",
  "timestamp": 1736412100000,
  "userId": "user-abc123",
  "sessionId": "sess-xyz",
  "version": "1.0.0",
  "payload": {}
}
```

---

## **2. LOBBY MESSAGES**

### **2.1. LOBBY.SNAPSHOT**

**Direction**: Core → Gateway → Frontend  
**Purpose**: Full lobby state (sent after login)  
**DTO**: `LobbySnapshotDto`

```json
{
  "type": "LOBBY.SNAPSHOT",
  "correlationId": "cor-lobby-001",
  "timestamp": 1736412010000,
  "userId": "user-abc123",
  "version": "1.0.0",
  "payload": {
    "timestamp": 1736412010000,
    "totalOnline": 15,
    "onlinePlayers": [
      {
        "userId": "user-abc123",
        "username": "player01",
        "displayName": "Player One",
        "status": "IDLE",
        "gamesWon": 6,
        "gamesPlayed": 10,
        "lastActivity": 1736412010000
      },
      {
        "userId": "user-def456",
        "username": "player02",
        "displayName": "Player Two",
        "status": "IN_QUEUE",
        "gamesWon": 3,
        "gamesPlayed": 8,
        "lastActivity": 1736412009000
      }
    ]
  }
}
```

---

### **2.2. LOBBY.UPDATE**

**Direction**: Core → Gateway → Frontend (Broadcast)  
**Purpose**: Delta update when players join/leave/change status  
**DTO**: `LobbyUpdateDto`

```json
{
  "type": "LOBBY.UPDATE",
  "correlationId": "cor-lobby-upd-001",
  "timestamp": 1736412015000,
  "version": "1.0.0",
  "payload": {
    "joined": [
      {
        "userId": "user-ghi789",
        "username": "player03",
        "displayName": "Player Three",
        "status": "IDLE",
        "gamesWon": 0,
        "gamesPlayed": 0,
        "lastActivity": 1736412015000
      }
    ],
    "left": ["user-xyz999"],
    "statusChanged": [
      {
        "userId": "user-def456",
        "oldStatus": "IN_QUEUE",
        "newStatus": "IN_GAME",
        "matchId": "match-001"
      }
    ]
  }
}
```

---

## **3. MATCH MESSAGES**

### **3.1. MATCH.REQUEST**

**Direction**: Frontend → Gateway → Core  
**Purpose**: Player requests quick match  
**DTO**: `MatchRequestDto`

```json
{
  "type": "MATCH.REQUEST",
  "correlationId": "cor-match-req-001",
  "timestamp": 1736412020000,
  "userId": "user-abc123",
  "version": "1.0.0",
  "payload": {
    "gameMode": "QUICK_MATCH",
    "userId": "user-abc123"
  }
}
```

---

### **3.2. MATCH.CANCEL**

**Direction**: Frontend → Gateway → Core  
**Purpose**: Cancel pending match request  
**DTO**: `MatchCancelDto`

```json
{
  "type": "MATCH.CANCEL",
  "correlationId": "cor-match-cancel-001",
  "timestamp": 1736412025000,
  "userId": "user-abc123",
  "version": "1.0.0",
  "payload": {
    "userId": "user-abc123",
    "reason": "USER_CANCELLED"
  }
}
```

---

### **3.3. MATCH.FOUND**

**Direction**: Core → Gateway → Frontend (both players)  
**Purpose**: Match found, opponent information  
**DTO**: `MatchFoundDto`

```json
{
  "type": "MATCH.FOUND",
  "correlationId": "cor-match-req-001",
  "timestamp": 1736412030000,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "opponent": {
      "userId": "user-def456",
      "username": "player02",
      "displayName": "Player Two",
      "gamesWon": 3,
      "gamesPlayed": 8
    },
    "estimatedStartTime": 1736412033000
  }
}
```

---

### **3.4. MATCH.START**

**Direction**: Core → Gateway → Frontend (both players)  
**Purpose**: Game starting, transition to game board  
**DTO**: `MatchStartDto`

```json
{
  "type": "MATCH.START",
  "correlationId": "cor-match-start-001",
  "timestamp": 1736412033000,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "player1Id": "user-abc123",
    "player2Id": "user-def456",
    "totalRounds": 3,
    "roundTimeoutSeconds": 10,
    "countdownMs": 3000
  }
}
```

---

### **3.5. MATCH.RESULT**

**Direction**: Core → Gateway → Frontend (both players)  
**Purpose**: Final game result  
**DTO**: `GameResultDto`

```json
{
  "type": "MATCH.RESULT",
  "correlationId": "cor-match-result-001",
  "timestamp": 1736412100000,
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "player1Id": "user-abc123",
    "player2Id": "user-def456",
    "player1FinalScore": 21,
    "player2FinalScore": 18,
    "winnerId": "user-abc123",
    "result": "PLAYER1_WIN",
    "totalRounds": 3,
    "completedAt": 1736412100000,
    "durationSeconds": 67,
    "player1NewGamesWon": 7,
    "player2NewGamesWon": 3
  }
}
```

---

### **3.6. MATCH.OPPONENT_LEFT**

**Direction**: Core → Gateway → Frontend (remaining player)  
**Purpose**: Opponent disconnected/quit  
**DTO**: `OpponentLeftDto`

```json
{
  "type": "MATCH.OPPONENT_LEFT",
  "correlationId": "cor-opp-left-001",
  "timestamp": 1736412055000,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "opponentId": "user-def456",
    "reason": "CONNECTION_LOST",
    "autoWinAwarded": true,
    "remainingTimeSeconds": 30
  }
}
```

---

## **4. GAME MESSAGES**

### **4.1. GAME.ROUND_START**

**Direction**: Core → Gateway → Frontend (both players)  
**Purpose**: New round begins  
**DTO**: `RoundStartDto`

```json
{
  "type": "GAME.ROUND_START",
  "correlationId": "cor-round-001",
  "timestamp": 1736412035000,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "deadlineEpochMs": 1736412045000,
    "durationMs": 10000,
    "availableCards": [
      {"id": 1, "rank": "A", "suit": "H", "value": 1},
      {"id": 2, "rank": "2", "suit": "H", "value": 2},
      {"id": 3, "rank": "3", "suit": "H", "value": 3}
      // ... 33 more cards (total 36)
    ]
  }
}
```

---

### **4.2. GAME.PLAY_CARD**

**Direction**: Frontend → Gateway → Core  
**Purpose**: Player selects a card  
**DTO**: `PlayCardRequestDto`

```json
{
  "type": "GAME.PLAY_CARD",
  "correlationId": "cor-play-001",
  "timestamp": 1736412038000,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "cardId": 15,
    "requestId": "req-client-abc-001"
  }
}
```

---

### **4.3. GAME.PICK_ACK**

**Direction**: Core → Gateway → Frontend (sender only)  
**Purpose**: Card selection acknowledged  
**DTO**: `PlayCardAckDto`

```json
{
  "type": "GAME.PICK_ACK",
  "correlationId": "cor-play-001",
  "timestamp": 1736412038200,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "cardId": 15,
    "card": {"id": 15, "rank": "7", "suit": "H", "value": 7},
    "requestId": "req-client-abc-001",
    "status": "ACCEPTED"
  }
}
```

---

### **4.4. GAME.PICK_NACK**

**Direction**: Core → Gateway → Frontend (sender only)  
**Purpose**: Card selection rejected  
**DTO**: `PlayCardNackDto`

```json
{
  "type": "GAME.PICK_NACK",
  "correlationId": "cor-play-002",
  "timestamp": 1736412038500,
  "userId": "user-abc123",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "cardId": 15,
    "requestId": "req-client-abc-002",
    "error": "CARD_ALREADY_TAKEN",
    "message": "Card has already been selected by opponent",
    "canRetry": true,
    "remainingTimeMs": 5000
  }
}
```

---

### **4.5. GAME.OPPONENT_STATUS**

**Direction**: Core → Gateway → Frontend (opponent of player who selected)  
**Purpose**: Notify opponent has selected card  
**DTO**: `PlayerStatusDto`

```json
{
  "type": "GAME.OPPONENT_STATUS",
  "correlationId": "cor-opp-status-001",
  "timestamp": 1736412038300,
  "userId": "user-def456",
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "opponentId": "user-abc123",
    "status": "READY",
    "message": "Opponent has selected a card"
  }
}
```

---

### **4.6. GAME.ROUND_REVEAL**

**Direction**: Core → Gateway → Frontend (both players)  
**Purpose**: Reveal both cards, show round winner  
**DTO**: `RoundRevealDto`

```json
{
  "type": "GAME.ROUND_REVEAL",
  "correlationId": "cor-reveal-001",
  "timestamp": 1736412045000,
  "matchId": "match-xyz",
  "version": "1.0.0",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "player1Card": {"id": 15, "rank": "7", "suit": "H", "value": 7},
    "player2Card": {"id": 23, "rank": "5", "suit": "D", "value": 5},
    "player1IsAutoPicked": false,
    "player2IsAutoPicked": false,
    "roundWinnerId": "user-abc123",
    "player1RoundScore": 7,
    "player2RoundScore": 5,
    "player1TotalScore": 7,
    "player2TotalScore": 5
  }
}
```

**Auto-pick Example**:
```json
{
  "type": "GAME.ROUND_REVEAL",
  "matchId": "match-xyz",
  "payload": {
    "roundNumber": 2,
    "player1Card": {"id": 3, "rank": "3", "suit": "H", "value": 3},
    "player2Card": {"id": 29, "rank": "2", "suit": "C", "value": 2},
    "player1IsAutoPicked": false,
    "player2IsAutoPicked": true,  // ⚠️ Player 2 timed out
    "roundWinnerId": "user-abc123",
    "player1RoundScore": 3,
    "player2RoundScore": 2,
    "player1TotalScore": 10,
    "player2TotalScore": 7
  }
}
```

---

## **5. LEADERBOARD MESSAGES**

### **5.1. LEADERBOARD.REQUEST**

**Direction**: Frontend → Gateway → Core  
**Purpose**: Request leaderboard data  
**DTO**: `LeaderboardRequestDto`

```json
{
  "type": "LEADERBOARD.REQUEST",
  "correlationId": "cor-lb-001",
  "timestamp": 1736412110000,
  "userId": "user-abc123",
  "version": "1.0.0",
  "payload": {
    "limit": 100,
    "offset": 0
  }
}
```

---

### **5.2. LEADERBOARD.RESPONSE**

**Direction**: Core → Gateway → Frontend  
**Purpose**: Return leaderboard data  
**DTO**: `LeaderboardResponseDto`

```json
{
  "type": "LEADERBOARD.RESPONSE",
  "correlationId": "cor-lb-001",
  "timestamp": 1736412110500,
  "userId": "user-abc123",
  "version": "1.0.0",
  "payload": {
    "entries": [
      {
        "rank": 1,
        "userId": "user-top1",
        "username": "champion",
        "displayName": "The Champion",
        "gamesWon": 150,
        "gamesPlayed": 200,
        "winRate": 0.75
      },
      {
        "rank": 2,
        "userId": "user-top2",
        "username": "runner",
        "displayName": "Runner Up",
        "gamesWon": 120,
        "gamesPlayed": 180,
        "winRate": 0.67
      }
    ],
    "totalPlayers": 1500,
    "generatedAt": 1736412110500
  }
}
```

---

## **6. SYSTEM MESSAGES**

### **6.1. SYS.PING**

**Direction**: Frontend → Gateway → Core  
**Purpose**: Heartbeat / latency check  
**DTO**: `PingDto`

```json
{
  "type": "SYS.PING",
  "correlationId": "cor-ping-001",
  "timestamp": 1736412120000,
  "userId": "user-abc123",
  "sessionId": "sess-xyz",
  "version": "1.0.0",
  "payload": {
    "nonce": "ping-uuid-123",
    "clientTimestamp": 1736412120000
  }
}
```

---

### **6.2. SYS.PONG**

**Direction**: Core → Gateway → Frontend  
**Purpose**: Heartbeat response  
**DTO**: `PongDto`

```json
{
  "type": "SYS.PONG",
  "correlationId": "cor-ping-001",
  "timestamp": 1736412120050,
  "userId": "user-abc123",
  "sessionId": "sess-xyz",
  "version": "1.0.0",
  "payload": {
    "nonce": "ping-uuid-123",
    "clientTimestamp": 1736412120000,
    "serverTimestamp": 1736412120050,
    "roundTripMs": 50
  }
}
```

---

### **6.3. SYS.ERROR**

**Direction**: Any → Any  
**Purpose**: System-level error  
**DTO**: `ErrorResponseDto`

```json
{
  "type": "SYS.ERROR",
  "correlationId": "cor-original-request",
  "timestamp": 1736412125000,
  "version": "1.0.0",
  "error": {
    "code": "VALIDATION_INVALID_INPUT",
    "message": "Invalid card ID provided",
    "details": {
      "field": "cardId",
      "value": 99,
      "constraint": "Must be between 1 and 36"
    },
    "timestamp": 1736412125000,
    "path": "/game/play-card"
  }
}
```

---

## **7. ERROR HANDLING**

### **7.1. Error Response Structure**

```json
{
  "type": "SYS.ERROR",
  "correlationId": "cor-xxx",
  "timestamp": 1736412130000,
  "version": "1.0.0",
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": {},
    "timestamp": 1736412130000,
    "path": "/context/path",
    "retryable": false
  }
}
```

### **7.2. Common Error Codes**

| Code | Message | Retryable |
|------|---------|-----------|
| `AUTH_INVALID_CREDENTIALS` | Invalid username or password | No |
| `VALIDATION_INVALID_INPUT` | Input validation failed | No |
| `GAME_CARD_NOT_AVAILABLE` | Card already selected | Yes |
| `GAME_TIMEOUT` | Round time expired | No |
| `SYSTEM_RATE_LIMIT` | Too many requests | Yes (after delay) |
| `SYSTEM_SERVER_ERROR` | Internal server error | Yes |

---

## **8. MESSAGE FLOW DIAGRAMS**

### **8.1. Login Flow**

```
Frontend                    Gateway                     Core
   |                           |                          |
   |-- AUTH.LOGIN_REQUEST ---->|                          |
   |                           |-- Forward Request ------>|
   |                           |                          |
   |                           |<-- Authenticate User ----|
   |                           |                          |
   |<-- AUTH.LOGIN_OK ---------|                          |
   |                           |                          |
   |<-- LOBBY.SNAPSHOT --------|<-- Send Lobby State -----|
```

---

### **8.2. Quick Match Flow**

```
Player 1                    Core                    Player 2
   |                          |                          |
   |-- MATCH.REQUEST -------->|<-- MATCH.REQUEST --------|
   |                          |                          |
   |                          |-- Match Players -------->|
   |                          |                          |
   |<-- MATCH.FOUND ----------|<-- MATCH.FOUND ----------|
   |                          |                          |
   |<-- MATCH.START ----------|<-- MATCH.START ----------|
   |                          |                          |
   |<-- GAME.ROUND_START -----|-- GAME.ROUND_START ----->|
```

---

### **8.3. Round Play Flow**

```
Player 1                         Core                         Player 2
   |                              |                              |
   |-- GAME.PLAY_CARD (card 15) ->|                              |
   |<-- GAME.PICK_ACK ------------|                              |
   |                              |-- GAME.OPPONENT_STATUS ----->|
   |                              |                              |
   |                              |<-- GAME.PLAY_CARD (card 23)--|
   |                              |-- GAME.PICK_ACK ------------>|
   |<-- GAME.OPPONENT_STATUS -----|                              |
   |                              |                              |
   |<-- GAME.ROUND_REVEAL --------|-- GAME.ROUND_REVEAL -------->|
```

---

### **8.4. Timeout Auto-Pick Flow**

```
Player 1                         Core                         Player 2
   |                              |                              |
   |-- GAME.PLAY_CARD ----------->|                              |
   |<-- GAME.PICK_ACK ------------|                              |
   |                              |-- GAME.OPPONENT_STATUS ----->|
   |                              |                              |
   |                              |-- Wait 10 seconds ---------->|
   |                              |                              |
   |                              |-- Auto-pick random card ---->|
   |                              |                              |
   |<-- GAME.ROUND_REVEAL --------|-- GAME.ROUND_REVEAL -------->|
   |    (player2IsAutoPicked=true)|                              |
```

---

## 📊 **MESSAGE STATISTICS**

| Category | Message Count | DTOs Required |
|----------|--------------|---------------|
| Authentication | 6 | 6 |
| Lobby | 2 | 3 |
| Match | 6 | 5 |
| Game | 6 | 7 |
| Leaderboard | 2 | 3 |
| System | 3 | 3 |
| **TOTAL** | **25** | **27** |

---

## ✅ **VALIDATION CHECKLIST**

- [ ] All message types follow `DOMAIN.ACTION[.MODIFIER]` convention
- [ ] Every message has correlationId for tracking
- [ ] Timestamps are epoch milliseconds
- [ ] Version field present for compatibility
- [ ] Payload schemas match DTO definitions
- [ ] Error responses use standardized ErrorInfo structure
- [ ] All examples are valid JSON
- [ ] Message flows documented with sequence diagrams

---

**Next**: See `DTO_SCHEMAS.md` for detailed JSON schemas and validation rules.
