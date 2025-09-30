# WebSocket Protocol Specification
## 🌐 **FRONTEND ↔ GATEWAY COMMUNICATION**

### **Connection Establishment**
```javascript
// Frontend Connection Setup
const gameSocket = new WebSocket('ws://localhost:8080/ws/game');

// Connection with Authentication
const gameSocket = new WebSocket('ws://localhost:8080/ws/game', [], {
    headers: {
        'Authorization': 'Bearer ' + authToken,
        'User-Agent': 'GameClient/1.0'
    }
});

// Event Handlers
gameSocket.onopen = (event) => {
    console.log('Connected to game server');
    // Send initial authentication
    sendMessage('AUTH.LOGIN_REQUEST', { username, password });
};

gameSocket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    handleGameMessage(message);
};

gameSocket.onerror = (error) => {
    console.error('WebSocket error:', error);
    handleConnectionError(error);
};

gameSocket.onclose = (event) => {
    if (event.wasClean) {
        console.log('Connection closed cleanly');
    } else {
        console.log('Connection died');
        attemptReconnection();
    }
};
```

### **Message Format Standard**
```typescript
// TypeScript Interface for Type Safety
interface GameMessage {
    type: string;                    // Message type (required)
    correlationId: string;           // UUID for request tracking (required)
    userId?: string;                 // User identifier (after auth)
    sessionId?: string;              // Session identifier
    timestamp: number;               // Client timestamp (epoch millis)
    payload?: any;                   // Message-specific data
}

// Example Messages
const loginRequest: GameMessage = {
    type: 'AUTH.LOGIN_REQUEST',
    correlationId: uuidv4(),
    timestamp: Date.now(),
    payload: {
        username: 'player1',
        password: 'hashedPassword',
        clientVersion: '1.0.0'
    }
};

const playCardRequest: GameMessage = {
    type: 'GAME.CARD_PLAY_REQUEST',
    correlationId: uuidv4(),
    userId: 'user-123',
    sessionId: 'session-abc',
    timestamp: Date.now(),
    payload: {
        matchId: 'match-456',
        cardIndex: 5,
        roundNumber: 2
    }
};
```

### **Error Handling Pattern**
```javascript
// Frontend Error Handler
function handleGameMessage(message) {
    if (message.type === 'SYSTEM.ERROR') {
        const error = message.payload;
        switch (error.code) {
            case 'AUTH_FAILED':
                redirectToLogin();
                break;
            case 'GAME_NOT_FOUND':
                showError('Game session not found');
                returnToLobby();
                break;
            case 'INVALID_MOVE':
                showError('Invalid card play');
                enableCardSelection();
                break;
            default:
                showError('An unexpected error occurred');
        }
        return;
    }
    
    // Normal message processing
    routeMessage(message);
}
```

---

## 🔌 **GATEWAY ↔ CORE COMMUNICATION**

### **TCP Protocol Design**
```java
// Protocol: [4-byte length prefix][UTF-8 JSON payload]
public class CoreProtocolHandler {
    private static final int HEADER_SIZE = 4;
    private final Socket coreSocket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    
    public CoreProtocolHandler(Socket socket) throws IOException {
        this.coreSocket = socket;
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }
    
    public void sendMessage(String jsonMessage) throws IOException {
        byte[] messageBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
        
        // Send length prefix (4 bytes, big-endian)
        outputStream.writeInt(messageBytes.length);
        
        // Send JSON payload
        outputStream.write(messageBytes);
        outputStream.flush();
        
        logger.debug("Sent to Core: {} bytes", messageBytes.length);
    }
    
    public String receiveMessage() throws IOException {
        // Read length prefix (4 bytes)
        int messageLength = inputStream.readInt();
        
        if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
            throw new IOException("Invalid message length: " + messageLength);
        }
        
        // Read JSON payload
        byte[] messageBytes = new byte[messageLength];
        inputStream.readFully(messageBytes);
        
        String message = new String(messageBytes, StandardCharsets.UTF_8);
        logger.debug("Received from Core: {} bytes", messageLength);
        
        return message;
    }
}
```

### **Connection Pool Management**
```java
@Component
public class CoreConnectionPool {
    private final BlockingQueue<CoreConnection> availableConnections;
    private final Set<CoreConnection> allConnections;
    private final int maxConnections = 10;
    
    @PostConstruct
    public void initializePool() {
        for (int i = 0; i < maxConnections; i++) {
            try {
                CoreConnection conn = new CoreConnection();
                conn.connect();
                availableConnections.offer(conn);
                allConnections.add(conn);
            } catch (IOException e) {
                logger.error("Failed to create core connection", e);
            }
        }
    }
    
    public CoreConnection borrowConnection() throws InterruptedException {
        return availableConnections.take(); // Blocks if none available
    }
    
    public void returnConnection(CoreConnection conn) {
        if (conn.isHealthy()) {
            availableConnections.offer(conn);
        } else {
            // Replace with new connection
            replaceConnection(conn);
        }
    }
}
```

