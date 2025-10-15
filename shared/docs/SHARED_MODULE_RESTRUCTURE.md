# 🔧 SHARED MODULE - KẾ HOẠCH TÁI CẤU TRÚC

**Version**: 2.0.0 (MVP Complete)  
**Updated**: 2025-01-05  
**Status**: 🚧 In Progress  

---

## 📁 **1. CẤU TRÚC MỚI ĐỀ XUẤT**

### **1.1. Directory Structure**

```
shared/
├── pom.xml
├── README.md (Quick start)
├── docs/
│   ├── README.md (Comprehensive guide)
│   ├── SHARED_MODULE_ASSESSMENT.md ✅ Created
│   ├── MESSAGE_CATALOG.md (All messages documented)
│   ├── DTO_SCHEMAS.md (JSON examples for each DTO)
│   ├── INTEGRATION_GUIDE.md (How to use in Frontend/Backend)
│   └── CHANGELOG.md (Version history)
└── src/
    └── main/
        └── java/
            └── com/n9/shared/
                ├── constants/                    🆕 NEW PACKAGE
                │   ├── GameConstants.java
                │   ├── ProtocolConstants.java
                │   ├── ValidationConstants.java
                │   └── TimeConstants.java
                │
                ├── model/
                │   ├── dto/
                │   │   ├── auth/
                │   │   │   ├── LoginRequestDto.java           ✅ Exists
                │   │   │   ├── LoginSuccessDto.java           ✅ Exists
                │   │   │   ├── LoginFailureDto.java           🆕 NEW
                │   │   │   ├── RegisterRequestDto.java        🆕 NEW
                │   │   │   ├── RegisterResponseDto.java       🆕 NEW
                │   │   │   ├── LogoutRequestDto.java          🆕 NEW
                │   │   │   └── SessionDto.java                🆕 NEW
                │   │   │
                │   │   ├── game/
                │   │   │   ├── CardDto.java                   ✅ Exists (NEEDS UPDATE)
                │   │   │   ├── GameDto.java                   🆕 NEW
                │   │   │   ├── GameStateDto.java              🆕 NEW
                │   │   │   ├── RoundDto.java                  🆕 NEW
                │   │   │   ├── RoundStartDto.java             🆕 NEW
                │   │   │   ├── RoundRevealDto.java            🆕 NEW
                │   │   │   ├── PlayCardRequestDto.java        🆕 NEW
                │   │   │   ├── PlayCardAckDto.java            🆕 NEW
                │   │   │   ├── PlayCardNackDto.java           🆕 NEW
                │   │   │   ├── GameResultDto.java             🆕 NEW
                │   │   │   └── GameScoreDto.java              🆕 NEW
                │   │   │
                │   │   ├── lobby/                             🆕 NEW PACKAGE
                │   │   │   ├── LobbySnapshotDto.java
                │   │   │   ├── LobbyUpdateDto.java
                │   │   │   ├── PlayerDto.java
                │   │   │   ├── OpponentDto.java
                │   │   │   └── PlayerStatusDto.java
                │   │   │
                │   │   ├── match/                             🆕 NEW PACKAGE
                │   │   │   ├── MatchRequestDto.java
                │   │   │   ├── MatchFoundDto.java
                │   │   │   ├── MatchStartDto.java
                │   │   │   ├── MatchCancelDto.java
                │   │   │   └── OpponentLeftDto.java
                │   │   │
                │   │   ├── leaderboard/                       🆕 NEW PACKAGE
                │   │   │   ├── LeaderboardRequestDto.java
                │   │   │   ├── LeaderboardResponseDto.java
                │   │   │   └── LeaderboardEntryDto.java
                │   │   │
                │   │   ├── system/                            🆕 NEW PACKAGE
                │   │   │   ├── HeartbeatDto.java
                │   │   │   ├── PingDto.java
                │   │   │   ├── PongDto.java
                │   │   │   ├── ErrorResponseDto.java
                │   │   │   └── SystemStatusDto.java
                │   │   │
                │   │   └── common/                            🆕 NEW PACKAGE
                │   │       ├── BaseDto.java (abstract)
                │   │       ├── PageRequestDto.java
                │   │       └── PageResponseDto.java
                │   │
                │   └── enums/
                │       ├── CardSuit.java                      ✅ Exists
                │       ├── CardRank.java                      🆕 NEW (A-9 for MVP)
                │       ├── ErrorCode.java                     ✅ Exists
                │       ├── GameState.java                     ✅ Exists
                │       ├── PlayerStatus.java                  🆕 NEW
                │       ├── MatchResult.java                   🆕 NEW
                │       ├── RoundPhase.java                    🆕 NEW
                │       └── GameMode.java                      🆕 NEW
                │
                ├── protocol/
                │   ├── MessageEnvelope.java                   ✅ Exists
                │   ├── MessageType.java                       ✅ Exists
                │   ├── ProtocolVersion.java                   🆕 NEW
                │   ├── MessageFactory.java                    🆕 NEW (Builder helper)
                │   └── ErrorInfo.java                         🆕 NEW (Extract from envelope)
                │
                └── util/
                    ├── IdUtils.java                           ✅ Exists
                    ├── JsonUtils.java                         ✅ Exists
                    ├── ValidationUtils.java                   ✅ Exists
                    ├── CardUtils.java                         🆕 NEW
                    ├── GameRuleUtils.java                     🆕 NEW
                    └── TimeUtils.java                         🆕 NEW
```

