# Core Module Security & Best Practices Guide

## 🔐 **ENTERPRISE SECURITY FRAMEWORK**

Security implementation cho Core module theo industry best practices với comprehensive protection layers cho university project standards.

---

## 🛡️ **MULTI-LAYER SECURITY ARCHITECTURE**

### **Security Layers Overview**
```
┌─────────────────────────────────────────────────┐
│              Application Layer                   │
│  • Input Validation • Business Logic Security   │
├─────────────────────────────────────────────────┤
│              Transport Layer                     │
│  • Message Authentication • Protocol Security   │
├─────────────────────────────────────────────────┤
│              Session Layer                       │
│  • User Authentication • Session Management     │
├─────────────────────────────────────────────────┤
│              Data Layer                          │
│  • Database Security • Encryption at Rest       │
└─────────────────────────────────────────────────┘
```

---

## 🔒 **1. INPUT VALIDATION & SANITIZATION**

### **Comprehensive Validation Framework**

```java
@Component
public class InputValidationService {
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    /**
     * Comprehensive user input validation
     */
    public ValidationResult validateUserRegistration(UserRegistrationDTO dto) {
        ValidationResult result = new ValidationResult();
        
        // Username validation
        if (!USERNAME_PATTERN.matcher(dto.getUsername()).matches()) {
            result.addError("username", "Username must be 3-20 alphanumeric characters");
        }
        
        // Email validation
        if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            result.addError("email", "Invalid email format");
        }
        
        // Password strength validation
        if (!isPasswordStrong(dto.getPassword())) {
            result.addError("password", "Password must meet security requirements");
        }
        
        // XSS prevention
        dto.setUsername(sanitizeInput(dto.getUsername()));
        dto.setEmail(sanitizeInput(dto.getEmail()));
        
        return result;
    }
    
    /**
     * Game input validation
     */
    public ValidationResult validateGameMove(GameMoveDTO move) {
        ValidationResult result = new ValidationResult();
        
        // Player ID validation
        if (!isValidUUID(move.getPlayerId())) {
            result.addError("playerId", "Invalid player ID format");
        }
        
        // Card index bounds check
        if (move.getCardIndex() < 0 || move.getCardIndex() > 51) {
            result.addError("cardIndex", "Card index out of bounds");
        }
        
        // Round number validation
        if (move.getRoundNumber() < 1 || move.getRoundNumber() > 13) {
            result.addError("roundNumber", "Invalid round number");
        }
        
        return result;
    }
    
    private boolean isPasswordStrong(String password) {
        return password.length() >= 8 &&
               password.matches(".*[a-z].*") &&  // lowercase
               password.matches(".*[A-Z].*") &&  // uppercase
               password.matches(".*[0-9].*") &&  // digit
               password.matches(".*[!@#$%^&*].*"); // special char
    }
    
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'&]", "")  // Basic XSS prevention
                   .trim()
                   .substring(0, Math.min(input.length(), 255)); // Length limit
    }
    
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

@Data
@Builder
public class ValidationResult {
    private Map<String, String> errors = new HashMap<>();
    private boolean valid = true;
    
    public void addError(String field, String message) {
        errors.put(field, message);
        valid = false;
    }
}
```

### **Custom Validation Annotations**

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GameModeValidator.class)
public @interface ValidGameMode {
    String message() default "Invalid game mode";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class GameModeValidator implements ConstraintValidator<ValidGameMode, String> {
    private static final Set<String> VALID_MODES = Set.of("QUICK", "RANKED", "TOURNAMENT");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && VALID_MODES.contains(value.toUpperCase());
    }
}

// Usage trong DTOs
@Data
public class CreateGameDTO {
    @NotBlank(message = "Player ID required")
    @Size(min = 36, max = 36, message = "Invalid player ID format")
    private String playerId;
    
    @ValidGameMode
    private String gameMode;
    
