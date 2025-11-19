# KIẾN TRÚC TRIỂN KHAI HỆ THỐNG GAME RÚT BÀI MAY MẮN

## 📐 TỔNG QUAN KIẾN TRÚC

Hệ thống Game Rút Bài May Mắn được thiết kế theo mô hình **4-Tier Distributed Architecture** (Kiến trúc phân tán 4 tầng) với sự phân tách rõ ràng giữa các lớp trách nhiệm, đảm bảo tính module hóa, khả năng mở rộng và bảo trì dễ dàng.

### 🎯 Mục Tiêu Thiết Kế

1. **Tách biệt trách nhiệm (Separation of Concerns)**
   - Mỗi tầng có nhiệm vụ riêng biệt, không phụ thuộc chặt chẽ vào nhau
   - Frontend chỉ quan tâm đến UI/UX
   - Gateway xử lý protocol translation và routing
   - Core chứa toàn bộ business logic
   - Database quản lý persistent data

2. **Khả năng mở rộng (Scalability)**
   - Có thể scale horizontal từng tầng độc lập
   - Gateway có thể load balance nhiều Core servers
   - Database có thể replicate/shard khi cần

3. **Bảo mật (Security)**
   - Core server không expose trực tiếp ra internet
   - Gateway đóng vai trò reverse proxy và firewall
   - Authentication/Authorization tập trung

4. **Hiệu năng cao (High Performance)**
   - WebSocket cho realtime communication
   - TCP socket với binary framing cho throughput cao
   - Connection pooling và thread pooling

---

## 🏗️ KIẾN TRÚC 4 TẦNG CHI TIẾT

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT TIER (Tầng Giao Diện)                        │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                    Web Browser (React.js Application)              │     │
│  │                           Port: 5173 (Development)                 │     │
│  │                                                                    │     │
│  │  Components:                                                       │     │
│  │  • <<component>> AuthView        - Đăng ký/Đăng nhập             │     │
│  │  • <<component>> LobbyView       - Tìm trận, Leaderboard         │     │
│  │  • <<component>> GameView        - Chơi game, hiển thị bài       │     │
│  │  • AppContext (State Management) - Global state với useReducer   │     │
│  │  • useWebSocket Hook             - WebSocket connection manager  │     │
│  │                                                                    │     │
│  │  Technology Stack:                                                 │     │
│  │  [React 18.2] [WebSocket API] [Tailwind CSS] [Vite]             │     │
│  └────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
                                       ↓
                            WebSocket Connection
                            ws://localhost:8080/ws
                            ┌────────────────────────┐
                            │ Protocol: WebSocket    │
                            │ Format: JSON           │
                            │ Encoding: UTF-8        │
                            │ Auto-Reconnect: ✅     │
                            │ Exponential Backoff    │
                            └────────────────────────┘
                                       ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION TIER (Tầng Trung Gian)                     │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │              Gateway Server (Spring Boot Application)              │     │
│  │                           Port: 8080                               │     │
│  │                                                                    │     │
│  │  Components:                                                       │     │
│  │  • <<component>> GatewayWebSocketHandler                          │     │
│  │    ├─ afterConnectionEstablished() - Handle new WebSocket conn   │     │
│  │    ├─ handleTextMessage()          - Route messages to Core      │     │
│  │    └─ afterConnectionClosed()      - Cleanup on disconnect       │     │
│  │                                                                    │     │
│  │  • <<component>> CoreTcpClient                                    │     │
│  │    ├─ connect()                    - Establish TCP to Core       │     │
│  │    ├─ startListening()             - Background thread read Core │     │
│  │    ├─ startHeartbeat()             - PING/PONG every 5s         │     │
│  │    └─ sendMessageToCore()          - Write to TCP socket        │     │
│  │                                                                    │     │
│  │  • <<component>> Message Translator                               │     │
│  │    ├─ WebSocket ↔ TCP Protocol Translation                       │     │
│  │    ├─ correlationId Mapping (Request/Response)                   │     │
│  │    └─ sessionId Routing (Notifications)                          │     │
│  │                                                                    │     │
│  │  Technology Stack:                                                 │     │
│  │  [Spring Boot 3.2] [Spring WebSocket] [TCP Client] [Java 17]    │     │
│  └────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
                                       ↓
                            TCP Socket Connection
                            localhost:9090
                            ┌─────────────────────────────────┐
                            │ Protocol: TCP Socket            │
                            │ Format: Length-Prefixed JSON    │
                            │   ├─ 4 bytes: length (int)     │
                            │   └─ N bytes: JSON payload     │
                            │ Buffering: BufferedStream       │
                            │ Heartbeat: PING/PONG (5s)      │
                            └─────────────────────────────────┘
                                       ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                    BUSINESS LOGIC TIER (Tầng Xử Lý Logic)                   │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                Core Server (Java TCP Server)                       │     │
