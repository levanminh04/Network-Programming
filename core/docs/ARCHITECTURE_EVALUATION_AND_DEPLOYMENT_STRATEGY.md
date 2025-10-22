# Đánh Giá Kiến Trúc và Chiến Lược Triển Khai
# Game Rút Bài May Mắn - MVP Network Programming

**Ngày tạo:** 20/10/2025  
**Phiên bản:** 1.0  
**Dự án:** Network Programming Course - MVP Phase  
**Nhóm:** 4 thành viên (1 Team Leader, 1 Frontend, 2 Backend Core)

---

## Mục Lục

1. [Tổng Quan Hệ Thống](#1-tổng-quan-hệ-thống)
2. [Đánh Giá Kiến Trúc Hiện Tại](#2-đánh-giá-kiến-trúc-hiện-tại)
3. [Điểm Mạnh và Hạn Chế](#3-điểm-mạnh-và-hạn-chế)
4. [Phân Tích Rủi Ro Kỹ Thuật](#4-phân-tích-rủi-ro-kỹ-thuật)
5. [Chiến Lược Triển Khai](#5-chiến-lược-triển-khai)
6. [Phân Công Chi Tiết](#6-phân-công-chi-tiết)
7. [Timeline và Milestone](#7-timeline-và-milestone)
8. [Kết Luận và Khuyến Nghị](#8-kết-luận-và-khuyến-nghị)

---

## 1. Tổng Quan Hệ Thống

### 1.1. Kiến Trúc Tổng Thể

```
┌─────────────────┐
│  Web Browser    │
│   (Frontend)    │
│   React + WS    │
└────────┬────────┘
         │ WebSocket (ws://host/ws)
         ▼
┌─────────────────┐
│    Gateway      │
│  Spring Boot    │
│  WebSocket →    │
│  TCP Client     │
└────────┬────────┘
         │ TCP Socket (port 9090)
         │ Length-Prefixed Protocol
         ▼
┌─────────────────┐
│   Core Server   │
│   Pure Java     │
│   TCP Server    │
│   • Threading   │
│   • Services    │
└────────┬────────┘
         │ JDBC + HikariCP
         ▼
┌─────────────────┐
│   MySQL 8.0+    │
│   Database      │
└─────────────────┘
```

### 1.2. Công Nghệ Sử Dụng

**Backend Core (Pure Java):**
- Java 17
- TCP Socket (ServerSocket, Socket)
- DataInputStream/DataOutputStream
- Thread Pool (ExecutorService)
- HikariCP 5.1.0 (Connection Pooling)
- Jackson 2.15.2 (JSON Serialization)
- MySQL Connector 8.0+

**Gateway (Spring Boot):**
- Spring Boot 3.2.0
- Spring WebSocket
- TCP Client (Socket)
- ConcurrentHashMap (Request Routing)

**Shared Module:**
- DTOs (Data Transfer Objects)
- Protocol Definitions
- Utility Classes (CardUtils, GameRuleUtils, JsonUtils)

**Frontend:**
- React (chưa phân tích chi tiết)
- WebSocket Client
- Tailwind CSS

**Database:**
- MySQL 8.0+
- 6 tables: users, user_profiles, cards, games, game_rounds, active_sessions

### 1.3. Giao Thức Truyền Thông

**Length-Prefixed Framing Protocol:**
```
┌──────────────┬─────────────────────┐
│  4 bytes     │  N bytes            │
│  Length (N)  │  JSON Payload       │
└──────────────┴─────────────────────┘
```

**Đặc điểm:**
- **4 byte đầu**: Big-endian integer chỉ độ dài của JSON payload
- **N byte tiếp theo**: Chuỗi JSON UTF-8 (MessageEnvelope)
- **Thread-safe**: Synchronized write, blocking read
- **Tương thích**: Core và Gateway implement giống hệt nhau

**Ví dụ MessageEnvelope:**
```json
{
  "type": "AUTH_REQUEST",
  "action": "LOGIN",
  "correlationId": "uuid-1234",
  "sessionId": "session-5678",
  "timestamp": 1729400000000,
  "payload": {
    "username": "player1",
    "password": "hashed_password"
  }
}
```

---

## 2. Đánh Giá Kiến Trúc Hiện Tại

### 2.1. Core Server Architecture

**CoreServer.java** - Entry Point
```
Responsibilities:
  ✓ Initialize DatabaseManager (HikariCP)
  ✓ Create AuthService (STUB - chỉ có constructor)
  ✓ Create SessionManager (COMPLETE)
  ✓ Create GameService (COMPLETE)
  ✓ Start CoreServerListener
  ✓ Shutdown hook registration
```

**CoreServerListener.java** - Accept Loop
```
Thread Model: Dedicated "accept-loop" thread
Responsibilities:
  ✓ ServerSocket.accept() blocking call
  ✓ Socket timeout: 30 seconds
  ✓ Create ClientConnectionHandler per connection
  ✓ Pass all services to handler
  ✓ Graceful shutdown support
```

**ClientConnectionHandler.java** - Connection Handler (317 lines)
```
Thread Model:
  • I/O Thread: Reads messages from socket (blocking)
  • Worker Pool: Processes business logic (CachedThreadPool)

Protocol Implementation:
  ✓ Length-prefixed reading:
      int length = in.readInt();
      byte[] bytes = new byte[length];
      in.readFully(bytes);
  
  ✓ Synchronized writing:
      synchronized(out) {
          out.writeInt(jsonBytes.length);
          out.write(jsonBytes);
          out.flush();
      }

Business Logic Handlers: **ALL STUBBED**
  ⚠️ handleRegister() → returns NOT_IMPLEMENTED
  ⚠️ handleLogin() → returns NOT_IMPLEMENTED
  ⚠️ handleLogout() → returns NOT_IMPLEMENTED
  ⚠️ handleMatchRequest() → returns NOT_IMPLEMENTED
  ⚠️ handleGameStart() → returns NOT_IMPLEMENTED
  ⚠️ handlePlayCard() → returns NOT_IMPLEMENTED
```

### 2.2. Service Layer

**AuthService.java** - Authentication Service
```
Status: ⚠️ EMPTY STUB (4 lines of code)

Current Implementation:
  public class AuthService {
      private final DatabaseManager dbManager;
      
      public AuthService(DatabaseManager dbManager) {
          this.dbManager = dbManager;
      }
  }

Missing Methods:
  ❌ register(String username, String password, String email)
  ❌ login(String username, String password)
  ❌ hashPassword(String plainPassword)
  ❌ verifyPassword(String plainPassword, String hashedPassword)

Impact: 🔴 CRITICAL - Blocks all authentication functionality
```

**SessionManager.java** - Session Management
```
Status: ✅ COMPLETE

Features:
  ✓ ConcurrentHashMap<String, SessionContext> (O(1) lookup)
  ✓ createSession(String userId, String username) → sessionId
  ✓ getSession(String sessionId) → SessionContext
  ✓ removeSession(String sessionId)
  ✓ setMatchId(String sessionId, String matchId)
  ✓ DB persistence (INSERT/DELETE to active_sessions table)

Thread Safety: ✓ ConcurrentHashMap + synchronized DB operations
```

**GameService.java** - Game Logic
```
Status: ✅ COMPLETE

Features:
  ✓ GameState inner class (tracks match state)
  ✓ initializeGame(String matchId, String player1Id, String player2Id)
      → Uses CardUtils.generateDeck(), shuffle(), dealForGame()
  ✓ playCard(String matchId, String playerId, int cardId)
  ✓ autoPickCard(String matchId, String playerId)
  ✓ executeRound(String matchId)
      → Uses GameRuleUtils for card comparison
  ✓ Full game loop for 3 rounds
  ✓ Win condition: Best of 3 rounds

Dependencies: CardUtils, GameRuleUtils, IdUtils (all from shared module)
```

**DatabaseManager.java** - Connection Pool
```
Status: ✅ COMPLETE

Configuration:
  • Max connections: 10
  • Min idle: 2
  • Connection timeout: 30s
  • Idle timeout: 10min
  • Max lifetime: 30min
  • Leak detection: 60s

Features:
  ✓ Singleton pattern
  ✓ getConnection() → try-with-resources pattern
  ✓ Health check: SELECT 1
  ✓ Graceful shutdown

Thread Safety: ✓ HikariCP handles internal synchronization
```

### 2.3. Gateway Architecture

**CoreTcpClient.java** - TCP Client
```
Status: ✅ COMPLETE

Features:
  ✓ Persistent connection to Core (localhost:9090)
  ✓ Length-prefixed protocol (matches Core exactly)
  ✓ Listener thread: Reads responses from Core
  ✓ Synchronized sendMessageToCore()
  ✓ Spring lifecycle: InitializingBean, DisposableBean
  ✓ Auto-reconnect on connection failure

Thread Safety: ✓ Synchronized write, dedicated read thread
```

**GatewayWebSocketHandler.java** - WebSocket Handler
```
Status: ✅ COMPLETE

Routing Strategy:
  1. Request-Response:
     • pendingRequests: ConcurrentHashMap<correlationId, WebSocketSession>
     • Client sends request → forward to Core → wait for response → route back
  
  2. Push Notifications:
     • activeClientSessions: ConcurrentHashMap<sessionId, WebSocketSession>
     • Core sends notification → lookup sessionId → push to client

Features:
  ✓ afterConnectionEstablished()
  ✓ handleTextMessage() → forward to Core
  ✓ forwardMessageToClient() → route response/notification
  ✓ afterConnectionClosed() → cleanup maps

Thread Safety: ✓ ConcurrentHashMap + WebSocket session handling
```

### 2.4. Shared Module

**Protocol Classes:**
```
✓ MessageEnvelope (type, action, correlationId, sessionId, timestamp, payload)
✓ MessageType (AUTH_REQUEST, AUTH_RESPONSE, GAME_EVENT, etc.)
✓ MessageFactory (create methods for common messages)
✓ ErrorInfo (code, message, details)
```

**DTOs (Data Transfer Objects):**
```
Auth Domain:
  ✓ LoginRequestDto, RegisterRequestDto
  ✓ LoginSuccessDto, LoginFailureDto, RegisterResponseDto
  ✓ SessionDto

Game Domain:
  ✓ CardDto (id, suit, rank, value, imageUrl)
  ✓ RoundRevealDto, PlayCardRequestDto
  ✓ GameResultDto, MatchStateDto

System Domain:
  ✓ ErrorResponseDto
```

**Utilities:**
```
✓ JsonUtils: Jackson wrapper (toJson, fromJson, toMap)
✓ CardUtils: Deck generation, shuffling, dealing
✓ GameRuleUtils: Card comparison, winner determination
✓ IdUtils: UUID generation
✓ ValidationUtils: Input validation
```

**Constants:**
```
✓ GameConstants (TOTAL_ROUNDS=3, DECK_SIZE=52)
✓ ProtocolConstants (MAX_MESSAGE_SIZE, TIMEOUT values)
✓ TimeConstants (SESSION_TIMEOUT, etc.)
```

---

## 3. Điểm Mạnh và Hạn Chế

### 3.1. Điểm Mạnh (Strengths)

#### ✅ 1. Kiến Trúc Phân Tầng Rõ Ràng
- **Separation of Concerns**: Core, Gateway, Shared module tách biệt hoàn toàn
- **Dependency Direction**: Gateway và Core đều depend vào Shared, không có circular dependency
- **Testability**: Mỗi layer có thể test độc lập

#### ✅ 2. Protocol Design Xuất Sắc
- **Length-Prefixed Framing**: Giải quyết TCP stream fragmentation problem
- **JSON Flexibility**: Dễ debug, dễ extend
- **Consistent Implementation**: Core và Gateway implement giống hệt nhau
- **Network Programming Focus**: Thể hiện rõ kiến thức về socket, framing protocol

#### ✅ 3. Threading Model Đúng Đắn
- **I/O Thread + Worker Pool**: Best practice cho TCP server
- **CachedThreadPool**: Tự động scale theo số connections
- **Thread Safety**: Proper use của synchronized, ConcurrentHashMap
- **Blocking I/O**: Đơn giản, dễ debug (phù hợp cho học tập)

#### ✅ 4. Connection Pooling Professional
- **HikariCP**: Industry-standard connection pool
- **Proper Configuration**: Max 10, min 2 idle, leak detection
- **Resource Management**: try-with-resources pattern
- **Health Check**: SELECT 1 before returning connection

#### ✅ 5. Shared Module Reusability
- **No Duplication**: DTOs, utilities được reuse hoàn toàn
- **Type Safety**: Compile-time checking cho protocol messages
- **Utility Classes**: CardUtils, GameRuleUtils giảm code duplication

#### ✅ 6. Gateway Routing Thông Minh
- **Dual Routing**: correlationId (request-response) + sessionId (push)
- **ConcurrentHashMap**: O(1) lookup, thread-safe
- **WebSocket Bridge**: Tách biệt HTTP/WS khỏi TCP logic

#### ✅ 7. Game Logic Hoàn Chỉnh
- **GameService**: Full implementation với CardUtils, GameRuleUtils
- **State Management**: GameState tracks match progress
- **Game Rules**: 3 rounds, best of 3, card comparison logic

### 3.2. Hạn Chế (Limitations)

#### 🔴 1. AuthService Hoàn Toàn Trống
**Severity:** CRITICAL - Blocking  
**Impact:** Không thể login/register  
**Current State:**
```java
// Chỉ có constructor, không có method nào
public class AuthService {
    private final DatabaseManager dbManager;
    public AuthService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
}
```

**Required Work:**
- Implement `register(username, password, email)` method
- Implement `login(username, password)` method
- Implement `hashPassword(plainPassword)` with SHA-256
- Implement `verifyPassword(plainPassword, hashedPassword)`
- SQL queries: INSERT users, SELECT users WHERE username
- Password security: Salt + Hash (recommended: use BCrypt or PBKDF2)

**Estimated Effort:** 6-8 hours for 1 developer

#### 🟠 2. ClientConnectionHandler Có Stub Methods
**Severity:** HIGH - Blocking business logic  
**Impact:** Tất cả handlers return NOT_IMPLEMENTED  
**Current State:**
```java
private void handleRegister(MessageEnvelope msg) {
    sendError(msg.getCorrelationId(), "NOT_IMPLEMENTED", "Register not implemented");
}

private void handleLogin(MessageEnvelope msg) {
    sendError(msg.getCorrelationId(), "NOT_IMPLEMENTED", "Login not implemented");
}

// Tương tự cho: handleLogout, handleMatchRequest, handleGameStart, handlePlayCard
```

**Required Work:**
- Wire up handlers to AuthService, SessionManager, GameService
- Parse request DTOs from JSON payload
- Call appropriate service methods
- Create response DTOs
- Send responses with correlationId

**Estimated Effort:** 8-10 hours for 1 developer (after AuthService done)

#### 🟡 3. MatchmakingService Không Thấy
**Severity:** MEDIUM - Feature incomplete  
**Impact:** Không có cơ chế matchmaking  
**Current State:** Không có file MatchmakingService.java trong codebase

**Required Work:**
- Create MatchmakingService.java
- Implement queue-based matchmaking (FIFO)
- Match 2 players → create GameState via GameService
- Notify both players via SessionManager

**Estimated Effort:** 4-6 hours for 1 developer

#### 🟡 4. Frontend Chưa Được Phân Tích
**Severity:** MEDIUM - Unknown status  
**Impact:** Không biết tình trạng UI  
**Current State:** Có React project structure nhưng chưa review code

**Required Work:**
- Review WebSocket client implementation
- Check protocol compatibility (MessageEnvelope structure)
- Verify UI components exist
- Test end-to-end flow

**Estimated Effort:** 4-6 hours for 1 developer (review + testing)

#### 🟢 5. Thiếu Error Handling Toàn Diện
**Severity:** LOW - Code quality  
**Impact:** Khó debug, crash handling  
**Current State:** Có try-catch cơ bản, nhưng không có error logging framework

**Recommended:**
- Add SLF4J + Logback for logging
- Structured error responses (ErrorInfo DTO)
- Log exceptions with stack traces
- Client-friendly error messages

**Estimated Effort:** 2-3 hours per module

#### 🟢 6. Thiếu Integration Tests
**Severity:** LOW - Testing coverage  
**Impact:** Không test được full flow  
**Current State:** Có TestCoreClient.java nhưng chưa comprehensive

**Recommended:**
- End-to-end test: Client → Gateway → Core → DB
- Load test: Multiple concurrent connections
- Protocol test: Message serialization/deserialization
- Game logic test: Full 3-round game

**Estimated Effort:** 6-8 hours for testing suite

---

## 4. Phân Tích Rủi Ro Kỹ Thuật

### 4.1. Rủi Ro CRITICAL (Phải giải quyết ngay)

| Rủi Ro | Mô Tả | Impact | Giải Pháp |
|--------|-------|--------|-----------|
| **AuthService rỗng** | Không có authentication logic | 🔴 Block toàn bộ user flow | Priority 1: Implement ngay |
| **Handler stubs** | ClientConnectionHandler không có business logic | 🔴 Block message processing | Priority 2: Wire handlers sau khi có AuthService |

### 4.2. Rủi Ro HIGH (Cần giải quyết sớm)

| Rủi Ro | Mô Tả | Impact | Giải Pháp |
|--------|-------|--------|-----------|
| **No MatchmakingService** | Không thể ghép cặp người chơi | 🟠 Feature incomplete | Implement simple FIFO queue |
| **Frontend unknown** | Không biết UI status | 🟠 Integration risk | Review code + test WebSocket |

### 4.3. Rủi Ro MEDIUM (Cải thiện dần)

| Rủi Ro | Mô Tả | Impact | Giải Pháp |
|--------|-------|--------|-----------|
| **No logging framework** | Khó debug production issues | 🟡 Operational risk | Add SLF4J + Logback |
| **No load testing** | Không biết system limits | 🟡 Performance risk | Use JMeter or custom test |
| **No monitoring** | Không track server health | 🟡 Operational risk | Add health check endpoint |

### 4.4. Technical Debt

1. **Password Security**: Hiện tại chỉ plan SHA-256, nên upgrade lên BCrypt/PBKDF2
2. **Connection Limits**: CachedThreadPool không limit threads, có thể OOM với nhiều connections
3. **Database Schema**: Chưa verify schema với ERD design
4. **Error Messages**: English mixed với Vietnamese, cần standardize

---

## 5. Chiến Lược Triển Khai

### 5.1. Nguyên Tắc Triển Khai

1. **Incremental Development**: Làm từng phần nhỏ, test liên tục
2. **Vertical Slicing**: Mỗi feature làm full stack (Core → Gateway → Frontend)
3. **Test-Driven**: Viết test trước khi implement (recommended)
4. **Code Review**: Team leader review code trước khi merge
5. **Daily Sync**: 15 phút standup mỗi ngày

### 5.2. Phase 1: Foundation (Week 1) - 🔥 PRIORITY

**Mục tiêu:** Hoàn thiện Core Backend infrastructure

**Tasks:**

#### Task 1.1: Implement AuthService (6-8h)
- Người thực hiện: **Backend Core A**
- Deliverables:
  - `register(username, password, email)` method
  - `login(username, password)` method
  - `hashPassword()` với SHA-256 (hoặc BCrypt)
  - SQL queries cho users table
  - Unit tests cho AuthService

**Code Skeleton:**
```java
public class AuthService {
    private final DatabaseManager dbManager;
    
    public AuthService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    public RegisterResponseDto register(String username, String password, String email) {
        // 1. Validate input
        // 2. Check username exists
        // 3. Hash password
        // 4. INSERT INTO users
        // 5. Return RegisterResponseDto
    }
    
    public LoginSuccessDto login(String username, String password) {
        // 1. SELECT user WHERE username
        // 2. Verify password hash
        // 3. Return LoginSuccessDto with userId
        // throws LoginFailureDto if invalid
    }
    
    private String hashPassword(String plainPassword) {
        // Use SHA-256 or BCrypt
    }
}
```

#### Task 1.2: Wire ClientConnectionHandler (8-10h)
- Người thực hiện: **Backend Core A**
- Dependencies: Task 1.1 (AuthService)
- Deliverables:
  - Implement `handleRegister()` → call AuthService.register()
  - Implement `handleLogin()` → call AuthService.login() + SessionManager.createSession()
  - Implement `handleLogout()` → call SessionManager.removeSession()
  - Parse JSON payload to DTOs
  - Create response MessageEnvelope với correlationId
  - Unit tests cho handlers

**Example Implementation:**
```java
private void handleLogin(MessageEnvelope msg) {
    try {
        // 1. Parse payload
        LoginRequestDto request = JsonUtils.fromJson(
            JsonUtils.toJson(msg.getPayload()), 
            LoginRequestDto.class
        );
        
        // 2. Call AuthService
        LoginSuccessDto result = authService.login(
            request.getUsername(), 
            request.getPassword()
        );
        
        // 3. Create session
        String sessionId = sessionManager.createSession(
            result.getUserId(), 
            result.getUsername()
        );
        
        // 4. Build response
        result.setSessionId(sessionId);
        MessageEnvelope response = MessageFactory.createAuthResponse(
            "LOGIN_SUCCESS",
            msg.getCorrelationId(),
            sessionId,
            result
        );
        
        // 5. Send response
        sendMessage(response);
        
    } catch (Exception e) {
        sendError(msg.getCorrelationId(), "LOGIN_FAILED", e.getMessage());
    }
}
```

#### Task 1.3: Database Schema Verification (2-3h)
- Người thực hiện: **Backend Core B**
- Deliverables:
  - Verify DB_SCRIPT.sql với ERD design
  - Add indexes cho performance (users.username, active_sessions.session_id)
  - Add foreign keys nếu thiếu
  - Test connection pool với DatabaseManager

**SQL Review Checklist:**
```sql
-- Check indexes
SHOW INDEX FROM users;
SHOW INDEX FROM active_sessions;

-- Add missing indexes
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_session_id ON active_sessions(session_id);

-- Test connection
SELECT 1;
```

### 5.3. Phase 2: Matchmaking (Week 2)

**Mục tiêu:** Implement matchmaking và game flow

#### Task 2.1: MatchmakingService Implementation (4-6h)
- Người thực hiện: **Backend Core B**
- Dependencies: Phase 1 complete
- Deliverables:
  - Create MatchmakingService.java
  - FIFO queue cho waiting players
  - Match 2 players → initializeGame() via GameService
  - Notify both players via SessionManager
  - Unit tests

**Code Skeleton:**
```java
public class MatchmakingService {
    private final Queue<MatchRequest> waitingQueue = new ConcurrentLinkedQueue<>();
    private final GameService gameService;
    private final SessionManager sessionManager;
    
    public void requestMatch(String sessionId, String userId) {
        // 1. Add to queue
        // 2. Try to match
        // 3. If matched, create game
    }
    
    private void tryMatch() {
        if (waitingQueue.size() >= 2) {
            MatchRequest p1 = waitingQueue.poll();
            MatchRequest p2 = waitingQueue.poll();
            
            String matchId = gameService.initializeGame(p1.userId, p2.userId);
            
            // Notify both players
            sessionManager.setMatchId(p1.sessionId, matchId);
            sessionManager.setMatchId(p2.sessionId, matchId);
            
            // Send MATCH_FOUND notifications
        }
    }
}
```

#### Task 2.2: Wire Game Handlers (4-5h)
- Người thực hiện: **Backend Core B**
- Dependencies: Task 2.1
- Deliverables:
  - Implement `handleMatchRequest()` → call MatchmakingService
  - Implement `handleGameStart()` → verify match exists
  - Implement `handlePlayCard()` → call GameService.playCard()
  - Send game events to both players

#### Task 2.3: Gateway Testing (2-3h)
- Người thực hiện: **Team Leader**
- Deliverables:
  - Test full flow: WebSocket → Gateway → Core → DB
  - Verify correlationId routing works
  - Verify sessionId push notifications work
  - Load test với 10 concurrent connections

### 5.4. Phase 3: Frontend Integration (Week 2-3)

**Mục tiêu:** Connect frontend với backend

#### Task 3.1: Frontend Protocol Implementation (6-8h)
- Người thực hiện: **Frontend Developer**
- Dependencies: Phase 1 complete (Auth working)
- Deliverables:
  - WebSocket client với length-prefixed protocol
  - MessageEnvelope serialization/deserialization
  - correlationId generation và tracking
  - Store sessionId from login response
  - Handle push notifications (MATCH_FOUND, GAME_EVENT)

**JavaScript Example:**
```javascript
class GameClient {
    constructor(wsUrl) {
        this.ws = new WebSocket(wsUrl);
        this.pendingRequests = new Map(); // correlationId → Promise
        this.sessionId = null;
        
        this.ws.onmessage = (event) => {
            const envelope = JSON.parse(event.data);
            
            // Handle response
            if (envelope.correlationId && this.pendingRequests.has(envelope.correlationId)) {
                const resolve = this.pendingRequests.get(envelope.correlationId);
                resolve(envelope);
                this.pendingRequests.delete(envelope.correlationId);
            }
            
            // Handle push notification
            if (envelope.type === 'GAME_EVENT') {
                this.onGameEvent(envelope);
            }
        };
    }
    
    async login(username, password) {
        const envelope = {
            type: 'AUTH_REQUEST',
            action: 'LOGIN',
            correlationId: uuidv4(),
            timestamp: Date.now(),
            payload: { username, password }
        };
        
        const response = await this.send(envelope);
        this.sessionId = response.sessionId;
        return response;
    }
    
    send(envelope) {
        return new Promise((resolve) => {
            this.pendingRequests.set(envelope.correlationId, resolve);
            this.ws.send(JSON.stringify(envelope));
        });
    }
}
```

#### Task 3.2: UI Components (8-10h)
- Người thực hiện: **Frontend Developer**
- Deliverables:
  - Login/Register forms
  - Matchmaking lobby (waiting screen)
  - Game board (3 rounds UI)
  - Card display components
  - Result screen

#### Task 3.3: End-to-End Testing (3-4h)
- Người thực hiện: **Team Leader + Frontend**
- Deliverables:
  - Test register → login → matchmaking → game → result
  - Test 2 players simultaneously
  - Test error cases (invalid login, disconnect)
  - Performance test (latency, throughput)

### 5.5. Phase 4: Polish (Week 3)

#### Task 4.1: Error Handling & Logging (3-4h)
- Người thực hiện: **Team Leader**
- Deliverables:
  - Add SLF4J + Logback to Core
  - Log all exceptions with stack traces
  - Structured error responses
  - Client-friendly error messages (Vietnamese)

#### Task 4.2: Documentation (2-3h)
- Người thực hiện: **Team Leader**
- Deliverables:
  - Update README with setup instructions
  - API documentation (message types)
  - Architecture diagram
  - Deployment guide

#### Task 4.3: Code Review & Refactoring (4-5h)
- Người thực hiện: **Toàn bộ team**
- Deliverables:
  - Code review sessions
  - Fix code smells
  - Add comments
  - Standardize naming conventions

---

## 6. Phân Công Chi Tiết

### 6.1. Team Leader (Bạn)

**Vai trò:** Architecture oversight, integration, code review, blocking issue resolution

**Responsibilities:**
1. **Code Review**: Review tất cả PRs trước khi merge
2. **Integration Testing**: Test full stack flow sau mỗi phase
3. **Blocking Issues**: Giải quyết khi team members bị stuck
4. **Documentation**: Maintain README, architecture docs
5. **Gateway Module**: Chịu trách nhiệm Gateway code quality
6. **Deployment**: Setup server, database, deployment scripts

**Weekly Tasks:**

**Week 1:**
- Review AuthService implementation (Backend Core A)
- Review ClientConnectionHandler changes (Backend Core A)
- Test Core server với multiple connections
- Setup development environment cho team

**Week 2:**
- Review MatchmakingService (Backend Core B)
- Review Game handlers (Backend Core B)
- Gateway integration testing
- Help Frontend with WebSocket client issues

**Week 3:**
- End-to-end testing với Frontend
- Code review sessions
- Performance testing & optimization
- Prepare demo & documentation

**Time Allocation:**
- Code Review: 30%
- Integration Testing: 30%
- Architecture/Documentation: 20%
- Blocking Issues: 20%

### 6.2. Backend Core A (Networking & Auth Focus)

**Vai trò:** Socket programming, authentication, connection handling

**Primary Responsibilities:**
1. **AuthService**: Complete implementation
2. **ClientConnectionHandler**: Wire handlers to services
3. **Protocol**: Ensure length-prefixed framing correctness
4. **Session Management**: Work với SessionManager
5. **Thread Safety**: Ensure concurrent access correctness

**Detailed Tasks:**

**Week 1 (20-25 hours):**
- [ ] Implement AuthService.register() method (3h)
  - Input validation
  - Check duplicate username
  - Hash password với SHA-256
  - INSERT INTO users table
  - Handle SQL exceptions
  
- [ ] Implement AuthService.login() method (3h)
  - SELECT user by username
  - Verify password hash
  - Return LoginSuccessDto
  - Handle invalid credentials
  
- [ ] Write unit tests cho AuthService (2h)
  - Test register with valid inputs
  - Test register with duplicate username
  - Test login with valid credentials
  - Test login with invalid credentials
  
- [ ] Implement handleRegister() in ClientConnectionHandler (2h)
  - Parse LoginRequestDto from JSON
  - Call AuthService.register()
  - Build response MessageEnvelope
  - Send response với correlationId
  
- [ ] Implement handleLogin() in ClientConnectionHandler (2h)
  - Parse LoginRequestDto from JSON
  - Call AuthService.login()
  - Create session via SessionManager
  - Send response với sessionId
  
- [ ] Implement handleLogout() in ClientConnectionHandler (1h)
  - Parse request
  - Call SessionManager.removeSession()
  - Send confirmation
  
- [ ] Test handlers với TestCoreClient.java (3h)
  - Test register flow
  - Test login flow
  - Test logout flow
  - Test error cases
  
- [ ] Code review với Team Leader (2h)
- [ ] Fix issues từ review (2h)

**Week 2 (10-15 hours):**
- [ ] Support Backend Core B với MatchmakingService integration (3h)
- [ ] Help debug connection issues (2h)
- [ ] Add logging to handlers (2h)
- [ ] Performance testing (load test) (3h)

**Week 3 (5-10 hours):**
- [ ] Frontend integration support (3h)
- [ ] Fix bugs từ integration testing (3h)
- [ ] Code refactoring (2h)

**Skills Required:**
- ✅ Java Socket Programming (ServerSocket, Socket, DataInputStream)
- ✅ Threading (ExecutorService, synchronized)
- ✅ JSON parsing (Jackson)
- ✅ SQL (INSERT, SELECT)
- ✅ Password hashing (MessageDigest)

### 6.3. Backend Core B (Game Logic & Data Focus)

**Vai trò:** Game logic, database, matchmaking

**Primary Responsibilities:**
1. **MatchmakingService**: Implement FIFO queue matchmaking
2. **Database Schema**: Verify and optimize
3. **Game Handlers**: Wire GameService to ClientConnectionHandler
4. **Data Flow**: Ensure game state persistence
5. **Testing**: Game logic testing

**Detailed Tasks:**

**Week 1 (10-15 hours):**
- [ ] Review GameService implementation (2h)
  - Understand GameState structure
  - Understand CardUtils, GameRuleUtils
  - Test game logic manually
  
- [ ] Verify database schema (3h)
  - Check DB_SCRIPT.sql
  - Add missing indexes (users.username, etc.)
  - Test connection pool
  - Run health check queries
  
- [ ] Review SessionManager (1h)
  - Understand session lifecycle
  - Test createSession, removeSession
  
- [ ] Plan MatchmakingService design (2h)
  - Design FIFO queue structure
  - Plan notification flow
  - Design MatchRequest DTO

**Week 2 (20-25 hours):**
- [ ] Implement MatchmakingService (6h)
  - Create MatchmakingService.java
  - ConcurrentLinkedQueue for waiting players
  - requestMatch() method
  - tryMatch() method (pair 2 players)
  - Integration with GameService.initializeGame()
  
- [ ] Wire handleMatchRequest() (2h)
  - Parse request
  - Call MatchmakingService.requestMatch()
  - Send confirmation
  
- [ ] Wire handleGameStart() (1h)
  - Verify matchId exists
  - Retrieve GameState
  - Send initial game state to both players
  
- [ ] Wire handlePlayCard() (3h)
  - Parse PlayCardRequestDto
  - Call GameService.playCard()
  - Handle round completion
  - Send game events to both players
  
- [ ] Implement push notifications (3h)
  - MATCH_FOUND notification
  - ROUND_COMPLETED notification
  - GAME_FINISHED notification
  
- [ ] Write unit tests (4h)
  - Test MatchmakingService queue logic
  - Test game handlers
  - Test notification delivery
  
- [ ] Integration testing (3h)
  - Test full game flow with 2 clients
  - Test edge cases (disconnect, timeout)

**Week 3 (5-10 hours):**
- [ ] Database optimization (2h)
  - Add composite indexes
  - Optimize queries
- [ ] Game logic bug fixes (3h)
- [ ] Load testing (2h)

**Skills Required:**
- ✅ Java Collections (Queue, Map)
- ✅ Concurrency (ConcurrentLinkedQueue)
- ✅ Game Logic (understand CardUtils, GameRuleUtils)
- ✅ SQL (indexes, optimization)
- ✅ Testing (unit tests, integration tests)

### 6.4. Frontend Developer

**Vai trò:** React UI, WebSocket client, user experience

**Primary Responsibilities:**
1. **WebSocket Client**: Implement protocol-compatible client
2. **UI Components**: Login, lobby, game board, results
3. **State Management**: Track game state in React
4. **User Experience**: Smooth animations, error handling
5. **Testing**: Frontend integration testing

**Detailed Tasks:**

**Week 1 (15-20 hours):**
- [ ] Setup development environment (1h)
  - npm install
  - Configure Vite
  - Test dev server
  
- [ ] Review protocol documentation (2h)
  - Read MessageEnvelope structure
  - Understand correlationId routing
  - Understand sessionId for push
  
- [ ] Implement WebSocket client (6h)
  - Create GameClient.js class
  - WebSocket connection management
  - MessageEnvelope serialization
  - correlationId tracking (Promise-based)
  - Handle push notifications
  - Auto-reconnect logic
  
- [ ] Implement Auth service (3h)
  - login() method
  - register() method
  - Store sessionId in localStorage
  - Handle errors
  
- [ ] Create Login/Register UI (4h)
  - Login form component
  - Register form component
  - Form validation
  - Error display

**Week 2 (20-25 hours):**
- [ ] Implement Matchmaking UI (4h)
  - Lobby component
  - "Find Match" button
  - Waiting spinner
  - Cancel match button
  
- [ ] Implement Game Board UI (8h)
  - Game board layout
  - Player 1 & Player 2 areas
  - Card display components
  - Current round indicator
  - Score display
  - Play card button
  
- [ ] Implement Game State Management (4h)
  - React Context for game state
  - Handle MATCH_FOUND notification
  - Handle ROUND_COMPLETED notification
  - Handle GAME_FINISHED notification
  - Update UI based on game state
  
- [ ] Implement Result Screen (2h)
  - Winner/Loser display
  - Final score
  - "Play Again" button

**Week 3 (10-15 hours):**
- [ ] Integration testing với Backend (5h)
  - Test full flow: login → match → play → result
  - Test error cases
  - Fix bugs
  
- [ ] UI Polish (4h)
  - Animations (card flip, etc.)
  - Responsive design
  - Tailwind CSS styling
  
- [ ] User Experience improvements (3h)
  - Loading states
  - Error messages
  - Confirmation dialogs

**Skills Required:**
- ✅ React (components, hooks, state management)
- ✅ WebSocket API
- ✅ JavaScript (async/await, Promises)
- ✅ JSON serialization
- ✅ Tailwind CSS

---

## 7. Timeline và Milestone

### 7.1. Timeline Overview (3 Weeks)

```
Week 1: Foundation
├─ Day 1-2: AuthService implementation
├─ Day 3-4: ClientConnectionHandler wiring
├─ Day 5: Testing & code review
└─ Milestone 1: Auth working ✓

Week 2: Game Flow
├─ Day 1-2: MatchmakingService implementation
├─ Day 3-4: Game handlers wiring
├─ Day 5: Frontend integration start
└─ Milestone 2: Matchmaking working ✓

Week 3: Integration & Polish
├─ Day 1-2: Frontend completion
├─ Day 3: End-to-end testing
├─ Day 4: Bug fixes & polish
├─ Day 5: Demo preparation
└─ Milestone 3: MVP complete ✓
```

### 7.2. Milestone Definitions

#### Milestone 1: Authentication Working (End of Week 1)
**Definition of Done:**
- [ ] AuthService implements register(), login(), hashPassword()
- [ ] ClientConnectionHandler implements handleRegister(), handleLogin(), handleLogout()
- [ ] Database có users table với indexes
- [ ] SessionManager tích hợp với login flow
- [ ] Unit tests pass cho AuthService
- [ ] Integration test: TestCoreClient có thể register → login → logout
- [ ] Code review complete, không có blocking issues

**Demo Scenario:**
```
1. Start CoreServer
2. Run TestCoreClient
3. Register user "player1"
4. Login with "player1" credentials
5. Receive sessionId
6. Logout
7. Verify session removed from database
```

**Rollback Plan:** Nếu không đạt milestone, Week 2 delay để complete

#### Milestone 2: Matchmaking Working (End of Week 2)
**Definition of Done:**
- [ ] MatchmakingService implements FIFO queue
- [ ] ClientConnectionHandler implements handleMatchRequest(), handleGameStart(), handlePlayCard()
- [ ] GameService tích hợp với handlers
- [ ] Push notifications working (MATCH_FOUND, ROUND_COMPLETED, GAME_FINISHED)
- [ ] Gateway routes notifications correctly
- [ ] Integration test: 2 clients có thể match và play full game
- [ ] Frontend có WebSocket client working

**Demo Scenario:**
```
1. Start CoreServer, Gateway
2. Client 1 login → request match
3. Client 2 login → request match
4. Both clients receive MATCH_FOUND notification
5. Client 1 plays card
6. Client 2 plays card
7. Both receive ROUND_COMPLETED notification
8. Repeat for 3 rounds
9. Both receive GAME_FINISHED notification
```

**Rollback Plan:** Nếu không đạt milestone, Week 3 focus vào backend completion trước frontend

#### Milestone 3: MVP Complete (End of Week 3)
**Definition of Done:**
- [ ] Frontend UI complete (login, lobby, game, result)
- [ ] End-to-end flow working: register → login → match → play → result
- [ ] 2 players có thể play simultaneously
- [ ] Error handling working (invalid login, disconnect, etc.)
- [ ] Performance test pass (10 concurrent users)
- [ ] Documentation complete (README, architecture, API docs)
- [ ] Demo video recorded
- [ ] Code cleaned up, comments added

**Demo Scenario:**
```
1. Open 2 browsers
2. Browser 1: Register "player1" → login → find match
3. Browser 2: Register "player2" → login → find match
4. Both see game board
5. Play 3 rounds alternating
6. See winner screen
7. Play again
```

**Success Criteria:**
- ✅ Thầy giáo có thể chạy demo trên localhost
- ✅ Không có critical bugs trong happy path
- ✅ Code quality đạt chuẩn (clean, commented, tested)

### 7.3. Daily Standup Format

**Time:** 9:00 AM mỗi ngày (15 phút)

**Format:**
1. **Yesterday**: Mỗi người báo cáo đã làm gì
2. **Today**: Mỗi người commit sẽ làm gì
3. **Blockers**: Có vấn đề gì đang block không?

**Example:**
```
Backend Core A:
  Yesterday: Implemented AuthService.register() và login()
  Today: Wire handleLogin() in ClientConnectionHandler
  Blockers: Không có

Backend Core B:
  Yesterday: Reviewed database schema, added indexes
  Today: Start MatchmakingService implementation
  Blockers: Cần clarify về notification flow

Frontend:
  Yesterday: Implemented WebSocket client basic connection
  Today: Implement login/register UI
  Blockers: Chưa rõ MessageEnvelope structure

Team Leader:
  Yesterday: Setup dev environment, reviewed code
  Today: Code review AuthService, help Frontend với protocol
  Blockers: Không có
```

### 7.4. Code Review Process

**Frequency:** Mỗi khi có PR (Pull Request)

**Checklist:**
- [ ] Code compiles without errors
- [ ] Code follows naming conventions
- [ ] No hardcoded values (use constants)
- [ ] Error handling present
- [ ] Comments for complex logic
- [ ] Unit tests included (if applicable)
- [ ] No code duplication
- [ ] Thread-safe (if multi-threaded)

**Team Leader Review Priorities:**
1. **Architecture**: Có vi phạm separation of concerns không?
2. **Protocol**: MessageEnvelope usage đúng không?
3. **Thread Safety**: Có race conditions không?
4. **Error Handling**: Có handle exceptions không?
5. **Code Quality**: Readable, maintainable không?

---

## 8. Kết Luận và Khuyến Nghị

### 8.1. Tóm Tắt Đánh Giá

**Kiến trúc tổng thể:** 🟢 EXCELLENT
- Separation of concerns rõ ràng
- Protocol design professional
- Threading model đúng đắn
- Shared module reusability tốt

**Implementation hiện tại:** 🟡 PARTIAL
- Infrastructure: ✅ Complete (90%)
- Business Logic: ⚠️ Stub (20%)
- Game Logic: ✅ Complete (100%)
- Frontend: ❓ Unknown

**Rủi ro chính:** 🔴 AuthService và handlers chưa implement
- Impact: CRITICAL blocking
- Effort: 14-18 hours cho 1 developer
- Priority: Phải làm ngay Week 1

### 8.2. Khuyến Nghị Ưu Tiên

#### Ưu Tiên 1 (CRITICAL): Complete AuthService
**Tại sao:**
- Block toàn bộ user flow
- Không có auth = không có game
- Foundation cho tất cả features khác

**Hành động:**
- Backend Core A focus 100% vào task này Week 1
- Team Leader review code daily
- Target: Complete by Day 3 of Week 1

#### Ưu Tiên 2 (HIGH): Wire ClientConnectionHandler
**Tại sao:**
- Bridge giữa network và business logic
- Nhiều handlers cần wire (6 methods)
- Phụ thuộc vào AuthService

**Hành động:**
- Backend Core A làm sau khi AuthService xong
- Test từng handler riêng biệt
- Target: Complete by Day 5 of Week 1

#### Ưu Tiên 3 (MEDIUM): MatchmakingService
**Tại sao:**
- Core feature cho multiplayer game
- Không phức tạp (FIFO queue)
- Cần có trước khi frontend test

**Hành động:**
- Backend Core B làm Week 2
- Simple implementation trước, optimize sau
- Target: Complete by Day 2 of Week 2

#### Ưu Tiên 4 (MEDIUM): Frontend Integration
**Tại sao:**
- Cần có để test end-to-end
- User-facing, quan trọng cho demo
- Phụ thuộc vào backend API stable

**Hành động:**
- Frontend Developer start Week 1 với protocol client
- UI components Week 2
- Integration Week 3
- Target: Basic UI by end of Week 2

### 8.3. Khuyến Nghị Kỹ Thuật

#### 1. Testing Strategy
**Recommendation:** Test-driven development cho business logic

**Lý do:**
- Catch bugs sớm
- Regression testing khi refactor
- Documentation qua tests

**Action:**
- Mỗi service method phải có unit test
- Mỗi handler phải có integration test
- End-to-end test cho full flow

#### 2. Logging
**Recommendation:** Add SLF4J + Logback ngay từ đầu

**Lý do:**
- Debug production issues
- Track server performance
- Audit trail cho user actions

**Action:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

```java
// In code
private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

logger.info("User {} attempting login", username);
logger.error("Login failed for user {}: {}", username, e.getMessage(), e);
```

#### 3. Password Security
**Recommendation:** Upgrade từ SHA-256 lên BCrypt

**Lý do:**
- SHA-256 là hash algorithm, không phải password hashing
- BCrypt có built-in salt và adaptive cost
- Industry standard cho password storage

**Action:**
```java
// Use Spring Security BCrypt
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

String hashedPassword = passwordEncoder.encode(plainPassword);
boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);
```

#### 4. Connection Limits
**Recommendation:** Replace CachedThreadPool với FixedThreadPool

**Lý do:**
- CachedThreadPool unlimited threads → OOM risk
- FixedThreadPool limits max threads
- Predictable resource usage

**Action:**
```java
// CoreServer.java
ExecutorService executor = Executors.newFixedThreadPool(50); // Max 50 connections
```

#### 5. Error Messages
**Recommendation:** Standardize error messages (Vietnamese)

**Lý do:**
- User-facing errors phải dễ hiểu
- Consistent UX
- Easier support

**Action:**
```java
// ErrorMessages.java
public class ErrorMessages {
    public static final String INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng";
    public static final String USERNAME_EXISTS = "Tên đăng nhập đã tồn tại";
    public static final String MATCH_NOT_FOUND = "Không tìm thấy trận đấu";
    // ...
}
```

### 8.4. Lộ Trình Dài Hạn (Post-MVP)

Sau khi hoàn thành MVP, có thể consider các features sau:

1. **Leaderboard System**
   - Track win/loss ratio
   - Ranking system
   - Statistics dashboard

2. **Replay System**
   - Save game history
   - Replay games
   - Learn from mistakes

3. **Chat System**
   - In-game chat
   - Emojis
   - Friend system

4. **Advanced Matchmaking**
   - Skill-based matchmaking (MMR)
   - Ranked mode
   - Tournament system

5. **Mobile App**
   - React Native
   - Same backend
   - Push notifications

### 8.5. Lời Kết

Dự án của bạn có **foundation rất vững**, đặc biệt là:
- ✅ Protocol design professional (length-prefixed framing)
- ✅ Threading model đúng đắn (I/O + Worker pool)
- ✅ Kiến trúc phân tầng rõ ràng (Core, Gateway, Shared)
- ✅ Game logic hoàn chỉnh (GameService, CardUtils, GameRuleUtils)

**Gap chính** là AuthService và handlers chưa implement, nhưng đây là **implementation work**, không phải architecture problem. Với tài liệu A3_B1_IMPLEMENTATION_GUIDE.md đã có, team có thể follow step-by-step để complete.

**Chiến lược triển khai** phù hợp cho nhóm 4 người:
- Week 1: Backend Core A làm Auth (foundation)
- Week 2: Backend Core B làm Matchmaking, Frontend làm UI
- Week 3: Integration và polish

**Thành công phụ thuộc vào:**
1. ⏰ Time management (stick to timeline)
2. 🔄 Daily communication (standup)
3. 👀 Code review (quality control)
4. 🧪 Testing (catch bugs early)

Chúc team thành công! 💪

---

## Phụ Lục

### A. Tài Liệu Tham Khảo

1. **Implementation Guides:**
   - `A3_B1_IMPLEMENTATION_GUIDE.md` - Full code samples cho AuthService và handlers
   - `CORESERVER_UPDATE_GUIDE.md` - CoreServer configuration guide
   - `DATABASE_SETUP.md` - HikariCP setup guide
   - `GAME_LOGIC_MVP.md` - Game rules specification

2. **External Resources:**
   - Java Socket Programming: https://docs.oracle.com/javase/tutorial/networking/sockets/
   - HikariCP Documentation: https://github.com/brettwooldridge/HikariCP
   - Jackson JSON: https://github.com/FasterXML/jackson
   - WebSocket API: https://developer.mozilla.org/en-US/docs/Web/API/WebSocket

### B. Glossary

- **Length-Prefixed Framing**: Protocol pattern where message length (4 bytes) precedes payload
- **correlationId**: UUID để match request với response
- **sessionId**: UUID để track user session và route push notifications
- **I/O Thread**: Thread chuyên đọc/ghi socket
- **Worker Pool**: ThreadPool xử lý business logic
- **CachedThreadPool**: ThreadPool tự động tạo threads on-demand
- **HikariCP**: Connection pool library cho JDBC
- **MessageEnvelope**: Protocol message wrapper (type, action, payload, etc.)
- **DTO**: Data Transfer Object - POJO cho serialization

### C. Quick Reference

**Ports:**
- Core TCP Server: 9090
- Gateway WebSocket: 8080 (default Spring Boot port)
- MySQL: 3306

**Connection Limits:**
- HikariCP Max: 10 connections
- CachedThreadPool: Unlimited (recommend change to FixedThreadPool)

**Timeouts:**
- Socket Accept: 30s
- Connection Timeout: 30s
- Session Timeout: 30 minutes

**Database:**
- Schema: cardgame_db
- User: root
- Tables: users, user_profiles, cards, games, game_rounds, active_sessions

---

**Document Version:** 1.0  
**Last Updated:** 20/10/2025  
**Author:** Team Leader - Network Programming Project  
**Contact:** [Your Contact Info]