    @Min(value = 1, message = "Bet amount must be positive")
    @Max(value = 1000, message = "Bet amount too high")
    private Integer betAmount;
}
```

---

## 🔐 **2. AUTHENTICATION & AUTHORIZATION**

### **JWT-Based Authentication Service**

```java
@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Authenticate user và generate JWT token
     */
    public AuthenticationResponse authenticate(LoginRequest request) {
        // Rate limiting check
        checkRateLimit(request.getUsername());
        
        // Validate user credentials
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordFailedAttempt(request.getUsername());
            throw new AuthenticationException("Invalid credentials");
        }
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new AuthenticationException("Account is locked");
        }
        
        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        // Store refresh token
        storeRefreshToken(user.getId(), refreshToken);
        
        // Clear failed attempts
        clearFailedAttempts(request.getUsername());
        
        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
            .build();
    }
    
    /**
     * Rate limiting implementation
     */
    private void checkRateLimit(String username) {
        String key = "auth_attempts:" + username;
        String attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts != null && Integer.parseInt(attempts) >= 5) {
            throw new RateLimitExceededException("Too many authentication attempts");
        }
    }
    
    private void recordFailedAttempt(String username) {
        String key = "auth_attempts:" + username;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(15));
    }
    
    private void storeRefreshToken(String userId, String refreshToken) {
        String key = "refresh_token:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofDays(7));
    }
}

@Component
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.access-token-expiration}")
    private int accessTokenExpiration;
    
    @Value("${app.jwt.refresh-token-expiration}")
    private int refreshTokenExpiration;
    
    public String generateAccessToken(User user) {
        Date expiryDate = new Date(System.currentTimeMillis() + accessTokenExpiration);
        
        return Jwts.builder()
            .setSubject(user.getId())
            .setIssuedAt(new Date())
            .setExpiration(expiryDate)
            .claim("username", user.getUsername())
            .claim("role", user.getRole())
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
}
```

### **Method-Level Security**

```java
@RestController
@RequestMapping("/api/core")
@PreAuthorize("hasRole('USER')")
public class GameController {
    
    @PostMapping("/games")
    @PreAuthorize("hasPermission(#request.playerId, 'USER', 'CREATE_GAME')")
    public ResponseEntity<GameResponse> createGame(@RequestBody CreateGameRequest request) {
        // Game creation logic
    }
    
    @PostMapping("/games/{gameId}/moves")
    @PreAuthorize("hasPermission(#gameId, 'GAME', 'MAKE_MOVE') and #request.playerId == authentication.principal.id")
    public ResponseEntity<MoveResponse> makeMove(
            @PathVariable String gameId,
            @RequestBody MoveRequest request) {
        // Move processing logic
    }
}

@Component
public class GamePermissionEvaluator implements PermissionEvaluator {
    
    private final GameService gameService;
    
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || !(permission instanceof String)) {
            return false;
        }
        
        String permissionString = (String) permission;
        return hasPrivilege(auth, targetDomainObject.toString(), permissionString);
    }
    
    private boolean hasPrivilege(Authentication auth, String targetId, String permission) {
        // Custom permission logic
        switch (permission) {
            case "CREATE_GAME":
                return canCreateGame(auth.getName());
            case "MAKE_MOVE":
                return canMakeMove(auth.getName(), targetId);
            default:
                return false;
        }
    }
}
```

---

## 🔐 **3. SESSION MANAGEMENT & PROTECTION**

### **Secure Session Manager**

```java
@Service
public class SecureSessionManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredSessions() {
        Set<String> expiredSessions = activeSessions.entrySet().stream()
            .filter(entry -> entry.getValue().isExpired())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        expiredSessions.forEach(this::invalidateSession);
    }
    
    public SessionInfo createSession(String userId, String connectionId) {
        // Check for existing sessions
        invalidateExistingUserSessions(userId);
        
        SessionInfo session = SessionInfo.builder()
            .sessionId(generateSecureSessionId())
            .userId(userId)
            .connectionId(connectionId)
            .createdAt(Instant.now())
            .lastActivity(Instant.now())
            .ipAddress(getCurrentClientIP())
            .userAgent(getCurrentUserAgent())
            .build();
        
        activeSessions.put(session.getSessionId(), session);
        storeSessionInRedis(session);
        
        return session;
    }
    
    public void updateSessionActivity(String sessionId) {
        SessionInfo session = activeSessions.get(sessionId);
        if (session != null) {
            session.setLastActivity(Instant.now());
            storeSessionInRedis(session);
        }
    }
    
    public boolean validateSession(String sessionId, String userId) {
        SessionInfo session = activeSessions.get(sessionId);
        if (session == null) {
            session = getSessionFromRedis(sessionId);
        }
        
        return session != null && 
               session.getUserId().equals(userId) &&
               !session.isExpired() &&
               validateSessionIntegrity(session);
    }
    
    private boolean validateSessionIntegrity(SessionInfo session) {
        // Check for session hijacking indicators
        String currentIP = getCurrentClientIP();
        String currentUserAgent = getCurrentUserAgent();
        
        // Allow IP changes but log suspicious activity
        if (!session.getIpAddress().equals(currentIP)) {
            log.warn("IP address changed for session {}: {} -> {}", 
                session.getSessionId(), session.getIpAddress(), currentIP);
        }
        
        // Strict user agent validation
        if (!session.getUserAgent().equals(currentUserAgent)) {
            log.error("User agent mismatch for session {}", session.getSessionId());
            return false;
        }
        
        return true;
    }
    
    private String generateSecureSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