---

## 🎯 **2. ALIGNMENT VỚI MVP REQUIREMENTS**

### **2.1. Database Schema Mapping**

| Database Table | Shared DTO | Status |
|----------------|------------|--------|
| **users** | UserDto | 🆕 NEW |
| **user_profiles** | UserProfileDto | 🆕 NEW |
| **cards** | CardDto | ⚠️ UPDATE (36 cards, not 52) |
| **games** | GameDto, GameStateDto | 🆕 NEW |
| **game_rounds** | RoundDto, RoundRevealDto | 🆕 NEW |
| **active_sessions** | SessionDto | 🆕 NEW |

### **2.2. MVP Flows Coverage**

#### **(A) Authentication Flow**

| Message Type | Request DTO | Response DTO | Status |
|--------------|-------------|--------------|--------|
| AUTH.LOGIN_REQUEST | LoginRequestDto | LoginSuccessDto | ✅ ✅ |
| AUTH.LOGIN_FAIL | - | LoginFailureDto | 🆕 NEW |
| AUTH.REGISTER_REQUEST | RegisterRequestDto | RegisterResponseDto | 🆕 🆕 |
| AUTH.LOGOUT_REQUEST | LogoutRequestDto | - | 🆕 NEW |

#### **(B) Matchmaking Flow**

| Message Type | Request DTO | Response DTO | Status |
|--------------|-------------|--------------|--------|
| LOBBY.SNAPSHOT | - | LobbySnapshotDto | 🆕 NEW |
| LOBBY.UPDATE | - | LobbyUpdateDto | 🆕 NEW |
| MATCH.REQUEST | MatchRequestDto | - | 🆕 NEW |
| MATCH.FOUND | - | MatchFoundDto | 🆕 NEW |
| MATCH.START | - | MatchStartDto | 🆕 NEW |

#### **(C) Gameplay Flow (3 rounds)**

| Message Type | Request DTO | Response DTO | Status |
|--------------|-------------|--------------|--------|
| GAME.ROUND_START | - | RoundStartDto | 🆕 NEW |
| GAME.PLAY_CARD | PlayCardRequestDto | - | 🆕 NEW |
| GAME.PICK_ACK | - | PlayCardAckDto | 🆕 NEW |
| GAME.PICK_NACK | - | PlayCardNackDto | 🆕 NEW |
| GAME.OPPONENT_STATUS | - | PlayerStatusDto | 🆕 NEW |
| GAME.ROUND_REVEAL | - | RoundRevealDto | 🆕 NEW |

#### **(D) Completion Flow**

| Message Type | Request DTO | Response DTO | Status |
|--------------|-------------|--------------|--------|
| MATCH.RESULT | - | GameResultDto | 🆕 NEW |
| LEADERBOARD.REQUEST | LeaderboardRequestDto | - | 🆕 NEW |
| LEADERBOARD.RESPONSE | - | LeaderboardResponseDto | 🆕 NEW |

#### **System Messages**

