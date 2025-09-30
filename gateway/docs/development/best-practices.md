# Gateway Development Best Practices

## 📋 **CODING STANDARDS**

### **1. Code Structure Guidelines**

```java
/**
 * Class Documentation Template
 * 
 * @author N9 Team
 * @version 1.0
 * @since 2025-01-01
 * 
 * Purpose: Brief description of class responsibility
 * Dependencies: List key dependencies
 * Thread Safety: Specify thread safety guarantees
 */
@Component
@Slf4j  // Use Lombok for logging
public class ExampleService {
    
    // Constants at the top
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final String LOG_PREFIX = "ExampleService";
    
    // Dependencies (final and injected)
    private final SomeDependency dependency;
    private final SomeOtherService service;
    
    // Constructor injection (preferred)
    public ExampleService(SomeDependency dependency, SomeOtherService service) {
        this.dependency = dependency;
        this.service = service;
    }
    
    // Public methods first
    public Result doSomething(String input) {
        log.debug("{}: Processing input: {}", LOG_PREFIX, input);
        
        // Validate inputs early
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        
        try {
            return processInternal(input);
        } catch (Exception e) {
            log.error("{}: Failed to process input: {}", LOG_PREFIX, input, e);
            throw new ProcessingException("Processing failed", e);
        }
    }
    
    // Private methods at the bottom
    private Result processInternal(String input) {
        // Implementation
        return new Result();
    }
}
```

### **2. Error Handling Patterns**

```java
// Custom Exception Hierarchy
public class GatewayException extends RuntimeException {
    private final String errorCode;
    private final Object[] params;
    
    public GatewayException(String errorCode, String message, Object... params) {
        super(message);
        this.errorCode = errorCode;
        this.params = params;
    }
}

public class ValidationException extends GatewayException {
    public ValidationException(String field, String message) {
        super("VALIDATION_ERROR", "Validation failed for field: " + field, field, message);
    }
}

public class CoreConnectionException extends GatewayException {
    public CoreConnectionException(String message, Throwable cause) {
        super("CORE_CONNECTION_ERROR", message, cause);
    }
}

// Global Exception Handler
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }
    
    @ExceptionHandler(CoreConnectionException.class)
    public ResponseEntity<ErrorResponse> handleCoreConnection(CoreConnectionException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(e.getErrorCode(), "Service temporarily unavailable"));
    }
}
```

### **3. Configuration Management**

```yaml
# application.yml - Environment-specific configurations
spring:
  profiles:
    active: ${ENVIRONMENT:dev}

---
spring:
  config:
    activate:
      on-profile: dev
    
gateway:
  core:
    host: localhost
    port: 5000
  security:
    rate-limiting:
      enabled: false
  logging:
    level: DEBUG

---
spring:
  config:
    activate:
      on-profile: prod
      
gateway:
  core:
    host: ${CORE_HOST:core-server}
    port: ${CORE_PORT:5000}
  security:
    rate-limiting:
      enabled: true
      strict-mode: true
  logging:
    level: INFO
```

```java
// Configuration Properties
@ConfigurationProperties(prefix = "gateway")
@Data
@Validated
public class GatewayProperties {
    
    @Valid
    private Core core = new Core();
    
    @Valid
    private Security security = new Security();
    
    @Data
    public static class Core {
        @NotBlank
        private String host = "localhost";
        
        @Min(1)
        @Max(65535)
        private int port = 5000;
        
        @Valid
        private ConnectionPool connectionPool = new ConnectionPool();
    }
    
    @Data
    public static class Security {
        private boolean rateLimitingEnabled = true;
        private boolean strictMode = false;
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }
}
```

---

## 🧪 **TESTING STRATEGY**

### **1. Unit Testing**