@Data
@Builder
public class SessionInfo {
    private String sessionId;
    private String userId;
    private String connectionId;
    private Instant createdAt;
    private Instant lastActivity;
    private String ipAddress;
    private String userAgent;
    
    public boolean isExpired() {
        return lastActivity.isBefore(Instant.now().minus(Duration.ofHours(24)));
    }
}
```

### **Connection Security Filter**

```java
@Component
public class ConnectionSecurityFilter {
    
    private final SecureSessionManager sessionManager;
    private final RateLimitService rateLimitService;
    
    public boolean validateConnection(String connectionId, MessageEnvelope message) {
        // Rate limiting per connection
        if (!rateLimitService.isAllowed(connectionId)) {
            log.warn("Rate limit exceeded for connection: {}", connectionId);
            return false;
        }
        
        // Message size validation
        if (message.getPayload().length() > MAX_MESSAGE_SIZE) {
            log.warn("Message size exceeded for connection: {}", connectionId);
            return false;
        }
        
        // Session validation
        if (message.getSessionId() != null) {
            return sessionManager.validateSession(message.getSessionId(), message.getUserId());
        }
        
        return true;
    }
}
```

---

## 🔐 **4. DATABASE SECURITY**

### **Secure Repository Layer**

```java
@Repository
public class SecureUserRepository {
    
    private final EntityManager entityManager;
    private final AuditService auditService;
    
    /**
     * Secure user query với parameter binding
     */
    public Optional<User> findByUsernameSecure(String username) {
        // Use parameterized query to prevent SQL injection
        Query query = entityManager.createQuery(
            "SELECT u FROM User u WHERE u.username = :username AND u.deleted = false");
        query.setParameter("username", username);
        
        try {
            User user = (User) query.getSingleResult();
            auditService.logDataAccess("USER_READ", user.getId());
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Secure password update với audit trail
     */
    @Transactional
    public void updatePasswordSecure(String userId, String newPasswordHash) {
        // Validate user exists
        User user = findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Store old password hash for audit
        String oldPasswordHash = user.getPasswordHash();
        
        // Update password
        Query updateQuery = entityManager.createQuery(
            "UPDATE User u SET u.passwordHash = :newHash, u.passwordUpdatedAt = :now WHERE u.id = :userId");
        updateQuery.setParameter("newHash", newPasswordHash);
        updateQuery.setParameter("now", Instant.now());
        updateQuery.setParameter("userId", userId);
        
        int updated = updateQuery.executeUpdate();
        
        if (updated == 1) {
            auditService.logPasswordChange(userId, oldPasswordHash, newPasswordHash);
        } else {
            throw new SecurityException("Password update failed");
        }
    }
    
    /**
     * Secure data deletion với soft delete
     */
    @Transactional
    public void deleteUserSecure(String userId, String reason) {
        Query softDeleteQuery = entityManager.createQuery(
            "UPDATE User u SET u.deleted = true, u.deletedAt = :now, u.deletionReason = :reason WHERE u.id = :userId");
        softDeleteQuery.setParameter("now", Instant.now());
        softDeleteQuery.setParameter("reason", reason);
        softDeleteQuery.setParameter("userId", userId);
        
        int updated = softDeleteQuery.executeUpdate();
        
        if (updated == 1) {
            auditService.logUserDeletion(userId, reason);
        }
    }
}
```

### **Data Encryption Service**

```java
@Service
public class DataEncryptionService {
    
    @Value("${app.encryption.key}")
    private String encryptionKey;
    
    private final AESUtil aesUtil;
    
    /**
     * Encrypt sensitive data before storage
     */
    public String encryptSensitiveData(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            return aesUtil.encrypt(plainText, encryptionKey);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new SecurityException("Data encryption failed");
        }
    }
    
    /**
     * Decrypt sensitive data after retrieval
     */
    public String decryptSensitiveData(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            return aesUtil.decrypt(encryptedText, encryptionKey);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new SecurityException("Data decryption failed");
        }
    }
}