| Message Type | Request DTO | Response DTO | Status |
|--------------|-------------|--------------|--------|
| SYS.PING | PingDto | - | 🆕 NEW |
| SYS.PONG | - | PongDto | 🆕 NEW |
| SYS.ERROR | - | ErrorResponseDto | 🆕 NEW |

---

## 📝 **3. CHI TIẾT CÁC DTO MỚI**

### **3.1. Authentication DTOs**

#### **RegisterRequestDto.java** 🆕

```java
package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.*;

public class RegisterRequestDto {
    @JsonProperty("username")
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    private String username;
    
    @JsonProperty("email")
    @NotBlank
    @Email
    private String email;
    
    @JsonProperty("password")
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
    
    @JsonProperty("displayName")
    @Size(max = 100)
    private String displayName;
    
    // Constructors, getters, setters, equals, hashCode, toString
}
```

**JSON Example**:
```json
{
  "type": "AUTH.REGISTER_REQUEST",
  "correlationId": "cor-abc123",
  "timestamp": 1736412000000,
  "payload": {
    "username": "player01",
    "email": "player01@example.com",
    "password": "secret123",
    "displayName": "Player One"
  }
}
```

---

#### **RegisterResponseDto.java** 🆕

```java
package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterResponseDto {
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("success")
    private Boolean success;
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "AUTH.REGISTER_SUCCESS",
  "correlationId": "cor-abc123",
  "timestamp": 1736412001500,
  "payload": {
    "userId": "user-xyz789",
    "username": "player01",
    "message": "Registration successful. Please login.",
    "success": true
  }
}
```

---

### **3.2. Match DTOs**

#### **MatchRequestDto.java** 🆕

```java
package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.n9.shared.model.enums.GameMode;

public class MatchRequestDto {
    @JsonProperty("gameMode")
    private GameMode gameMode;  // QUICK_MATCH for MVP
    
    @JsonProperty("userId")
    private String userId;
    
    // For future: preferredOpponent, wagerAmount, etc.
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "MATCH.REQUEST",
  "correlationId": "cor-match-001",
  "timestamp": 1736412010000,
  "userId": "user-abc",
  "payload": {
    "gameMode": "QUICK_MATCH"
  }
}
```

---

#### **MatchFoundDto.java** 🆕

```java
package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.n9.shared.model.dto.lobby.OpponentDto;

public class MatchFoundDto {
    @JsonProperty("matchId")
    private String matchId;
    
    @JsonProperty("opponent")
    private OpponentDto opponent;
    
    @JsonProperty("estimatedStartTime")
    private Long estimatedStartTime;  // Epoch millis
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "MATCH.FOUND",
  "correlationId": "cor-match-001",
  "timestamp": 1736412015000,
  "matchId": "match-xyz",
  "payload": {
    "matchId": "match-xyz",
    "opponent": {
      "userId": "user-def",
      "username": "player02",
      "displayName": "Player Two",
      "gamesWon": 5,
      "gamesPlayed": 10
    },
    "estimatedStartTime": 1736412017000
  }
}
```

---

### **3.3. Game DTOs**

#### **RoundStartDto.java** 🆕

```java
package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoundStartDto {
    @JsonProperty("matchId")
    private String matchId;
    
    @JsonProperty("roundNumber")
    private Integer roundNumber;  // 1, 2, 3
    
    @JsonProperty("deadlineEpochMs")
    private Long deadlineEpochMs;  // 10 seconds from now
    
    @JsonProperty("durationMs")
    private Integer durationMs;  // 10000 ms
    
    @JsonProperty("availableCards")
    private List<CardDto> availableCards;  // Player's remaining cards
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "GAME.ROUND_START",
  "correlationId": "cor-round-001",
  "timestamp": 1736412020000,
  "matchId": "match-xyz",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "deadlineEpochMs": 1736412030000,
    "durationMs": 10000,
    "availableCards": [
      {"rank": "A", "suit": "H", "value": 1, "index": 0},
      {"rank": "2", "suit": "H", "value": 2, "index": 1},
      // ... 34 more cards
    ]
  }
}
```

---

#### **PlayCardRequestDto.java** 🆕