│  │                        Port: 9090                                  │     │
│  │                                                                    │     │
│  │  Components:                                                       │     │
│  │                                                                    │     │
│  │  • <<component>> ClientConnectionHandler                          │     │
│  │    └─ Thread Pool: CachedThreadPool (Worker Pool)                │     │
│  │       - I/O Thread: Read/Write socket                             │     │
│  │       - Worker Thread: Process business logic                     │     │
│  │                                                                    │     │
│  │  • <<component>> AuthService                                      │     │
│  │    ├─ register() - Tạo tài khoản, BCrypt hashing                 │     │
│  │    ├─ login()    - Xác thực, tạo sessionId                       │     │
│  │    └─ logout()   - Cleanup session                               │     │
│  │                                                                    │     │
│  │  • <<component>> SessionManager                                   │     │
│  │    └─ ConcurrentHashMap<sessionId, SessionContext>               │     │
│  │       - Track active sessions, online users                       │     │
│  │       - Auto-cleanup expired sessions                             │     │
│  │                                                                    │     │
│  │  • <<component>> GameService                                      │     │
│  │    ├─ ConcurrentHashMap<matchId, GameState>                      │     │
│  │    ├─ Lock Striping: Map<matchId, ReentrantLock>                │     │
│  │    ├─ initializeGame()     - Tạo bộ bài, shuffle                │     │
│  │    ├─ playCard()           - Xử lý chọn bài (with Lock)         │     │
│  │    ├─ handleRoundTimeout() - Auto-pick khi hết giờ              │     │
│  │    └─ finalizeGame()       - Tính winner, lưu DB                │     │
│  │                                                                    │     │
│  │  • <<component>> MatchmakingService                               │     │
│  │    ├─ Queue<userId>: FIFO queue                                  │     │
│  │    ├─ Set<userId>: Track users in queue                          │     │
│  │    └─ ScheduledExecutor: tryMatchmaking() every 1s               │     │
│  │                                                                    │     │
│  │  • <<component>> ChallengeService                                 │     │
│  │    ├─ ConcurrentHashMap<challengeId, ChallengeSession>           │     │
│  │    ├─ createChallenge() - Gửi lời mời 1v1                       │     │
│  │    ├─ handleResponse() - Accept/Reject                           │     │
│  │    └─ Timeout: 15 seconds auto-cancel                            │     │
│  │                                                                    │     │
│  │  • <<component>> LeaderboardService                               │     │
│  │    ├─ getTopPlayers()  - Top 20 by score                        │     │
│  │    ├─ getUserRank()    - Calculate rank for user                │     │
│  │    └─ getOnlineStatus()- Join with active_sessions              │     │
│  │                                                                    │     │
│  │  Technology Stack:                                                 │     │
│  │  [Java 17] [JDBC] [ExecutorService] [ConcurrentHashMap]         │     │
│  └────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
                                       ↓
                            JDBC Connection Pool
                            jdbc:mysql://localhost:3306/lucky_card_game
                            ┌─────────────────────────────────┐
                            │ Connection Pool: HikariCP       │
                            │ Max Connections: 10             │
                            │ Connection Timeout: 30s         │
                            │ Idle Timeout: 600s              │
                            │ Max Lifetime: 1800s             │
                            └─────────────────────────────────┘
                                       ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                       DATA TIER (Tầng Cơ Sở Dữ Liệu)                        │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                    MySQL Database (Version 8.0)                    │     │
