# Game Logic Specification (MVP)
## "Rút Bài May Mắn" - Network Programming Course

> **Version:** 1.1.0 (MVP - MySQL Persistence)  
> **Date:** 15/10/2025  
> **Priority:** SPEED TO MARKET - Đơn giản hóa tối đa, mở rộng sau  
> **Team:** 4 người (1 Gateway, 1 Frontend, 2 Core Backend)  
> **Database:** MySQL 8.0+ (Schema: `DB_SCRIPT.sql`)

---

## 📋 MỤC LỤC

1. [Game Rules (Luật Chơi)](#1-game-rules)
2. [Matchmaking Flow (Đơn giản FIFO)](#2-matchmaking-flow)
3. [Game Flow (3 Rounds)](#3-game-flow)
4. [State Management (MySQL + In-Memory Cache)](#4-state-management)
5. [Error Scenarios (Minimal)](#5-error-scenarios)
6. [Message Protocol Mapping](#6-message-protocol-mapping)
7. [Implementation Checklist](#7-implementation-checklist)
8. [Optimization Notes](#8-optimization-notes)

---

## 1. GAME RULES

### 1.1. Cơ Bản

**Setup:**
- **2 người chơi** chia sẻ **36 lá bài** (A-9 của 4 chất)
- **3 rounds**, mỗi round mỗi người chọn **1 lá**
- **Lá đã chọn = KHÔNG dùng lại** (bị loại khỏi bộ bài chung)

**Cách Chơi:**
```
Round 1:
  Player A chọn lá "5♥" → bộ bài còn 35 lá
  Player B chọn lá "3♦" → bộ bài còn 34 lá
  So sánh: 5 > 3 → Player A +1 điểm
  Score: A=1, B=0

Round 2:
  Player A chọn lá "2♠" → bộ bài còn 33 lá
  Player B chọn lá "7♣" → bộ bài còn 32 lá
  So sánh: 2 < 7 → Player B +1 điểm
  Score: A=1, B=1

Round 3:
  Player A chọn lá "9♥" → bộ bài còn 31 lá
  Player B chọn lá "6♦" → bộ bài còn 30 lá
  So sánh: 9 > 6 → Player A +1 điểm
  Final Score: A=2, B=1 → Player A WINS
```

### 1.2. Win Condition

**Winner:** Người có điểm cao hơn sau 3 rounds

**Tie-break (Hòa):**
```
Score sau 3 rounds:
- 2-1 hoặc 3-0 → Người nhiều điểm hơn THẮNG
- 1.5-1.5 (3 rounds hòa) → ❓ BẠN CHỌN:
```

**🎯 PHƯƠNG ÁN ĐƠN GIẢN NHẤT (ĐỀ XUẤT):**

**Option A - Random Winner (NHANH NHẤT):**
```java
if (player1Score == player2Score) {
    // Random 50/50
    winnerId = Math.random() < 0.5 ? player1Id : player2Id;
    tieBreakReason = "RANDOM_DRAW";
}
```
✅ **Ưu điểm:** 1 dòng code, không cần logic phức tạp  
⚠️ **Nhược điểm:** Không công bằng 100%, nhưng xác suất hòa rất thấp

**Option B - Sudden Death Round 4 (PHỨC TẠP HƠN):**
```java
if (player1Score == player2Score) {
    // Chơi thêm 1 round nữa từ 30 lá còn lại
    startRound(4); 
}
```
❌ **Không khuyến nghị cho MVP** - tốn thời gian code + test

**Option C - Both Win (Điểm cả 2):**
```java
if (player1Score == player2Score) {
    // Cả 2 đều +1 games_won
    winnerId = null;
}
```
⚠️ **Ảnh hưởng leaderboard** - cần xử lý thêm logic

---

**❓ BẠN CHỌN OPTION NÀO?** (Tôi khuyên **Option A - Random** cho MVP)

---

### 1.3. Timeout Logic

**Rule:** Mỗi round có **10 giây** để chọn bài

**Auto-Pick:**
```
IF player không chọn trong 10s:
  → Tự động chọn LÁ NHỎ NHẤT còn lại trong bộ 36 lá
  → Gắn flag: player_is_auto_picked = TRUE
  → Tiếp tục so sánh như bình thường
```

**Example:**
```
Bộ bài còn: [1♥, 3♦, 5♠, 7♣, 9♥]
Player A timeout → auto-pick: 1♥ (lá nhỏ nhất)
Player B chọn: 3♦
So sánh: 1 < 3 → Player B +1 điểm
```

**Implementation:**
```java
// ScheduledExecutorService
timeoutTask = scheduler.schedule(() -> {
    if (player1CardThisRound == null) {
        player1CardThisRound = autoPickSmallestCard(availableCards);
        player1IsAutoPicked = true;
    }
    if (player2CardThisRound == null) {
        player2CardThisRound = autoPickSmallestCard(availableCards);
        player2IsAutoPicked = true;
    }
    resolveRound();
}, 10, TimeUnit.SECONDS);
```

---

## 2. MATCHMAKING FLOW

### 2.1. FIFO Queue (Đơn Giản Nhất)

**Logic:**
```java
ConcurrentLinkedQueue<WaitingPlayer> queue = new ConcurrentLinkedQueue<>();

public String joinQueue(String userId) {
    synchronized (queueLock) {
        // Thêm vào queue
        queue.add(new WaitingPlayer(userId, System.currentTimeMillis()));
        
        // Nếu >= 2 người → match ngay
        if (queue.size() >= 2) {
            WaitingPlayer p1 = queue.poll();
            WaitingPlayer p2 = queue.poll();
            
            String matchId = UUID.randomUUID().toString();
            createMatch(matchId, p1.userId, p2.userId);
            return matchId; // Trả về cho cả 2 người
        }
        
        return null; // Vẫn đang chờ
    }
}
```

**Message Flow:**
```
Client A → Core: MATCH.REQUEST
Core → Client A: MATCH.STATUS {status: "WAITING", queueSize: 1}

Client B → Core: MATCH.REQUEST
Core → Client A: MATCH.FOUND {matchId: "M1", opponent: "B"}
Core → Client B: MATCH.FOUND {matchId: "M1", opponent: "A"}

[Auto start game ngay]
Core → Client A: GAME.ROUND_START {round: 1, deadline: 10s}
Core → Client B: GAME.ROUND_START {round: 1, deadline: 10s}
```

### 2.2. Cancel Queue (Optional - Nếu Có Thời Gian)

```java
public boolean cancelQueue(String userId) {
    synchronized (queueLock) {
        return queue.removeIf(p -> p.userId.equals(userId));
    }
}
```

**🎯 MVP:** KHÔNG cần cancel - user đóng browser = tự động remove (disconnect handling)

---

## 3. GAME FLOW

### 3.1. Round Lifecycle

```
┌─────────────────────────────────────────┐
│ ROUND_START (10s countdown)             │
│   ↓                                      │
│ Player A chọn lá → PICK_ACK              │
│   ↓ (chờ Player B...)                    │
│ Player B chọn lá → PICK_ACK              │
│   ↓                                      │
│ ROUND_REVEAL (cả 2 lá + winner)         │
│   ↓                                      │
│ [Repeat cho Round 2, 3]                  │
│   ↓                                      │
│ MATCH_RESULT (final winner)             │
└─────────────────────────────────────────┘
```

### 3.2. Card Selection Logic

**Available Cards Tracking:**
```java
class GameState {
    Set<Integer> availableCardIds; // Ban đầu: 1-36
    
    public void playCard(String playerId, int cardId) {
        // Validate
        if (!availableCardIds.contains(cardId)) {
            throw new IllegalArgumentException("Card already taken");
        }
        
        // Remove khỏi bộ bài
        availableCardIds.remove(cardId);
        
        // Ghi nhận lựa chọn
        if (playerId.equals(player1Id)) {
            player1CardThisRound = cardId;
        } else {
            player2CardThisRound = cardId;
        }
    }
}
```

**🎯 ĐƠN GIẢN:** Dùng `HashSet<Integer>` - O(1) lookup, thread-safe với `synchronized`

---

### 3.3. Round Resolution

```java
private void resolveRound() {
    // 1. Lấy giá trị 2 lá
    int val1 = getCardValue(player1CardThisRound);
    int val2 = getCardValue(player2CardThisRound);
    
    // 2. So sánh
    if (val1 > val2) {
        player1Score++;
        roundWinnerId = player1Id;
    } else if (val2 > val1) {
        player2Score++;
        roundWinnerId = player2Id;
    } else {
        // Hòa round này → không ai +1 điểm
        roundWinnerId = null;
    }
    
    // 3. Lưu vào DB (nếu có)
    saveRound(currentRound, player1CardThisRound, player2CardThisRound, roundWinnerId);
    
    // 4. Broadcast kết quả
    broadcastRoundResult();
    
    // 5. Check nếu xong 3 rounds → end game
    if (currentRound >= 3) {
        endGame();
    } else {
        // Tăng round, reset card selections
        currentRound++;
        player1CardThisRound = null;
        player2CardThisRound = null;
        startRound(currentRound);
    }
}
```

---

## 4. STATE MANAGEMENT

### 4.1. MySQL Database Schema (Đã Có Sẵn)

**Tables:**
- ✅ `users` - User accounts (user_id INT AUTO_INCREMENT)
- ✅ `user_profiles` - Stats (games_won, games_lost, games_played)
- ✅ `cards` - 36 cards reference (card_id 1-36)
- ✅ `games` - Match records (match_id VARCHAR(36) UUID)
- ✅ `game_rounds` - Round history (player*_card_id, player*_is_auto_picked)
- ✅ `active_sessions` - Session tracking (session_id, last_heartbeat)

**Stored Procedures:**
- ✅ `update_user_stats_after_game(match_id)` - Auto update games_won/lost

**Triggers:**
- ✅ `after_user_insert` - Auto create user_profile

---

### 4.2. Hybrid State: MySQL + In-Memory Cache

**Strategy:**
```
┌─────────────────────────────────────────────────┐
│ IN-MEMORY (ConcurrentHashMap)                   │
│ - Active game state (currentRound, cardPicks)   │
│ - Timeout tasks (ScheduledFuture)               │
│ - Available cards (Set<Integer>)                │
│ → Fast read/write, lost on restart              │
└─────────────────────────────────────────────────┘
              ↓ (Write when events happen)
┌─────────────────────────────────────────────────┐
│ MYSQL DATABASE                                  │
│ - games table (status, scores, winner)          │
│ - game_rounds table (each round result)         │
│ - users/user_profiles (auth + stats)            │
│ → Persistent, slow but reliable                 │
└─────────────────────────────────────────────────┘
```

**GameState (In-Memory Cache):**
```java
class GameState {
    // Metadata (sync với DB games table)
    String matchId;              // → games.match_id
    int player1Id;               // → games.player1_id (INT)
    int player2Id;               // → games.player2_id
    
    // Game progress (in-memory only)
    int currentRound;            // 1, 2, 3
    Set<Integer> availableCardIds; // 36 lá ban đầu, giảm dần
    
    // Current round state (volatile - mất khi restart)
    Integer player1CardThisRound; // null = chưa chọn
    Integer player2CardThisRound;
    boolean player1IsAutoPicked;
    boolean player2IsAutoPicked;
    ScheduledFuture<?> timeoutTask; // Cancel khi cả 2 chọn xong
    
    // Scores (sync với DB mỗi round)
    int player1Score;            // → games.player1_score
    int player2Score;            // → games.player2_score
    
    // Connection references (for broadcast)
    ClientConnectionHandler player1Handler;
    ClientConnectionHandler player2Handler;
}
```

**Storage:**
```java
// In-memory cache for active games
ConcurrentHashMap<String, GameState> activeGames = new ConcurrentHashMap<>();
```

---

### 4.3. Database Operations (When To Persist)

**1️⃣ Game Start (After Match Found):**
```java
// Insert vào DB
String matchId = UUID.randomUUID().toString();
INSERT INTO games (match_id, player1_id, player2_id, game_mode, status, created_at, started_at)
VALUES (?, ?, ?, 'QUICK', 'IN_PROGRESS', NOW(), NOW());

// Tạo in-memory state
GameState game = new GameState(matchId, p1, p2);
game.availableCardIds = new HashSet<>(IntStream.rangeClosed(1, 36).boxed().toList());
activeGames.put(matchId, game);
```

**2️⃣ Round End (After Both Played or Timeout):**
```java
// Insert round vào DB ngay
INSERT INTO game_rounds (
    match_id, round_number,
    player1_card_id, player1_card_value, player1_is_auto_picked,
    player2_card_id, player2_card_value, player2_is_auto_picked,
    round_winner_id, player1_round_score, player2_round_score,
    completed_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW());

// Update games table scores
UPDATE games 
SET player1_score = ?, player2_score = ?, completed_rounds = ?
WHERE match_id = ?;

// In-memory: tiếp tục round tiếp theo
game.currentRound++;
game.player1CardThisRound = null;
game.player2CardThisRound = null;
```

**3️⃣ Game End (After 3 Rounds):**
```java
// Update games table
UPDATE games 
SET status = 'COMPLETED', winner_id = ?, completed_at = NOW()
WHERE match_id = ?;

// Call stored procedure để update stats
CALL update_user_stats_after_game(?);

// Cleanup in-memory
activeGames.remove(matchId);
```

**4️⃣ Disconnect (Mid-Game):**
```java
// Update games table ngay
UPDATE games 
SET status = 'ABANDONED', winner_id = ?, completed_at = NOW(),
    player1_score = ?, player2_score = ?
WHERE match_id = ?;

// Call stored procedure
CALL update_user_stats_after_game(?);

// Cleanup
activeGames.remove(matchId);
```

---

### 4.4. Session Tracking (MySQL + In-Memory)

**In-Memory (Fast Lookup):**
```java
class SessionContext {
    String sessionId;
    int userId;                  // INT thay vì String
    String currentMatchId;       // null nếu đang ở lobby
    long lastHeartbeat;
    ClientConnectionHandler handler; // Để broadcast
}

ConcurrentHashMap<String, SessionContext> activeSessions = new ConcurrentHashMap<>();
```

**Database (Persist Sessions):**
```java
// Login → Insert/Update session
INSERT INTO active_sessions (session_id, user_id, status, last_heartbeat)
VALUES (?, ?, 'IN_LOBBY', NOW())
ON DUPLICATE KEY UPDATE last_heartbeat = NOW();

// Join game → Update session
UPDATE active_sessions 
SET match_id = ?, status = 'IN_GAME', last_activity = NOW()
WHERE session_id = ?;

// Disconnect → Delete session
DELETE FROM active_sessions WHERE session_id = ?;
```

**🎯 MVP Strategy:**
- **In-memory:** Fast access cho routing messages
- **DB:** Persist để admin monitor, không cần reconnect logic

---

### 4.5. Auth Service (MySQL)

**AuthService with Database:**
```java
public class AuthService {
    private Connection dbConnection;
    
    // Register
    public User register(String username, String password, String email) throws SQLException {
        // Hash password (SHA-256 cho MVP)
        String passwordHash = hashPassword(password);
        
        // Insert vào DB
        String sql = "INSERT INTO users (username, email, password_hash, status) " +
                     "VALUES (?, ?, ?, 'ACTIVE')";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.executeUpdate();
            
            // Get user_id (AUTO_INCREMENT)
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int userId = rs.getInt(1);
                
                // Trigger tự động tạo user_profile
                return new User(userId, username, email);
            }
        }
        throw new SQLException("Failed to register user");
    }
    
    // Login
    public User login(String username, String password) throws SQLException {
        String passwordHash = hashPassword(password);
        
        String sql = "SELECT user_id, username, email FROM users " +
                     "WHERE username = ? AND password_hash = ? AND status = 'ACTIVE'";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Update last_login
                updateLastLogin(rs.getInt("user_id"));
                
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("email")
                );
            }
        }
        throw new SQLException("Invalid credentials");
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

**🎯 MVP:** SHA-256 hash, no salt (đủ dùng cho project học tập)

---

## 5. ERROR SCENARIOS

### 5.1. Invalid Card Play

**Case 1: Card Already Taken**
```java
if (!availableCardIds.contains(cardId)) {
    return createError("CARD_TAKEN", "Card already picked by opponent");
}
```

**Case 2: Card Not In Valid Range**
```java
if (cardId < 1 || cardId > 36) {
    return createError("INVALID_CARD", "Card ID must be 1-36");
}
```

**Case 3: Play After Round Ended**
```java
if (player1CardThisRound != null && player2CardThisRound != null) {
    return createError("ROUND_ENDED", "Round already resolved");
}
```

**🎯 MVP:** 3 error codes đủ - KHÔNG cần validate thêm

---

### 5.2. Disconnect Handling

**Detection:**
```java
// Socket close → ClientConnectionHandler.cleanup()
private void cleanup() {
    // 1. Remove session
    sessionManager.removeSession(sessionId);
    
    // 2. Nếu đang trong game → forfeit
    if (currentMatchId != null) {
        gameService.handleDisconnect(currentMatchId, userId);
    }
}
```

**Forfeit Logic:**
```java
public void handleDisconnect(String matchId, String disconnectedUserId) {
    GameState game = activeGames.get(matchId);
    if (game == null) return;
    
    // Xác định người thắng (người còn lại)
    String winnerId = disconnectedUserId.equals(game.player1Id) 
        ? game.player2Id 
        : game.player1Id;
    
    // Set điểm: người thắng = 3, người thua = 0
    if (winnerId.equals(game.player1Id)) {
        game.player1Score = 3;
        game.player2Score = 0;
    } else {
        game.player1Score = 0;
        game.player2Score = 3;
    }
    
    // End game ngay
    game.isComplete = true;
    game.winnerId = winnerId;
    
    // Broadcast OPPONENT_LEFT
    broadcastOpponentLeft(matchId, disconnectedUserId);
    
    // Sau đó MATCH_RESULT
    broadcastMatchResult(matchId);
}
```

**🎯 MVP:** Disconnect = auto-lose, không cần reconnect logic

---

### 5.3. Timeout Scenarios

**Case 1: Cả 2 timeout**
```java
// Auto-pick cả 2
player1CardThisRound = getSmallestCard(availableCards);
player2CardThisRound = getSmallestCard(availableCards.minus(player1Card));

// Resolve bình thường
resolveRound();
```

**Case 2: 1 người timeout, 1 người chọn**
```java
// Chỉ auto-pick cho người timeout
if (player1CardThisRound == null) {
    player1CardThisRound = getSmallestCard(availableCards);
    player1IsAutoPicked = true;
}

resolveRound();
```

**🎯 MVP:** Timeout = penalty (lá nhỏ nhất), KHÔNG kick người chơi

---

## 6. MESSAGE PROTOCOL MAPPING

### 6.1. Auth Flow

```
┌─────────┐                    ┌──────┐
│ Client  │                    │ Core │
└─────────┘                    └──────┘
     │                              │
     │ USER.REGISTER                │
     │ {username, password}         │
     ├─────────────────────────────►│
     │                              │ AuthService.register()
     │                              │ SessionManager.createSession()
     │                              │
     │ USER.PROFILE                 │
     │ {userId, sessionId}          │
     │◄─────────────────────────────┤
     │                              │
```

**Messages:**
- `USER.REGISTER` → `USER.PROFILE` (success) or `SYSTEM.ERROR` (fail)
- `USER.LOGIN` → `USER.PROFILE` (success) or `SYSTEM.ERROR` (fail)

---

### 6.2. Matchmaking Flow

```
┌─────────┐                    ┌──────┐
│ Client  │                    │ Core │
└─────────┘                    └──────┘
     │                              │
     │ MATCH.REQUEST                │
     ├─────────────────────────────►│
     │                              │ MatchmakingService.joinQueue()
     │                              │
     │ MATCH.STATUS (WAITING)       │
     │◄─────────────────────────────┤
     │                              │
     │ [Client B joins]             │
     │                              │
     │ MATCH.FOUND                  │
     │ {matchId, opponent}          │
     │◄─────────────────────────────┤
     │                              │
```

**Messages:**
- `MATCH.REQUEST` → `MATCH.STATUS` (waiting) or `MATCH.FOUND` (matched)
- `MATCH.CANCEL` → `MATCH.STATUS` (cancelled) - **SKIP MVP**

---

### 6.3. Game Flow

```
┌──────────┐                    ┌──────┐
│ Client A │                    │ Core │
└──────────┘                    └──────┘
     │                              │
     │ [After MATCH.FOUND]          │
     │                              │
     │ GAME.ROUND_START             │
     │ {round:1, deadline:10s}      │
     │◄─────────────────────────────┤
     │                              │
     │ GAME.PLAY_CARD               │
     │ {cardId: 5}                  │
     ├─────────────────────────────►│
     │                              │ GameState.playCard()
     │                              │ availableCards.remove(5)
     │                              │
     │ GAME.PICK_ACK                │
     │ {cardId:5, waiting:true}     │
     │◄─────────────────────────────┤
     │                              │
     │ [Wait for Player B...]       │
     │                              │
     │ GAME.ROUND_REVEAL            │
     │ {p1:5, p2:3, winner:A}       │
     │◄─────────────────────────────┤
     │                              │
     │ [Repeat round 2, 3...]       │
     │                              │
     │ GAME.MATCH_RESULT            │
     │ {winner:A, score:2-1}        │
     │◄─────────────────────────────┤
     │                              │
```

**Messages:**
- `GAME.ROUND_START` (Core → Both)
- `GAME.PLAY_CARD` (Client → Core)
- `GAME.PICK_ACK` (Core → Client who played)
- `GAME.ROUND_REVEAL` (Core → Both after both played or timeout)
- `GAME.MATCH_RESULT` (Core → Both after 3 rounds)

---

## 7. IMPLEMENTATION CHECKLIST

### 7.1. Người 3 - Core Backend A (Networking & Auth)

**Week 1:**
- [ ] **A1:** com.n9.core.CoreServer + CoreServerListener (DONE ✅)
- [ ] **A2:** DatabaseManager (MySQL connection pool với HikariCP hoặc JDBC)
- [ ] **A3:** ClientConnectionHandler routing (`USER.*`, `MATCH.*`, `GAME.*`)
- [ ] **B1:** AuthService (MySQL - INSERT/SELECT users table)
- [ ] **B2:** SessionManager (Hybrid: In-memory `ConcurrentHashMap` + DB sync)
- [ ] **B3:** Password hashing (SHA-256 via `MessageDigest`)
- [ ] **E1:** ErrorMapper (`Exception → MessageEnvelope`)
- [ ] **E2:** Error code definitions (enum `ErrorCode`)

**Database Setup:**
```bash
# Tạo database (nếu chưa có)
mysql -u root -p < core/db/DB_SCRIPT.sql

# Verify tables
mysql> USE cardgame_db;
mysql> SHOW TABLES;
# Expected: users, user_profiles, cards, games, game_rounds, active_sessions
```

**Test:**
```bash
TestCoreClient → USER.REGISTER → INSERT vào users + user_profiles
TestCoreClient → USER.LOGIN → SELECT users, UPDATE last_login
```

---

### 7.2. Người 4 - Core Backend B (Game Logic)

**Week 1:**
- [ ] **C1:** MatchmakingService (FIFO queue với `ConcurrentLinkedQueue`)
- [ ] **C2:** Pairing logic (auto-create game → INSERT games table)
- [ ] **C3:** GameDAO (Data Access Object cho games/game_rounds)

**Week 2:**
- [ ] **D1:** GameSession class (in-memory state + DB sync)
- [ ] **D2:** Round state machine (`ROUND_START → PLAYING → REVEAL`)
- [ ] **D3:** Timeout scheduling (10s với `ScheduledExecutorService`)
- [ ] **D4:** Auto-pick smallest card logic
- [ ] **D5:** GameService orchestration (manage `activeGames` map + DB persistence)
- [ ] **D6:** Round end → INSERT game_rounds, UPDATE games scores
- [ ] **D7:** Game end → UPDATE games status, CALL update_user_stats_after_game

**Database Queries:**
```sql
-- Create game
INSERT INTO games (match_id, player1_id, player2_id, status, started_at) 
VALUES (?, ?, ?, 'IN_PROGRESS', NOW());

-- Save round
INSERT INTO game_rounds (match_id, round_number, player1_card_id, player2_card_id, 
                         player1_is_auto_picked, player2_is_auto_picked, round_winner_id)
VALUES (?, ?, ?, ?, ?, ?, ?);

-- End game
UPDATE games SET status='COMPLETED', winner_id=?, completed_at=NOW() WHERE match_id=?;
CALL update_user_stats_after_game(?);
```

**Test:**
```bash
2 TestCoreClients → MATCH.REQUEST → games table có 1 row
→ PLAY_CARD → game_rounds table có 1 row
→ (3 rounds) → games.status = 'COMPLETED', user_profiles.games_won updated
```

---

### 7.3. Integration (Week 2-3)

**Người 3 + 4:**
- [ ] Wire `MatchmakingService` vào `ClientConnectionHandler`
- [ ] Implement callback để broadcast messages (`ROUND_START`, `ROUND_REVEAL`)
- [ ] Session validation trước mỗi message

**Người 1 (Gateway):**
- [ ] WebSocket → TCP forwarding
- [ ] TCP → WebSocket broadcasting

**Người 2 (Frontend):**
- [ ] Login/Register UI
- [ ] Matchmaking screen (waiting indicator)
- [ ] Game board (36 cards, click to play)
- [ ] Real-time updates (WebSocket listeners)

---

## 8. OPTIMIZATION NOTES

### 8.1. Code Đã Tốt (Không Cần Sửa)

✅ **com.n9.core.CoreServer.java:**
- `CachedThreadPool` phù hợp cho MVP (unknown connection count)
- Shutdown hook cleanup đúng

✅ **ClientConnectionHandler.java:**
- Message parsing với Jackson OK
- Error handling cơ bản đủ dùng

✅ **GameService.java:**
- `ConcurrentHashMap` cho state management đúng hướng

---

### 8.2. Cần Refactor (Nếu Có Thời Gian)

⚠️ **ClientConnectionHandler:**
- Tách routing logic ra `MessageRouter` class (SRP)
- Inject dependencies qua constructor thay vì singleton

⚠️ **GameService:**
- Tách `GameSession` thành class riêng (hiện tại lồng trong GameService)
- Move timeout logic vào `GameSession`

⚠️ **TestCoreClient:**
- Tạo test cases riêng cho từng message type
- Dùng JUnit thay vì print console

---

### 8.3. Performance Optimization (MySQL)

� **Database writes (Optimized):**
- ✅ **Connection pooling:** Dùng HikariCP (max 10 connections)
- ✅ **Prepared statements:** Tái sử dụng, tránh SQL injection
- ✅ **Batch writes:** Mỗi round insert ngay (3 queries/game = OK)
- ⚠️ **Indexes:** DB_SCRIPT.sql đã có indexes cho games, users, rounds

**Connection Pool Config:**
```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/cardgame_db");
config.setUsername("root");
config.setPassword("password");
config.setMaximumPoolSize(10); // Đủ cho MVP
config.setConnectionTimeout(30000); // 30s
dataSource = new HikariDataSource(config);
```

🟡 **Broadcast logic:**
- **MVP:** Chỉ 2 players → O(2) = OK, lưu reference trong `GameState`
- **Phase 2:** Pub/sub (Redis) nếu cần scale nhiều games

� **Card validation:**
- ✅ Dùng `HashSet<Integer>` → O(1) lookup
- ✅ Cards table có index trên `card_id` → fast JOIN

🟢 **Session validation:**
- ✅ In-memory `ConcurrentHashMap` → O(1) lookup
- ✅ DB chỉ để persist, không query mỗi message

---

### 8.4. Tech Debt (Để Phase 2)

✅ **Database:**
- ✅ MVP: MySQL với hybrid cache (DONE)
- Phase 2: Persist full game state để support reconnect

⚠️ **Authentication:**
- ✅ MVP: SHA-256 (đủ cho project học tập)
- Phase 2: BCrypt + salt cho production

✅ **Session Management:**
- ✅ MVP: Hybrid (in-memory + DB)
- Phase 2: Redis distributed sessions cho multi-server

❌ **Disconnect Handling:**
- MVP: Auto-lose ngay
- Phase 2: Grace period 30s cho reconnect

✅ **Leaderboard:**
- ✅ MVP: Query `SELECT * FROM user_profiles ORDER BY games_won DESC LIMIT 10`
- Phase 2: ELO rating system (DB đã có cột `current_rating` HOÃN)

✅ **Transaction Management:**
- ✅ MVP: Auto-commit mỗi query
- Phase 2: JDBC transactions cho multi-step operations

---

## 9. RISK MITIGATION

| Risk | Probability | Impact | Mitigation (MVP) |
|------|-------------|--------|------------------|
| **Race condition (2 players chọn cùng lá)** | Medium | High | `synchronized` block khi update `availableCards` |
| **Timeout không fire** | Low | High | Test với mock scheduler, verify cancel() logic |
| **Memory leak (game không cleanup)** | Medium | Medium | Cleanup ngay sau `MATCH_RESULT`, monitor với `jmap` |
| **Disconnect mid-game** | High | Medium | Auto-forfeit ngay, không cần reconnect |
| **Tie-break logic chưa rõ** | High | Low | Random winner (1 dòng code) |

---

## 10. TESTING STRATEGY

### 10.1. Unit Tests (JUnit)

```java
@Test
public void testAutoPickSmallestCard() {
    Set<Integer> available = Set.of(3, 7, 1, 9, 5);
    int picked = GameSession.autoPickSmallestCard(available);
    assertEquals(1, picked);
}

@Test
public void testRoundResolution_Player1Wins() {
    GameSession game = new GameSession("M1", "P1", "P2", scheduler);
    game.playCard("P1", 5);
    game.playCard("P2", 3);
    
    assertEquals("P1", game.getRoundWinnerId());
    assertEquals(1, game.getPlayer1Score());
    assertEquals(0, game.getPlayer2Score());
}
```

---

### 10.2. Integration Tests (2 TestClients)

```bash
# Terminal 1: Client A
java TestCoreClient --action=REGISTER --username=alice
java TestCoreClient --action=MATCH --sessionId=<session>

# Terminal 2: Client B
java TestCoreClient --action=REGISTER --username=bob
java TestCoreClient --action=MATCH --sessionId=<session>

# Verify: Both receive MATCH.FOUND → GAME.ROUND_START
```

---

### 10.3. Manual Test Checklist

- [ ] Register 2 users → thành công
- [ ] Login với wrong password → error `AUTH_FAILED`
- [ ] 2 users join queue → matched ngay
- [ ] Player A chọn lá 5, Player B chọn lá 5 → error `CARD_TAKEN`
- [ ] Player A timeout → auto-pick lá nhỏ nhất
- [ ] Cả 2 timeout → auto-pick 2 lá nhỏ nhất khác nhau
- [ ] Chơi đủ 3 rounds → MATCH_RESULT đúng winner
- [ ] Disconnect giữa game → người còn lại nhận OPPONENT_LEFT + MATCH_RESULT

---

## 📌 TÓM TẮT QUYẾT ĐỊNH QUAN TRỌNG

### ✅ ĐÃ XÁC NHẬN:

1. **Luật chơi:** 36 lá chung, 3 rounds, lá lớn hơn thắng ✅
2. **Timeout:** 10s → auto-pick lá nhỏ nhất ✅
3. **Disconnect:** Auto-lose ngay, không reconnect ✅
4. **State:** MySQL + In-memory cache (hybrid strategy) ✅
5. **Priority:** Auth → Matchmaking → Game Flow → Polish ✅
6. **Database:** MySQL 8.0+ với schema `DB_SCRIPT.sql` ✅

### ❓ CẦN XÁC NHẬN:

1. **Tie-break khi hòa 3 rounds:** Random winner? Sudden death? Both win?
   - **Đề xuất:** Random (1 dòng code, nhanh nhất)

2. **Session timeout:** 30 phút? 1 giờ? Không timeout?
   - **Đề xuất:** 30 phút (standard)

3. **Error response format:** Dùng `ErrorCode` enum hay string?
   - **Đề xuất:** Enum (type-safe)

---

**XIN BẠN XÁC NHẬN 3 ĐIỂM TRÊN**, tôi sẽ update file này và bắt đầu tạo **TODO chi tiết** cho team! 🚀

**File location:** `core/docs/GAME_LOGIC_MVP.md`
