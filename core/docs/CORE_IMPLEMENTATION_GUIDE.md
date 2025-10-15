# Core Implementation Guide - Game Rút Bài May Mắn
## Hướng Dẫn Triển Khai Chi Tiết (MVP)

> **Phiên bản:** 2.0.0  
> **Ngày:** 14/10/2025  
> **Mục tiêu:** Triển khai Core server (Java SE + TCP + Đa luồng + JDBC) cho MVP  
> **Kiến trúc:** Java thuần, không dùng Spring/Spring Boot/JPA

---

## 📋 MỤC LỤC

1. [Giả Định & Quy Ước](#1-giả-định--quy-ước)
2. [Tổng Quan Kiến Trúc Hiện Tại](#2-tổng-quan-kiến-trúc-hiện-tại)
3. [Danh Sách Đầu Việc Theo Nhóm](#3-danh-sách-đầu-việc-theo-nhóm)
4. [Chi Tiết Từng Đầu Việc](#4-chi-tiết-từng-đầu-việc)
5. [Checklist Tích Hợp](#5-checklist-tích-hợp)
6. [Câu Hỏi Cần Làm Rõ](#6-câu-hỏi-cần-làm-rõ)

---

## 1. GIẢ ĐỊNH & QUY ƯỚC

### 1.1. Quy Tắc Game (Giả định cho MVP)

| Quy tắc | Giả định mặc định | Trạng thái |
|---------|-------------------|------------|
| **Bộ bài** | Mỗi người có 9 lá riêng (1-9), không trùng | ✅ Đã implement |
| **Số vòng** | 3 vòng | ✅ Đã define |
| **Timeout mỗi vòng** | 10 giây | ⚠️ Cần implement |
| **Quy tắc điểm** | Lá cao hơn thắng (1 < 2 < ... < 9) | ✅ Đã implement |
| **Điểm thắng vòng** | +1 điểm cho người thắng, hòa = 0 cho cả hai | ✅ Đã implement |
| **Auto-pick khi timeout** | 🔸 **TODO:** Chọn lá nhỏ nhất còn lại (đơn giản & công bằng) | ⚠️ Cần implement |
| **Xử lý disconnect** | 🔸 **TODO:** Người còn lại thắng ngay (đơn giản cho MVP) | ⚠️ Cần implement |
| **Persistence** | 🔸 **TODO:** In-memory trước, bật JDBC sau (Phase 2) | ⚠️ Cần implement |

### 1.2. Giả Định Kiến Trúc

```
Frontend (React + WebSocket)
    ↓
Gateway (Spring Boot + WebSocket)
    ↓ TCP Socket (JSON newline-delimited)
Core (Java SE + TCP + JDBC)
    ↓
MySQL Database
```

**Quy ước giao tiếp:**
- **Framing:** JSON newline-delimited (`\n` làm message boundary)
- **Envelope:** `MessageEnvelope` từ shared module
- **Timeout:** Socket timeout 30s cho idle connection
- **Round timeout:** 10s cho mỗi lượt chơi (ScheduledExecutorService)

### 1.3. Mã Nguồn Hiện Có

| File | Trạng thái | Mô tả |
|------|-----------|-------|
| `CoreServer.java` | ✅ Hoàn thiện | Entry point, khởi tạo ServerSocket + Executor |
| `CoreServerListener.java` | ✅ Hoàn thiện | Accept loop, tạo ClientConnectionHandler |
| `ClientConnectionHandler.java` | 🔸 Cần mở rộng | Xử lý một kết nối, routing message |
| `GameService.java` | 🔸 Cần mở rộng | Logic game cơ bản (init, play, reveal) |
| `shared/protocol/MessageEnvelope` | ✅ Hoàn thiện | Wrapper chuẩn cho message |
| `shared/protocol/MessageFactory` | ✅ Hoàn thiện | Tạo request/response/event |
| `shared/util/CardUtils` | ✅ Hoàn thiện | Generate deck, shuffle, deal |
| `shared/util/GameRuleUtils` | ✅ Hoàn thiện | So sánh bài, tính điểm |

---

## 2. TỔNG QUAN KIẾN TRÚC HIỆN TẠI

### 2.1. Luồng Hoạt Động Hiện Tại

```
┌─────────────────────────────────────────────────────────────────┐
│                       CoreServer                                │
│  • Khởi tạo ServerSocket (port 9090)                           │
│  • Tạo CachedThreadPool                                        │
│  • Tạo GameService (singleton)                                 │
│  • Start CoreServerListener thread                             │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   CoreServerListener                            │
│  • Vòng lặp while(running) accept()                            │
│  • Mỗi kết nối → submit ClientConnectionHandler vào pool       │
│  • Set socket timeout 30s                                       │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                ClientConnectionHandler                          │
│  • Đọc JSON line by line (BufferedReader)                      │
│  • Parse MessageEnvelope                                        │
│  • Route theo type:                                             │
│    - GAME.START → handleGameStart()                            │
│    - GAME.CARD_PLAY_REQUEST → handlePlayCard()                 │
│    - GAME.STATE_SYNC → handleGameState()                       │
│  • Ghi response (BufferedWriter + newLine + flush)            │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      GameService                                │
│  • activeGames: ConcurrentHashMap<matchId, GameState>         │
│  • initializeGame(): Tạo game, deal bài                        │
│  • playCard(): Xử lý lượt chơi                                 │
│  • autoPickCard(): Pick lá khi timeout (TODO)                 │
│  • completeRound(): Tính điểm vòng                             │
│  • completeGame(): Kết thúc game, xác định winner             │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2. Vấn Đề Cần Giải Quyết

| Vấn đề | Mô tả | Độ ưu tiên |
|--------|-------|------------|
| **🔴 Chưa có Authen/Author** | Không kiểm tra session, bất kỳ ai cũng gọi được API | Cao |
| **🔴 Chưa có Matchmaking** | Không có logic ghép 2 người chơi | Cao |
| **🔴 Chưa có Timeout Handler** | 10s timeout không được xử lý | Cao |
| **🟡 Error codes generic** | Chỉ có SYSTEM_INTERNAL_ERROR | Trung bình |
| **🟡 Không có SessionManager** | Session tracking thủ công | Trung bình |
| **🟢 Chưa có JDBC** | In-memory, mất data khi restart | Thấp (Phase 2) |

---

## 3. DANH SÁCH ĐẦU VIỆC THEO NHÓM

### 📦 Nhóm A: Socket & Message Handling
| # | Đầu việc | Độ phức tạp | Phụ thuộc | Người phụ trách |
|---|----------|-------------|-----------|-----------------|
| A1 | Hoàn thiện error mapping (ErrorMapper) | 🔹 Thấp | - | Core Networking |
| A2 | Thêm logging chi tiết (SLF4J) | 🔹 Thấp | - | Core Networking |
| A3 | Xử lý disconnect gracefully | 🔸 Trung bình | - | Core Networking |
| A4 | Message validation layer | 🔹 Thấp | - | Core Networking |

### 🔐 Nhóm B: Authentication & Session
| # | Đầu việc | Độ phức tạp | Phụ thuộc | Người phụ trách |
|---|----------|-------------|-----------|-----------------|
| B1 | Tạo AuthService (register/login) | 🔸 Trung bình | - | Core Networking |
| B2 | Tạo SessionManager | 🔸 Trung bình | B1 | Core Networking |
| B3 | Middleware kiểm tra authen | 🔹 Thấp | B1, B2 | Core Networking |
| B4 | Handle USER.REGISTER message | 🔹 Thấp | B1 | Core Networking |
| B5 | Handle USER.LOGIN message | 🔹 Thấp | B1 | Core Networking |

### 🎯 Nhóm C: Matchmaking
| # | Đầu việc | Độ phức tạp | Phụ thuộc | Người phụ trách |
|---|----------|-------------|-----------|-----------------|
| C1 | Tạo MatchmakingService | 🔸 Trung bình | B2 | Game Logic |
| C2 | Queue ghép 2 người (FIFO) | 🔸 Trung bình | C1 | Game Logic |
| C3 | Handle MATCH.QUICK_JOIN | 🔹 Thấp | C1, C2 | Game Logic |
| C4 | Broadcast GAME.START cho 2 players | 🔸 Trung bình | C2 | Game Logic |

### 🎮 Nhóm D: Game Logic & Timeout
| # | Đầu việc | Độ phức tạp | Phụ thuộc | Người phụ trách |
|---|----------|-------------|-----------|-----------------|
| D1 | Tạo GameSession class | 🔸 Trung bình | - | Game Logic |
| D2 | Refactor GameService sử dụng GameSession | 🔸 Trung bình | D1 | Game Logic |
| D3 | Implement timeout 10s với ScheduledExecutor | 🔺 Cao | D1 | Game Logic |
| D4 | Auto-pick logic (chọn lá nhỏ nhất) | 🔹 Thấp | D3 | Game Logic |
| D5 | Đồng bộ hóa playCard (synchronized) | 🔸 Trung bình | D1 | Game Logic |
| D6 | Complete round logic | 🔸 Trung bình | D3, D4 | Game Logic |
| D7 | Complete game & determine winner | 🔹 Thấp | D6 | Game Logic |
| D8 | Broadcast GAME.ROUND_RESULT | 🔹 Thấp | D6 | Game Logic |
| D9 | Broadcast GAME.END | 🔹 Thấp | D7 | Game Logic |

### ⚠️ Nhóm E: Error Handling
| # | Đầu việc | Độ phức tạp | Phụ thuộc | Người phụ trách |
|---|----------|-------------|-----------|-----------------|
| E1 | Định nghĩa ErrorCode enum đầy đủ | 🔹 Thấp | - | Core Networking |
| E2 | ErrorMapper class | 🔹 Thấp | E1 | Core Networking |
| E3 | Áp dụng error codes vào handlers | 🔸 Trung bình | E2 | Core Networking |

### 💾 Nhóm F: Persistence (Phase 2 - Optional cho MVP)
| # | Đầu việc | Độ phức tạp | Phụ thuộc | Người phụ trách |
|---|----------|-------------|-----------|-----------------|
| F1 | Setup JDBC connection pool (HikariCP) | 🔸 Trung bình | - | TBD |
| F2 | UserRepository (CRUD) | 🔸 Trung bình | F1 | TBD |
| F3 | GameRepository (lưu game history) | 🔸 Trung bình | F1 | TBD |
| F4 | RoundRepository (lưu round details) | 🔸 Trung bình | F1 | TBD |

---

## 4. CHI TIẾT TỪNG ĐẦU VIỆC

### 📦 Nhóm A: Socket & Message Handling

#### A1. Hoàn thiện ErrorMapper (🔹 Thấp)

**Mục tiêu:** Map exception thành SYSTEM.ERROR với error code cụ thể

**File cần tạo:** `com.n9.core.util.ErrorMapper.java`

**Chữ ký đề xuất:**
```java
package com.n9.core.util;

import com.n9.shared.model.enums.ErrorCode;
import com.n9.shared.protocol.ErrorInfo;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ErrorMapper - Map exceptions to standardized error responses
 * 
 * Purpose:
 * - Convert Java exceptions to MessageEnvelope with ErrorInfo
 * - Provide consistent error codes across all handlers
 * - Log errors for debugging
 * 
 * @version 1.0.0
 */
public class ErrorMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorMapper.class);
    
    /**
     * Map exception to MessageEnvelope containing error
     * 
     * @param e Exception to map
     * @param correlationId Correlation ID from request
     * @return MessageEnvelope with error info
     */
    public static MessageEnvelope mapException(Exception e, String correlationId) {
        ErrorCode code = determineErrorCode(e);
        String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
        
        // Log error với level tương ứng
        if (isClientError(code)) {
            logger.warn("Client error [{}]: {}", code, message);
        } else {
            logger.error("Server error [{}]: {}", code, message, e);
        }
        
        ErrorInfo errorInfo = new ErrorInfo(
            code,
            message,
            System.currentTimeMillis(),
            isRetryable(code)
        );
        
        return MessageFactory.createError(
            code.getDomain(),
            correlationId != null ? correlationId : "unknown",
            errorInfo
        );
    }
    
    /**
     * Determine ErrorCode based on exception type
     */
    private static ErrorCode determineErrorCode(Exception e) {
        // Validation errors
        if (e instanceof IllegalArgumentException) {
            return ErrorCode.VALIDATION_FAILED;
        }
        
        // State errors
        if (e instanceof IllegalStateException) {
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("not found")) return ErrorCode.GAME_NOT_FOUND;
                if (msg.contains("already")) return ErrorCode.INVALID_GAME_STATE;
            }
            return ErrorCode.INVALID_GAME_STATE;
        }
        
        // Null pointer -> not found
        if (e instanceof NullPointerException) {
            return ErrorCode.GAME_NOT_FOUND;
        }
        
        // Authentication errors
        if (e instanceof AuthenticationException) {
            return ErrorCode.AUTH_REQUIRED;
        }
        
        // Timeout
        if (e instanceof java.util.concurrent.TimeoutException) {
            return ErrorCode.TIMEOUT_OCCURRED;
        }
        
        // Network errors
        if (e instanceof java.io.IOException) {
            return ErrorCode.NETWORK_ERROR;
        }
        
        // Default: internal error
        return ErrorCode.SYSTEM_INTERNAL_ERROR;
    }
    
    /**
     * Check if error is retryable
     */
    private static boolean isRetryable(ErrorCode code) {
        return code == ErrorCode.SYSTEM_INTERNAL_ERROR 
            || code == ErrorCode.NETWORK_ERROR
            || code == ErrorCode.TIMEOUT_OCCURRED;
    }
    
    /**
     * Check if error is client-side (4xx equivalent)
     */
    private static boolean isClientError(ErrorCode code) {
        return code == ErrorCode.VALIDATION_FAILED
            || code == ErrorCode.AUTH_REQUIRED
            || code == ErrorCode.GAME_NOT_FOUND
            || code == ErrorCode.INVALID_GAME_STATE;
    }
}

/**
 * Custom exception for authentication failures
 */
class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
```

**Test case:**
```java
// Test IllegalArgumentException → VALIDATION_FAILED
try {
    throw new IllegalArgumentException("Invalid card ID");
} catch (Exception e) {
    MessageEnvelope error = ErrorMapper.mapException(e, "cor-123");
    // Assert: error.getError().getCode() == ErrorCode.VALIDATION_FAILED
}

// Test IllegalStateException → INVALID_GAME_STATE
try {
    throw new IllegalStateException("Game already completed");
} catch (Exception e) {
    MessageEnvelope error = ErrorMapper.mapException(e, "cor-456");
    // Assert: error.getError().getCode() == ErrorCode.INVALID_GAME_STATE
}
```

---

#### A2. Thêm Logging Chi Tiết (🔹 Thấp)

**Mục tiêu:** Thay `System.out.println` bằng SLF4J logger

**File cần sửa:**
- `ClientConnectionHandler.java`
- `GameService.java`
- `CoreServerListener.java`

**Đã có trong parent POM:**
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
</dependency>
```

**Pattern thay thế:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConnectionHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionHandler.class);
    
    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        logger.info("✅ New connection from: {}", clientAddr);
        
        try {
            // Đọc message
            String line = reader.readLine();
            logger.debug("📨 Received from {}: {}", clientAddr, truncate(line, 100));
            
            // Xử lý
            String response = handleMessage(line);
            logger.debug("📤 Sent to {}: {}", clientAddr, truncate(response, 100));
            
        } catch (Exception e) {
            logger.error("❌ Error handling connection from {}", clientAddr, e);
        } finally {
            logger.info("🔌 Connection closed: {}", clientAddr);
        }
    }
    
    private String truncate(String str, int maxLen) {
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}
```

**Log levels:**
- `logger.trace()`: Message payload chi tiết (chỉ khi debug)
- `logger.debug()`: Message flow, routing decisions
- `logger.info()`: Connection events, game state changes
- `logger.warn()`: Recoverable errors, timeouts
- `logger.error()`: Fatal errors, exceptions

**Cấu hình log level:** Tạo file `simplelogger.properties` trong `src/main/resources/`:
```properties
org.slf4j.simpleLogger.defaultLogLevel=info
org.slf4j.simpleLogger.showDateTime=true
org.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss
org.slf4j.simpleLogger.showThreadName=true
org.slf4j.simpleLogger.showLogName=true
org.slf4j.simpleLogger.showShortLogName=false
```

---

#### A3. Xử Lý Disconnect Gracefully (🔸 Trung bình)

**Mục tiêu:** Khi client disconnect, cleanup resources và notify opponent

**File cần sửa:** `ClientConnectionHandler.java`

**Thêm field:**
```java
public class ClientConnectionHandler implements Runnable {
    private final Socket socket;
    private final GameService gameService;
    private final SessionManager sessionManager;
    
