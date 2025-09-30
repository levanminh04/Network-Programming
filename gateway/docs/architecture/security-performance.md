# Gateway Security & Performance Guide

## 🛡️ **SECURITY ARCHITECTURE**

### **1. Multi-Layer Security Model**

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend Client                      │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTPS/WSS + CORS
                      ▼
┌─────────────────────────────────────────────────────────┐
│              Load Balancer / Proxy                      │
│              (Rate Limiting)                           │  
└─────────────────────┬───────────────────────────────────┘
                      │ Internal Network
                      ▼
┌─────────────────────────────────────────────────────────┐
│                Gateway Service                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐      │
│  │ Auth Filter │ │ Rate Limiter│ │ Validator   │      │
│  └─────────────┘ └─────────────┘ └─────────────┘      │
└─────────────────────┬───────────────────────────────────┘
                      │ TCP (Internal)
                      ▼
┌─────────────────────────────────────────────────────────┐
│                 Core Game Server                        │
└─────────────────────────────────────────────────────────┘
```

### **2. Authentication & Authorization**

#### **JWT Token-Based Authentication**
```java
@Service
public class AuthenticationService {
    private static final String JWT_SECRET = "${jwt.secret}";
    private static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000; // 24 hours
    
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
```

#### **WebSocket Authentication Interceptor**
```java
@Component
public class AuthenticationInterceptor implements HandshakeInterceptor {
    private final AuthenticationService authService;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                  ServerHttpResponse response,
                                  WebSocketHandler wsHandler, 
                                  Map<String, Object> attributes) throws Exception {
        
        // Extract token from query parameter or header
        String token = extractToken(request);
        
        if (token == null || !authService.validateToken(token)) {
            logger.warn("Authentication failed for WebSocket connection");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        
        // Store user info in WebSocket session attributes
        String username = authService.getUsernameFromToken(token);
        attributes.put("username", username);
        attributes.put("token", token);
        
        return true;
    }
    
    private String extractToken(ServerHttpRequest request) {
        // Try query parameter first
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        
        if (token != null) return token;
        
        // Try Authorization header
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        
        return null;
    }
}
```

### **3. Rate Limiting Implementation**

```java
@Service
public class RateLimitingService {
    private final RedisTemplate<String, String> redisTemplate;
    
    // Rate limits per message type (requests per minute)
    private static final Map<String, Integer> RATE_LIMITS = Map.of(
        "AUTH.LOGIN_REQUEST", 5,
        "AUTH.REGISTER_REQUEST", 3,
        "GAME.CARD_PLAY_REQUEST", 60,
        "LOBBY.MATCH_REQUEST", 10,
        "SYSTEM.HEARTBEAT", 120
    );
    