```java
@ExtendWith(MockitoExtension.class)
class GameWebSocketHandlerTest {
    
    @Mock
    private CoreBridgeService coreBridgeService;
    
    @Mock
    private SessionManager sessionManager;
    
    @Mock
    private MessageValidator messageValidator;
    
    @Mock
    private WebSocketSession webSocketSession;
    
    @InjectMocks
    private GameWebSocketHandler handler;
    
    @Test
    void shouldHandleValidMessage() throws Exception {
        // Given
        String validMessage = """
            {
                "type": "AUTH.LOGIN_REQUEST",
                "correlationId": "test-123",
                "timestamp": 1640995200000,
                "payload": {
                    "username": "testuser",
                    "password": "password123"
                }
            }
            """;
        
        when(messageValidator.validateMessage(validMessage))
                .thenReturn(ValidationResult.success());
        when(coreBridgeService.forwardMessage(anyString(), eq(validMessage)))
                .thenReturn("success_response");
        when(webSocketSession.getId()).thenReturn("session-123");
        
        // When
        handler.handleMessage(webSocketSession, new TextMessage(validMessage));
        
        // Then
        verify(coreBridgeService).forwardMessage("session-123", validMessage);
        verify(webSocketSession).sendMessage(new TextMessage("success_response"));
    }
    
    @Test
    void shouldRejectInvalidMessage() throws Exception {
        // Given
        String invalidMessage = "invalid json";
        
        when(messageValidator.validateMessage(invalidMessage))
                .thenReturn(ValidationResult.error("INVALID_JSON", "Invalid format"));
        when(webSocketSession.getId()).thenReturn("session-123");
        
        // When
        handler.handleMessage(webSocketSession, new TextMessage(invalidMessage));
        
        // Then
        verify(coreBridgeService, never()).forwardMessage(anyString(), anyString());
        
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(webSocketSession).sendMessage(messageCaptor.capture());
        
        String errorResponse = messageCaptor.getValue().getPayload();
        assertThat(errorResponse).contains("VALIDATION_ERROR");
    }
}
```

### **2. Integration Testing**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "gateway.core.host=localhost",
    "gateway.core.port=15000" // Test port
})
class GatewayIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    private WebSocketStompClient stompClient;
    private MockCoreServer mockCoreServer;
    
    @BeforeEach
    void setUp() throws Exception {
        // Start mock core server
        mockCoreServer = new MockCoreServer(15000);
        mockCoreServer.start();
        
        // Setup WebSocket client
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (mockCoreServer != null) {
            mockCoreServer.stop();
        }
    }
    
    @Test
    void shouldEstablishWebSocketConnection() throws Exception {
        // Given
        String url = "ws://localhost:" + port + "/ws/game";
        WebSocketSession session = null;
        
        try {
            // When
            session = stompClient.doHandshake(new TestWebSocketHandler(), url).get(5, TimeUnit.SECONDS);
            
            // Then
            assertThat(session.isOpen()).isTrue();
            
            // Send test message
            String testMessage = """
                {
                    "type": "SYSTEM.PING",
                    "correlationId": "test-ping",
                    "timestamp": %d
                }
                """.formatted(System.currentTimeMillis());
            
            session.sendMessage(new TextMessage(testMessage));
            
            // Verify mock core server received message
            String receivedMessage = mockCoreServer.getLastReceivedMessage(5000);
            assertThat(receivedMessage).contains("SYSTEM.PING");
            
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
```

### **3. Load Testing**

```java
@Test
@Disabled("Load test - run manually")
void loadTestWebSocketConnections() throws Exception {
    int numberOfConnections = 100;
    int messagesPerConnection = 50;
    CountDownLatch latch = new CountDownLatch(numberOfConnections);
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    // Create multiple concurrent connections
    for (int i = 0; i < numberOfConnections; i++) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                simulateClientConnection(messagesPerConnection);
            } finally {
                latch.countDown();
            }
        });
        futures.add(future);
    }
    
    // Wait for all connections to complete
    boolean completed = latch.await(60, TimeUnit.SECONDS);
    assertThat(completed).isTrue();
    
    // Verify no exceptions occurred
    for (CompletableFuture<Void> future : futures) {
        assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    }
}

private void simulateClientConnection(int messageCount) {
    // Implementation for simulating client behavior
}
```

---

## 📚 **DOCUMENTATION STANDARDS**

### **1. API Documentation**

```java
/**
 * WebSocket Message API Documentation
 * 
 * This service handles real-time communication between game clients and the core server.
 * All messages follow a standard JSON envelope format for consistency.
 * 
 * @apiNote WebSocket Endpoint: /ws/game
 * @apiNote Supported Protocols: WebSocket, SockJS
 * @apiNote Authentication: JWT token required
 * 
 * @example Connection Example:
 * <pre>
 * const socket = new WebSocket('ws://localhost:8080/ws/game?token=jwt_token_here');
 * 
 * socket.onopen = function(event) {
 *     console.log('Connected to game server');
 * };
 * 
 * socket.onmessage = function(event) {
 *     const message = JSON.parse(event.data);
 *     handleGameMessage(message);
 * };
 * </pre>
 * 
 * @see MessageProtocol for complete message type definitions
 * @see GatewaySecurityConfig for authentication requirements
 */
