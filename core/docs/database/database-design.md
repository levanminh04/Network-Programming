# Database Architecture - Enterprise Data Management

## 🎯 **DATABASE STRATEGY OVERVIEW**

Database Architecture được thiết kế để support high-performance game operations với ACID compliance, optimal concurrency, và comprehensive data integrity cho card game system.

### **Database Selection: MySQL 8.0+**

**Strategic Rationale:**
- **ACID Compliance**: Critical cho game consistency và fair play
- **High Concurrency**: InnoDB engine với row-level locking
- **Performance**: Optimized for read-heavy workloads
- **Reliability**: Proven stability in production environments
- **Team Familiarity**: Widespread knowledge và tooling support
- **Cost Effective**: Open source với enterprise features

### **Performance Requirements**
- **Read Operations**: < 5ms average response time
- **Write Operations**: < 10ms average response time  
- **Concurrent Users**: Support 100+ simultaneous connections
- **Transaction Throughput**: 1000+ transactions per second
- **Data Consistency**: Zero tolerance for data corruption

---

## 🏗️ **DATABASE SCHEMA DESIGN**

### **Core Tables Structure**

```sql
-- User Management Schema
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    status ENUM('ACTIVE', 'SUSPENDED', 'BANNED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_last_login (last_login)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Profiles and Statistics
CREATE TABLE user_profiles (
    user_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100),
    rank_tier ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') DEFAULT 'BRONZE',
    current_rating DECIMAL(10,2) DEFAULT 1000.00,
    peak_rating DECIMAL(10,2) DEFAULT 1000.00,
    total_score DECIMAL(12,2) DEFAULT 0.00,
    games_played INT DEFAULT 0,
    games_won INT DEFAULT 0,
    games_lost INT DEFAULT 0,
    games_drawn INT DEFAULT 0,
    win_streak_current INT DEFAULT 0,
    win_streak_best INT DEFAULT 0,
    total_playtime_minutes INT DEFAULT 0,
    achievements JSON,
    preferences JSON,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_rating (current_rating),
    INDEX idx_rank (rank_tier),
    INDEX idx_games_played (games_played),
    INDEX idx_win_rate ((games_won / NULLIF(games_played, 0)))
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Game Sessions Management
CREATE TABLE games (
    id VARCHAR(50) PRIMARY KEY,
    player1_id VARCHAR(50) NOT NULL,
    player2_id VARCHAR(50) NOT NULL,
    game_mode ENUM('QUICK', 'RANKED', 'CUSTOM', 'TOURNAMENT') NOT NULL,
    status ENUM('WAITING_TO_START', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED', 'CANCELLED') DEFAULT 'WAITING_TO_START',
    winner_id VARCHAR(50) NULL,
    player1_final_score INT DEFAULT 0,
    player2_final_score INT DEFAULT 0,
    total_rounds INT DEFAULT 3,
    completed_rounds INT DEFAULT 0,
    game_seed BIGINT, -- For reproducible random deck shuffling
    
    -- Timing information
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_seconds INT NULL,
    
    -- Rating changes for ranked games
    player1_rating_before DECIMAL(10,2),
    player1_rating_after DECIMAL(10,2),
    player2_rating_before DECIMAL(10,2),
    player2_rating_after DECIMAL(10,2),
    
    -- Additional metadata
    server_instance VARCHAR(50),
    game_data JSON, -- Store additional game state if needed
    
    FOREIGN KEY (player1_id) REFERENCES users(id),
    FOREIGN KEY (player2_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id),
    
    INDEX idx_players (player1_id, player2_id),
    INDEX idx_status (status),
    INDEX idx_game_mode (game_mode),
    INDEX idx_created_at (created_at),
    INDEX idx_completed_at (completed_at),
    INDEX idx_winner (winner_id),
    INDEX idx_player1_games (player1_id, created_at),
    INDEX idx_player2_games (player2_id, created_at)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Game Rounds and Moves
CREATE TABLE game_rounds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    round_number INT NOT NULL,
    
    -- Player moves
    player1_card_index INT,
    player1_card_rank VARCHAR(3),
    player1_card_suit ENUM('HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES'),
    player1_card_value INT,
    
    player2_card_index INT,
    player2_card_rank VARCHAR(3), 
    player2_card_suit ENUM('HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES'),
    player2_card_value INT,
    
    -- Round results
    round_winner_id VARCHAR(50),
    player1_round_score INT DEFAULT 0,
    player2_round_score INT DEFAULT 0,
    
    -- Timing
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    player1_moved_at TIMESTAMP NULL,
    player2_moved_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    round_duration_ms INT,
    
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (round_winner_id) REFERENCES users(id),
    
    UNIQUE KEY uk_game_round (game_id, round_number),
    INDEX idx_game_rounds (game_id, round_number),
    INDEX idx_round_winner (round_winner_id),
    INDEX idx_completed_at (completed_at)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Statistics and Analytics
CREATE TABLE user_statistics (
    user_id VARCHAR(50) PRIMARY KEY,
    
    -- Daily statistics
    games_today INT DEFAULT 0,
    wins_today INT DEFAULT 0,
    playtime_today_minutes INT DEFAULT 0,
    last_daily_reset DATE DEFAULT (CURRENT_DATE),
    
    -- Weekly statistics  
    games_this_week INT DEFAULT 0,
    wins_this_week INT DEFAULT 0,
    playtime_this_week_minutes INT DEFAULT 0,
    last_weekly_reset DATE DEFAULT (CURRENT_DATE),
    
    -- Monthly statistics
    games_this_month INT DEFAULT 0,
    wins_this_month INT DEFAULT 0,
    playtime_this_month_minutes INT DEFAULT 0,
    last_monthly_reset DATE DEFAULT (CURRENT_DATE),
    
    -- Performance metrics
    average_game_duration_seconds DECIMAL(8,2),
    average_move_time_seconds DECIMAL(6,2),
    fastest_game_seconds INT,
    longest_game_seconds INT,
    
    -- Streak information
    current_win_streak INT DEFAULT 0,
    current_loss_streak INT DEFAULT 0,
    best_win_streak INT DEFAULT 0,
    worst_loss_streak INT DEFAULT 0,
    
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_games_today (games_today),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Leaderboards and Rankings
CREATE TABLE leaderboards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    rank_position INT NOT NULL,
    rating DECIMAL(10,2) NOT NULL,
    games_played INT NOT NULL,
    win_rate DECIMAL(5,4) NOT NULL, -- Stored as decimal for precise calculations
    
    -- Leaderboard type and period
    leaderboard_type ENUM('GLOBAL', 'DAILY', 'WEEKLY', 'MONTHLY', 'SEASONAL') NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    -- Additional ranking factors
    tier_rank INT, -- Rank within tier (Bronze 1, Bronze 2, etc.)
    points_to_next_tier INT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_user_leaderboard_period (user_id, leaderboard_type, period_start),
    INDEX idx_leaderboard_ranking (leaderboard_type, period_start, rank_position),
    INDEX idx_rating (rating DESC),
    INDEX idx_user_rankings (user_id, leaderboard_type)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Active Game Sessions (In-Memory Cache Table)
CREATE TABLE active_sessions (
    session_id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    game_id VARCHAR(50),
    connection_id VARCHAR(100),
    status ENUM('CONNECTED', 'IN_LOBBY', 'IN_GAME', 'DISCONNECTED') DEFAULT 'CONNECTED',
    server_instance VARCHAR(50),
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    connection_data JSON,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE SET NULL,
    
    INDEX idx_user_session (user_id),
    INDEX idx_game_session (game_id),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat),
    INDEX idx_server_instance (server_instance)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit and Security Logs
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50), -- 'USER', 'GAME', 'SESSION'
    entity_id VARCHAR(50),
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45), -- IPv6 compatible
    user_agent TEXT,
    session_id VARCHAR(100),
    server_instance VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_user_audit (user_id, created_at),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at),
    INDEX idx_session_audit (session_id)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- System Configuration
CREATE TABLE system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    value_type ENUM('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON') DEFAULT 'STRING',
    description TEXT,
    is_sensitive BOOLEAN DEFAULT FALSE,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## ⚙️ **DATABASE CONFIGURATION**

### **1. Connection Pool Configuration**

```yaml
# database.yml
database:
  primary:
    url: jdbc:mysql://localhost:3306/cardgame_db?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:cardgame_user}
    password: ${DB_PASSWORD:secure_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  # Connection Pool Settings (HikariCP)
  hikari:
    pool-name: CardGamePool
    maximum-pool-size: 20
    minimum-idle: 5
    connection-timeout: 20000      # 20 seconds
    idle-timeout: 300000          # 5 minutes
    max-lifetime: 1200000         # 20 minutes
    leak-detection-threshold: 60000 # 1 minute
    
  # Performance Optimizations
  properties:
    cachePrepStmts: true
    prepStmtCacheSize: 300
    prepStmtCacheSqlLimit: 2048
    useServerPrepStmts: true
    useLocalSessionState: true
    rewriteBatchedStatements: true
    cacheResultSetMetadata: true
    cacheServerConfiguration: true
    elideSetAutoCommits: true
    maintainTimeStats: false
    
# JPA/Hibernate Configuration
jpa:
  hibernate:
    ddl-auto: validate  # Use 'create-drop' for development, 'validate' for production
    naming:
      physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
  properties:
    hibernate:
      dialect: org.hibernate.dialect.MySQL8Dialect
      show_sql: false
      format_sql: false
      use_sql_comments: false
      
      # Performance optimizations
      jdbc:
        batch_size: 25
        batch_versioned_data: true
        order_inserts: true
        order_updates: true
      
      # Second-level cache
      cache:
        use_second_level_cache: true
        use_query_cache: true
        region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        
      # Connection handling
      connection:
        provider_disables_autocommit: true
        autocommit: false
        
      # Statistics for monitoring
      generate_statistics: true
```

### **2. Database Initialization Scripts**

```sql
-- schema.sql - Database Initialization
CREATE DATABASE IF NOT EXISTS cardgame_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE cardgame_db;

-- Create database user with proper permissions
CREATE USER IF NOT EXISTS 'cardgame_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON cardgame_db.* TO 'cardgame_user'@'localhost';
GRANT EXECUTE ON cardgame_db.* TO 'cardgame_user'@'localhost';
FLUSH PRIVILEGES;

-- Enable performance optimizations
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB buffer pool
SET GLOBAL innodb_log_file_size = 268435456;     -- 256MB log files
SET GLOBAL innodb_flush_log_at_trx_commit = 2;   -- Performance optimization
SET GLOBAL innodb_file_per_table = 1;            -- Separate file per table
SET GLOBAL query_cache_size = 134217728;         -- 128MB query cache
```

```sql
-- initial-data.sql - Seed Data
INSERT INTO system_config (config_key, config_value, value_type, description) VALUES
('game.max_rounds', '3', 'INTEGER', 'Maximum rounds per game'),
('game.round_timeout_seconds', '30', 'INTEGER', 'Timeout for each round'),
('game.game_timeout_minutes', '30', 'INTEGER', 'Maximum game duration'),
('matchmaking.rating_range', '200', 'INTEGER', 'Rating difference for matchmaking'),
('server.max_concurrent_games', '100', 'INTEGER', 'Maximum concurrent games'),
('performance.enable_query_cache', 'true', 'BOOLEAN', 'Enable MySQL query cache'),
('security.max_login_attempts', '5', 'INTEGER', 'Maximum failed login attempts'),
('security.lockout_duration_minutes', '15', 'INTEGER', 'Account lockout duration');

-- Create initial rank tiers with thresholds
INSERT INTO system_config (config_key, config_value, value_type, description) VALUES
('ranking.bronze_threshold', '0', 'INTEGER', 'Minimum rating for Bronze tier'),
('ranking.silver_threshold', '1200', 'INTEGER', 'Minimum rating for Silver tier'),
('ranking.gold_threshold', '1500', 'INTEGER', 'Minimum rating for Gold tier'),
('ranking.platinum_threshold', '1800', 'INTEGER', 'Minimum rating for Platinum tier'),
('ranking.diamond_threshold', '2100', 'INTEGER', 'Minimum rating for Diamond tier');
```

---

## 🔄 **REPOSITORY PATTERN IMPLEMENTATION**

### **1. Base Repository Interface**

```java
package com.n9.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

/**
 * Base repository interface with common CRUD operations.
 * Provides consistent API across all repository implementations.
 */
public interface BaseRepository<T, ID> {
    
    T save(T entity);
    List<T> saveAll(Iterable<T> entities);
    
    Optional<T> findById(ID id);
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    
    boolean existsById(ID id);
    long count();
    
    void deleteById(ID id);
    void delete(T entity);
    void deleteAll();
    
    // Batch operations for performance
    void flush();
    T saveAndFlush(T entity);
    void deleteInBatch(Iterable<T> entities);
}
```

### **2. User Repository Implementation**

```java
package com.n9.core.repository;

import com.n9.core.model.entity.User;
import com.n9.core.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity with optimized queries for game operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // Authentication queries
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.status = 'ACTIVE'")
    Optional<User> findActiveUserByUsername(@Param("username") String username);
    
    // Security queries
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void updateLoginAttempts(@Param("userId") String userId, 
                           @Param("attempts") int attempts, 
                           @Param("lockedUntil") LocalDateTime lockedUntil);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId);
    
    // Performance optimized queries
    @Query("SELECT u.id, u.username FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.username")
    List<Object[]> findActiveUsersSummary();
    
    // Statistics queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUsersCreatedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin >= :since")
    long countActiveUsersSince(@Param("since") LocalDateTime since);
}
```

### **3. Game Repository Implementation**

```java
package com.n9.core.repository;

