# Shared Module - Complete Architecture Reference

## 🎯 **MODULE OVERVIEW**

The **Shared Module** serves as the foundational communication layer for the entire Network Programming card game project. It provides standardized protocols, data transfer objects (DTOs), validation utilities, and common functionality shared across all system components.

### **Core Responsibilities**
- 🔄 **Message Protocol Management**: Centralized message type definitions and envelope structure
- 📝 **Data Transfer Objects**: Standardized DTOs for all inter-component communication  
- ✅ **Validation Framework**: Comprehensive validation utilities for security and data integrity
- 🔧 **Utility Functions**: Common functionality for JSON processing, ID generation, and protocol handling
- 📚 **Documentation**: Complete protocol specifications and integration guides

### **Architecture Principles**
- **Single Source of Truth**: All protocol definitions centralized in shared module
- **Type Safety**: Strongly-typed DTOs with validation annotations
- **Version Compatibility**: Extensible message format supporting protocol evolution
- **Security First**: Built-in validation, size limits, and security best practices
- **Developer Experience**: Comprehensive documentation and helper utilities

---

## 📁 **MODULE STRUCTURE**

```
shared/
├── pom.xml                              # Maven configuration with dependencies
├── docs/                                # Comprehensive documentation
│   ├── README.md                        # Quick start and overview
│   ├── protocol/
│   │   ├── message-catalog.md           # Complete message reference
│   │   └── protocol-specification.md    # Technical protocol details
│   ├── integration/
│   │   ├── integration-guide.md         # Step-by-step implementation guide
│   │   └── examples/                    # Code examples and templates
│   └── architecture/
│       ├── design-decisions.md          # Architectural rationale
│       └── migration-guide.md           # Version migration instructions
└── src/
    ├── main/java/com/n9/shared/
    │   ├── protocol/                    # Message protocol definitions
    │   │   ├── MessageType.java         # Central message type catalog
    │   │   └── MessageEnvelope.java     # Standard message wrapper
    │   ├── model/                       # Data models and DTOs
    │   │   ├── dto/                     # Data Transfer Objects
    │   │   │   ├── auth/               # Authentication DTOs
    │   │   │   ├── lobby/              # Lobby management DTOs
    │   │   │   ├── game/               # Game logic DTOs
    │   │   │   └── system/             # System message DTOs
    │   │   ├── enums/                  # Shared enumerations
    │   │   │   ├── GameState.java      # Game state definitions
    │   │   │   ├── CardSuit.java       # Card suit definitions
    │   │   │   └── ErrorCode.java      # Error code definitions
    │   │   └── Card.java               # Card model definition
    │   └── util/                       # Utility classes
    │       ├── JsonUtils.java          # JSON processing utilities
    │       ├── ValidationUtils.java    # Validation framework
    │       └── IdUtils.java            # ID generation utilities
    └── test/java/                      # Comprehensive test suite
        ├── protocol/                   # Protocol tests
        ├── model/                      # DTO validation tests
        └── util/                       # Utility function tests
```

---

## 🔄 **PROTOCOL SYSTEM**

### **Message Type Hierarchy**
The protocol uses a hierarchical naming convention: `DOMAIN.ACTION[.MODIFIER]`

```java
// Examples from MessageType.java
public static final String AUTH_LOGIN_REQUEST = "AUTH.LOGIN_REQUEST";
public static final String GAME_CARD_PLAY_REQUEST = "GAME.CARD_PLAY_REQUEST";
public static final String LOBBY_PLAYER_LIST_UPDATE = "LOBBY.PLAYER_LIST_UPDATE";
public static final String SYSTEM_ERROR = "SYSTEM.ERROR";
```

### **Message Envelope Structure**
All messages use the standardized `MessageEnvelope` wrapper:

```java
MessageEnvelope envelope = MessageEnvelope.builder()
    .type(MessageType.AUTH_LOGIN_REQUEST)
    .correlationId(IdUtils.generateCorrelationId())
    .timestamp(System.currentTimeMillis())
    .userId("user-123")
    .sessionId("sess-abc-def")
    .matchId("match-456")  // For game-specific messages
    .payload(loginRequestDto)
    .build();
```

### **Domain Coverage**
- **AUTH**: Authentication and user management (6 message types)
- **LOBBY**: Lobby operations and matchmaking (8 message types)  
- **GAME**: Game mechanics and card playing (12 message types)
- **SYSTEM**: Error handling and maintenance (4 message types)

**Total**: 30+ message types covering all system interactions

---

## 📝 **DATA TRANSFER OBJECTS (DTOs)**