    public boolean isAllowed(String clientId, String messageType) {
        String key = "rate_limit:" + clientId + ":" + messageType;
        Integer limit = RATE_LIMITS.getOrDefault(messageType, 30);
        
        try {
            // Use Redis for distributed rate limiting
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            if (currentCount >= limit) {
                logger.warn("Rate limit exceeded for {}: {} (limit: {})", 
                           clientId, currentCount, limit);
                return false;
            }
            
            // Increment counter with 1-minute expiry
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofMinutes(1));
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit", e);
            // Fail open - allow request if Redis is down
            return true;
        }
    }
}
```

### **4. Input Validation & Sanitization**

```java
@Component
public class MessageValidator {
    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64KB
    private static final int MAX_STRING_LENGTH = 1000;
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_\\.@]+$");
    
    public ValidationResult validateMessage(String rawMessage) {
        // Size validation
        if (rawMessage.length() > MAX_MESSAGE_SIZE) {
            return ValidationResult.error("MESSAGE_TOO_LARGE", "Message exceeds size limit");
        }
        
        try {
            JsonNode message = objectMapper.readTree(rawMessage);
            
            // Structure validation
            if (!hasRequiredFields(message)) {
                return ValidationResult.error("MISSING_FIELDS", "Required fields missing");
            }
            
            // Content validation
            return validateMessageContent(message);
            
        } catch (JsonProcessingException e) {
            return ValidationResult.error("INVALID_JSON", "Invalid JSON format");
        }
    }
    
    private ValidationResult validateMessageContent(JsonNode message) {
        String type = message.get("type").asText();
        
        // Validate message type
        if (!isValidMessageType(type)) {
            return ValidationResult.error("INVALID_TYPE", "Unknown message type");
        }
        
        // Validate payload based on message type
        JsonNode payload = message.get("payload");
        if (payload != null) {
            return validatePayload(type, payload);
        }
        
        return ValidationResult.success();
    }
    
    private ValidationResult validatePayload(String messageType, JsonNode payload) {
        switch (messageType) {
            case "AUTH.LOGIN_REQUEST":
                return validateLoginPayload(payload);
            case "GAME.CARD_PLAY_REQUEST":
                return validateCardPlayPayload(payload);
            case "LOBBY.MATCH_REQUEST":
                return validateMatchRequestPayload(payload);
            default:
                return ValidationResult.success();
        }
    }
    
    private ValidationResult validateLoginPayload(JsonNode payload) {
        String username = payload.path("username").asText();
        String password = payload.path("password").asText();
        
        if (username.length() < 3 || username.length() > 50) {
            return ValidationResult.error("INVALID_USERNAME", "Username must be 3-50 characters");
        }
        
        if (!SAFE_STRING_PATTERN.matcher(username).matches()) {
            return ValidationResult.error("INVALID_USERNAME", "Username contains invalid characters");
        }
        
        if (password.length() < 6 || password.length() > 100) {
            return ValidationResult.error("INVALID_PASSWORD", "Password must be 6-100 characters");
        }
        
        return ValidationResult.success();
    }
}
```

---

## ⚡ **PERFORMANCE OPTIMIZATION**

### **1. Connection Pool Management**

```java
@Service
public class CoreConnectionPool {
    private final BlockingQueue<CoreConnection> availableConnections;
    private final Set<CoreConnection> allConnections;
    private final ScheduledExecutorService healthChecker;
    
    @Value("${core.connection-pool.max-connections:10}")
    private int maxConnections;
    
    @Value("${core.connection-pool.connection-timeout:5000}")
    private int connectionTimeout;
    
    @PostConstruct
    public void initialize() {
        this.availableConnections = new LinkedBlockingQueue<>();
        this.allConnections = ConcurrentHashMap.newKeySet();
        this.healthChecker = Executors.newSingleThreadScheduledExecutor();
        
        // Create initial connections
        for (int i = 0; i < maxConnections; i++) {
            createConnection();
        }
        
        // Start health checking
        healthChecker.scheduleAtFixedRate(this::checkConnectionHealth, 
                                         30, 30, TimeUnit.SECONDS);
    }
    
    public CoreConnection borrowConnection() throws InterruptedException, IOException {
        CoreConnection connection = availableConnections.poll(connectionTimeout, TimeUnit.MILLISECONDS);
        
        if (connection == null) {
            throw new IOException("No available connections to core server");
        }
        
        if (!connection.isHealthy()) {
            // Replace unhealthy connection
            replaceConnection(connection);
            return borrowConnection(); // Retry
        }
        
        return connection;
    }
    
    public void returnConnection(CoreConnection connection) {
        if (connection.isHealthy()) {
            availableConnections.offer(connection);
        } else {
            replaceConnection(connection);
        }
    }
    
    private void createConnection() {
        try {
            CoreConnection conn = new CoreConnection(coreHost, corePort);
            conn.connect();
            availableConnections.offer(conn);
            allConnections.add(conn);
            logger.info("Created new core connection: {}", conn.getId());
        } catch (IOException e) {
            logger.error("Failed to create core connection", e);
        }
    }
    
    private void replaceConnection(CoreConnection oldConnection) {
        allConnections.remove(oldConnection);
        oldConnection.close();
        createConnection();
    }
    
    private void checkConnectionHealth() {
        allConnections.parallelStream().forEach(conn -> {
            if (!conn.isHealthy()) {
                logger.warn("Detected unhealthy connection: {}", conn.getId());
                replaceConnection(conn);
            }
        });
    }
}
```

### **2. Asynchronous Message Processing**

```java
@Service
public class MessageProcessingService {
    private final ExecutorService messageProcessor = Executors.newFixedThreadPool(20);
    private final BlockingQueue<MessageTask> messageQueue = new LinkedBlockingQueue<>(1000);
    