│  │                           Port: 3306                               │     │
│  │                                                                    │     │
│  │  Tables & Indexes:                                                 │     │
│  │                                                                    │     │
│  │  • <<table>> users                                                │     │
│  │    Columns: user_id, username(UNIQUE), email(UNIQUE), password   │     │
│  │    Indexes: PRIMARY KEY(user_id), UNIQUE(username), UNIQUE(email)│     │
│  │    Purpose: Authentication, user credentials                      │     │
│  │                                                                    │     │
│  │  • <<table>> user_profiles                                        │     │
│  │    Columns: user_id, score, games_played, games_won, win_rate    │     │
│  │    Indexes: PRIMARY KEY(user_id), INDEX(score DESC, games_won)   │     │
│  │    Purpose: Leaderboard ranking, game statistics                 │     │
│  │                                                                    │     │
│  │  • <<table>> active_sessions                                      │     │
│  │    Columns: session_id, user_id, last_activity_timestamp         │     │
│  │    Indexes: PRIMARY KEY(session_id), INDEX(user_id)              │     │
│  │    Purpose: Track online users, session management               │     │
│  │                                                                    │     │
│  │  • <<table>> games                                                │     │
│  │    Columns: match_id, player1_id, player2_id, winner_id, status  │     │
│  │    Indexes: PRIMARY KEY(match_id), INDEX(player1_id, player2_id) │     │
│  │    Purpose: Game history, match records                           │     │
│  │                                                                    │     │
│  │  • <<table>> game_rounds                                          │     │
│  │    Columns: round_id, match_id, round_number, player1_card, ...  │     │
│  │    Indexes: PRIMARY KEY(round_id), FOREIGN KEY(match_id)         │     │
│  │    Purpose: Detailed round history (3 rounds per game)           │     │
│  │                                                                    │     │
│  │  • <<table>> cards                                                │     │
│  │    Columns: card_id, rank, suit, display_name                    │     │
│  │    Purpose: 52 cards reference data                              │     │
│  │                                                                    │     │
│  │  Storage Engine: InnoDB (ACID transactions, Foreign Keys)         │     │
│  │  Character Set: utf8mb4 (Emoji support)                           │     │
│  │  Collation: utf8mb4_unicode_ci                                    │     │
│  └────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 LUỒNG DỮ LIỆU (DATA FLOW)

### 🔄 Request Flow (Client → Server → Database)

```
1. User Action (Click button, Select card)
   ↓
2. Frontend (React) - Dispatch action, update local state
   ↓
3. WebSocket Client - Send JSON message
   {
     "type": "GAME.CARD_PLAY_REQUEST",
     "correlationId": "c-1700123456-abc123",
     "sessionId": "sess-xyz789",
     "payload": { "cardId": 5 }
   }
   ↓
4. Gateway - GatewayWebSocketHandler.handleTextMessage()
   - Parse JSON
   - Store: pendingRequests.put(correlationId, webSocketSession)
   - Forward to Core via TCP
   ↓
5. Core - ClientConnectionHandler.run()
   - I/O Thread: Read length-prefixed message
   - Submit to Worker Pool: pool.submit(() -> processMessage())
   ↓
6. Core - GameService.playCard()
   - Acquire Lock: gameLocks.get(matchId).lock()
   - Validate card, update GameState
   - Check if both players played
   - Release Lock: lock.unlock()
   ↓
7. Core - Database Query
   - INSERT INTO game_rounds (match_id, round_number, player1_card, ...)
   - UPDATE user_profiles SET score = score + 10 WHERE user_id = ?
   ↓
8. Core - Send Response back to Gateway
   {
     "type": "GAME.CARD_PLAY_SUCCESS",
     "correlationId": "c-1700123456-abc123",
     "sessionId": "sess-xyz789",
     "payload": { "availableCards": [...] }
   }
   ↓
9. Gateway - Listener Thread receives response
   - Lookup: webSocketSession = pendingRequests.get(correlationId)
   - Forward to client via WebSocket
   ↓
10. Frontend - useWebSocket.onmessage()
    - Parse JSON
    - Dispatch Redux action: CARD_PLAY_SUCCESS
    - React re-renders with new state
```

### 🔔 Notification Flow (Server Push)