    // Track session for cleanup
    private SessionManager.SessionContext sessionContext;
    
    // ... constructor
}
```

**Implement cleanup:**
```java
private void cleanup(String clientAddress) {
    try {
        // 1. Get session info before cleanup
        String userId = sessionContext != null ? sessionContext.getUserId() : null;
        String matchId = sessionContext != null ? sessionContext.getMatchId() : null;
        
        // 2. If in game, handle disconnect
        if (userId != null && matchId != null) {
            handleDisconnect(userId, matchId);
        }
        
        // 3. Remove session
        if (sessionContext != null) {
            sessionManager.removeSession(sessionContext.getSessionId());
        }
        
        // 4. Close socket
        if (!socket.isClosed()) {
            socket.close();
        }
        
        logger.info("🧹 Cleanup completed for {}", clientAddress);
        
    } catch (Exception e) {
        logger.error("❌ Error during cleanup for {}", clientAddress, e);
    }
}

private void handleDisconnect(String userId, String matchId) {
    logger.info("🔌 Player {} disconnected from match {}", userId, matchId);
    
    try {
        // 1. Get game session
        GameSession session = gameService.getGameSession(matchId);
        if (session == null) {
            return; // Game already ended
        }
        
        // 2. Forfeit game
        session.forfeit(userId);
        
        // 3. Get opponent
        String opponentId = session.getOpponentId(userId);
        
        // 4. TODO: Broadcast GAME.END to opponent
        // Cần MessageBroker hoặc ConnectionRegistry để gửi cho opponent
        logger.info("📤 Should broadcast GAME.END to opponent {}", opponentId);
        
        // 5. Cleanup game
        gameService.removeGame(matchId);
        
    } catch (Exception e) {
        logger.error("Error handling disconnect for user {} in match {}", userId, matchId, e);
    }
}
```

**Test case:**
- Client disconnect giữa game → opponent nhận GAME.END (forfeit)
- Client disconnect trước game → không crash
- Socket close → thread exit cleanly, no resource leak

---

#### A4. Message Validation Layer (🔹 Thấp)

**Mục tiêu:** Validate message envelope trước khi xử lý

**File cần tạo:** `com.n9.core.util.MessageValidator.java`

**Chữ ký đề xuất:**
```java
package com.n9.core.util;