@Component
public class AESUtil {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    public String encrypt(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        byte[] encryptedText = cipher.doFinal(plainText.getBytes());
        
        // Combine IV và encrypted data
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedText.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedText);
        
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }
    
    public String decrypt(String encryptedText, String key) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedText);
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        
        byte[] encrypted = new byte[byteBuffer.remaining()];
        byteBuffer.get(encrypted);
        
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        byte[] plainText = cipher.doFinal(encrypted);
        return new String(plainText);
    }
}
```

---

## 🔐 **5. COMPREHENSIVE AUDIT SYSTEM**

### **Security Audit Service**

```java
@Service
public class SecurityAuditService {
    
    private final AuditEventRepository auditRepository;
    private final AsyncAuditProcessor asyncProcessor;
    
    /**
     * Log authentication events
     */
    public void logAuthenticationEvent(String eventType, String userId, String details) {
        AuditEvent event = AuditEvent.builder()
            .eventType("AUTH_" + eventType)
            .userId(userId)
            .timestamp(Instant.now())
            .details(details)
            .ipAddress(getCurrentClientIP())
            .userAgent(getCurrentUserAgent())
            .severity(determineSeverity(eventType))
            .build();
        
        asyncProcessor.processAuditEvent(event);
    }
    
    /**
     * Log data access events
     */
    public void logDataAccess(String operation, String resourceId) {
        AuditEvent event = AuditEvent.builder()
            .eventType("DATA_ACCESS")
            .operation(operation)
            .resourceId(resourceId)
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .ipAddress(getCurrentClientIP())
            .severity(AuditSeverity.INFO)
            .build();
        
        asyncProcessor.processAuditEvent(event);
    }
    
    /**
     * Log security violations
     */
    public void logSecurityViolation(String violationType, String details) {
        AuditEvent event = AuditEvent.builder()
            .eventType("SECURITY_VIOLATION")
            .violationType(violationType)
            .details(details)
            .timestamp(Instant.now())
            .ipAddress(getCurrentClientIP())
            .userAgent(getCurrentUserAgent())
            .severity(AuditSeverity.CRITICAL)
            .build();
        
        asyncProcessor.processAuditEvent(event);
        
        // Immediate notification for critical events
        if (isCriticalViolation(violationType)) {
            notificationService.sendSecurityAlert(event);
        }
    }
    
    /**
     * Generate security reports
     */
    public SecurityReport generateSecurityReport(Instant from, Instant to) {
        List<AuditEvent> events = auditRepository.findByTimestampBetween(from, to);
        
        return SecurityReport.builder()
            .reportPeriod(from + " to " + to)
            .totalEvents(events.size())
            .authenticationAttempts(countEventsByType(events, "AUTH_"))
            .failedLogins(countEventsByType(events, "AUTH_FAILED"))
            .securityViolations(countEventsByType(events, "SECURITY_VIOLATION"))
            .dataAccessEvents(countEventsByType(events, "DATA_ACCESS"))
            .criticalEvents(events.stream()
                .filter(e -> e.getSeverity() == AuditSeverity.CRITICAL)
                .collect(Collectors.toList()))
            .build();
    }
}