```
1. Core - GameService detects event (e.g., Both players played)
   ↓
2. Core - Send notification to BOTH players
   {
     "type": "GAME.ROUND_REVEAL",
     "sessionId": "sess-player1-xyz",
     "payload": { 
       "playerCard": "A♥", 
       "opponentCard": "K♠",
       "result": "WIN"
     }
   }
   ↓
3. Gateway - Listener Thread receives notification
   - Lookup: webSocketSession = activeClientSessions.get(sessionId)
   - Forward to client via WebSocket
   ↓
4. Frontend - useWebSocket.onmessage()
   - Dispatch: ROUND_REVEAL action
   - Show result modal, update scores
```

---

## 🔐 BẢO MẬT & XÁC THỰC

```
┌─────────────────────────────────────────────────────────────┐
│              Security & Authentication Layer                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Password Hashing:                                       │
│     • Algorithm: BCrypt                                     │
│     • Cost Factor: 10 (2^10 = 1024 rounds)                │
│     • Salt: Automatically generated per password           │
│                                                             │
│  2. Session Management:                                     │
│     • SessionId: UUID v4 (random, 128-bit)                │
│     • Storage: active_sessions table + In-memory cache     │
│     • Expiration: 24 hours (auto-cleanup)                  │
│     • Validation: Every request checks sessionId validity  │
│                                                             │
│  3. SQL Injection Prevention:                               │
│     • PreparedStatement for all queries                    │
│     • Input validation & sanitization                      │
│     • Parameterized queries only                           │
│                                                             │
│  4. Input Validation:                                       │
│     • Username: 3-50 chars, alphanumeric + underscore      │
│     • Email: RFC 5322 format validation                    │
│     • Password: Min 6 chars, no max limit                  │
│     • Card ID: Must be in availableCards list              │
│                                                             │
│  5. Authorization:                                          │
│     • User can only play in their own game                 │
│     • Cannot access other users' sessions                  │
│     • Admin operations require special role (future)       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚡ HIỆU NĂNG & TỐI ƯU HÓA

### 🧵 Threading Model

**Core Server:**
```
┌─────────────────────────────────────────────────────┐
│                  Thread Architecture                │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Main Thread:                                       │
│  ├─ CoreServer.main() - Start TCP listener         │
│  └─ ServerSocket.accept() - Accept connections     │
│                                                     │
│  I/O Threads (per connection):                     │
│  ├─ ClientConnectionHandler.run()                  │
│  ├─ Read from socket: in.readInt() + in.readFully()│
│  └─ Write to socket: out.writeInt() + out.write()  │
│                                                     │
│  Worker Pool (CachedThreadPool):                   │
│  ├─ Process business logic (playCard, login, etc.) │
│  ├─ Database queries (JDBC operations)             │
│  └─ Auto-scale: Create threads as needed           │
│                                                     │
│  Scheduler Threads:                                 │
│  ├─ MatchmakingService: tryMatchmaking() every 1s  │
│  ├─ GameService: Round timeout handlers (15s)      │
│  └─ SessionManager: Cleanup expired sessions       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**Gateway Server:**
```
┌─────────────────────────────────────────────────────┐
│              Gateway Thread Architecture            │
├─────────────────────────────────────────────────────┤
│                                                     │
│  WebSocket Threads:                                 │
│  ├─ Spring WebSocket Handler (per connection)      │
│  └─ Async message processing                       │
│                                                     │
│  TCP Listener Thread:                               │
│  ├─ CoreTcpClient.startListening()                 │
│  └─ Continuous read from Core: in.readInt()        │
│                                                     │
│  Heartbeat Thread:                                  │
│  ├─ CoreTcpClient.startHeartbeat()                 │
│  └─ PING/PONG every 5 seconds                      │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 🔒 Concurrency Control

```
┌─────────────────────────────────────────────────────┐
│           Concurrency & Synchronization             │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. Lock Striping (GameService):                    │
│     • Map<matchId, ReentrantLock> gameLocks        │
│     • Each game has its own lock                   │
│     • Prevent race condition when both play card   │
│                                                     │
│  2. ConcurrentHashMap Usage:                        │
│     • activeGames: Thread-safe game state storage  │
│     • activeSessions: Thread-safe session tracking │
│     • pendingRequests: correlationId → client map  │
│                                                     │
│  3. Atomic Operations:                              │
│     • matchmakingQueue: ConcurrentLinkedQueue      │
│     • usersInQueue: ConcurrentHashMap.newKeySet()  │
│                                                     │
│  4. Critical Sections:                              │
│     playCard() {                                    │
│       lock.lock();                                  │
│       try {                                         │
│         // Update game state                       │
│         // Check if both played                    │
│       } finally {                                   │
│         lock.unlock();                              │
│       }                                             │
│     }                                               │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 💾 Database Optimization