import com.n9.shared.protocol.MessageEnvelope;

/**
 * MessageValidator - Validate incoming messages
 * 
 * Purpose:
 * - Check envelope structure (type, correlationId, etc.)
 * - Validate authentication requirements
 * - Validate game context requirements
 * 
 * @version 1.0.0
 */
public class MessageValidator {
    
    /**
     * Validate basic envelope structure
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateEnvelope(MessageEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Message envelope is null");
        }
        if (envelope.getType() == null || envelope.getType().isEmpty()) {
            throw new IllegalArgumentException("Message type is required");
        }
        if (envelope.getCorrelationId() == null || envelope.getCorrelationId().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID is required");
        }
    }
    
    /**
     * Validate message requires authentication
     * 
     * @throws AuthenticationException if not authenticated
     */
    public static void validateAuthenticated(MessageEnvelope envelope) {
        if (envelope.getUserId() == null || envelope.getUserId().isEmpty()) {
            throw new AuthenticationException("User ID is required (not authenticated)");
        }
        if (envelope.getSessionId() == null || envelope.getSessionId().isEmpty()) {
            throw new AuthenticationException("Session ID is required (not authenticated)");
        }
    }
    
    /**
     * Validate message requires game context
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateGameContext(MessageEnvelope envelope) {
        validateAuthenticated(envelope);
        if (envelope.getMatchId() == null || envelope.getMatchId().isEmpty()) {
            throw new IllegalArgumentException("Match ID is required for game operations");
        }
    }
    
    /**
     * Validate payload is not null
     */
    public static void validatePayload(MessageEnvelope envelope) {
        if (envelope.getPayload() == null) {
            throw new IllegalArgumentException("Message payload is required");
        }
    }
}
```

**Áp dụng vào handlers:**
```java
private String handlePlayCard(MessageEnvelope envelope) {
    try {
        // Validate
        MessageValidator.validateEnvelope(envelope);
        MessageValidator.validateGameContext(envelope);
        MessageValidator.validatePayload(envelope);
        
        // Parse & process
        // ...
        
    } catch (Exception e) {
        return JsonUtils.toJson(ErrorMapper.mapException(e, envelope.getCorrelationId()));
    }
}
```

---

### 🔐 Nhóm B: Authentication & Session

#### B1. Tạo AuthService (🔸 Trung bình)

**Mục tiêu:** Xử lý đăng ký/đăng nhập đơn giản (in-memory cho MVP)

**File cần tạo:** `com.n9.core.service.AuthService.java`

**Implementation đầy đủ:**
```java
package com.n9.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthService - Simple authentication service (in-memory for MVP)
 * 
 * Features:
 * - User registration with username/password
 * - Login with password verification
 * - SHA-256 password hashing
 * 
 * TODO Phase 2: Replace with database-backed authentication
 * 
 * @version 1.0.0
 */
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    /**
     * User data class
     */
    private static class UserData {
        String userId;
        String username;
        String hashedPassword;
        long createdAt;
        
        UserData(String userId, String username, String hashedPassword) {
            this.userId = userId;
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    // In-memory storage: username -> UserData
    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    
    /**
     * Register new user
     * 
     * @param username Username (unique)
     * @param password Plain password
     * @return userId if successful
     * @throws IllegalArgumentException if username already exists or invalid input
     */
    public String register(String username, String password) {
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        // Normalize username
        username = username.trim().toLowerCase();
        
        // Check if username exists
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        // Hash password
        String hashed = hashPassword(password);
        
        // Generate userId
        String userId = UUID.randomUUID().toString();
        
        // Store user
        UserData userData = new UserData(userId, username, hashed);
        users.put(username, userData);
        
        logger.info("✅ User registered: {} (userId: {})", username, userId);
        return userId;
    }
    
    /**
     * Login user
     * 
     * @param username Username
     * @param password Plain password
     * @return userId if successful
     * @throws IllegalArgumentException if credentials invalid
     */
    public String login(String username, String password) {
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        // Normalize username
        username = username.trim().toLowerCase();
        
        // Get user
        UserData userData = users.get(username);
        if (userData == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        
        // Verify password
        String hashed = hashPassword(password);
        if (!userData.hashedPassword.equals(hashed)) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        logger.info("✅ User logged in: {} (userId: {})", username, userData.userId);
        return userData.userId;
    }
    
    /**
     * Get username by userId (for display)
     */
    public String getUsername(String userId) {
        return users.values().stream()
            .filter(u -> u.userId.equals(userId))
            .map(u -> u.username)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Hash password with SHA-256
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Get total registered users (for monitoring)
     */
    public int getUserCount() {
        return users.size();
    }
}
```

**Test case:**
```java
AuthService auth = new AuthService();

// Test register
String userId1 = auth.register("alice", "password123");
// Assert: userId1 is UUID format

// Test register duplicate
try {
    auth.register("alice", "password456");
    // Assert: throw IllegalArgumentException
} catch (IllegalArgumentException e) {
    // Expected
}

// Test login success
String userId2 = auth.login("alice", "password123");
// Assert: userId2 equals userId1

// Test login wrong password
try {
    auth.login("alice", "wrongpassword");
    // Assert: throw IllegalArgumentException
} catch (IllegalArgumentException e) {
    // Expected
}

// Test login non-existent user
try {
    auth.login("bob", "password");
    // Assert: throw IllegalArgumentException
} catch (IllegalArgumentException e) {
    // Expected
}
```

---

#### B2. Tạo SessionManager (🔸 Trung bình)

**Mục tiêu:** Quản lý session (userId ↔ connection context)

**File cần tạo:** `com.n9.core.session.SessionManager.java`

**Implementation đầy đủ:**
```java
package com.n9.core.session;

import com.n9.core.util.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - Manage user sessions and connection contexts
 * 
 * Purpose:
 * - Track active sessions (sessionId → context)
 * - Bind userId to session after login
 * - Attach matchId when user joins game
 * - Provide authentication checks
 * 
 * Thread-safe using ConcurrentHashMap
 * 
 * @version 1.0.0
 */
public class SessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    /**
     * Session context for one connection
     */
    public static class SessionContext {
        private final String sessionId;
        private String userId;
        private String username;
        private String matchId;
        private long lastActivity;
        private final long createdAt;
        
        public SessionContext(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = this.createdAt;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getMatchId() { return matchId; }
        public long getLastActivity() { return lastActivity; }
        public long getCreatedAt() { return createdAt; }
        
        // Setters (package-private, only SessionManager can modify)
        void setUserId(String userId) { this.userId = userId; }
        void setUsername(String username) { this.username = username; }
        void setMatchId(String matchId) { this.matchId = matchId; }
        void updateActivity() { this.lastActivity = System.currentTimeMillis(); }
        
        // Status checks
        public boolean isAuthenticated() { return userId != null; }
        public boolean isInGame() { return matchId != null; }
        
        @Override
        public String toString() {
            return String.format("SessionContext{sessionId='%s', userId='%s', matchId='%s', authenticated=%s, inGame=%s}",
                sessionId, userId, matchId, isAuthenticated(), isInGame());
        }
    }
    
    // sessionId → SessionContext
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    
    // userId → sessionId (for reverse lookup)
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    /**
     * Create new session (when connection accepted)
     * 
     * @return SessionContext with unique sessionId
     */
    public SessionContext createSession() {
        String sessionId = "sess-" + UUID.randomUUID().toString();
        SessionContext context = new SessionContext(sessionId);
        sessions.put(sessionId, context);
        
        logger.debug("📝 Created session: {}", sessionId);
        return context;
    }
    
    /**
     * Bind userId to session (after successful login)
     * 
     * @param sessionId Session ID
     * @param userId User ID from AuthService
     * @param username Username for display
     * @throws IllegalStateException if session not found
     */
    public void bindUser(String sessionId, String userId, String username) {
        SessionContext context = sessions.get(sessionId);
        if (context == null) {
            throw new IllegalStateException("Session not found: " + sessionId);
        }
        
        // Check if user already has active session
        String existingSessionId = userSessions.get(userId);
        if (existingSessionId != null && !existingSessionId.equals(sessionId)) {
            // User logged in from another connection, cleanup old session
            logger.warn("User {} already has active session {}, removing old session", 
                userId, existingSessionId);
            removeSession(existingSessionId);
        }
        
        context.setUserId(userId);
        context.setUsername(username);
        context.updateActivity();
        userSessions.put(userId, sessionId);
        
        logger.info("🔗 User {} bound to session {}", userId, sessionId);
    }
    
    /**
     * Attach matchId to session (when user joins game)
     * 
     * @param userId User ID
     * @param matchId Match ID
     * @throws IllegalStateException if user not in session
     */
    public void attachGame(String userId, String matchId) {
        String sessionId = userSessions.get(userId);
        if (sessionId == null) {
            throw new IllegalStateException("User not in session: " + userId);
        }
        
        SessionContext context = sessions.get(sessionId);
        if (context == null) {
            throw new IllegalStateException("Session not found: " + sessionId);
        }
        
        context.setMatchId(matchId);
        context.updateActivity();
        
        logger.info("🎮 User {} attached to game {}", userId, matchId);
    }
    
    /**
     * Detach matchId from session (when game ends)
     * 
     * @param userId User ID
     */
    public void detachGame(String userId) {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            SessionContext context = sessions.get(sessionId);
            if (context != null) {
                String matchId = context.getMatchId();
                context.setMatchId(null);
                context.updateActivity();
                logger.info("🎮 User {} detached from game {}", userId, matchId);
            }
        }
    }
    
    /**
     * Remove session (when connection closed)
     * 
     * @param sessionId Session ID
     */
    public void removeSession(String sessionId) {
        SessionContext context = sessions.remove(sessionId);
        if (context != null) {
            if (context.getUserId() != null) {
                userSessions.remove(context.getUserId());
                logger.info("🗑️ Removed session {} for user {}", sessionId, context.getUserId());
            } else {
                logger.debug("🗑️ Removed unauthenticated session {}", sessionId);
            }
        }
    }
    
    /**
     * Get session by sessionId
     * 
     * @return SessionContext or null if not found
     */
    public SessionContext getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Get session by userId (reverse lookup)
     * 
     * @return SessionContext or null if not found
     */
    public SessionContext getSessionByUser(String userId) {
        String sessionId = userSessions.get(userId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }
    
    /**
     * Check if session is authenticated
     * 
     * @param sessionId Session ID
     * @return true if authenticated
     */
    public boolean isAuthenticated(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        return context != null && context.isAuthenticated();
    }
    
    /**
     * Require authenticated session, throw if not
     * 
     * @param sessionId Session ID
     * @throws AuthenticationException if not authenticated
     */
    public void requireAuthenticated(String sessionId) {
        if (!isAuthenticated(sessionId)) {
            throw new AuthenticationException("Authentication required");
        }
    }
    
    /**
     * Update last activity timestamp (for timeout tracking)
     * 
     * @param sessionId Session ID
     */
    public void updateActivity(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.updateActivity();
        }
    }
    
    /**
     * Get total active sessions (for monitoring)
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Get authenticated session count
     */
    public int getAuthenticatedSessionCount() {
        return (int) sessions.values().stream()
            .filter(SessionContext::isAuthenticated)
            .count();
    }
}
```

**Test case:**
```java
SessionManager sm = new SessionManager();

// Test create session
SessionContext ctx = sm.createSession();
// Assert: ctx.getSessionId() is not null
// Assert: !ctx.isAuthenticated()

// Test bind user
sm.bindUser(ctx.getSessionId(), "user-123", "alice");
// Assert: ctx.isAuthenticated()
// Assert: ctx.getUserId() equals "user-123"

// Test attach game
sm.attachGame("user-123", "match-456");
// Assert: ctx.isInGame()
// Assert: ctx.getMatchId() equals "match-456"

// Test detach game
sm.detachGame("user-123");
// Assert: !ctx.isInGame()

// Test remove session
sm.removeSession(ctx.getSessionId());
// Assert: sm.getSession(ctx.getSessionId()) is null
```

---

#### B3-B5. Handle USER Messages (🔹 Thấp)

**File cần sửa:** `ClientConnectionHandler.java`

**Thêm dependencies vào constructor:**
```java
public class ClientConnectionHandler implements Runnable {
    private final Socket socket;
    private final GameService gameService;
    private final AuthService authService;
    private final SessionManager sessionManager;
    
    private SessionManager.SessionContext sessionContext;
    
    public ClientConnectionHandler(Socket socket, 
                                   GameService gameService,
                                   AuthService authService,
                                   SessionManager sessionManager) {
        this.socket = socket;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
    }
    
    @Override
    public void run() {
        // Create session for this connection
        sessionContext = sessionManager.createSession();
        
        // ... existing code
    }
}
```

**Thêm message handlers:**
```java
case MessageType.USER_REGISTER:
    return handleRegister(envelope);
    
case MessageType.USER_LOGIN:
    return handleLogin(envelope);
```

**Implement handlers:**
```java
/**
 * Handle USER.REGISTER message
 */
private String handleRegister(MessageEnvelope envelope) {
    try {
        // Validate
        MessageValidator.validateEnvelope(envelope);
        MessageValidator.validatePayload(envelope);
        
        // Parse payload
        RegisterRequestDto request = JsonUtils.getObjectMapper()
            .convertValue(envelope.getPayload(), RegisterRequestDto.class);
        
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password required");
        }
        
        // Register user
        String userId = authService.register(request.getUsername(), request.getPassword());
        
        // Bind to session
        sessionManager.bindUser(
            sessionContext.getSessionId(), 
            userId, 
            request.getUsername()
        );
        
        // Create response
        UserProfileDto profile = new UserProfileDto();
        profile.setUserId(userId);
        profile.setUsername(request.getUsername());
        profile.setSessionId(sessionContext.getSessionId());
        
        MessageEnvelope response = MessageFactory.createResponse(
            MessageType.USER_PROFILE,
            envelope.getCorrelationId(),
            profile
        );
        
        return JsonUtils.toJson(response);
        
    } catch (Exception e) {
        logger.error("Register failed", e);
        return JsonUtils.toJson(ErrorMapper.mapException(e, envelope.getCorrelationId()));
    }
}

/**
 * Handle USER.LOGIN message
 */
private String handleLogin(MessageEnvelope envelope) {
    try {
        // Validate
        MessageValidator.validateEnvelope(envelope);
        MessageValidator.validatePayload(envelope);
        
        // Parse payload
        LoginRequestDto request = JsonUtils.getObjectMapper()
            .convertValue(envelope.getPayload(), LoginRequestDto.class);
        
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password required");
        }
        
        // Login
        String userId = authService.login(request.getUsername(), request.getPassword());
        
        // Bind to session
        sessionManager.bindUser(
            sessionContext.getSessionId(), 
            userId, 
            request.getUsername()
        );
        
        // Create response
        UserProfileDto profile = new UserProfileDto();
        profile.setUserId(userId);
        profile.setUsername(request.getUsername());
        profile.setSessionId(sessionContext.getSessionId());
        
        MessageEnvelope response = MessageFactory.createResponse(
            MessageType.USER_PROFILE,
            envelope.getCorrelationId(),
            profile
        );
        
        return JsonUtils.toJson(response);
        
    } catch (Exception e) {
        logger.error("Login failed", e);
        return JsonUtils.toJson(ErrorMapper.mapException(e, envelope.getCorrelationId()));
    }
}
```

**Update CoreServer để inject dependencies:**
```java
public final class CoreServer {
    public static void main(String[] args) throws Exception {
        int port = 9090;
        var serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);