@Entity
@Table(name = "audit_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String eventType;
    
    private String operation;
    private String resourceId;
    private String userId;
    private String violationType;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(length = 1000)
    private String details;
    
    private String ipAddress;
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    private AuditSeverity severity;
    
    @Column(length = 4000)
    private String stackTrace;
}
```

---

## 🔐 **6. RATE LIMITING & DDOS PROTECTION**

### **Advanced Rate Limiting Service**

```java
@Service
public class RateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, RateLimitConfig> rateLimitConfigs;
    
    @PostConstruct
    public void initializeRateLimits() {
        rateLimitConfigs.put("AUTH", new RateLimitConfig(5, Duration.ofMinutes(15)));
        rateLimitConfigs.put("GAME_MOVE", new RateLimitConfig(30, Duration.ofMinutes(1)));
        rateLimitConfigs.put("MESSAGE", new RateLimitConfig(100, Duration.ofMinutes(1)));
        rateLimitConfigs.put("CONNECTION", new RateLimitConfig(10, Duration.ofMinutes(5)));
    }
    
    /**
     * Check if action is allowed under rate limit
     */
    public boolean isAllowed(String identifier, String actionType) {
        RateLimitConfig config = rateLimitConfigs.get(actionType);
        if (config == null) {
            return true; // No limit configured
        }
        
        String key = buildKey(identifier, actionType);
        return checkTokenBucket(key, config);
    }
    
    /**
     * Token bucket algorithm implementation
     */
    private boolean checkTokenBucket(String key, RateLimitConfig config) {
        Long currentTime = System.currentTimeMillis();
        
        // Get current bucket state
        String bucketData = redisTemplate.opsForValue().get(key);
        TokenBucket bucket = parseBucketData(bucketData, config);
        
        // Refill tokens based on time elapsed
        long timeElapsed = currentTime - bucket.getLastRefill();
        long tokensToAdd = (timeElapsed * config.getMaxRequests()) / 
                          config.getTimeWindow().toMillis();
        
        bucket.setTokens(Math.min(config.getMaxRequests(), 
                                 bucket.getTokens() + tokensToAdd));
        bucket.setLastRefill(currentTime);
        
        // Check if request can be processed
        if (bucket.getTokens() > 0) {
            bucket.setTokens(bucket.getTokens() - 1);
            saveBucketData(key, bucket, config.getTimeWindow());
            return true;
        } else {
            saveBucketData(key, bucket, config.getTimeWindow());
            return false;
        }
    }
    
    /**
     * Progressive penalty for repeated violations
     */
    public void recordViolation(String identifier, String actionType) {
        String violationKey = "violations:" + identifier + ":" + actionType;
        Long violations = redisTemplate.opsForValue().increment(violationKey);
        redisTemplate.expire(violationKey, Duration.ofHours(1));
        
        // Progressive penalties
        if (violations >= 10) {
            // Temporary ban
            String banKey = "banned:" + identifier;
            redisTemplate.opsForValue().set(banKey, "RATE_LIMIT_VIOLATION", Duration.ofHours(1));
            
            // Log security event
            auditService.logSecurityViolation("RATE_LIMIT_ABUSE", 
                "Repeated rate limit violations: " + violations);
        }
    }
    
    public boolean isBanned(String identifier) {
        return redisTemplate.hasKey("banned:" + identifier);
    }
}

@Data
@Builder
public class TokenBucket {
    private long tokens;
    private long lastRefill;
}

@Data
@AllArgsConstructor
public class RateLimitConfig {
    private long maxRequests;
    private Duration timeWindow;
}
```

---

## 🔐 **7. SECURE CONFIGURATION MANAGEMENT**

### **Environment-Specific Security Configuration**

```yaml
# application-security.yml
app:
  security:
    jwt:
      secret: ${JWT_SECRET:default-secret-change-in-production}
      access-token-expiration: 3600000  # 1 hour
      refresh-token-expiration: 604800000  # 7 days
    
    encryption:
      key: ${ENCRYPTION_KEY:32-char-key-change-in-production}
      algorithm: AES/GCM/NoPadding
    
    rate-limiting:
      enabled: true
      redis-prefix: "rate_limit:"
    
    audit:
      enabled: true
      async-processing: true
      retention-days: 90
    
    session:
      timeout: 86400  # 24 hours
      max-concurrent-sessions: 3
      
    password:
      min-length: 8
      require-special-chars: true
      require-numbers: true
      require-uppercase: true
      require-lowercase: true
      
  cors:
    allowed-origins: ${ALLOWED_ORIGINS:http://localhost:3000}
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
    
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/cardgame_db}
    username: ${DATABASE_USERNAME:cardgame_user}
    password: ${DATABASE_PASSWORD:secure_password}
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-create in production
    properties:
      hibernate:
        show_sql: false  # Never log SQL in production
        format_sql: false
        
