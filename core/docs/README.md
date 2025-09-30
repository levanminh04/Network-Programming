# Core Module - Strategic Architecture & Implementation Guide

## 🎯 **EXECUTIVE SUMMARY**

Core Module là **trái tim xử lý business logic** của hệ thống Card Game, chịu tr책任 xử lý:
- **Game Logic Engine**: Xử lý toàn bộ quy tắc game, logic rút bài, tính điểm
- **TCP Socket Server**: Giao tiếp với Gateway qua TCP protocol 
- **Multi-threading Architecture**: Xử lý concurrent users và game sessions
- **Database Management**: Persistent storage cho user data, game history, statistics
- **Session Management**: Quản lý game rooms, player sessions, matchmaking

### **Strategic Importance**
- **Performance Critical**: Xử lý real-time game logic với latency < 50ms
- **Scalability Target**: Support 100+ concurrent players
- **Reliability**: 99.9% uptime trong môi trường production
- **Maintainability**: Modular design cho easy extension và debugging

---

## 🏗️ **RECOMMENDED MODULE STRUCTURE**

```
core/
├── pom.xml                          # Maven configuration
├── docs/                            # Comprehensive documentation
│   ├── README.md                    # Quick start guide
│   ├── architecture/
│   │   ├── system-design.md         # High-level architecture
│   │   ├── threading-model.md       # Concurrency design
│   │   ├── database-design.md       # Data architecture
│   │   └── performance-tuning.md    # Optimization guide
│   ├── implementation/
│   │   ├── tcp-server-guide.md      # Socket implementation
│   │   ├── game-logic-guide.md      # Business logic
│   │   ├── testing-strategy.md      # QA approach
│   │   └── deployment-guide.md      # Production deployment
│   └── database/
│       ├── schema.sql               # Database schema
│       ├── initial-data.sql         # Seed data
│       └── migration-scripts/       # Version migrations
├── src/
│   ├── main/
│   │   ├── java/com/n9/core/
│   │   │   ├── CoreServerApplication.java    # Main application entry
│   │   │   ├── config/                       # Configuration classes
│   │   │   │   ├── DatabaseConfig.java       # DB connection config
│   │   │   │   ├── ThreadPoolConfig.java     # Threading configuration
│   │   │   │   ├── SocketConfig.java         # TCP server config
│   │   │   │   └── GameConfig.java           # Game rules config
│   │   │   ├── network/                      # TCP Socket Management
│   │   │   │   ├── TcpServer.java            # Main TCP server
│   │   │   │   ├── ClientConnectionHandler.java  # Per-client handler
│   │   │   │   ├── MessageProcessor.java     # Protocol message processing
│   │   │   │   ├── ConnectionManager.java    # Connection lifecycle
│   │   │   │   └── protocol/                 # Protocol implementations
│   │   │   │       ├── TcpMessageHandler.java
│   │   │   │       └── ProtocolValidator.java
│   │   │   ├── game/                         # Game Logic Engine
│   │   │   │   ├── GameEngine.java           # Core game logic
│   │   │   │   ├── GameSession.java          # Individual game session
│   │   │   │   ├── GameRoom.java             # Multi-player room
│   │   │   │   ├── CardDeck.java             # Deck management
│   │   │   │   ├── GameRules.java            # Rule engine
│   │   │   │   ├── ScoreCalculator.java      # Scoring logic
│   │   │   │   └── matchmaking/              # Matchmaking system
│   │   │   │       ├── MatchmakingService.java
│   │   │   │       ├── PlayerQueue.java
│   │   │   │       └── MatchmakingStrategy.java
│   │   │   ├── service/                      # Business Services
│   │   │   │   ├── UserService.java          # User management
│   │   │   │   ├── GameHistoryService.java   # Game records
│   │   │   │   ├── StatisticsService.java    # Player statistics
│   │   │   │   ├── RankingService.java       # Leaderboard system
│   │   │   │   └── NotificationService.java  # Event notifications
│   │   │   ├── repository/                   # Data Access Layer
│   │   │   │   ├── UserRepository.java       # User data access
│   │   │   │   ├── GameRepository.java       # Game data access
│   │   │   │   ├── StatisticsRepository.java # Stats data access
│   │   │   │   └── impl/                     # Implementation classes
│   │   │   ├── model/                        # Domain Models
│   │   │   │   ├── entity/                   # JPA entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Game.java
│   │   │   │   │   ├── GameMove.java
│   │   │   │   │   └── UserStatistics.java
│   │   │   │   ├── dto/                      # Internal DTOs
│   │   │   │   └── enums/                    # Core enumerations
│   │   │   ├── thread/                       # Threading Framework
│   │   │   │   ├── GameThreadPool.java       # Game-specific thread pool
│   │   │   │   ├── ConnectionThreadPool.java # Connection handling threads
│   │   │   │   ├── TaskExecutor.java         # Custom task execution
│   │   │   │   └── ThreadSafetyUtils.java    # Concurrency utilities
│   │   │   ├── util/                         # Core Utilities
│   │   │   │   ├── DatabaseUtils.java        # DB utility functions
│   │   │   │   ├── NetworkUtils.java         # Network utilities
│   │   │   │   ├── GameUtils.java            # Game-specific utilities
│   │   │   │   └── PerformanceMonitor.java   # Performance tracking
│   │   │   └── exception/                    # Exception Handling
│   │   │       ├── GameException.java        # Game-specific exceptions
│   │   │       ├── NetworkException.java     # Network exceptions
│   │   │       ├── DatabaseException.java    # Database exceptions
│   │   │       └── handlers/                 # Global exception handlers
│   │   └── resources/
│   │       ├── application.yml               # Main configuration
│   │       ├── database.yml                  # Database configuration
│   │       ├── game-rules.yml               # Game configuration
│   │       ├── logback-spring.xml           # Logging configuration
│   │       └── sql/                         # SQL scripts
│   │           ├── schema.sql
│   │           └── data.sql
│   └── test/
│       ├── java/com/n9/core/
│       │   ├── integration/                  # Integration tests
│       │   │   ├── TcpServerIntegrationTest.java
│       │   │   ├── GameFlowIntegrationTest.java
│       │   │   └── DatabaseIntegrationTest.java
│       │   ├── unit/                         # Unit tests
│       │   │   ├── game/                     # Game logic tests
│       │   │   ├── network/                  # Network tests
│       │   │   ├── service/                  # Service tests
│       │   │   └── repository/               # Repository tests
│       │   ├── performance/                  # Performance tests
│       │   │   ├── LoadTest.java
│       │   │   ├── ConcurrencyTest.java
│       │   │   └── StressTest.java
│       │   └── util/                         # Test utilities
│       │       ├── TestDataBuilder.java
│       │       ├── MockGameSession.java
│       │       └── IntegrationTestBase.java
│       └── resources/
│           ├── test-application.yml          # Test configuration
│           └── test-data/                    # Test data files
├── scripts/                                  # Utility scripts
│   ├── start-server.sh                       # Server startup script
│   ├── stop-server.sh                        # Server shutdown script
│   ├── database-setup.sh                     # Database initialization
│   └── performance-test.sh                   # Performance testing
└── target/                                   # Maven build output
    ├── core-0.1.0-SNAPSHOT.jar
    └── classes/
```