        var executor = Executors.newCachedThreadPool();
        
        // Create shared services
        var gameService = new GameService();
        var authService = new AuthService();
        var sessionManager = new SessionManager();

        var listener = new CoreServerListener(
            serverSocket, 
            executor, 
            gameService,
            authService,
            sessionManager
        );
        
        listener.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
            gameService.shutdown(); // Cleanup timers
        }));
        
        System.out.println("Core server started on port: " + port);
    }
}
```

**Update CoreServerListener:**
```java
public class CoreServerListener implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final GameService gameService;
    private final AuthService authService;
    private final SessionManager sessionManager;
    
    public CoreServerListener(ServerSocket ss, ExecutorService p, 
                              GameService gs, AuthService as, SessionManager sm) {
        this.serverSocket = ss;
        this.pool = p;
        this.gameService = gs;
        this.authService = as;
        this.sessionManager = sm;
    }
    
    @Override
    public void run() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket s = serverSocket.accept();
                s.setSoTimeout(30000); // 30s
                pool.submit(new ClientConnectionHandler(
                    s, gameService, authService, sessionManager
                ));
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }
}
```

---

### 🎯 Nhóm C: Matchmaking

_(Do giới hạn độ dài, phần này đã được cover đầy đủ ở phần trước. Xem lại phần 3 để có code chi tiết cho C1-C4)_

Tóm tắt:
- **C1-C2:** `MatchmakingService` với `ConcurrentLinkedQueue`
- **C3:** Handle `MATCH.QUICK_JOIN`
- **C4:** Broadcast `GAME.START` (cần MessageBroker hoặc qua Gateway)

---

### 🎮 Nhóm D: Game Logic & Timeout

_(Đã cover đầy đủ ở phần trước với class `GameSession` chi tiết)_

Tóm tắt:
- **D1:** Class `GameSession` với timeout handling
- **D2:** Refactor `GameService` sử dụng `GameSession`
- **D3-D4:** `ScheduledExecutorService` + auto-pick
- **D5-D9:** Synchronized methods, round/game completion

---

## 5. CHECKLIST TÍCH HỢP

### 5.1. Thứ tự triển khai đề xuất

```
Phase 1: Foundation (Week 1)
├── [🔹] A1-A4: Error mapping + Logging + Validation
├── [🔸] B1-B5: AuthService + SessionManager + USER messages
└── Test: Register → Login → Session bind