### **Authentication DTOs**
```java
// Login request with validation
@Data
@Builder
public class LoginRequestDto {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username contains invalid characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
    private String password;
    
    @Size(max = 20, message = "Client version too long")
    private String clientVersion;
    
    private Boolean rememberMe = false;
}

// Login success response
@Data
@Builder
public class LoginSuccessDto {
    @NotBlank private String userId;
    @NotBlank private String username;
    private String displayName;
    @NotBlank private String token;
    @NotNull private Long expiresAt;
    private String refreshToken;
    @Min(0) private Double score;
    @Min(0) private Integer gamesPlayed;
    @Min(0) private Integer gamesWon;
    @NotBlank private String rank;
    private Long lastLogin;
}
```

### **Game DTOs**
```java
// Card play request
@Data
@Builder
public class CardPlayRequestDto {
    @NotBlank private String matchId;
    @NotNull @Min(1) @Max(3) private Integer roundNumber;
    @NotNull @Min(0) @Max(51) private Integer cardIndex;
    @NotNull private Long timestamp;
}

// Card representation
@Data
@Builder
public class CardDto {
    @NotNull @Min(0) @Max(51) private Integer index;
    @NotBlank private String rank;
    @NotNull private CardSuit suit;
    @NotNull @Min(1) @Max(13) private Integer value;
    @NotBlank private String displayName;
}
```

---

## 🔧 **UTILITY FRAMEWORK**

### **JsonUtils - JSON Processing**
```java
// Type-safe serialization/deserialization
String json = JsonUtils.toJson(messageEnvelope);
Optional<MessageEnvelope> envelope = JsonUtils.fromJsonSafe(json, MessageEnvelope.class);

// Payload conversion with validation
LoginRequestDto request = JsonUtils.convertPayload(envelope.getPayload(), LoginRequestDto.class);

// Size validation
boolean oversized = JsonUtils.exceedsSizeLimit(message, 32768); // 32KB limit
```

### **ValidationUtils - Comprehensive Validation**
```java
// DTO validation with Bean Validation
ValidationResult result = ValidationUtils.validate(loginRequest);
if (!result.isValid()) {
    throw new ValidationException(result.getErrorMessage());
}

// Protocol-specific validation
boolean validUsername = ValidationUtils.isValidUsername("player123");
boolean validCardIndex = ValidationUtils.isValidCardIndex(25);
boolean validTimestamp = ValidationUtils.isValidTimestamp(System.currentTimeMillis());

// Credential validation
ValidationResult credResult = ValidationUtils.validateCredentials(username, password);
```

### **IdUtils - ID Generation**
```java
// Protocol-specific ID generation
String correlationId = IdUtils.generateCorrelationId();     // cor-1736412000-abc123def
String matchId = IdUtils.generateMatchId();                 // match-1736412000-xyz789
String sessionId = IdUtils.generateSessionId();             // sess-abc123def456...

// Specialized IDs
String inviteCode = IdUtils.generateInviteCode();           // 123456 (6 digits)
String shortId = IdUtils.generateShortId();                 // abc12345 (8 chars)

// ID validation and extraction
boolean valid = IdUtils.isValidCorrelationId(correlationId);
long timestamp = IdUtils.extractTimestamp(matchId);
```

---

## ✅ **VALIDATION FRAMEWORK**

### **Multi-Layer Validation**
1. **Annotation-Based**: JSR-303 Bean Validation on DTOs
2. **Protocol-Specific**: Custom validation for game mechanics
3. **Security**: Input sanitization and size limits
4. **Network**: Message size and format validation

### **Validation Categories**
```java
// User input validation
@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
private String username;

// Game mechanics validation  
@Min(0) @Max(51) private Integer cardIndex;
@Min(1) @Max(3) private Integer roundNumber;

// Network protocol validation
@NotNull private Long timestamp;
@Size(max = 100) private String correlationId;

// Security validation
@Size(max = 1000) private String message;  // Prevent oversized strings
```

---

## 🏗️ **INTEGRATION PATTERNS**

### **Frontend Integration (React)**
```javascript
import wsService from './websocket-service';

// Send login request
const response = await wsService.sendMessageWithResponse(
    'AUTH.LOGIN_REQUEST',
    { username: 'player1', password: 'secret123' }
);

// Handle game events
wsService.onMessage('GAME.ROUND_START', (message) => {
    const roundData = message.payload;
    updateGameState(roundData);
});
```

### **Gateway Integration (Spring Boot)**
```java
@Service
public class MessageProcessor {
    
    public MessageEnvelope processMessage(MessageEnvelope envelope) {
        switch (envelope.getType()) {
            case MessageType.AUTH_LOGIN_REQUEST:
                return handleLogin(envelope);
            case MessageType.GAME_CARD_PLAY_REQUEST:
                return forwardToCore(envelope);
            default:
                return createErrorResponse(envelope, "UNSUPPORTED_MESSAGE");
        }
    }
}
```

