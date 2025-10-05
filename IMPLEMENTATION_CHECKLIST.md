# MVP Implementation Checklist

## ✅ **PHASE 1: DATABASE SETUP** (Priority: 🔴 CRITICAL)

### **Database Installation & Configuration**
- [ ] Install MySQL 8.0+ trên development environment
- [ ] Create database user `cardgame_user` với proper permissions
- [ ] Configure MySQL character set UTF8MB4
- [ ] Set InnoDB as default storage engine
- [ ] Configure connection pool settings (HikariCP)

### **Schema Initialization**
- [ ] Execute `core/docs/database/V1__cardgame_mvp.sql`
- [ ] Verify 6 tables created successfully
  ```sql
  USE cardgame_db;
  SHOW TABLES;
  -- Expected: users, user_profiles, cards, games, game_rounds, active_sessions
  ```
- [ ] Verify 36 cards seeded correctly
  ```sql
  SELECT suit, COUNT(*) FROM cards GROUP BY suit;
  -- Expected: 4 suits × 9 cards = 36 total
  ```
- [ ] Test foreign key constraints
- [ ] Verify all indexes created

### **Documentation Review**
- [ ] Read `core/docs/database/README.md` (quick start)
- [ ] Review `core/docs/database/database-design.md` (complete schema)
- [ ] Understand ACTIVE vs DEFERRED fields strategy
- [ ] Review common queries section
- [ ] Understand transaction boundaries

**Acceptance Criteria:**
✅ Database `cardgame_db` exists  
✅ All 6 tables present with correct structure  
✅ 36 cards seeded (A-9 in 4 suits)  
✅ Can execute sample queries successfully  
✅ Connection pool configured in application  

---

## 📦 **PHASE 2: SHARED MODULE** (Priority: 🟡 HIGH)

### **Message Protocol**
- [ ] Review `shared/MessageProtocol.java`
- [ ] Define message types for MVP:
  - `LOGIN_REQUEST`, `LOGIN_RESPONSE`
  - `REGISTER_REQUEST`, `REGISTER_RESPONSE`
  - `CREATE_GAME_REQUEST`, `CREATE_GAME_RESPONSE`
  - `QUICK_MATCH_REQUEST`, `QUICK_MATCH_RESPONSE`
  - `PLAY_CARD_REQUEST`, `PLAY_CARD_RESPONSE`
  - `ROUND_COMPLETED`, `GAME_COMPLETED`
  - `PLAYER_QUIT`, `HEARTBEAT`
- [ ] Implement serialization/deserialization (JSON)
- [ ] Add validation logic
- [ ] Write unit tests for protocol

### **Shared Models**
- [ ] Create `User` model (matches database schema)
- [ ] Create `Card` model (36 cards)
- [ ] Create `Game` model
- [ ] Create `GameRound` model
- [ ] Create `Session` model
- [ ] Add toString(), equals(), hashCode() methods

### **Utility Classes**
- [ ] Card deck utilities (shuffle, deal)
- [ ] Game score calculation
- [ ] Winner determination logic
- [ ] Validation utilities

**Acceptance Criteria:**
✅ MessageProtocol compiles và passes tests  
✅ All models match database schema  
✅ Shared JAR builds successfully  
✅ No dependencies on core or gateway  

---

## 🖥️ **PHASE 3: CORE SERVER** (Priority: 🔴 CRITICAL)

### **TCP Server Setup**
- [ ] Review `core/docs/implementation/tcp-server-guide.md`
- [ ] Implement `CoreServer.java` (port 9999)
- [ ] Add multi-threaded connection handling
- [ ] Implement heartbeat mechanism (30s interval)
- [ ] Add graceful shutdown logic
- [ ] Configure thread pool (20-50 threads)

