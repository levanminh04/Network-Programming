# 📦 SHARED MODULE - Complete Reference Guide

**Version**: 1.0.0 (MVP)  
**Last Updated**: 2025-01-05  
**Module Type**: Multi-Platform Shared Library  

> **📚 Main Documentation Hub**: See all documents in `shared/docs/` folder

---

## 🎯 **QUICK NAVIGATION**

| Document | Purpose | For |
|----------|---------|-----|
| **[📊 Assessment](./docs/SHARED_MODULE_ASSESSMENT.md)** | Current state analysis | Architects |
| **[🔧 Restructure Plan](./docs/SHARED_MODULE_RESTRUCTURE.md)** | Implementation roadmap | Team Leads |
| **[📬 Message Catalog](./docs/MESSAGE_CATALOG.md)** | All messages + examples | All Devs |
| **[🔗 Integration Guide](./docs/INTEGRATION_GUIDE.md)** | How to use in projects | Implementers |

---

## ⚡ **QUICK START**

### **Backend (Java)**

```java
// 1. Add dependency to pom.xml
<dependency>
    <groupId>com.N9</groupId>
    <artifactId>shared</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

// 2. Import & Use
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;

LoginRequestDto dto = new LoginRequestDto("user", "pass");
MessageEnvelope msg = MessageFactory.createRequest(
    MessageType.AUTH_LOGIN_REQUEST, dto
);
```

### **Frontend (React/TypeScript)**

```typescript
// 1. Copy types from docs/INTEGRATION_GUIDE.md

// 2. Import & Use
import { MessageType, LoginRequestDto } from '@/types/shared';

const msg: MessageEnvelope<LoginRequestDto> = {
  type: MessageType.AUTH_LOGIN_REQUEST,
  correlationId: generateId(),
  timestamp: Date.now(),
  version: '1.0.0',
  payload: { username: 'user', password: 'pass' }
};
```

---

## 📁 **MODULE STRUCTURE**

```
shared/
├── docs/                     📄 Complete Documentation
│   ├── SHARED_MODULE_ASSESSMENT.md      (Analysis)
│   ├── SHARED_MODULE_RESTRUCTURE.md     (Plan)
│   ├── MESSAGE_CATALOG.md               (Messages)
│   └── INTEGRATION_GUIDE.md             (Usage)
│
└── src/main/java/com/n9/shared/
    ├── constants/            🔢 Game & Protocol Constants
    ├── model/
    │   ├── dto/              📦 Data Transfer Objects
    │   └── enums/            🎭 Enumerations
    ├── protocol/             📡 Message Protocol
    └── util/                 🛠️ Utilities
```

---

## 📊 **CURRENT STATUS**

### **✅ COMPLETE**
- MessageEnvelope, MessageType
- LoginRequestDto, LoginSuccessDto, CardDto
- CardSuit, GameState, ErrorCode enums
- JsonUtils, ValidationUtils, IdUtils

### **🆕 TODO (Week 1-3)**
See [SHARED_MODULE_RESTRUCTURE.md](./docs/SHARED_MODULE_RESTRUCTURE.md) for complete plan:

- [ ] **Auth DTOs**: Register, LoginFailure, Logout
- [ ] **Game DTOs**: RoundStart, PlayCard, Reveal, Result  
- [ ] **Match DTOs**: Request, Found, Start
- [ ] **Enums**: CardRank, PlayerStatus, MatchResult
- [ ] **Constants**: GameConstants, ProtocolConstants

---

## 🎮 **MVP CONSTANTS**

```java
GameConstants.DECK_SIZE = 36              // 4 suits × 9 ranks (A-9)
GameConstants.TOTAL_ROUNDS = 3            // Fixed 3 rounds
GameConstants.ROUND_TIMEOUT_SECONDS = 10  // 10s per round
GameConstants.PLAYERS_PER_GAME = 2        // 1v1 only
```

---

## 📬 **MESSAGE EXAMPLES**

See [MESSAGE_CATALOG.md](./docs/MESSAGE_CATALOG.md) for all 25+ messages.

**Login Request**:
```json
{
  "type": "AUTH.LOGIN_REQUEST",
  "correlationId": "cor-abc",
  "timestamp": 1736412000000,
  "version": "1.0.0",
  "payload": {
    "username": "player01",
    "password": "secret"
  }
}
```

---

## 🔗 **INTEGRATION**

See [INTEGRATION_GUIDE.md](./docs/INTEGRATION_GUIDE.md) for:
- Maven dependency setup
- WebSocket message handling
- TypeScript definitions
- Validation & error handling
- Testing strategies

---

## 🧪 **BUILD & TEST**

```bash
# Build shared module
cd shared/
mvn clean install

# Run tests
mvn test

# Use in other modules (auto-included via Maven)
cd ../core/
mvn clean install
```

---

## ❓ **FAQ**

**Q: Why 36 cards?**  
A: MVP uses A-9 only (no face cards). 4 suits × 9 ranks = 36 cards.

**Q: How to add new DTO?**  
A: See [Contributing Guidelines](./docs/SHARED_MODULE_RESTRUCTURE.md#implementation-checklist)

**Q: Where are TypeScript definitions?**  
A: See [INTEGRATION_GUIDE.md](./docs/INTEGRATION_GUIDE.md#frontend-integration-reactjavascript)

---

## 📞 **SUPPORT**

- **Architecture Questions**: See [ASSESSMENT.md](./docs/SHARED_MODULE_ASSESSMENT.md)
- **Implementation Help**: See [INTEGRATION_GUIDE.md](./docs/INTEGRATION_GUIDE.md)
- **Message Examples**: See [MESSAGE_CATALOG.md](./docs/MESSAGE_CATALOG.md)

---

**Status**: 🚧 30% Complete | **Target**: Week 3 Full MVP Coverage

**For complete documentation, navigate to `shared/docs/` folder.**
