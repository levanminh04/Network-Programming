# Gateway Module Development Roadmap
## 🎯 **GIAI ĐOẠN 1: FOUNDATION SETUP (Tuần 1-2)**

### **Phase 1.1: Core Infrastructure (3-4 ngày)**
```bash
Priority: CRITICAL | Timeline: Week 1
Team Assignment: 1 Backend Developer + 1 DevOps
```

#### **Tasks:**
1. **WebSocket Configuration**
   ```java
   // Target: gateway/src/main/java/com/n9/gateway/config/WebSocketConfig.java
   @Configuration
   @EnableWebSocket
   public class WebSocketConfig implements WebSocketConfigurer {
       @Override
       public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
           registry.addHandler(new GameWebSocketHandler(), "/ws/game")
                   .setAllowedOrigins("http://localhost:3000", "http://localhost:5173")
                   .withSockJS();
       }
   }
   ```

2. **Core TCP Bridge Setup**
   ```java
   // Target: gateway/src/main/java/com/n9/gateway/service/CoreBridgeService.java
   @Service
   public class CoreBridgeService {
       private Socket coreSocket;
       private final String CORE_HOST = "localhost";
       private final int CORE_PORT = 5000;
       
       public void connectToCore() throws IOException {
           coreSocket = new Socket(CORE_HOST, CORE_PORT);
       }
   }
   ```

#### **Deliverables:**
- [ ] WebSocket endpoint `/ws/game` functional
- [ ] TCP connection to Core module established
- [ ] Basic message forwarding implemented
- [ ] Health check endpoint active

---

### **Phase 1.2: Message Protocol Implementation (3-4 ngày)**
```bash
Priority: HIGH | Timeline: Week 1-2
Team Assignment: 1 Backend Developer + 1 Protocol Designer
```

#### **Message Flow Architecture:**
```
Frontend → Gateway → Core → Gateway → Frontend
   WS         TCP        TCP         WS
```

#### **Protocol Design:**
```java
// Message Envelope Structure
{
    "type": "GAME.ACTION_TYPE",           // Required: Message type
    "correlationId": "uuid-string",       // Required: Request tracking
    "userId": "user-123",                 // Optional: After authentication
    "sessionId": "session-abc",           // Optional: WebSocket session
    "timestamp": 1736412000000,           // Required: Client timestamp
    "payload": { ... }                    // Optional: Message data
}
```

#### **Core Message Types:**
```java
// Authentication Messages
AUTH.LOGIN_REQUEST          // Client login
AUTH.LOGIN_SUCCESS          // Login successful
AUTH.LOGIN_FAILURE          // Login failed
AUTH.LOGOUT_REQUEST         // Client logout

// Lobby Messages  
LOBBY.JOIN_REQUEST          // Join lobby
LOBBY.PLAYER_LIST_UPDATE    // Update player list
LOBBY.MATCH_REQUEST         // Request match

// Game Messages
GAME.MATCH_FOUND           // Match found
GAME.MATCH_START           // Game starts
GAME.ROUND_START           // Round begins
GAME.CARD_PLAY_REQUEST     // Play card
GAME.CARD_PLAY_RESPONSE    // Card played
GAME.ROUND_RESULT          // Round result
GAME.GAME_END              // Game ends

// System Messages
SYSTEM.ERROR               // Error occurred
SYSTEM.HEARTBEAT          // Keep alive
SYSTEM.DISCONNECT         // Connection lost
```

---

## 🎯 **GIAI ĐOẠN 2: CORE FUNCTIONALITY (Tuần 3-4)**

### **Phase 2.1: Session Management (5 ngày)**
```bash
Priority: CRITICAL | Timeline: Week 3
Team Assignment: 1 Senior Backend Developer
```

#### **Session Architecture:**
```java
@Component
public class SessionManager {
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> userToSession = new ConcurrentHashMap<>();
    
    public class GameSession {
        private String sessionId;
        private String userId;
        private WebSocketSession wsSession;
        private Socket coreSocket;
        private GameState gameState;
        private Instant lastActivity;
    }
}
```

#### **Key Features:**
- **Session Lifecycle**: Create, maintain, cleanup
- **Heartbeat Mechanism**: Detect disconnections
- **Session Recovery**: Reconnection handling
- **Concurrent Access**: Thread-safe operations