@RestController
@Tag(name = "WebSocket API", description = "Real-time game communication")
public class WebSocketDocumentationController {
    
    /**
     * Authentication Message Example
     * 
     * @apiNote Message Type: AUTH.LOGIN_REQUEST
     * @apiNote Required Fields: username, password
     * @apiNote Response: AUTH.LOGIN_SUCCESS or AUTH.LOGIN_FAILURE
     * 
     * @example Request:
     * <pre>
     * {
     *   "type": "AUTH.LOGIN_REQUEST",
     *   "correlationId": "uuid-here",
     *   "timestamp": 1640995200000,
     *   "payload": {
     *     "username": "player1",
     *     "password": "hashed_password"
     *   }
     * }
     * </pre>
     * 
     * @example Success Response:
     * <pre>
     * {
     *   "type": "AUTH.LOGIN_SUCCESS",
     *   "correlationId": "uuid-here",
     *   "timestamp": 1640995201000,
     *   "payload": {
     *     "userId": "user-123",
     *     "username": "player1",
     *     "token": "jwt_token"
     *   }
     * }
     * </pre>
     */
    public void loginExample() {}
}
```

### **2. Architecture Decision Records (ADRs)**

```markdown
# ADR-001: WebSocket vs HTTP for Real-time Communication

## Status
Accepted

## Context
The card game requires real-time bidirectional communication between clients and server.
We need to decide between HTTP polling, Server-Sent Events (SSE), and WebSocket.

## Decision
We will use WebSocket with SockJS fallback for real-time communication.

## Consequences

### Positive
- True bidirectional communication
- Low latency for game actions
- Efficient bandwidth usage
- Native browser support

### Negative
- More complex connection management
- Need for heartbeat mechanism
- Potential firewall/proxy issues (mitigated by SockJS)

## Implementation Notes
- Use Spring WebSocket framework
- Implement connection pooling for core server
- Add authentication at handshake level
- Include rate limiting and validation
```

---

## 🚀 **DEPLOYMENT CHECKLIST**

### **Pre-Production Checklist**

```markdown
## Code Quality
- [ ] All unit tests passing (>90% coverage)
- [ ] Integration tests passing
- [ ] Load tests completed successfully
- [ ] Security scan completed
- [ ] Code review approved
- [ ] Documentation updated

## Configuration
- [ ] Environment-specific configs validated
- [ ] Secrets properly externalized
- [ ] Connection limits configured
- [ ] Rate limits configured
- [ ] Monitoring enabled

## Infrastructure
- [ ] Load balancer configured
- [ ] Health checks working
- [ ] Logging aggregation setup
- [ ] Metrics collection enabled
- [ ] Alerting rules configured
- [ ] Backup procedures tested

## Security
- [ ] HTTPS/WSS enabled
- [ ] CORS configured correctly
- [ ] Rate limiting active
- [ ] Input validation enabled
- [ ] Authentication working
- [ ] Authorization tested

## Performance
- [ ] Connection pooling optimized
- [ ] Caching configured
- [ ] Memory limits set
- [ ] GC tuning completed
- [ ] Load testing passed
```

### **Production Monitoring**

```yaml
# prometheus-alerts.yml
groups:
  - name: gateway.rules
    rules:
      - alert: HighWebSocketConnections
        expr: gateway_connections_active > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High number of WebSocket connections"
          description: "WebSocket connections: {{ $value }}"
          
      - alert: CoreConnectionDown
        expr: gateway_core_connections_healthy == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "No healthy core connections"
          description: "All core server connections are down"
          
      - alert: HighErrorRate
        expr: rate(gateway_errors_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate: {{ $value | humanizePercentage }}"
```

This completes the comprehensive Gateway module architecture and implementation guide. The documentation provides a solid foundation for enterprise-level development while being accessible to university students.