### **Database Integration**
- [ ] Add JDBC dependencies (MySQL Connector)
- [ ] Create `DatabaseConnection` class
- [ ] Implement connection pooling (HikariCP)
- [ ] Create DAO classes:
  - [ ] `UserDAO` (login, register, updateLastLogin)
  - [ ] `UserProfileDAO` (getProfile, updateStatistics)
  - [ ] `CardDAO` (getDeck, getCardById)
  - [ ] `GameDAO` (createGame, updateGame, completeGame)
  - [ ] `GameRoundDAO` (createRound, updateRound)
  - [ ] `SessionDAO` (createSession, updateHeartbeat, deleteSession)

### **Game Logic Implementation**
- [ ] Review `core/docs/implementation/game-logic-guide.md`
- [ ] Implement user authentication (plain text password)
- [ ] Implement matchmaking logic (QUICK mode)
- [ ] Implement game creation (2 players, 3 rounds)
- [ ] Implement round handling:
  - [ ] 10-second timeout per round
  - [ ] Auto-pick random card on timeout
  - [ ] Simultaneous card reveal
  - [ ] Winner determination (higher card value)
- [ ] Implement game completion:
  - [ ] Total score calculation
  - [ ] Winner determination
  - [ ] Statistics update (games_played, games_won, games_lost)
- [ ] Handle player quit (remaining player wins)

### **Message Handlers**
- [ ] `LoginHandler` (authenticate user, create session)
- [ ] `RegisterHandler` (create user, create profile)
- [ ] `QuickMatchHandler` (find opponent, create game)
- [ ] `PlayCardHandler` (validate card, update round)
- [ ] `HeartbeatHandler` (update last_heartbeat)
- [ ] `QuitHandler` (handle disconnect, update game status)

### **Testing**
- [ ] Unit tests for all DAOs
- [ ] Integration tests for game flow
- [ ] Load testing (20 concurrent games)
- [ ] Test timeout auto-pick logic
- [ ] Test player quit handling

**Acceptance Criteria:**
✅ TCP server starts on port 9999  
✅ Can handle 20+ concurrent connections  
✅ All game flows (A-D) working  
✅ Database operations transactional  
✅ Proper error handling and logging  

---

## 🌐 **PHASE 4: GATEWAY (WebSocket)** (Priority: 🟡 HIGH)

### **Spring Boot Configuration**
- [ ] Review `gateway/src/main/resources/application.properties`
- [ ] Configure WebSocket endpoint (`/ws/game`)
- [ ] Configure CORS for frontend (localhost:5173)
- [ ] Add connection timeout settings
- [ ] Configure logging levels

### **WebSocket Handler**
- [ ] Implement `GameWebSocketHandler`
- [ ] Add session management
- [ ] Implement message routing (Frontend → Core)
- [ ] Add error handling
- [ ] Implement reconnection logic

### **Message Translation**
- [ ] WebSocket message → TCP message (JSON → custom protocol)
- [ ] TCP response → WebSocket message (custom protocol → JSON)
- [ ] Add message validation
- [ ] Implement request/response correlation

### **Session Management**
- [ ] Track WebSocket sessions
- [ ] Map WebSocket session → User ID
- [ ] Handle disconnection (update active_sessions table)
- [ ] Implement heartbeat forwarding

### **Testing**
- [ ] WebSocket connection test
- [ ] Message routing test
- [ ] Load testing (50+ concurrent WebSocket connections)
- [ ] Reconnection test

**Acceptance Criteria:**
✅ Gateway starts on port 8080  
✅ WebSocket endpoint accessible  
✅ Messages route correctly to Core  
✅ Frontend can connect and communicate  
✅ Proper error responses  

---

## 💻 **PHASE 5: FRONTEND** (Priority: 🟡 HIGH)

### **Project Setup**
- [ ] Review `frontend/docs/README.md`
- [ ] Install dependencies (`npm install`)
- [ ] Configure Vite (`vite.config.js`)
- [ ] Configure Tailwind CSS
- [ ] Set up ESLint and Prettier

### **WebSocket Integration**
- [ ] Review `frontend/docs/architecture/websocket-strategy.md`
- [ ] Implement `WebSocketManager.js`
- [ ] Add connection handling (connect, disconnect, reconnect)
- [ ] Implement message sending/receiving
- [ ] Add heartbeat mechanism (15s interval)