### **Architecture Principles**

1. **Separation of Concerns**: Rõ ràng phân chia giữa network, game logic, data access
2. **Dependency Injection**: Spring-based DI cho loose coupling
3. **Thread Safety**: Concurrent-safe design patterns
4. **Performance First**: Optimized for low latency và high throughput
5. **Testability**: Comprehensive testing strategy
6. **Monitoring**: Built-in performance và health monitoring

---

## 🎯 **STRATEGIC IMPLEMENTATION PHASES**

### **Phase 1: Foundation Setup (Week 1)**
- [x] Project structure creation
- [ ] Maven dependencies configuration  
- [ ] Database schema design
- [ ] Basic TCP server skeleton
- [ ] Logging framework setup

### **Phase 2: Core Infrastructure (Week 2)**
- [ ] TCP server implementation
- [ ] Connection management
- [ ] Message protocol handling
- [ ] Basic threading model
- [ ] Database connectivity

### **Phase 3: Game Logic Engine (Week 3)**
- [ ] Card deck implementation
- [ ] Game rules engine
- [ ] Game session management
- [ ] Score calculation
- [ ] Basic game flow

### **Phase 4: Advanced Features (Week 4)**
- [ ] Matchmaking system
- [ ] User statistics
- [ ] Performance optimization
- [ ] Comprehensive testing
- [ ] Gateway integration