### **Phase 2.2: Core Communication Bridge (5 ngày)**
```bash
Priority: CRITICAL | Timeline: Week 3-4  
Team Assignment: 1 Network Programming Specialist
```

#### **TCP Protocol Implementation:**
```java
// Message Format: [4-byte length][JSON payload]
public class CoreProtocolHandler {
    public void sendToCore(String message) {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + messageBytes.length);
        buffer.putInt(messageBytes.length);  // Length prefix
        buffer.put(messageBytes);           // JSON payload
        
        coreSocket.getOutputStream().write(buffer.array());
    }
    
    public String receiveFromCore() {
        byte[] lengthBytes = new byte[4];
        coreSocket.getInputStream().read(lengthBytes);
        int messageLength = ByteBuffer.wrap(lengthBytes).getInt();
        
        byte[] messageBytes = new byte[messageLength];
        coreSocket.getInputStream().read(messageBytes);
        return new String(messageBytes, StandardCharsets.UTF_8);
    }
}
```

---

## 🎯 **GIAI ĐOẠN 3: ADVANCED FEATURES (Tuần 5-6)**

### **Phase 3.1: Security & Validation (4 ngày)**
```bash
Priority: HIGH | Timeline: Week 5
Team Assignment: 1 Security-focused Developer
```

#### **Security Layers:**
```java
@Component
public class SecurityService {
    // 1. Origin Validation
    public boolean validateOrigin(String origin) {
        return allowedOrigins.contains(origin);
    }
    
    // 2. Rate Limiting
    @RateLimiter(name = "websocket", fallbackMethod = "rateLimitFallback")
    public void processMessage(String userId, String message) { }
    
    // 3. Input Sanitization
    public String sanitizeInput(String input) {
        return Jsoup.clean(input, Whitelist.none());
    }
    
    // 4. Authentication Token
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }
}
```

### **Phase 3.2: Performance Optimization (4 ngày)**
```bash
Priority: MEDIUM | Timeline: Week 5-6
Team Assignment: 1 Performance Engineer
```

#### **Optimization Targets:**
- **Connection Pooling**: Core TCP connections
- **Message Queuing**: Asynchronous processing  
- **Memory Management**: Session cleanup
- **Monitoring**: Performance metrics

---

## 🎯 **GIAI ĐOẠN 4: TESTING & DEPLOYMENT (Tuần 7-8)**

### **Phase 4.1: Comprehensive Testing (5 ngày)**
```bash
Priority: HIGH | Timeline: Week 7
Team Assignment: 1 QA Engineer + 1 Developer
```

#### **Testing Strategy:**
```java
// Unit Tests
@Test
public void testMessageRouting() { }

// Integration Tests  
@Test
public void testWebSocketToCoreFlow() { }

// Load Tests
@Test
public void test100ConcurrentConnections() { }

// Security Tests
@Test
public void testUnauthorizedAccess() { }
```

### **Phase 4.2: Production Deployment (3 ngày)**
```bash
Priority: CRITICAL | Timeline: Week 8
Team Assignment: 1 DevOps Engineer
```

#### **Deployment Checklist:**
- [ ] Environment configuration
- [ ] Load balancer setup
- [ ] Monitoring & logging
- [ ] Rollback procedures

---

## 📊 **TEAM ASSIGNMENT MATRIX**

| Role | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|---------|---------|---------|---------|
| **Team Lead** | Architecture Design | Code Review | Security Review | Deployment |
| **Backend Dev 1** | WebSocket Setup | Session Mgmt | Performance | Testing |
| **Backend Dev 2** | TCP Bridge | Core Comm | Validation | Integration |
| **DevOps** | Infrastructure | Monitoring | Security | Deployment |

## 🎯 **SUCCESS CRITERIA**

### **Week 2 Milestone:**
- [ ] WebSocket connections stable
- [ ] Basic message forwarding works
- [ ] Core TCP communication established

### **Week 4 Milestone:**
- [ ] Complete message protocol implemented
- [ ] Session management functional
- [ ] Error handling robust

### **Week 6 Milestone:**
- [ ] Security features implemented
- [ ] Performance optimized
- [ ] Load testing passed

### **Week 8 Milestone:**
- [ ] Production ready
- [ ] Documentation complete
- [ ] Team handover done