logging:
  level:
    com.n9.core.security: INFO
    org.springframework.security: WARN
    # Never log at DEBUG level in production
```

### **Security Configuration Class**

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong cost factor
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // API doesn't need CSRF protection
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            );
        
        return http.build();
    }
}
```

---

## 🔐 **8. SECURITY TESTING FRAMEWORK**

### **Comprehensive Security Tests**

```java
@SpringBootTest
@TestPropertySource("classpath:application-test.yml")
public class SecurityTestSuite {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private InputValidationService validationService;
    
    @Test
    @DisplayName("Should prevent SQL injection attacks")
    public void testSQLInjectionPrevention() {
        // Test SQL injection patterns
        String[] sqlInjectionPatterns = {
            "'; DROP TABLE users; --",
            "1' OR '1'='1",
            "admin'/*",
            "' UNION SELECT * FROM users --"
        };
        
        for (String pattern : sqlInjectionPatterns) {
            LoginRequest request = new LoginRequest(pattern, "password");
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", request, String.class);
            
            // Should not cause internal server error
            assertNotEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            // Should return unauthorized or bad request
            assertTrue(response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                      response.getStatusCode() == HttpStatus.BAD_REQUEST);
        }
    }
    
    @Test
    @DisplayName("Should prevent XSS attacks")
    public void testXSSPrevention() {
        String[] xssPatterns = {
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "<img src=x onerror=alert('xss')>",
            "';alert('xss');//"
        };
        
        for (String pattern : xssPatterns) {
            UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .username(pattern)
                .email("test@example.com")
                .password("ValidPassword123!")
                .build();
            
            ValidationResult result = validationService.validateUserRegistration(dto);
            
            // XSS patterns should be sanitized or rejected
            if (result.isValid()) {
                assertFalse(dto.getUsername().contains("<script"));
                assertFalse(dto.getUsername().contains("javascript:"));
            }
        }
    }
    
    @Test
    @DisplayName("Should enforce rate limiting")
    public void testRateLimiting() {
        String username = "testuser" + System.currentTimeMillis();
        
        // Attempt multiple rapid requests
        for (int i = 0; i < 10; i++) {
            LoginRequest request = new LoginRequest(username, "wrongpassword");
            ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", request, String.class);
            
            if (i >= 5) {
                // Should be rate limited after 5 attempts
                assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
            }
        }
    }
    
    @Test
    @DisplayName("Should validate JWT token security")
    public void testJWTSecurity() {
        // Test with expired token
        String expiredToken = generateExpiredToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/games", HttpMethod.GET, entity, String.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        // Test with malformed token
        headers.setBearerAuth("invalid.token.here");
        entity = new HttpEntity<>(headers);
        
        response = restTemplate.exchange(
            "/api/games", HttpMethod.GET, entity, String.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    @DisplayName("Should enforce input validation")
    public void testInputValidation() {
        // Test oversized input
        String oversizedInput = "a".repeat(1000);
        GameMoveDTO move = GameMoveDTO.builder()
            .playerId(oversizedInput)
            .cardIndex(999)
            .roundNumber(-1)
            .build();
        
        ValidationResult result = validationService.validateGameMove(move);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("playerId"));
        assertTrue(result.getErrors().containsKey("cardIndex"));
        assertTrue(result.getErrors().containsKey("roundNumber"));
    }
}
```

---

## 🔒 **SECURITY MONITORING DASHBOARD**

```java
@RestController
@RequestMapping("/api/admin/security")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityMonitoringController {
    
    @GetMapping("/dashboard")
    public SecurityDashboard getSecurityDashboard() {
        return SecurityDashboard.builder()
            .activeSecurityThreats(threatDetectionService.getActiveThreats())
            .recentSecurityEvents(auditService.getRecentEvents(Duration.ofHours(24)))
            .systemSecurityScore(calculateSecurityScore())
            .rateLimit Violations(rateLimitService.getRecentViolations())
            .suspiciousActivities(suspiciousActivityDetector.getRecentActivities())
            .build();
    }
}
```

**Comprehensive security framework này đảm bảo Core module có enterprise-grade security protection với multiple layers của defense, comprehensive monitoring, và detailed audit trails suitable cho university project requirements với professional standards.**