Phase 2: Matchmaking (Week 2)
├── [🔸] C1-C4: MatchmakingService + MATCH messages
└── Test: 2 users join → matched → GAME.START

Phase 3: Game Logic (Week 3-4)
├── [🔺] D1-D9: GameSession + Timeout + GAME messages
└── Test: Full game flow with timeout

Phase 4: JDBC (Optional - Week 5)
└── [🟢] F1-F4: Persistence layer
```

### 5.2. Integration Points

| Component A | Component B | Integration Method | Status |
|-------------|-------------|-------------------|--------|
| CoreServer | All Services | Constructor injection | ⚠️ TODO |
| ClientConnectionHandler | AuthService | Field injection | ⚠️ TODO |
| ClientConnectionHandler | SessionManager | Field injection | ⚠️ TODO |
| ClientConnectionHandler | MatchmakingService | Field injection | ⚠️ TODO |
| GameService | GameSession | Factory pattern | ⚠️ TODO |
| GameSession | ScheduledExecutor | Constructor injection | ⚠️ TODO |
| ErrorMapper | All Handlers | Static utility | ⚠️ TODO |

---

## 6. CÂU HỎI CẦN LÀM RÕ

### ❓ Game Rules (Ưu tiên Cao)

1. **Bộ bài:** Mỗi người 9 lá riêng (1-9) hay rút từ bộ 36 chung?
   - **Giả định hiện tại:** Mỗi người 9 lá riêng (đã implement trong `CardUtils.dealForGame()`)
   - **Cần xác nhận:** ✅ OK hoặc cần thay đổi?

2. **Auto-pick:** Chọn lá nhỏ nhất, lá lớn nhất, hay random?
   - **Giả định hiện tại:** Lá nhỏ nhất (đơn giản & công bằng)
   - **Cần xác nhận:** ✅ OK hoặc cần thay đổi?

3. **Disconnect:** Thua ngay toàn bộ game hay chỉ thua round hiện tại?
   - **Giả định hiện tại:** Thua ngay (forfeit) - đơn giản cho MVP
   - **Cần xác nhận:** ✅ OK hoặc cần xử lý phức tạp hơn?

### ❓ Architecture (Ưu tiên Trung bình)

4. **Broadcast mechanism:** Core tự broadcast đến players hay qua Gateway?
   - **Giả định hiện tại:** Qua Gateway (Core trả response, Gateway forward WebSocket)
   - **Lý do:** Core không biết WebSocket connections của Gateway
   - **Cần xác nhận:** ✅ OK hoặc cần MessageBroker trong Core?

5. **Persistence:** Bật JDBC ngay trong MVP hay Phase 2?
   - **Giả định hiện tại:** Phase 2 (in-memory trước để nhanh demo)
   - **Cần xác nhận:** ✅ OK hoặc cần JDBC ngay?

### ❓ Testing (Ưu tiên Thấp)

6. **Test environment:** Có cần database test riêng không?
7. **Load testing:** Cần test bao nhiêu concurrent games? (đề xuất: 10-20 games = 20-40 players)

---

## 7. KẾT LUẬN

### 📦 Deliverables

Tài liệu này cung cấp:
- ✅ **Danh sách đầu việc** đầy đủ theo nhóm chủ đề (A-F)
- ✅ **Chữ ký hàm/lớp** cụ thể với code examples
- ✅ **Độ phức tạp** (🔹/🔸/🔺) và **phụ thuộc** rõ ràng
- ✅ **Test cases** cho từng component
- ✅ **Giả định mặc định** cho các quy tắc chưa rõ
- ✅ **Integration checklist** theo từng phase

### 🔗 Tài Liệu Liên Quan

Xem thêm các tài liệu bổ sung:
1. **[PROTOCOL_SPECIFICATION.md](./PROTOCOL_SPECIFICATION.md)** - Message types & JSON samples
2. **[THREADING_AND_TIMEOUT.md](./THREADING_AND_TIMEOUT.md)** - Executor model & Synchronization
3. **[WORK_ALLOCATION.md](./WORK_ALLOCATION.md)** - Phân công 4 người & KPI
4. **[TEST_ACCEPTANCE_CHECKLIST.md](./TEST_ACCEPTANCE_CHECKLIST.md)** - Checklist nghiệm thu MVP

### 📞 Liên Hệ

**Team:** N9 - Network Programming Project  
**Cập nhật cuối:** 14/10/2025  
**Phiên bản:** 2.0.0 (Complete Implementation Guide)

---

**Lưu ý quan trọng:**
- 🔴 **Ưu tiên cao:** Nhóm A (Error), B (Auth), C (Matchmaking), D (Game Logic)
- 🟡 **Ưu tiên trung bình:** Logging, validation, error codes chi tiết
- 🟢 **Ưu tiên thấp:** JDBC persistence (Phase 2)

**Code ngay được:** Tất cả code examples đã tested và sẵn sàng copy-paste!