    @PostConstruct
    public void startProcessing() {
        // Start message processing threads
        for (int i = 0; i < 10; i++) {
            messageProcessor.submit(this::processMessages);
        }
    }
    
    public CompletableFuture<String> processMessageAsync(String sessionId, String message) {
        CompletableFuture<String> future = new CompletableFuture<>();
        MessageTask task = new MessageTask(sessionId, message, future);
        
        if (!messageQueue.offer(task)) {
            future.completeExceptionally(new RuntimeException("Message queue full"));
        }
        
        return future;
    }
    
    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                MessageTask task = messageQueue.take();
                
                try {
                    String response = coreBridgeService.forwardMessage(
                        task.getSessionId(), task.getMessage());
                    task.getFuture().complete(response);
                } catch (Exception e) {
                    task.getFuture().completeExceptionally(e);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

### **3. Caching Strategy**

```java
@Service
public class CachingService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> localCache;
    
    public CachingService() {
        // Local cache for frequently accessed data
        this.localCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }
    
    public Object get(String key) {
        // Try local cache first
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        
        // Try Redis cache
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            localCache.put(key, value);
        }
        
        return value;
    }
    
    public void put(String key, Object value, Duration ttl) {
        // Store in both caches
        localCache.put(key, value);
        redisTemplate.opsForValue().set(key, value, ttl);
    }
    
    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
    }
}
```

---

## 📊 **MONITORING & ALERTING**

### **1. Custom Metrics**

```java
@Component
public class GatewayMetrics {
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter connectionCounter;
    private final Counter messageCounter;
    private final Counter errorCounter;
    
    // Timers
    private final Timer messageProcessingTimer;
    private final Timer coreResponseTimer;
    
    // Gauges
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);
    
    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.connectionCounter = Counter.builder("gateway.connections.total")
                .description("Total WebSocket connections")
                .register(meterRegistry);
                
        this.messageCounter = Counter.builder("gateway.messages.total")
                .tag("direction", "inbound")
                .description("Total messages processed")
                .register(meterRegistry);
                
        this.errorCounter = Counter.builder("gateway.errors.total")
                .description("Total errors")
                .register(meterRegistry);
                
        this.messageProcessingTimer = Timer.builder("gateway.message.processing.duration")
                .description("Message processing time")
                .register(meterRegistry);
                
        // Register gauges
        Gauge.builder("gateway.connections.active")
                .description("Currently active connections")
                .register(meterRegistry, activeConnections, AtomicInteger::get);
                
        Gauge.builder("gateway.queue.size")
                .description("Message queue size")
                .register(meterRegistry, queueSize, AtomicInteger::get);
    }
    
    public void recordConnection() {
        connectionCounter.increment();
        activeConnections.incrementAndGet();
    }
    
    public void recordDisconnection() {
        activeConnections.decrementAndGet();
    }
    
    public void recordMessage(String messageType) {
        messageCounter.increment(Tags.of("type", messageType));
    }
    
    public void recordError(String errorType) {
        errorCounter.increment(Tags.of("type", errorType));
    }
    
    public Timer.Sample startMessageProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void endMessageProcessing(Timer.Sample sample, String messageType) {
        sample.stop(Timer.builder("gateway.message.processing.duration")
                .tag("type", messageType)
                .register(meterRegistry));
    }
}
```

### **2. Health Checks**

```java
@Component
public class GatewayHealthIndicator implements HealthIndicator {
    private final CoreConnectionPool connectionPool;
    private final SessionManager sessionManager;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        // Check core connections
        if (!checkCoreConnections()) {
            healthBuilder.down().withDetail("core", "No healthy connections");
        }
        
        // Check Redis
        if (!checkRedis()) {
            healthBuilder.down().withDetail("redis", "Connection failed");
        }
        
        // Add metrics
        healthBuilder
                .withDetail("active_sessions", sessionManager.getActiveSessionCount())
                .withDetail("available_connections", connectionPool.getAvailableCount())
                .withDetail("uptime", getUptime());
        
        return healthBuilder.build();
    }
    
    private boolean checkCoreConnections() {
        return connectionPool.getHealthyConnectionCount() > 0;
    }
    
    private boolean checkRedis() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}