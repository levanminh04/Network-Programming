# Hướng Dẫn Triển Khai A3 & B1
## ClientConnectionHandler Routing & AuthService

> **Dành cho:** Person 3 - Core Backend A  
> **Thời gian:** Week 1  
> **Mức độ:** MVP - Ưu tiên hoàn thành nhanh

---

## 📋 MỤC LỤC

1. [Tổng Quan](#1-tổng-quan)
2. [A3 - ClientConnectionHandler Routing](#2-a3---clientconnectionhandler-routing)
3. [B1 - AuthService](#3-b1---authservice)
4. [B2 - SessionManager](#4-b2---sessionmanager)
5. [Testing](#5-testing)

---

## 1. TỔNG QUAN

### 1.1. Mục Tiêu

Triển khai **2 hạng mục quan trọng nhất** để hoàn thành luồng authentication:

```
Client → Gateway → Core (TCP Socket)
                     ↓
              ClientConnectionHandler (A3) ← Parse JSON, Route message
                     ↓
              AuthService (B1) ← Register, Login, Hash password
                     ↓
              DatabaseManager ← HikariCP pool
                     ↓
              MySQL (users, active_sessions)
```

### 1.2. Sử Dụng Shared Module (QUAN TRỌNG!)

**❌ KHÔNG tạo lại các class sau:**
- `MessageEnvelope` → Dùng `com.n9.shared.protocol.MessageEnvelope`
- `MessageType` → Dùng `com.n9.shared.protocol.MessageType`
- `MessageFactory` → Dùng `com.n9.shared.protocol.MessageFactory`
- `ErrorCode` → Dùng `com.n9.shared.model.enums.ErrorCode`
- `JsonUtils` → Dùng `com.n9.shared.util.JsonUtils`

**✅ CẦN tạo mới:**
- `AuthService.java` (package: `com.n9.core.service`)
- `SessionManager.java` (package: `com.n9.core.service`)
- Mở rộng `ClientConnectionHandler.java` (đã có sẵn)

### 1.3. Cấu Trúc Package

```
com.n9.core
 ┣ CoreServer.java
 ┣ database
 ┃ ┗ DatabaseManager.java (ĐÃ CÓ)
 ┣ network
 ┃ ┣ ClientConnectionHandler.java (MỞ RỘNG)
 ┃ ┗ CoreServerListener.java (ĐÃ CÓ)
 ┗ service
   ┣ AuthService.java (TẠO MỚI - B1)
   ┣ SessionManager.java (TẠO MỚI - B2)
   ┗ GameService.java (ĐÃ CÓ)
```

---

## 2. A3 - ClientConnectionHandler Routing

### 2.1. Nhiệm Vụ

`ClientConnectionHandler` hiện tại chỉ xử lý `GAME.*` messages. Cần mở rộng để xử lý:

| Domain | Message Types | Handler |
|--------|---------------|---------|
| `AUTH.*` | LOGIN_REQUEST, REGISTER_REQUEST, LOGOUT_REQUEST | AuthService |
| `LOBBY.*` | MATCH_REQUEST, MATCH_CANCEL, LEADERBOARD_REQUEST | MatchmakingService |
| `GAME.*` | CARD_PLAY_REQUEST, ROUND_REVEAL, MATCH_RESULT | GameService |

### 2.2. Bảng Routing (MVP)

```java
// Trong handleMessage()
switch (type) {
    // ========== AUTH DOMAIN ==========
    case MessageType.AUTH_LOGIN_REQUEST:
        return handleLogin(envelope);
    
    case MessageType.AUTH_REGISTER_REQUEST:
        return handleRegister(envelope);
    
    case MessageType.AUTH_LOGOUT_REQUEST:
        return handleLogout(envelope);
    
    // ========== LOBBY DOMAIN ==========
    case MessageType.LOBBY_MATCH_REQUEST:
        return handleMatchRequest(envelope);
    
    case MessageType.LOBBY_MATCH_CANCEL:
        return handleMatchCancel(envelope);
    
    case MessageType.LOBBY_LEADERBOARD_REQUEST:
        return handleLeaderboard(envelope);
    
    // ========== GAME DOMAIN (ĐÃ CÓ) ==========
    case MessageType.GAME_START:
        return handleGameStart(envelope);
    
    case MessageType.GAME_CARD_PLAY_REQUEST:
        return handlePlayCard(envelope);
    
    default:
        return createErrorResponse(correlationId, 
            ErrorCode.VALIDATION_INVALID_MESSAGE_TYPE);
}
```

### 2.3. Implementation - Mở Rộng ClientConnectionHandler

**File:** `d:\Project\Network-Programming\core\src\main\java\com\n9\core\network\ClientConnectionHandler.java`

**Thêm dependencies vào constructor:**

```java
public class ClientConnectionHandler implements Runnable {
    private final Socket socket;
    private final GameService gameService;
    private final AuthService authService;           // THÊM MỚI
    private final SessionManager sessionManager;     // THÊM MỚI
    
    // Connection state
    private BufferedReader reader;
    private BufferedWriter writer;
    
    public ClientConnectionHandler(Socket socket, 
                                   GameService gameService,
                                   AuthService authService,
                                   SessionManager sessionManager) {
        this.socket = socket;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
    }
    
    // ... existing code ...
}
```

**Thêm handler methods:**

```java
/**
 * Handle AUTH.REGISTER_REQUEST
 * 
 * Request: RegisterRequestDto {username, email, password}
 * Response: AUTH.REGISTER_SUCCESS với RegisterResponseDto
 *           hoặc AUTH.REGISTER_FAILURE với ErrorResponseDto
 */
private String handleRegister(MessageEnvelope envelope) {
    try {
        // 1. Parse request payload
        RegisterRequestDto request = JsonUtils.getObjectMapper().convertValue(
            envelope.getPayload(), 
            RegisterRequestDto.class
        );
        
        // 2. Validate input
        if (!request.isValid()) {
            return createErrorResponse(
                envelope.getCorrelationId(),
                ErrorCode.VALIDATION_INVALID_INPUT,
                "Invalid registration data"
            );
        }
        
        // 3. Call AuthService
        RegisterResponseDto response = authService.register(
            request.getUsername(),
            request.getEmail(), 
            request.getPassword(),
            request.getDisplayName()
        );
        
        // 4. Create session
        String sessionId = sessionManager.createSession(
            response.getUserId(),
            response.getUsername()
        );
        
        // 5. Build success response
        MessageEnvelope successEnvelope = MessageFactory.createResponse(
            MessageType.AUTH_REGISTER_SUCCESS,
            envelope.getCorrelationId(),
            response
        );
        successEnvelope.setSessionId(sessionId);
        successEnvelope.setUserId(response.getUserId());
        
        System.out.println("✅ User registered: " + request.getUsername());
        
        return JsonUtils.toJson(successEnvelope);
        
    } catch (IllegalArgumentException e) {
        // Username/email đã tồn tại
        return createErrorResponse(
            envelope.getCorrelationId(),
            ErrorCode.AUTH_USER_ALREADY_EXISTS,
            e.getMessage()
        );
    } catch (Exception e) {
        System.err.println("❌ Registration failed: " + e.getMessage());
        return createErrorResponse(
            envelope.getCorrelationId(),
            ErrorCode.SYSTEM_INTERNAL_ERROR,
            "Registration failed"
        );
    }
}

/**
 * Handle AUTH.LOGIN_REQUEST
 * 
 * Request: LoginRequestDto {username, password}
 * Response: AUTH.LOGIN_SUCCESS với LoginSuccessDto
 *           hoặc AUTH.LOGIN_FAILURE với LoginFailureDto
 */
private String handleLogin(MessageEnvelope envelope) {
    try {
        // 1. Parse request
        LoginRequestDto request = JsonUtils.getObjectMapper().convertValue(
            envelope.getPayload(),
            LoginRequestDto.class
        );
        
        // 2. Validate
        if (!request.isValid()) {
            return createErrorResponse(
                envelope.getCorrelationId(),
                ErrorCode.VALIDATION_INVALID_INPUT,
                "Username and password required"
            );
        }
        
        // 3. Authenticate
        LoginSuccessDto response = authService.login(
            request.getUsername(),
            request.getPassword()
        );
        
        // 4. Create session
        String sessionId = sessionManager.createSession(
            response.getUserId(),
            response.getUsername()
        );
        
        // 5. Build response
        MessageEnvelope successEnvelope = MessageFactory.createResponse(
            MessageType.AUTH_LOGIN_SUCCESS,
            envelope.getCorrelationId(),
            response
        );
        successEnvelope.setSessionId(sessionId);
        successEnvelope.setUserId(response.getUserId());
        
        System.out.println("✅ User logged in: " + request.getUsername());
        
        return JsonUtils.toJson(successEnvelope);
        
    } catch (IllegalArgumentException e) {
        // Invalid credentials
        LoginFailureDto failure = new LoginFailureDto();
        failure.setErrorCode(ErrorCode.AUTH_INVALID_CREDENTIALS.name());
        failure.setErrorMessage("Invalid username or password");
        failure.setTimestamp(System.currentTimeMillis());
        
        MessageEnvelope failureEnvelope = MessageFactory.createResponse(
            MessageType.AUTH_LOGIN_FAILURE,
            envelope.getCorrelationId(),
            failure
        );
        
        return JsonUtils.toJson(failureEnvelope);
    } catch (Exception e) {
        System.err.println("❌ Login failed: " + e.getMessage());
        return createErrorResponse(
            envelope.getCorrelationId(),
            ErrorCode.SYSTEM_INTERNAL_ERROR,
            "Login failed"
        );
    }
}

/**
 * Handle AUTH.LOGOUT_REQUEST
 */
private String handleLogout(MessageEnvelope envelope) {
    try {
        String sessionId = envelope.getSessionId();
        
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        
        MessageEnvelope response = MessageFactory.createResponse(
            MessageType.AUTH_LOGOUT_SUCCESS,
            envelope.getCorrelationId(),
            null
        );
        
        return JsonUtils.toJson(response);
        
    } catch (Exception e) {
        return createErrorResponse(
            envelope.getCorrelationId(),
            ErrorCode.SYSTEM_INTERNAL_ERROR,
            "Logout failed"
        );
    }
}

/**
 * Create error response với ErrorCode enum từ shared
 */
private String createErrorResponse(String correlationId, 
                                   ErrorCode errorCode, 
                                   String customMessage) {
    try {
        ErrorResponseDto errorDto = new ErrorResponseDto();
        errorDto.setErrorCode(errorCode.name());
        errorDto.setErrorMessage(customMessage != null ? customMessage : errorCode.getDefaultMessage());
        errorDto.setTimestamp(System.currentTimeMillis());
        
        MessageEnvelope errorEnvelope = MessageFactory.createResponse(
            MessageType.SYSTEM_ERROR,
            correlationId,
            errorDto
        );
        
        return JsonUtils.toJson(errorEnvelope);
    } catch (Exception e) {
        // Fallback
        return "{\"type\":\"SYSTEM.ERROR\",\"error\":{\"message\":\"" + 
               customMessage + "\"}}";
    }
}
```

### 2.4. Update CoreServerListener

**File:** `d:\Project\Network-Programming\core\src\main\java\com\n9\core\network\CoreServerListener.java`

```java
public class CoreServerListener implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final GameService gameService;
    private final AuthService authService;           // THÊM
    private final SessionManager sessionManager;     // THÊM
    
    public CoreServerListener(ServerSocket serverSocket,
                             ExecutorService executorService,
                             GameService gameService,
                             AuthService authService,
                             SessionManager sessionManager) {
        this.serverSocket = serverSocket;
        this.executorService = executorService;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
    }
    
    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                // Tạo handler với đầy đủ dependencies
                ClientConnectionHandler handler = new ClientConnectionHandler(
                    clientSocket,
                    gameService,
                    authService,
                    sessionManager
                );
                
                executorService.submit(handler);
                
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("❌ Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
}
```

---

## 3. B1 - AuthService

### 3.1. Tổng Quan

`AuthService` chịu trách nhiệm:
- ✅ Register user (INSERT vào `users` table)
- ✅ Login authentication (SELECT + hash password check)
- ✅ Hash password với SHA-256 (MVP - không salt)
- ✅ Tương tác với DatabaseManager (HikariCP pool)

### 3.2. Database Schema

```sql
-- Table: users
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    status ENUM('ACTIVE', 'BANNED', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Table: user_profiles (tự động tạo bởi trigger)
CREATE TABLE user_profiles (
    user_id INT PRIMARY KEY,
    games_played INT DEFAULT 0,
    games_won INT DEFAULT 0,
    games_lost INT DEFAULT 0,
    current_rating INT DEFAULT 1000,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

### 3.3. Implementation - AuthService.java

**File:** `d:\Project\Network-Programming\core\src\main\java\com\n9\core\service\AuthService.java`

```java
package com.n9.core.service;

import com.n9.core.database.DatabaseManager;
import com.n9.shared.model.dto.auth.LoginSuccessDto;
import com.n9.shared.model.dto.auth.RegisterResponseDto;
import com.n9.shared.model.dto.auth.SessionDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

/**
 * AuthService - Xác thực người dùng với MySQL
 * 
 * Chức năng:
 * - Register: Tạo user mới, hash password SHA-256
 * - Login: Verify credentials, update last_login
 * - Password hashing: SHA-256 (MVP - academic project)
 * 
 * Database: MySQL 8.0+ (HikariCP connection pool)
 * 
 * @version 1.0.0 (MVP)
 */
public class AuthService {
    
    private final DatabaseManager dbManager;
    
    public AuthService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Register new user
     * 
     * @param username Username (3-50 chars, alphanumeric)
     * @param email Email address
     * @param password Plain password (6-100 chars)
     * @param displayName Display name (nullable)
     * @return RegisterResponseDto with userId and session info
     * @throws IllegalArgumentException if username/email exists
     * @throws SQLException if database error
     */
    public RegisterResponseDto register(String username, 
                                       String email, 
                                       String password,
                                       String displayName) throws SQLException {
        // 1. Validate input (basic)
        if (username == null || username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be 3-50 characters");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        // 2. Hash password
        String passwordHash = hashPassword(password);
        
        // 3. Set default display name
        String finalDisplayName = (displayName != null && !displayName.trim().isEmpty()) 
            ? displayName 
            : username;
        
        // 4. Insert into database
        String sql = """
            INSERT INTO users (username, email, password_hash, display_name, status, created_at)
            VALUES (?, ?, ?, ?, 'ACTIVE', NOW())
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, username);
            stmt.setString(2, email.toLowerCase());
            stmt.setString(3, passwordHash);
            stmt.setString(4, finalDisplayName);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert user");
            }
            
            // 5. Get generated user_id
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    String userId = String.valueOf(generatedKeys.getInt(1));
                    
                    // 6. Build response DTO
                    RegisterResponseDto response = new RegisterResponseDto();
                    response.setUserId(userId);
                    response.setUsername(username);
                    response.setEmail(email);
                    response.setDisplayName(finalDisplayName);
                    response.setTimestamp(System.currentTimeMillis());
                    
                    System.out.println("✅ User registered: " + username + " (ID: " + userId + ")");
                    
                    return response;
                } else {
                    throw new SQLException("Failed to get generated user_id");
                }
            }
            
        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate username or email
            if (e.getMessage().contains("username")) {
                throw new IllegalArgumentException("Username already exists");
            } else if (e.getMessage().contains("email")) {
                throw new IllegalArgumentException("Email already registered");
            } else {
                throw new IllegalArgumentException("User already exists");
            }
        }
    }
    
    /**
     * Login user
     * 
     * @param username Username
     * @param password Plain password
     * @return LoginSuccessDto with user info
     * @throws IllegalArgumentException if invalid credentials
     * @throws SQLException if database error
     */
    public LoginSuccessDto login(String username, String password) throws SQLException {
        // 1. Hash password để so sánh
        String passwordHash = hashPassword(password);
        
        // 2. Query user
        String sql = """
            SELECT user_id, username, email, display_name, status
            FROM users
            WHERE username = ? AND password_hash = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 3. Check status
                    String status = rs.getString("status");
                    if (!"ACTIVE".equals(status)) {
                        throw new IllegalArgumentException("Account is " + status.toLowerCase());
                    }
                    
                    String userId = String.valueOf(rs.getInt("user_id"));
                    String email = rs.getString("email");
                    String displayName = rs.getString("display_name");
                    
                    // 4. Update last_login
                    updateLastLogin(conn, userId);
                    
                    // 5. Build response
                    LoginSuccessDto response = new LoginSuccessDto();
                    response.setUserId(userId);
                    response.setUsername(username);
                    response.setEmail(email);
                    response.setDisplayName(displayName);
                    response.setTimestamp(System.currentTimeMillis());
                    
                    // Session token sẽ được tạo bởi SessionManager
                    
                    System.out.println("✅ User logged in: " + username + " (ID: " + userId + ")");
                    
                    return response;
                    
                } else {
                    // Invalid credentials
                    throw new IllegalArgumentException("Invalid username or password");
                }
            }
        }
    }
    
    /**
     * Update last_login timestamp
     */
    private void updateLastLogin(Connection conn, String userId) throws SQLException {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(userId));
            stmt.executeUpdate();
        }
    }
    
    /**
     * Hash password với SHA-256 (MVP - không salt)
     * 
     * SECURITY NOTE: Đây là implementation đơn giản cho academic project.
     * Production nên dùng BCrypt hoặc Argon2 với salt.
     * 
     * @param password Plain password
     * @return Base64-encoded hash
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Verify password (for future use)
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        String inputHash = hashPassword(plainPassword);
        return inputHash.equals(hashedPassword);
    }
}
```

---

## 4. B2 - SessionManager

### 4.1. Tổng Quan

`SessionManager` quản lý active sessions (in-memory + DB sync):
- ✅ Tạo session khi login/register
- ✅ Validate sessionId trước mỗi request
- ✅ Sync với `active_sessions` table
- ✅ Thread-safe với `ConcurrentHashMap`

### 4.2. Implementation

**File:** `d:\Project\Network-Programming\core\src\main\java\com\n9\core\service\SessionManager.java`

```java
package com.n9.core.service;

import com.n9.core.database.DatabaseManager;
import com.n9.shared.util.IdUtils;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - Quản lý active sessions
 * 
 * Strategy: Hybrid (In-memory + DB)
 * - In-memory: Fast lookup với ConcurrentHashMap
 * - DB: Persist cho monitoring, không cần reconnect logic
 * 
 * @version 1.0.0 (MVP)
 */
public class SessionManager {
    
    /**
     * Session context (in-memory)
     */
    public static class SessionContext {
        private final String sessionId;
        private final String userId;
        private final String username;
        private String currentMatchId;  // null nếu ở lobby
        private long lastHeartbeat;
        
        public SessionContext(String sessionId, String userId, String username) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.lastHeartbeat = System.currentTimeMillis();
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getCurrentMatchId() { return currentMatchId; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        
        // Setters
        public void setCurrentMatchId(String matchId) { this.currentMatchId = matchId; }
        public void updateHeartbeat() { this.lastHeartbeat = System.currentTimeMillis(); }
    }
    
    private final DatabaseManager dbManager;
    private final ConcurrentHashMap<String, SessionContext> activeSessions;
    
    public SessionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    /**
     * Tạo session mới (sau login/register)
     * 
     * @param userId User ID
     * @param username Username
     * @return sessionId (UUID)
     */
    public String createSession(String userId, String username) {
        // 1. Generate session ID
        String sessionId = IdUtils.generateSessionId();
        
        // 2. Create in-memory context
        SessionContext context = new SessionContext(sessionId, userId, username);
        activeSessions.put(sessionId, context);
        
        // 3. Persist to database
        try {
            insertSessionToDB(sessionId, userId);
        } catch (SQLException e) {
            System.err.println("⚠️ Failed to persist session to DB: " + e.getMessage());
            // Continue anyway (in-memory works)
        }
        
        System.out.println("✅ Session created: " + sessionId + " for user: " + username);
        
        return sessionId;
    }
    
    /**
     * Validate session (check trước mỗi request)
     * 
     * @param sessionId Session ID
     * @return SessionContext hoặc null nếu invalid
     */
    public SessionContext getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        SessionContext context = activeSessions.get(sessionId);
        
        if (context != null) {
            // Update heartbeat
            context.updateHeartbeat();
            
            // Update DB (async - không block)
            updateHeartbeatAsync(sessionId);
        }
        
        return context;
    }
    
    /**
     * Remove session (logout hoặc disconnect)
     */
    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        
        SessionContext context = activeSessions.remove(sessionId);
        
        if (context != null) {
            // Delete from DB
            try {
                deleteSessionFromDB(sessionId);
            } catch (SQLException e) {
                System.err.println("⚠️ Failed to delete session from DB: " + e.getMessage());
            }
            
            System.out.println("🧹 Session removed: " + sessionId);
        }
    }
    
    /**
     * Update match ID (khi join game)
     */
    public void setMatchId(String sessionId, String matchId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context != null) {
            context.setCurrentMatchId(matchId);
            
            // Update DB
            try {
                updateMatchIdInDB(sessionId, matchId);
            } catch (SQLException e) {
                System.err.println("⚠️ Failed to update matchId in DB: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    // ==================== DATABASE OPERATIONS ====================
    
    private void insertSessionToDB(String sessionId, String userId) throws SQLException {
        String sql = """
            INSERT INTO active_sessions (session_id, user_id, status, last_heartbeat, created_at)
            VALUES (?, ?, 'IN_LOBBY', NOW(), NOW())
            ON DUPLICATE KEY UPDATE last_heartbeat = NOW()
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            stmt.setInt(2, Integer.parseInt(userId));
            stmt.executeUpdate();
        }
    }
    
    private void updateHeartbeatAsync(String sessionId) {
        // Async update để không block thread
        new Thread(() -> {
            try {
                String sql = "UPDATE active_sessions SET last_heartbeat = NOW() WHERE session_id = ?";
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, sessionId);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                // Silent fail - không critical
            }
        }).start();
    }
    
    private void updateMatchIdInDB(String sessionId, String matchId) throws SQLException {
        String sql = """
            UPDATE active_sessions 
            SET match_id = ?, status = 'IN_GAME', last_activity = NOW()
            WHERE session_id = ?
            """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, matchId);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        }
    }
    
    private void deleteSessionFromDB(String sessionId) throws SQLException {
        String sql = "DELETE FROM active_sessions WHERE session_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            stmt.executeUpdate();
        }
    }
}
```

---

## 5. TESTING

### 5.1. Test Registration

```bash
# Terminal 1: Start Core Server
cd d:\Project\Network-Programming\core
mvn clean compile
mvn exec:java -Dexec.mainClass="com.n9.core.CoreServer"

# Terminal 2: Test với TestCoreClient
# Gửi message:
{
  "type": "AUTH.REGISTER_REQUEST",
  "correlationId": "req-001",
  "timestamp": 1697462400000,
  "payload": {
    "username": "alice",
    "email": "alice@example.com",
    "password": "password123",
    "displayName": "Alice Wonderland"
  }
}

# Expected response:
{
  "type": "AUTH.REGISTER_SUCCESS",
  "correlationId": "req-001",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "1",
  "payload": {
    "userId": "1",
    "username": "alice",
    "email": "alice@example.com",
    "displayName": "Alice Wonderland"
  }
}
```

### 5.2. Test Login

```json
// Request
{
  "type": "AUTH.LOGIN_REQUEST",
  "correlationId": "req-002",
  "timestamp": 1697462500000,
  "payload": {
    "username": "alice",
    "password": "password123"
  }
}

// Response (Success)
{
  "type": "AUTH.LOGIN_SUCCESS",
  "correlationId": "req-002",
  "sessionId": "660f9500-f39c-52e5-b827-557766551111",
  "userId": "1",
  "payload": {
    "userId": "1",
    "username": "alice",
    "email": "alice@example.com",
    "displayName": "Alice Wonderland"
  }
}
```

### 5.3. Verify Database

```sql
-- Check users table
SELECT * FROM users WHERE username = 'alice';

-- Check active_sessions table
SELECT * FROM active_sessions WHERE user_id = 1;

-- Check user_profiles (tự động tạo bởi trigger)
SELECT * FROM user_profiles WHERE user_id = 1;
```

### 5.4. Checklist

- [ ] Register thành công → user_id tăng AUTO_INCREMENT
- [ ] Register với username trùng → AUTH_USER_ALREADY_EXISTS
- [ ] Login với password sai → AUTH_INVALID_CREDENTIALS
- [ ] Login thành công → sessionId được tạo
- [ ] Session xuất hiện trong `active_sessions` table
- [ ] Logout → session bị xóa khỏi DB

---

## 6. NEXT STEPS

**Week 1 còn lại:**
- [ ] **B3:** Password hashing utility (ĐÃ XONG - trong AuthService)
- [ ] **E1:** ErrorMapper (Đơn giản hóa - dùng ErrorCode enum)
- [ ] **E2:** Error code definitions (Dùng shared module)

**Week 2:**
- [ ] **C1:** MatchmakingService (FIFO queue)
- [ ] Tích hợp với ClientConnectionHandler
- [ ] Test matchmaking flow

---

## 7. TÀI LIỆU THAM KHẢO

- **Shared Module:** `d:\Project\Network-Programming\shared\`
- **Database Schema:** `d:\Project\Network-Programming\core\db\DB_SCRIPT.sql`
- **Game Logic:** `d:\Project\Network-Programming\core\docs\GAME_LOGIC_MVP.md`
- **Message Types:** `com.n9.shared.protocol.MessageType`
- **Error Codes:** `com.n9.shared.model.enums.ErrorCode`

---

**✅ HẾT PHẦN A3 & B1**

Tài liệu này đủ để bạn triển khai hoàn chỉnh routing và authentication cho Core Backend. Nếu gặp vấn đề, hãy kiểm tra:
1. DatabaseManager đã khởi tạo chưa?
2. MySQL đang chạy chưa?
3. Shared module đã compile chưa? (`mvn clean install -pl shared`)