import com.n9.core.model.entity.Game;
import com.n9.shared.model.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Game entity with performance-optimized game queries.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, String> {
    
    // Active game queries
    @Query("SELECT g FROM Game g WHERE g.status IN ('WAITING_TO_START', 'IN_PROGRESS')")
    List<Game> findActiveGames();
    
    @Query("SELECT g FROM Game g WHERE (g.player1Id = :playerId OR g.player2Id = :playerId) " +
           "AND g.status IN ('WAITING_TO_START', 'IN_PROGRESS')")
    List<Game> findActiveGamesByPlayer(@Param("playerId") String playerId);
    
    // Player history queries with pagination
    @Query("SELECT g FROM Game g WHERE (g.player1Id = :playerId OR g.player2Id = :playerId) " +
           "AND g.status = 'COMPLETED' ORDER BY g.completedAt DESC")
    List<Game> findPlayerGameHistory(@Param("playerId") String playerId);
    
    // Statistics queries
    @Query("SELECT COUNT(g) FROM Game g WHERE g.status = 'COMPLETED'")
    long getTotalCompletedGames();
    
    @Query("SELECT AVG(g.durationSeconds) FROM Game g WHERE g.status = 'COMPLETED' AND g.durationSeconds > 0")
    Double getAverageGameDuration();
    
    @Query("SELECT COUNT(g) FROM Game g WHERE g.status = 'COMPLETED' AND DATE(g.completedAt) = CURRENT_DATE")
    long getGamesCompletedToday();
    
    // Player statistics
    @Query("SELECT COUNT(g) FROM Game g WHERE g.winnerId = :playerId")
    long getPlayerWinCount(@Param("playerId") String playerId);
    
    @Query("SELECT COUNT(g) FROM Game g WHERE (g.player1Id = :playerId OR g.player2Id = :playerId) AND g.status = 'COMPLETED'")
    long getPlayerTotalGames(@Param("playerId") String playerId);
    
    // Cleanup queries
    @Modifying
    @Query("UPDATE Game g SET g.status = 'ABANDONED' WHERE g.status IN ('WAITING_TO_START', 'IN_PROGRESS') " +
           "AND g.createdAt < :cutoffTime")
    int markAbandonedGames(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Modifying
    @Query("DELETE FROM Game g WHERE g.status = 'ABANDONED' AND g.createdAt < :cutoffTime")
    int deleteOldAbandonedGames(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Performance monitoring
    @Query("SELECT g.gameMode, COUNT(g), AVG(g.durationSeconds) FROM Game g " +
           "WHERE g.status = 'COMPLETED' AND g.completedAt >= :since " +
           "GROUP BY g.gameMode")
    List<Object[]> getGameModeStatistics(@Param("since") LocalDateTime since);
}
```

---

## 🔒 **TRANSACTION MANAGEMENT & CONCURRENCY**

### **1. Transaction Configuration**

```java
package com.n9.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManagerFactory;

/**
 * Transaction management configuration for optimal database performance.
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        transactionManager.setDefaultTimeout(30); // 30 seconds default timeout
        return transactionManager;
    }
    
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setTimeout(30);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return template;
    }
}
```

### **2. Service Layer with Transaction Management**

```java
package com.n9.core.service;

import com.n9.core.model.entity.Game;
import com.n9.core.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

/**
 * Game service with proper transaction management and concurrency handling.
 */
@Service
@Transactional(readOnly = true) // Default to read-only transactions
public class GameService {
    
    private final GameRepository gameRepository;
    private final UserService userService;
    private final StatisticsService statisticsService;
    
    public GameService(GameRepository gameRepository,
                      UserService userService,
                      StatisticsService statisticsService) {
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.statisticsService = statisticsService;
    }
    
    /**
     * Create new game with proper transaction boundaries.
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        timeout = 10
    )
    public Game createGame(String player1Id, String player2Id, String gameMode) {
        // Validate players exist and are available
        userService.validateUserAvailable(player1Id);
        userService.validateUserAvailable(player2Id);
        
        // Create game entity
        Game game = Game.builder()
            .id(generateGameId())
            .player1Id(player1Id)
            .player2Id(player2Id)
            .gameMode(gameMode)
            .status(GameStatus.WAITING_TO_START)
            .createdAt(System.currentTimeMillis())
            .build();
        
        // Save game
        game = gameRepository.save(game);
        
        // Update user statistics
        statisticsService.incrementGamesStarted(player1Id);
        statisticsService.incrementGamesStarted(player2Id);
        
        return game;
    }
    
    /**
     * Complete game with atomic statistics update.
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        timeout = 15
    )
    public void completeGame(String gameId, String winnerId, 
                           Map<String, Integer> finalScores, long durationMs) {
        // Update game record
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
        
        game.setStatus(GameStatus.COMPLETED);
        game.setWinnerId(winnerId);
        game.setCompletedAt(System.currentTimeMillis());
        game.setDurationSeconds((int) (durationMs / 1000));
        
        gameRepository.save(game);
        
        // Update player statistics atomically
        String player1Id = game.getPlayer1Id();
        String player2Id = game.getPlayer2Id();
        
        if (winnerId.equals(player1Id)) {
            statisticsService.recordWin(player1Id);
            statisticsService.recordLoss(player2Id);
        } else if (winnerId.equals(player2Id)) {
            statisticsService.recordWin(player2Id);
            statisticsService.recordLoss(player1Id);
        } else {
            // Draw
            statisticsService.recordDraw(player1Id);
            statisticsService.recordDraw(player2Id);
        }
        
        // Update ratings for ranked games
        if ("RANKED".equals(game.getGameMode())) {
            userService.updateRatings(player1Id, player2Id, winnerId);
        }
    }
    
    /**
     * Cleanup abandoned games with batch processing.
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        timeout = 60
    )
    public int cleanupAbandonedGames(int hoursOld) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursOld);
        return gameRepository.markAbandonedGames(cutoffTime);
    }
}
```

---

## 📊 **DATABASE PERFORMANCE OPTIMIZATION**

### **1. Index Strategy**

```sql
-- Critical performance indexes
CREATE INDEX idx_games_player_status ON games(player1_id, player2_id, status);
CREATE INDEX idx_games_active ON games(status, created_at) WHERE status IN ('WAITING_TO_START', 'IN_PROGRESS');
CREATE INDEX idx_user_profiles_rating ON user_profiles(current_rating DESC, rank_tier);
CREATE INDEX idx_game_rounds_game_round ON game_rounds(game_id, round_number);
CREATE INDEX idx_audit_logs_user_date ON audit_logs(user_id, created_at);

-- Composite indexes for common queries
CREATE INDEX idx_games_completed_today ON games(status, completed_at) 
    WHERE status = 'COMPLETED' AND DATE(completed_at) = CURRENT_DATE;

-- Covering indexes for frequent queries
CREATE INDEX idx_user_leaderboard_cover ON user_profiles(rank_tier, current_rating DESC) 
    INCLUDE (user_id, display_name, games_played, games_won);
```

### **2. Query Optimization Examples**

```java
/**
 * Optimized queries with proper indexing and result limiting.
 */
@Repository
public class OptimizedGameRepository {
    
    @Query(value = """
        SELECT g.id, g.player1_id, g.player2_id, g.status, g.created_at
        FROM games g USE INDEX (idx_games_active)
        WHERE g.status IN ('WAITING_TO_START', 'IN_PROGRESS')
        AND g.created_at > :since
        ORDER BY g.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecentActiveGames(@Param("since") Timestamp since, 
                                       @Param("limit") int limit);
    
    @Query(value = """
        SELECT up.user_id, up.display_name, up.current_rating, up.games_won, up.games_played
        FROM user_profiles up USE INDEX (idx_user_leaderboard_cover)
        WHERE up.rank_tier = :tier
        ORDER BY up.current_rating DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopPlayersByTier(@Param("tier") String tier, 
                                      @Param("limit") int limit);
}
```

---

## 🧪 **DATABASE TESTING STRATEGY**

### **1. Repository Testing**

```java
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GameRepositoryTest {
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void testFindActiveGamesByPlayer() {
        // Given
        String playerId = "test-player-1";
        Game activeGame = createTestGame(playerId, "test-player-2", GameStatus.IN_PROGRESS);
        Game completedGame = createTestGame(playerId, "test-player-3", GameStatus.COMPLETED);
        
        entityManager.persistAndFlush(activeGame);
        entityManager.persistAndFlush(completedGame);
        
        // When
        List<Game> activeGames = gameRepository.findActiveGamesByPlayer(playerId);
        
        // Then
        assertThat(activeGames).hasSize(1);
        assertThat(activeGames.get(0).getId()).isEqualTo(activeGame.getId());
    }
    
    @Test
    void testGameStatisticsQuery() {
        // Create test data
        createMultipleTestGames();
        
        // Test statistics query
        long totalGames = gameRepository.getTotalCompletedGames();
        Double avgDuration = gameRepository.getAverageGameDuration();
        
        assertThat(totalGames).isGreaterThan(0);
        assertThat(avgDuration).isNotNull();
    }
}
```

### **2. Performance Testing**

```java
@Test
@Sql("/test-data/large-dataset.sql")
void testQueryPerformanceWithLargeDataset() {
    StopWatch stopWatch = new StopWatch();
    
    // Test critical query performance
    stopWatch.start();
    List<Game> activeGames = gameRepository.findActiveGames();
    stopWatch.stop();
    
    // Verify performance requirements
    assertThat(stopWatch.getLastTaskTimeMillis()).isLessThan(100); // < 100ms
    assertThat(activeGames).isNotEmpty();
}
```

---

Đây là comprehensive database architecture. Bạn có muốn tôi tiếp tục với **Threading Model** hoặc **Integration Testing Strategy** không?