# Shared Module Architecture Documentation

## 🎯 **TỔNG QUAN KIẾN TRÚC SHARED MODULE**

### **Mission Statement**
Shared module là **Single Source of Truth** cho toàn bộ hệ thống, cung cấp:
- **Protocol Definitions**: Message types, formats, schemas
- **Common Models**: DTOs, enums, constants
- **Utilities**: Helpers, validators, converters
- **Documentation**: Standards, examples, best practices

### **Design Principles**
1. **Backward Compatibility**: Không break existing code
2. **Versioning Strategy**: Semantic versioning cho API changes
3. **Zero Dependencies**: Chỉ depend vào JDK standard libs
4. **Self-Documenting**: Code phải readable như documentation
5. **Extensibility**: Dễ mở rộng cho future requirements

---

## 📁 **CẤU TRÚC THƯ MỤC ENTERPRISE-GRADE**

```
shared/
├── docs/                              # 📚 Documentation Hub
│   ├── protocol/                      # 🔌 Protocol Specifications
│   │   ├── message-catalog.md         # Complete message reference
│   │   ├── data-formats.md            # JSON schemas & examples
│   │   ├── versioning-strategy.md     # API versioning rules
│   │   └── migration-guides.md        # Breaking change guides
│   ├── examples/                      # 💡 Usage Examples
│   │   ├── client-integration.md      # Frontend examples
│   │   ├── server-integration.md      # Backend examples
│   │   └── testing-examples.md        # Test cases
│   ├── architecture/                  # 🏗️ Design Documents
│   │   ├── design-decisions.md        # ADRs (Architecture Decision Records)
│   │   ├── naming-conventions.md      # Naming standards
│   │   └── best-practices.md          # Development guidelines
│   └── api/                          # 📖 API Documentation
│       ├── CHANGELOG.md               # Version history
│       ├── MIGRATION.md               # Breaking changes
│       └── README.md                  # Quick start guide
├── src/main/java/com/n9/shared/      # 💻 Source Code
│   ├── protocol/                      # 🔌 Protocol Layer
│   │   ├── MessageType.java           # Message type definitions
│   │   ├── MessageEnvelope.java       # Standard message wrapper
│   │   ├── ProtocolVersion.java       # Version management
│   │   └── MessageValidator.java      # Protocol validation
│   ├── model/                        # 📊 Data Models
│   │   ├── dto/                       # Data Transfer Objects
│   │   │   ├── auth/                  # Authentication DTOs
│   │   │   ├── game/                  # Game-specific DTOs
│   │   │   ├── lobby/                 # Lobby management DTOs
│   │   │   └── system/                # System-level DTOs
│   │   ├── enums/                     # Enumerations
│   │   │   ├── GameState.java         # Game state enum
│   │   │   ├── CardSuit.java          # Card suit enum
│   │   │   ├── ConnectionStatus.java  # Connection states
│   │   │   └── ErrorCode.java         # Standardized error codes
│   │   └── constants/                 # Constants
│   │       ├── GameConstants.java     # Game-related constants
│   │       ├── NetworkConstants.java  # Network timeouts, limits
│   │       └── ValidationConstants.java # Validation rules
│   ├── util/                         # 🛠️ Utilities
│   │   ├── JsonUtils.java             # JSON serialization helpers
│   │   ├── ValidationUtils.java       # Input validation utilities
│   │   ├── TimeUtils.java             # Time/date utilities
│   │   └── IdGenerator.java           # ID generation utilities
│   ├── exception/                    # ⚠️ Custom Exceptions
│   │   ├── ProtocolException.java     # Protocol-related errors
│   │   ├── ValidationException.java   # Validation errors
│   │   └── SharedRuntimeException.java # Base exception
│   └── annotation/                   # 🏷️ Custom Annotations
│       ├── MessageHandler.java        # Message handler annotation
│       ├── ValidatedMessage.java      # Validation annotation
│       └── ProtocolVersion.java       # Version annotation
└── src/test/java/                    # 🧪 Test Suite
    ├── protocol/                      # Protocol tests
    ├── model/                        # Model validation tests
    ├── util/                         # Utility tests
    └── integration/                  # Integration tests
```

---

## 🎯 **CORE COMPONENTS SPECIFICATION**

### **1. Protocol Layer**
- **MessageType.java**: Centralized message type definitions
- **MessageEnvelope.java**: Standard wrapper cho tất cả messages
- **ProtocolVersion.java**: API versioning support
- **MessageValidator.java**: Protocol compliance validation

### **2. Data Models**
- **DTOs**: Clean data transfer objects cho mỗi domain
- **Enums**: Type-safe enumerations cho business logic
- **Constants**: Centralized configuration values

### **3. Utilities**
- **JsonUtils**: JSON processing với error handling
- **ValidationUtils**: Input validation với security focus
- **TimeUtils**: Timezone-aware time operations
- **IdGenerator**: Thread-safe ID generation

### **4. Exception Handling**
- **Hierarchical exceptions**: Clear error categorization
- **Error codes**: Standardized across system
- **Localization support**: I18n-ready error messages

---

## 📈 **BENEFITS FOR ENTERPRISE DEVELOPMENT**

### **1. Consistency**
- Unified message format across all modules
- Standardized error handling
- Consistent naming conventions

### **2. Maintainability**
- Single place to update protocol changes
- Centralized documentation
- Clear API versioning

### **3. Extensibility**
- Easy to add new message types
- Backward-compatible changes
- Module-independent development

### **4. Quality Assurance**
- Type safety with enums and constants
- Comprehensive validation
- Well-tested utilities

This architecture provides a solid foundation for enterprise-grade development while remaining accessible for academic projects.