```java
package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.*;

public class PlayCardRequestDto {
    @JsonProperty("matchId")
    @NotBlank
    private String matchId;
    
    @JsonProperty("roundNumber")
    @Min(1)
    @Max(3)
    private Integer roundNumber;
    
    @JsonProperty("cardId")
    @NotNull
    private Integer cardId;  // ID from cards table (1-36)
    
    @JsonProperty("requestId")
    private String requestId;  // Client-generated for ACK/NACK matching
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "GAME.PLAY_CARD",
  "correlationId": "cor-play-001",
  "timestamp": 1736412025000,
  "userId": "user-abc",
  "matchId": "match-xyz",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "cardId": 15,
    "requestId": "req-client-001"
  }
}
```

---

#### **RoundRevealDto.java** 🆕

```java
package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoundRevealDto {
    @JsonProperty("matchId")
    private String matchId;
    
    @JsonProperty("roundNumber")
    private Integer roundNumber;
    
    @JsonProperty("player1Card")
    private CardDto player1Card;
    
    @JsonProperty("player2Card")
    private CardDto player2Card;
    
    @JsonProperty("player1IsAutoPicked")
    private Boolean player1IsAutoPicked;
    
    @JsonProperty("player2IsAutoPicked")
    private Boolean player2IsAutoPicked;
    
    @JsonProperty("roundWinnerId")
    private String roundWinnerId;  // or null if draw
    
    @JsonProperty("player1RoundScore")
    private Integer player1RoundScore;
    
    @JsonProperty("player2RoundScore")
    private Integer player2RoundScore;
    
    @JsonProperty("player1TotalScore")
    private Integer player1TotalScore;
    
    @JsonProperty("player2TotalScore")
    private Integer player2TotalScore;
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "GAME.ROUND_REVEAL",
  "correlationId": "cor-reveal-001",
  "timestamp": 1736412030000,
  "matchId": "match-xyz",
  "payload": {
    "matchId": "match-xyz",
    "roundNumber": 1,
    "player1Card": {"rank": "7", "suit": "H", "value": 7, "index": 6},
    "player2Card": {"rank": "5", "suit": "D", "value": 5, "index": 17},
    "player1IsAutoPicked": false,
    "player2IsAutoPicked": false,
    "roundWinnerId": "user-abc",
    "player1RoundScore": 7,
    "player2RoundScore": 5,
    "player1TotalScore": 7,
    "player2TotalScore": 5
  }
}
```

---

#### **GameResultDto.java** 🆕

```java
package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.n9.shared.model.enums.MatchResult;

public class GameResultDto {
    @JsonProperty("matchId")
    private String matchId;
    
    @JsonProperty("player1Id")
    private String player1Id;
    
    @JsonProperty("player2Id")
    private String player2Id;
    
    @JsonProperty("player1FinalScore")
    private Integer player1FinalScore;
    
    @JsonProperty("player2FinalScore")
    private Integer player2FinalScore;
    
    @JsonProperty("winnerId")
    private String winnerId;  // null if draw
    
    @JsonProperty("result")
    private MatchResult result;  // PLAYER1_WIN, PLAYER2_WIN, DRAW
    
    @JsonProperty("totalRounds")
    private Integer totalRounds;  // 3 for MVP
    
    @JsonProperty("completedAt")
    private Long completedAt;
    
    @JsonProperty("durationSeconds")
    private Integer durationSeconds;
    
    // Statistics updates
    @JsonProperty("player1NewGamesWon")
    private Integer player1NewGamesWon;
    
    @JsonProperty("player2NewGamesWon")
    private Integer player2NewGamesWon;
    
    // Constructors, getters, setters
}
```

**JSON Example**:
```json
{
  "type": "MATCH.RESULT",
  "correlationId": "cor-result-001",
  "timestamp": 1736412090000,
  "matchId": "match-xyz",
  "payload": {
    "matchId": "match-xyz",
    "player1Id": "user-abc",
    "player2Id": "user-def",
    "player1FinalScore": 21,
    "player2FinalScore": 18,
    "winnerId": "user-abc",
    "result": "PLAYER1_WIN",
    "totalRounds": 3,
    "completedAt": 1736412090000,
    "durationSeconds": 70,
    "player1NewGamesWon": 6,
    "player2NewGamesWon": 3
  }
}
```

---

### **3.4. Lobby DTOs**

#### **LobbySnapshotDto.java** 🆕