```
┌─────────────────────────────────────────────────────┐
│          Database Performance Tuning                │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. Connection Pooling (HikariCP):                  │
│     • Pool Size: 10 connections                    │
│     • Formula: (core_count × 2) + effective_spindle│
│     • Timeout: 30s connection, 600s idle           │
│                                                     │
│  2. Indexes:                                        │
│     • user_profiles(score DESC, games_won DESC)    │
│       → Fast leaderboard query                     │
│     • active_sessions(user_id)                     │
│       → Fast online status lookup                  │
│     • games(player1_id, player2_id)                │
│       → Fast match history retrieval               │
│                                                     │
│  3. Query Optimization:                             │
│     • Use JOIN instead of multiple SELECTs         │
│     • LIMIT for pagination (leaderboard top 20)    │
│     • Avoid SELECT * (specify columns)             │
│                                                     │
│  4. Transaction Management:                         │
│     • Auto-commit for simple queries               │
│     • Explicit transaction for game finalization:  │
│       BEGIN TRANSACTION;                           │
│         UPDATE user_profiles ...;                  │
│         INSERT INTO games ...;                     │
│       COMMIT;                                       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 TRIỂN KHAI (DEPLOYMENT)

### 📦 Development Environment

```
┌─────────────────────────────────────────────────────┐
│            Local Development Setup                  │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Frontend (Port 5173):                              │
│  $ cd frontend                                      │
│  $ npm install                                      │
│  $ npm run dev                                      │
│  → Vite dev server with hot reload                 │
│                                                     │
│  Gateway (Port 8080):                               │
│  $ cd gateway                                       │
│  $ mvn spring-boot:run                              │
│  → Spring Boot embedded Tomcat                     │
│                                                     │
│  Core (Port 9090):                                  │
│  $ cd core                                          │
│  $ mvn compile exec:java                            │
│  → Pure Java application                           │
│                                                     │
│  Database (Port 3306):                              │
│  $ docker run -d -p 3306:3306 \                    │
│    -e MYSQL_ROOT_PASSWORD=root \                   │
│    -e MYSQL_DATABASE=lucky_card_game \             │
│    mysql:8.0                                        │
│  $ mysql -u root -p < db/schema.sql                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 🐳 Production Deployment (Docker Compose)

```yaml
version: '3.8'
services:
  database:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: lucky_card_game
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - backend

  core:
    build: ./core
    ports:
      - "9090:9090"
    environment:
      DB_HOST: database
      DB_PORT: 3306
    depends_on:
      - database
    networks:
      - backend

  gateway:
    build: ./gateway
    ports:
      - "8080:8080"
    environment:
      CORE_HOST: core
      CORE_PORT: 9090
    depends_on:
      - core
    networks:
      - backend
      - frontend

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - gateway
    networks:
      - frontend

networks:
  frontend:
  backend:

volumes:
  mysql_data:
```

---

## 📡 GIAO THỨC GIAO TIẾP

### 🔌 WebSocket Protocol (Client ↔ Gateway)

```json
{
  "type": "DOMAIN.ACTION_MODIFIER",
  "correlationId": "c-timestamp-random",
  "sessionId": "sess-uuid",
  "payload": {
    // Domain-specific data
  },
  "error": {
    "code": "ERR_CODE",
    "message": "Human-readable error"
  }
}
```

**Message Types:**
- `AUTH.*` → Authentication (LOGIN, REGISTER, LOGOUT)
- `LOBBY.*` → Matchmaking & Leaderboard
- `GAME.*` → Game logic (START, PLAY_CARD, END)
- `SYSTEM.*` → Heartbeat, Errors