### **Phase 5: Production Readiness (Week 5)**
- [ ] Load testing
- [ ] Security hardening
- [ ] Monitoring setup
- [ ] Documentation completion
- [ ] Deployment automation

---

## 🎮 **GAME LOGIC ARCHITECTURE**

### **Core Game Components**

1. **Game Engine**: Central coordinator cho tất cả game logic
2. **Game Session**: Individual game instance between players
3. **Card Deck**: 52-card deck management với shuffle và distribution
4. **Game Rules**: Configurable rule engine
5. **Score Calculator**: Tính toán điểm số và determine winners
6. **Matchmaking**: Pair players và create game sessions

### **Game Flow Design**

```
Player Join → Matchmaking Queue → Game Session Created → 
Card Distribution → Round 1-3 → Score Calculation → 
Game End → Statistics Update → Cleanup
```

---

## 🌐 **TCP SERVER ARCHITECTURE**

### **Network Layer Design**

1. **TCP Server**: Main server socket accepting connections
2. **Connection Handler**: Per-connection processing thread
3. **Message Processor**: Protocol message parsing và routing
4. **Connection Manager**: Lifecycle management of client connections
5. **Protocol Validator**: Message validation và security checks

### **Communication Protocol**

- **Transport**: TCP với length-prefixed JSON messages
- **Message Format**: Shared MessageEnvelope từ shared module
- **Connection Lifecycle**: Connect → Authenticate → Game Operations → Disconnect
- **Error Handling**: Comprehensive error response với retry logic

---

## 🔄 **THREADING MODEL**

### **Thread Pool Strategy**

1. **Connection Pool**: Handle incoming connections (Fixed size: 20 threads)
2. **Game Processing Pool**: Process game logic (Core count * 2 threads)
3. **Database Pool**: Handle database operations (10 threads)
4. **Notification Pool**: Send notifications (5 threads)

### **Concurrency Considerations**

- **Thread-Safe Collections**: ConcurrentHashMap cho session storage
- **Atomic Operations**: AtomicInteger cho counters
- **Synchronized Blocks**: Minimal locking cho critical sections
- **Lock-Free Design**: Prefer immutable objects và message passing

---

## 💾 **DATABASE ARCHITECTURE**

### **Database Selection: MySQL**

**Rationale**: 
- ACID compliance cho game consistency
- Excellent concurrency support
- Rich ecosystem và tools
- Team familiarity

### **Core Tables Design**

```sql
-- User Management
users (id, username, password_hash, email, created_at, last_login)
user_profiles (user_id, display_name, rank, total_score, games_played, games_won)

-- Game Management  
games (id, player1_id, player2_id, game_mode, status, created_at, completed_at)
game_rounds (game_id, round_number, player1_card, player2_card, winner, round_score)
game_results (game_id, winner_id, final_score_p1, final_score_p2, duration)

-- Statistics & Rankings
user_statistics (user_id, daily_games, weekly_games, best_streak, avg_score)
leaderboards (user_id, rank, score, period_type, period_start)

-- System Tables
game_sessions (session_id, user_id, status, created_at, last_activity)
audit_logs (id, user_id, action, details, timestamp)
```

---

## 🚀 **DETAILED IMPLEMENTATION ROADMAP**

Tôi sẽ tạo detailed documentation cho từng component. Bạn có muốn tôi bắt đầu với component nào cụ thể?

**Recommended Starting Order:**
1. **TCP Server Setup** - Network foundation
2. **Database Schema** - Data foundation  
3. **Game Logic Engine** - Business logic core
4. **Threading Model** - Concurrency framework
5. **Integration Testing** - Quality assurance

**Immediate Next Steps:**
- Setup project dependencies trong pom.xml
- Create database schema và connection
- Implement basic TCP server
- Design game session management

Bạn muốn tôi deep-dive vào component nào trước? Tôi khuyến nghị bắt đầu với **TCP Server implementation** vì đây là foundation cho tất cả communications.