### **State Management**
- [ ] Review `frontend/docs/architecture/state-management.md`
- [ ] Set up Redux Toolkit store
- [ ] Create slices:
  - [ ] `authSlice` (user, isAuthenticated, login, logout)
  - [ ] `gameSlice` (currentGame, playerHand, scores)
  - [ ] `lobbySlice` (onlineUsers, waitingPlayers)
  - [ ] `websocketSlice` (connectionStatus, messages)
- [ ] Add middleware for WebSocket

### **Component Development**
- [ ] Review `frontend/docs/architecture/component-design.md`
- [ ] Implement atomic components:
  - [ ] `Button`, `Input`, `Card`, `Modal`, `Toast`
- [ ] Implement pages:
  - [ ] `LoginPage` (username, password, remember me)
  - [ ] `RegisterPage` (username, email, password)
  - [ ] `LobbyPage` (online users, quick match button)
  - [ ] `GamePage` (game board, player hands, scores)
  - [ ] `LeaderboardPage` (top 100 by wins)
- [ ] Implement game components:
  - [ ] `CardHand` (player's cards, selection)
  - [ ] `GameBoard` (play area, opponent cards)
  - [ ] `RoundTimer` (10-second countdown)
  - [ ] `ScoreDisplay` (current scores)

### **Game Flow Implementation**
- [ ] Login/Register flow
- [ ] Lobby (display online users)
- [ ] Quick match (find opponent)
- [ ] Game start (receive deck, display hand)
- [ ] Round play:
  - [ ] Card selection (click to select)
  - [ ] 10-second timer countdown
  - [ ] Auto-pick indicator (if timeout)
  - [ ] Card reveal animation
  - [ ] Round result display
- [ ] Game completion:
  - [ ] Final scores
  - [ ] Winner announcement
  - [ ] Statistics update
  - [ ] Return to lobby

### **Testing**
- [ ] Component unit tests (Vitest)
- [ ] Integration tests (React Testing Library)
- [ ] E2E tests (Cypress) - Login → Play → Complete
- [ ] Accessibility tests (axe-core)
- [ ] Mobile responsiveness tests

**Acceptance Criteria:**
✅ Frontend runs on localhost:5173  
✅ Can login and see lobby  
✅ Can start quick match  
✅ Can play 3 rounds with timeout  
✅ Leaderboard displays correctly  
✅ Responsive design works on mobile  

---

## 🧪 **PHASE 6: INTEGRATION TESTING** (Priority: 🟡 HIGH)

### **End-to-End Testing**
- [ ] Test complete flow: Register → Login → Match → Play → Complete
- [ ] Test timeout auto-pick (wait 10s without selecting card)
- [ ] Test player quit (disconnect mid-game)
- [ ] Test leaderboard update after game
- [ ] Test online status (heartbeat monitoring)

### **Load Testing**
- [ ] 20 concurrent users online
- [ ] 10 concurrent games in progress
- [ ] Database query performance (< 100ms)
- [ ] WebSocket message latency (< 50ms)
- [ ] Memory usage monitoring

### **Error Scenarios**
- [ ] Database connection lost
- [ ] Core server crash (Gateway handles gracefully)
- [ ] WebSocket disconnect (auto-reconnect)
- [ ] Invalid card selection (validation)
- [ ] Duplicate card selection (constraint enforcement)

**Acceptance Criteria:**
✅ All flows working end-to-end  
✅ System stable under 20 concurrent users  
✅ Proper error handling throughout  
✅ No data corruption in database  
✅ Logs provide sufficient debugging info  

---

## 📝 **PHASE 7: DOCUMENTATION & DEPLOYMENT** (Priority: 🟢 MEDIUM)

### **Code Documentation**
- [ ] Add JavaDoc comments to all public methods
- [ ] Add JSDoc comments to frontend functions
- [ ] Update README files with latest changes
- [ ] Document API endpoints (Gateway)
- [ ] Document WebSocket message format

### **Deployment Preparation**
- [ ] Create deployment guide
- [ ] Document environment variables
- [ ] Create database migration checklist
- [ ] Document server requirements
- [ ] Create backup/restore procedures

### **User Documentation**
- [ ] Game rules documentation
- [ ] User guide (how to play)
- [ ] FAQ section
- [ ] Troubleshooting guide

**Acceptance Criteria:**
✅ All code properly commented  
✅ README files up to date  
✅ Deployment guide complete  
✅ User documentation available  

---

## 🎯 **FINAL CHECKLIST**

### **Functional Requirements**
- [ ] **(A) Authentication**: Register, Login, Online status ✅
- [ ] **(B) Matchmaking**: Quick match working ✅
- [ ] **(C) Gameplay**: 3 rounds, 10s timeout, auto-pick ✅
- [ ] **(D) Completion**: Scores, winner, leaderboard ✅

### **Non-Functional Requirements**
- [ ] Performance: < 100ms database queries
- [ ] Scalability: 20+ concurrent users
- [ ] Security: Input validation (XSS prevention)
- [ ] Reliability: Graceful error handling
- [ ] Maintainability: Clean code, documentation

### **Database**
- [ ] Schema matches specification (6 ACTIVE tables)
- [ ] 36 cards seeded correctly
- [ ] Foreign keys enforced
- [ ] Indexes optimized
- [ ] Transactions properly scoped

### **Testing**
- [ ] Unit tests passing (90%+ coverage)
- [ ] Integration tests passing
- [ ] E2E tests passing
- [ ] Load tests successful
- [ ] No critical bugs

### **Documentation**
- [ ] Database schema documented
- [ ] API endpoints documented
- [ ] Code comments complete
- [ ] User guide written
- [ ] Deployment guide ready

---

## 🚀 **DEPLOYMENT READY CRITERIA**

### **Minimum Viable Product (MVP)**
✅ All core features (A-D) implemented  
✅ Database stable and performant  
✅ Frontend functional and responsive  
✅ Backend handles 20+ concurrent users  
✅ End-to-end testing passed  
✅ Documentation complete  
✅ No critical bugs  

### **Production Readiness (DEFERRED)**
🔶 Password hashing (BCrypt)  
🔶 Email verification  
🔶 ELO rating system  
🔶 Advanced leaderboards  
🔶 Audit logging  
🔶 SSL/TLS encryption  
🔶 CDN deployment  
🔶 Monitoring/alerting  

---

## 📊 **Progress Tracking**

| Phase | Status | Completion | Priority |
|-------|--------|------------|----------|
| Database Setup | ⏳ In Progress | 0% | 🔴 CRITICAL |
| Shared Module | ⏳ Not Started | 0% | 🟡 HIGH |
| Core Server | ⏳ Not Started | 0% | 🔴 CRITICAL |
| Gateway | ⏳ Not Started | 0% | 🟡 HIGH |
| Frontend | ⏳ Not Started | 0% | 🟡 HIGH |
| Integration Testing | ⏳ Not Started | 0% | 🟡 HIGH |
| Documentation | ⏳ Not Started | 0% | 🟢 MEDIUM |

**Overall Progress**: 0% (0/7 phases complete)

---

## 📞 **Team Responsibilities**

### **Database Team**
- Execute `V1__cardgame_mvp.sql`
- Create DAOs in Core module
- Performance optimization
- Migration scripts

### **Backend Team**
- Core server implementation
- Gateway WebSocket handler
- Message protocol implementation
- Unit/integration testing

### **Frontend Team**
- React components
- WebSocket integration
- State management
- UI/UX implementation

### **QA Team**
- Test plan creation
- Test execution
- Bug tracking
- Performance testing

---

**Note**: Update this checklist as you progress. Mark items as complete ✅ and track blockers.

**Last Updated**: 2025-01-05  
**Version**: 1.0.0 (MVP)