### 🔗 TCP Protocol (Gateway ↔ Core)

**Length-Prefixed Framing:**
```
┌──────────────┬─────────────────────────────────────┐
│  4 bytes     │          N bytes                    │
├──────────────┼─────────────────────────────────────┤
│  Length (N)  │  JSON Payload (UTF-8 encoded)       │
└──────────────┴─────────────────────────────────────┘
```

**Java Implementation:**
```java
// Write
byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
out.writeInt(jsonBytes.length);  // 4 bytes
out.write(jsonBytes);             // N bytes
out.flush();

// Read
int length = in.readInt();        // 4 bytes
byte[] buffer = new byte[length];
in.readFully(buffer);             // N bytes
String json = new String(buffer, StandardCharsets.UTF_8);
```

---

## 🔄 XỬ LÝ LỖI & RECOVERY

```
┌─────────────────────────────────────────────────────┐
│           Error Handling & Recovery                 │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. Client Disconnect (Normal):                     │
│     • WebSocket onClose → Gateway cleanup          │
│     • Gateway → Core: AUTH.LOGOUT_REQUEST          │
│     • Core → SessionManager.removeSession()        │
│     • If in game → Opponent wins (forfeit)         │
│                                                     │
│  2. Client Disconnect (Crash):                      │
│     • WebSocket onClose → Gateway detect           │
│     • No response to PING → Connection dead        │
│     • Auto-logout after 30s inactivity             │
│     • Opponent notified: GAME.OPPONENT_LEFT        │
│                                                     │
│  3. Gateway Crash:                                  │
│     • Core detects: IOException on socket read     │
│     • Close ClientConnectionHandler                │
│     • Cleanup all sessions from crashed Gateway    │
│     • All users disconnected → Need reconnect      │
│                                                     │
│  4. Core Crash:                                     │
│     • Gateway detects: IOException on TCP read     │
│     • Gateway attempts reconnect (Exponential)     │
│     • Clients see "Connection lost" message        │
│     • Game state lost (not persisted mid-game)     │
│                                                     │
│  5. Database Connection Lost:                       │
│     • HikariCP auto-retry with backoff             │
│     • If retry fails → Return SYSTEM.ERROR         │
│     • Log error for investigation                  │
│                                                     │
│  6. Deadlock Prevention:                            │
│     • SYSTEM.WELCOME sent immediately on connect   │
│     • Heartbeat PING/PONG keeps connection alive   │
│     • Timeout for all blocking operations          │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 📊 MONITORING & LOGGING

```
┌─────────────────────────────────────────────────────┐
│         Observability & Logging Strategy            │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Console Logs (Development):                        │
│  • Connection events: "Client X connected"         │
│  • Message routing: "Received: GAME.PLAY_CARD"     │
│  • Error traces: "Failed to parse JSON: ..."       │
│                                                     │
│  Metrics to Track (Production):                     │
│  • Active connections: WebSocket + TCP             │
│  • Games in progress: activeGames.size()           │
│  • Matchmaking queue length: queue.size()          │
│  • Database query time: HikariCP metrics           │
│  • Message throughput: messages/second             │
│                                                     │
│  Health Checks:                                     │
│  • /health endpoint → Gateway status               │
│  • Database ping → Connection pool health          │
│  • Core TCP ping → Heartbeat status                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 🎯 KẾT LUẬN

Kiến trúc 4 tầng này mang lại:

✅ **Tách biệt trách nhiệm rõ ràng** - Mỗi tầng có nhiệm vụ riêng  
✅ **Dễ bảo trì & mở rộng** - Có thể thay đổi từng tầng độc lập  
✅ **Hiệu năng cao** - WebSocket + TCP + Connection pooling  
✅ **Bảo mật tốt** - Core không expose, authentication tập trung  
✅ **Xử lý lỗi tốt** - Graceful degradation, auto-recovery  
✅ **Realtime experience** - WebSocket push notifications  

Hệ thống sẵn sàng cho:
- 📈 Horizontal scaling (thêm nhiều Core servers)
- 🔄 Load balancing ở Gateway tier
- 💾 Database replication/sharding
- 📊 Monitoring & analytics integration