```java
package com.n9.shared.model.dto.lobby;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class LobbySnapshotDto {
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("onlinePlayers")
    private List<PlayerDto> onlinePlayers;
    
    @JsonProperty("totalOnline")
    private Integer totalOnline;
    
    // Constructors, getters, setters
}
```

---

#### **PlayerDto.java** 🆕

```java
package com.n9.shared.model.dto.lobby;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.n9.shared.model.enums.PlayerStatus;

public class PlayerDto {
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("displayName")
    private String displayName;
    
    @JsonProperty("status")
    private PlayerStatus status;  // IDLE, IN_QUEUE, IN_GAME
    
    @JsonProperty("gamesWon")
    private Integer gamesWon;
    
    @JsonProperty("gamesPlayed")
    private Integer gamesPlayed;
    
    @JsonProperty("lastActivity")
    private Long lastActivity;
    
    // Constructors, getters, setters
}
```

---

### **3.5. Leaderboard DTOs**

#### **LeaderboardResponseDto.java** 🆕

```java
package com.n9.shared.model.dto.leaderboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class LeaderboardResponseDto {
    @JsonProperty("entries")
    private List<LeaderboardEntryDto> entries;
    
    @JsonProperty("totalPlayers")
    private Integer totalPlayers;
    
    @JsonProperty("generatedAt")
    private Long generatedAt;
    
    // Constructors, getters, setters
}
```

---

#### **LeaderboardEntryDto.java** 🆕

```java
package com.n9.shared.model.dto.leaderboard;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LeaderboardEntryDto {
    @JsonProperty("rank")
    private Integer rank;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("displayName")
    private String displayName;
    
    @JsonProperty("gamesWon")
    private Integer gamesWon;
    
    @JsonProperty("gamesPlayed")
    private Integer gamesPlayed;
    
    @JsonProperty("winRate")
    private Double winRate;  // Calculated: gamesWon / gamesPlayed
    
    // Constructors, getters, setters
}
```

---

## 🔢 **4. ENUMS MỚI**

### **CardRank.java** 🆕

```java
package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Card Ranks for 36-Card Deck (MVP)
 * 
 * Only ranks A-9 are used in the MVP (no face cards).
 * 4 suits × 9 ranks = 36 cards total.
 */
public enum CardRank {
    ACE("A", "Ace", "Át", 1),
    TWO("2", "Two", "Hai", 2),
    THREE("3", "Three", "Ba", 3),
    FOUR("4", "Four", "Bốn", 4),
    FIVE("5", "Five", "Năm", 5),
    SIX("6", "Six", "Sáu", 6),
    SEVEN("7", "Seven", "Bảy", 7),
    EIGHT("8", "Eight", "Tám", 8),
    NINE("9", "Nine", "Chín", 9);
    
    // DEFERRED: JACK, QUEEN, KING for future 52-card expansion
    
    private final String code;
    private final String englishName;
    private final String vietnameseName;
    private final int value;
    
    CardRank(String code, String englishName, String vietnameseName, int value) {
        this.code = code;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.value = value;
    }
    
    @JsonValue
    public String getCode() { return code; }
    
    public String getEnglishName() { return englishName; }
    public String getVietnameseName() { return vietnameseName; }
    public int getValue() { return value; }
    
    public static CardRank fromCode(String code) {
        for (CardRank rank : values()) {
            if (rank.code.equals(code)) return rank;
        }
        throw new IllegalArgumentException("Unknown card rank: " + code);
    }
}
```

---

### **PlayerStatus.java** 🆕

```java
package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PlayerStatus {
    ONLINE("online", "Trực tuyến"),
    IDLE("idle", "Rảnh"),
    IN_QUEUE("in_queue", "Đang chờ ghép trận"),
    IN_GAME("in_game", "Đang chơi"),
    OFFLINE("offline", "Ngoại tuyến");
    
    private final String value;
    private final String displayName;
    
    PlayerStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    @JsonValue
    public String getValue() { return value; }
    
    public String getDisplayName() { return displayName; }
}
```

---

### **MatchResult.java** 🆕