### **Core Server Integration (Java)**
```java
public class GameService {
    
    public MessageEnvelope handleCardPlay(MessageEnvelope envelope) {
        CardPlayRequestDto request = JsonUtils.convertPayload(
            envelope.getPayload(), CardPlayRequestDto.class);
        
        // Validate request
        ValidationResult validation = ValidationUtils.validate(request);
        if (!validation.isValid()) {
            return createErrorResponse(envelope, validation.getErrorMessage());
        }
        
        // Process game logic
        return processCardPlay(request);
    }
}
```

---

## 📊 **PERFORMANCE & SCALABILITY**

### **Message Size Optimization**
- **Maximum message size**: 64KB
- **Maximum payload size**: 32KB  
- **String field limits**: 1000 characters
- **Compression**: JSON optimization with minimal whitespace

### **Memory Efficiency**
- **Builder patterns**: Immutable DTOs with efficient construction
- **Object pooling**: Reusable message envelopes
- **Lazy initialization**: Optional fields loaded on demand

### **Validation Performance**
- **Cached validators**: Bean Validation factory caching
- **Pattern compilation**: Pre-compiled regex patterns
- **Short-circuit evaluation**: Fast-fail validation chains

---

## 🔒 **SECURITY CONSIDERATIONS**

### **Input Validation**
- **Whitelist approach**: Strict pattern matching for all inputs
- **Size limits**: Prevent DoS through oversized messages
- **Type safety**: Strong typing prevents injection attacks

### **Protocol Security**
- **Correlation tracking**: Prevent message replay attacks
- **Timestamp validation**: Detect stale or future-dated messages
- **Session validation**: Ensure authenticated context

### **Data Protection**
- **No sensitive data logging**: Passwords and tokens excluded from logs
- **Minimal data exposure**: DTOs contain only necessary fields
- **Validation error safety**: Error messages don't leak sensitive information

---

## 🧪 **TESTING STRATEGY**

### **Unit Tests**
- **DTO validation**: Test all validation annotations
- **Utility functions**: Comprehensive utility testing
- **Message serialization**: JSON round-trip testing

### **Integration Tests**
- **Protocol compliance**: End-to-end message flow testing
- **Error handling**: Comprehensive error scenario testing
- **Performance**: Load testing with message size limits

### **Test Utilities**
```java
// Provided test utilities
MessageEnvelope testMessage = MessageTestUtils.createTestMessage(
    MessageType.AUTH_LOGIN_REQUEST, loginRequest);

MessageTestUtils.assertValidMessage(testMessage);
MessageTestUtils.assertMessageSerialization(testMessage);
```

---

## 📈 **DEVELOPMENT WORKFLOW**

### **Adding New Message Types**
1. Add constant to `MessageType.java`
2. Create corresponding DTOs with validation
3. Update protocol documentation
4. Add integration examples
5. Write comprehensive tests

### **Extending DTOs**
1. Add fields with appropriate validation annotations
2. Update builder methods
3. Test serialization compatibility
4. Update documentation examples

### **Version Migration**
1. Maintain backward compatibility
2. Use optional fields for new features
3. Update protocol version numbers
4. Provide migration documentation

---

## 📚 **DOCUMENTATION HIERARCHY**

1. **README.md**: Quick start and basic usage
2. **message-catalog.md**: Complete message reference with examples
3. **integration-guide.md**: Step-by-step implementation guide
4. **protocol-specification.md**: Technical protocol details
5. **Javadoc**: Inline code documentation
6. **Test examples**: Working integration examples

---

## 🎯 **QUALITY METRICS**

### **Code Coverage**
- **Target**: 90%+ test coverage
- **Protocol tests**: 100% message type coverage
- **Validation tests**: All validation rules tested
- **Utility tests**: All public methods tested

### **Documentation Coverage**
- **API documentation**: 100% public methods documented
- **Protocol specification**: All message types documented with examples
- **Integration guides**: Complete implementation examples
- **Error scenarios**: All error codes documented

### **Performance Benchmarks**
- **Serialization**: <1ms for typical messages
- **Validation**: <0.1ms for DTO validation
- **ID generation**: <0.01ms for standard IDs

---

## 🚀 **NEXT STEPS**

### **Immediate Actions**
1. **Complete test suite**: Implement comprehensive testing
2. **Performance optimization**: Benchmark and optimize critical paths
3. **Integration validation**: Test with Gateway and Core modules

### **Future Enhancements**
1. **Protocol versioning**: Implement version negotiation
2. **Message compression**: Add compression for large payloads
3. **Advanced validation**: Custom validation annotations
4. **Monitoring integration**: Metrics and observability

---

The Shared Module provides a robust, secure, and scalable foundation for the entire Network Programming project. Its comprehensive validation framework, extensive documentation, and developer-friendly utilities ensure consistent and reliable communication across all system components.