---

## 🛡️ **SECURITY & VALIDATION**

### **Input Validation**
```java
@Component
public class MessageValidator {
    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64KB
    private static final int MAX_PAYLOAD_SIZE = 32 * 1024; // 32KB
    
    public ValidationResult validateMessage(String rawMessage) {
        // Size check
        if (rawMessage.length() > MAX_MESSAGE_SIZE) {
            return ValidationResult.error("Message too large");
        }
        
        try {
            // JSON parsing
            JsonNode message = objectMapper.readTree(rawMessage);
            
            // Required fields
            if (!message.has("type") || !message.has("correlationId")) {
                return ValidationResult.error("Missing required fields");
            }
            
            // Message type validation
            String type = message.get("type").asText();
            if (!isValidMessageType(type)) {
                return ValidationResult.error("Invalid message type");
            }
            
            // Payload size check
            JsonNode payload = message.get("payload");
            if (payload != null && payload.toString().length() > MAX_PAYLOAD_SIZE) {
                return ValidationResult.error("Payload too large");
            }
            
            return ValidationResult.success();
            
        } catch (JsonProcessingException e) {
            return ValidationResult.error("Invalid JSON format");
        }
    }
    
    private boolean isValidMessageType(String type) {
        return type.matches("^[A-Z_]+\\.[A-Z_]+$") && 
               MessageTypes.isSupported(type);
    }
}
```

### **Rate Limiting**
```java
@Component
public class RateLimitingService {
    private final LoadingCache<String, AtomicInteger> requestCounts;
    
    public RateLimitingService() {
        this.requestCounts = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build(key -> new AtomicInteger(0));
    }
    
    public boolean isAllowed(String clientIp, String messageType) {
        String key = clientIp + ":" + messageType;
        int currentCount = requestCounts.get(key).incrementAndGet();
        
        int limit = getRateLimitForMessageType(messageType);
        return currentCount <= limit;
    }
    
    private int getRateLimitForMessageType(String messageType) {
        switch (messageType) {
            case "AUTH.LOGIN_REQUEST": return 5;      // 5 per minute
            case "GAME.CARD_PLAY_REQUEST": return 60; // 60 per minute  
            case "LOBBY.MATCH_REQUEST": return 10;    // 10 per minute
            default: return 30;                       // 30 per minute default
        }
    }
}
```

---

## 📊 **MONITORING & LOGGING**

### **Structured Logging**
```java
@Component
public class GameLogger {
    private static final Logger logger = LoggerFactory.getLogger(GameLogger.class);
    
    public void logMessage(String direction, String messageType, String userId, 
                          String sessionId, long processingTime) {
        MDC.put("direction", direction);
        MDC.put("messageType", messageType);
        MDC.put("userId", userId);
        MDC.put("sessionId", sessionId);
        MDC.put("processingTime", String.valueOf(processingTime));
        
        logger.info("Message processed: {} {} in {}ms", 
                   direction, messageType, processingTime);
        
        MDC.clear();
    }
    
    public void logError(String operation, Exception error, String context) {
        MDC.put("operation", operation);
        MDC.put("context", context);
        
        logger.error("Operation failed: {}", operation, error);
        
        MDC.clear();
    }
}
```

### **Performance Metrics**
```java
@Component
public class MetricsCollector {
    private final MeterRegistry meterRegistry;
    private final Timer messageProcessingTimer;
    private final Counter connectionCounter;
    private final Gauge activeSessionsGauge;
    
    public MetricsCollector(MeterRegistry meterRegistry, SessionManager sessionManager) {
        this.meterRegistry = meterRegistry;
        
        this.messageProcessingTimer = Timer.builder("gateway.message.processing")
                .description("Time taken to process messages")
                .register(meterRegistry);
                
        this.connectionCounter = Counter.builder("gateway.connections")
                .description("Total WebSocket connections")
                .register(meterRegistry);
                
        this.activeSessionsGauge = Gauge.builder("gateway.sessions.active")
                .description("Currently active game sessions")
                .register(meterRegistry, sessionManager, SessionManager::getActiveSessionCount);
    }
    
    public void recordMessageProcessing(String messageType, Duration duration) {
        messageProcessingTimer.record(duration);
    }
    
    public void incrementConnections() {
        connectionCounter.increment();
    }
}
```