```java
package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MatchResult {
    PLAYER1_WIN("player1_win", "Người chơi 1 thắng"),
    PLAYER2_WIN("player2_win", "Người chơi 2 thắng"),
    DRAW("draw", "Hòa"),
    ABANDONED("abandoned", "Bị bỏ");
    
    private final String value;
    private final String displayName;
    
    MatchResult(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    @JsonValue
    public String getValue() { return value; }
    
    public String getDisplayName() { return displayName; }
}
```

---

### **GameMode.java** 🆕

```java
package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GameMode {
    QUICK_MATCH("quick_match", "Ghép trận nhanh", true),
    RANKED("ranked", "Xếp hạng", false),  // DEFERRED
    CUSTOM("custom", "Tùy chỉnh", false); // DEFERRED
    
    private final String value;
    private final String displayName;
    private final boolean activeInMvp;
    
    GameMode(String value, String displayName, boolean activeInMvp) {
        this.value = value;
        this.displayName = displayName;
        this.activeInMvp = activeInMvp;
    }
    
    @JsonValue
    public String getValue() { return value; }
    
    public String getDisplayName() { return displayName; }
    public boolean isActiveInMvp() { return activeInMvp; }
}
```

---

## 📊 **5. CONSTANTS**

### **GameConstants.java** 🆕

```java
package com.n9.shared.constants;

/**
 * Game Rule Constants (MVP)
 * 
 * These values MUST match the database schema and frontend logic.
 * Any changes require coordination across all modules.
 */
public final class GameConstants {
    private GameConstants() {}
    
    // Card Deck
    public static final int DECK_SIZE = 36;  // 4 suits × 9 ranks
    public static final int SUITS_COUNT = 4;
    public static final int RANKS_COUNT = 9;  // A-9 only
    
    // Game Rules
    public static final int TOTAL_ROUNDS = 3;
    public static final int ROUND_TIMEOUT_SECONDS = 10;
    public static final int ROUND_TIMEOUT_MS = ROUND_TIMEOUT_SECONDS * 1000;
    
    // Scoring
    public static final int MIN_CARD_VALUE = 1;   // Ace
    public static final int MAX_CARD_VALUE = 9;   // 9
    
    // Player Limits
    public static final int PLAYERS_PER_GAME = 2;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 2;  // Only 1v1 for MVP
    
    // Leaderboard
    public static final int DEFAULT_LEADERBOARD_LIMIT = 100;
    public static final int MAX_LEADERBOARD_LIMIT = 500;
}
```

---

### **ProtocolConstants.java** 🆕

```java
package com.n9.shared.constants;

/**
 * Network Protocol Constants
 */
public final class ProtocolConstants {
    private ProtocolConstants() {}
    
    // Protocol Version
    public static final String PROTOCOL_VERSION = "1.0.0";
    
    // Message Limits
    public static final int MAX_MESSAGE_SIZE_BYTES = 1024 * 1024;  // 1 MB
    public static final int MAX_PAYLOAD_SIZE_BYTES = 512 * 1024;   // 512 KB
    
    // Timeouts
    public static final int CONNECTION_TIMEOUT_MS = 30000;     // 30 seconds
    public static final int READ_TIMEOUT_MS = 60000;           // 60 seconds
    public static final int WRITE_TIMEOUT_MS = 30000;          // 30 seconds
    
    // Heartbeat
    public static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    public static final int HEARTBEAT_TIMEOUT_SECONDS = 90;    // 3× interval
    
    // Reconnection
    public static final int MAX_RECONNECT_ATTEMPTS = 5;
    public static final int RECONNECT_DELAY_MS = 2000;         // 2 seconds
    public static final int RECONNECT_MAX_DELAY_MS = 30000;    // 30 seconds
    
    // Rate Limiting
    public static final int MAX_REQUESTS_PER_SECOND = 10;
    public static final int MAX_REQUESTS_PER_MINUTE = 300;
}
```

---

### **ValidationConstants.java** 🆕

```java
package com.n9.shared.constants;

/**
 * Validation Constants for DTOs
 */
public final class ValidationConstants {
    private ValidationConstants() {}
    
    // Username
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    
    // Password
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 100;
    
    // Email
    public static final int MAX_EMAIL_LENGTH = 255;
    
    // Display Name
    public static final int MAX_DISPLAY_NAME_LENGTH = 100;
    
    // Message Fields
    public static final int MAX_CORRELATION_ID_LENGTH = 64;
    public static final int MAX_SESSION_ID_LENGTH = 64;
    public static final int MAX_USER_ID_LENGTH = 64;
    public static final int MAX_MATCH_ID_LENGTH = 64;
}
```

---

## 🔄 **6. PROTOCOL UTILITIES**

### **ProtocolVersion.java** 🆕

```java
package com.n9.shared.protocol;

/**
 * Protocol Version Management
 */
public final class ProtocolVersion {
    private ProtocolVersion() {}
    
    public static final String V1_0_0 = "1.0.0";
    public static final String CURRENT = V1_0_0;
    
    /**
     * Check if version is compatible with current protocol
     */
    public static boolean isCompatible(String version) {
        if (version == null) return false;
        
        // MVP: Exact match only
        return CURRENT.equals(version);
        
        // Future: Semantic versioning compatibility
        // Major.Minor.Patch
        // Compatible if major versions match
    }
    
    /**
     * Get major version number
     */
    public static int getMajorVersion(String version) {
        if (version == null) return 0;
        String[] parts = version.split("\\.");
        return parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
    }
}
```

---

### **MessageFactory.java** 🆕

```java
package com.n9.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.n9.shared.util.IdUtils;
import com.n9.shared.util.JsonUtils;

/**
 * Factory for creating MessageEnvelope instances
 */
public final class MessageFactory {
    private MessageFactory() {}
    
    /**
     * Create request envelope
     */
    public static MessageEnvelope createRequest(String type, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .payload(JsonUtils.toJsonNode(payload))
            .build();
    }
    
    /**
     * Create response envelope
     */
    public static MessageEnvelope createResponse(String type, String correlationId, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(correlationId)
            .payload(JsonUtils.toJsonNode(payload))
            .build();
    }
    
    /**
     * Create error envelope
     */
    public static MessageEnvelope createError(String correlationId, ErrorInfo error) {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_ERROR)
            .correlationId(correlationId)
            .error(error)
            .build();
    }
    
    // More factory methods...
}
```

---

## ✅ **7. IMPLEMENTATION CHECKLIST**

### **Phase 1: Critical DTOs (Week 1)**

- [ ] **Create auth DTOs**:
  - [ ] RegisterRequestDto
  - [ ] RegisterResponseDto
  - [ ] LoginFailureDto
  - [ ] LogoutRequestDto

- [ ] **Create match DTOs**:
  - [ ] MatchRequestDto
  - [ ] MatchFoundDto
  - [ ] MatchStartDto

- [ ] **Create game DTOs**:
  - [ ] RoundStartDto
  - [ ] PlayCardRequestDto
  - [ ] PlayCardAckDto / PlayCardNackDto
  - [ ] RoundRevealDto
  - [ ] GameResultDto

- [ ] **Create lobby DTOs**:
  - [ ] LobbySnapshotDto
  - [ ] PlayerDto
  - [ ] OpponentDto

### **Phase 2: Enums & Constants (Week 1)**

- [ ] **Create enums**:
  - [ ] CardRank (A-9)
  - [ ] PlayerStatus
  - [ ] MatchResult
  - [ ] GameMode

- [ ] **Create constants**:
  - [ ] GameConstants
  - [ ] ProtocolConstants
  - [ ] ValidationConstants

### **Phase 3: Protocol & Utilities (Week 2)**

- [ ] **Protocol**:
  - [ ] ProtocolVersion
  - [ ] MessageFactory
  - [ ] ErrorInfo (extract from envelope)

- [ ] **Utilities**:
  - [ ] CardUtils (shuffle, deal, validate)
  - [ ] GameRuleUtils (calculate winner, scores)

### **Phase 4: Documentation (Week 2)**

- [ ] MESSAGE_CATALOG.md (all messages with examples)
- [ ] DTO_SCHEMAS.md (JSON schemas)
- [ ] INTEGRATION_GUIDE.md (usage in Frontend/Backend)

### **Phase 5: Testing (Week 3)**

- [ ] Unit tests for all DTOs
- [ ] Validation tests
- [ ] Serialization/deserialization tests
- [ ] Protocol version compatibility tests

---

**Next**: See `MESSAGE_CATALOG.md` for complete message examples.
