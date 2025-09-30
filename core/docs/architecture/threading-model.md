# Threading Model - High-Performance Concurrency Architecture

## 🎯 **THREADING STRATEGY OVERVIEW**

Threading Model được thiết kế để maximize performance, ensure thread safety, và handle high concurrency với optimal resource utilization cho real-time card game operations.

### **Concurrency Requirements**
- **100+ Concurrent Players**: Support simultaneous connections
- **50+ Parallel Games**: Multiple game sessions running concurrently
- **< 50ms Latency**: Real-time game response requirements
- **Thread Safety**: Zero race conditions và data corruption
- **Resource Efficiency**: Optimal CPU và memory utilization
- **Graceful Degradation**: Performance under high load

### **Architecture Principles**
- **Thread Pool Management**: Controlled thread creation và lifecycle
- **Lock-Free Design**: Minimize blocking operations
- **Message Passing**: Prefer immutable messages over shared state
- **Async Processing**: Non-blocking operations where possible
- **Resource Isolation**: Separate thread pools for different concerns

---

## 🏗️ **THREAD POOL ARCHITECTURE**

### **1. Multi-Tier Thread Pool Design**

```java
package com.n9.core.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * Comprehensive thread pool configuration for different workload types.
 * Each pool is optimized for specific use cases and performance characteristics.
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {
    
    /**
     * Connection Handler Pool - TCP connection processing
     * Characteristics: I/O bound, short-lived tasks, high throughput
     */
    @Bean("connectionPool")
    public ThreadPoolTaskExecutor connectionPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);          // Always-alive threads
        executor.setMaxPoolSize(50);           // Max during peak load
        executor.setQueueCapacity(100);        // Request queue size
        executor.setKeepAliveSeconds(60);      // Idle thread timeout
        executor.setThreadNamePrefix("conn-"); // Thread naming
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
    
    /**
     * Game Processing Pool - Core game logic execution
     * Characteristics: CPU bound, medium duration, critical latency
     */
    @Bean("gamePool")
    public ThreadPoolTaskExecutor gamePool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int coreCount = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(coreCount);           // CPU core count
        executor.setMaxPoolSize(coreCount * 2);        // 2x CPU cores
        executor.setQueueCapacity(200);                // Larger queue for game operations
        executor.setKeepAliveSeconds(120);             // Longer keep-alive
        executor.setThreadNamePrefix("game-");
        executor.setRejectedExecutionHandler(new GameRejectionHandler()); // Custom handler
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(45);
        return executor;
    }
    
    /**
     * Database Pool - Database operations
     * Characteristics: I/O bound, variable duration, transaction management
     */
    @Bean("databasePool")
    public ThreadPoolTaskExecutor databasePool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);          // Conservative for DB connections
        executor.setMaxPoolSize(25);           // Align with DB connection pool
        executor.setQueueCapacity(150);        // Handle DB operation bursts
        executor.setKeepAliveSeconds(300);     // 5-minute keep-alive
        executor.setThreadNamePrefix("db-");
        executor.setRejectedExecutionHandler(new DatabaseRejectionHandler());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }
    
    /**
     * Notification Pool - Event broadcasting and notifications
     * Characteristics: I/O bound, fire-and-forget, non-critical
     */
    @Bean("notificationPool")
    public ThreadPoolTaskExecutor notificationPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // Small core pool
        executor.setMaxPoolSize(15);           // Moderate max
        executor.setQueueCapacity(500);        // Large queue for notifications
        executor.setKeepAliveSeconds(180);     // 3-minute keep-alive
        executor.setThreadNamePrefix("notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false); // Don't wait for notifications
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
    
    /**
     * Scheduled Tasks Pool - Periodic maintenance and cleanup
     * Characteristics: Low frequency, system maintenance, background tasks
     */
    @Bean("scheduledPool")
    public ScheduledThreadPoolExecutor scheduledPool() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            3, // Small pool for scheduled tasks
            r -> new Thread(r, "scheduled-" + System.currentTimeMillis())
        );
        executor.setKeepAliveTime(5, TimeUnit.MINUTES);
        executor.allowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ScheduledThreadPoolExecutor.DiscardPolicy());
        return executor;
    }
    
    /**
     * Custom rejection handler for game operations.
     * Implements graceful degradation under extreme load.
     */
    public static class GameRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                // Log the rejection and attempt graceful handling
                logger.warn("Game task rejected due to pool saturation. Active: {}, Queue: {}", 
                           executor.getActiveCount(), executor.getQueue().size());
                
                // Try to execute in caller thread for critical game operations
                if (r instanceof GameCriticalTask) {
                    r.run();
                } else {
                    // Drop non-critical game tasks
                    logger.info("Dropping non-critical game task due to overload");
                }
            }
        }
    }
    
    /**
     * Custom rejection handler for database operations.
     * Implements retry logic and error reporting.
     */
    public static class DatabaseRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                logger.error("Database task rejected. This may indicate resource exhaustion.");
                
                // For critical DB operations, try to execute synchronously
                if (r instanceof DatabaseCriticalTask) {
                    try {
                        r.run();
                    } catch (Exception e) {
                        logger.error("Failed to execute critical database task", e);
                    }
                } else {
                    // Queue for retry or dead letter
                    handleNonCriticalDatabaseTaskRejection(r);
                }
            }
        }
        
        private void handleNonCriticalDatabaseTaskRejection(Runnable task) {
            // Implement retry queue or dead letter queue
            retryQueue.offer(task);
        }
    }
}
```

### **2. Thread-Safe Game Session Management**

```java
package com.n9.core.thread;

import com.n9.core.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Thread-safe manager for game sessions with optimized concurrent access.
 * Uses lock-striping and concurrent collections for maximum performance.
 */
@Component
public class ThreadSafeGameSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadSafeGameSessionManager.class);
    private static final int LOCK_STRIPE_COUNT = 16; // Power of 2 for efficient modulo
    
    // Game sessions indexed by game ID
    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();
    
    // Lock striping for fine-grained concurrency control
    private final ReadWriteLock[] lockStripes = new ReadWriteLock[LOCK_STRIPE_COUNT];
    
    // Executor for async game operations
    private final Executor gameExecutor;
    
    public ThreadSafeGameSessionManager(@Qualifier("gamePool") Executor gameExecutor) {
        this.gameExecutor = gameExecutor;
        
        // Initialize lock stripes
        for (int i = 0; i < LOCK_STRIPE_COUNT; i++) {
            lockStripes[i] = new ReentrantReadWriteLock();
        }
    }
    
    /**
     * Add new game session with thread safety.
     */
    public void addSession(String gameId, GameSession session) {
        ReadWriteLock lock = getLockForGame(gameId);
        lock.writeLock().lock();
        try {
            if (sessions.containsKey(gameId)) {
                throw new IllegalStateException("Game session already exists: " + gameId);
            }
            sessions.put(gameId, session);
            logger.info("Game session added: {}", gameId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get game session with read lock for thread safety.
     */
    public GameSession getSession(String gameId) {
        ReadWriteLock lock = getLockForGame(gameId);
        lock.readLock().lock();
        try {
            return sessions.get(gameId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Remove game session with write lock.
     */
    public GameSession removeSession(String gameId) {
        ReadWriteLock lock = getLockForGame(gameId);
        lock.writeLock().lock();
        try {
            GameSession removed = sessions.remove(gameId);
            if (removed != null) {
                logger.info("Game session removed: {}", gameId);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Process game action asynchronously with proper error handling.
     */
    public CompletableFuture<GameActionResult> processGameActionAsync(String gameId, GameAction action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                GameSession session = getSession(gameId);
                if (session == null) {
                    return GameActionResult.error("GAME_NOT_FOUND", "Game session not found");
                }
                
                // Execute game action with session-level synchronization
                synchronized (session) {
                    return session.processAction(action);
                }
                
            } catch (Exception e) {
                logger.error("Error processing game action for game {}", gameId, e);
                return GameActionResult.error("PROCESSING_ERROR", e.getMessage());
            }
        }, gameExecutor);
    }
    
    /**
     * Batch process multiple game actions for efficiency.
     */
    public CompletableFuture<List<GameActionResult>> processBatchActionsAsync(
            Map<String, List<GameAction>> gameActions) {
        
        List<CompletableFuture<GameActionResult>> futures = gameActions.entrySet().stream()
            .flatMap(entry -> {
                String gameId = entry.getKey();
                return entry.getValue().stream()
                    .map(action -> processGameActionAsync(gameId, action));
            })
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    /**
     * Get lock for specific game using consistent hashing.
     */
    private ReadWriteLock getLockForGame(String gameId) {
        int index = Math.abs(gameId.hashCode()) % LOCK_STRIPE_COUNT;
        return lockStripes[index];
    }
    
    /**
     * Get current session statistics for monitoring.
     */
    public SessionManagerStats getStats() {
        return SessionManagerStats.builder()
            .totalSessions(sessions.size())
            .lockStripesUsed(LOCK_STRIPE_COUNT)
            .averageSessionsPerStripe((double) sessions.size() / LOCK_STRIPE_COUNT)
            .build();
    }
    
    /**
     * Cleanup expired sessions periodically.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 30 * 60 * 1000; // 30 minutes
        
        sessions.entrySet().removeIf(entry -> {
            GameSession session = entry.getValue();
            if (currentTime - session.getLastActivityTime() > sessionTimeout) {
                logger.info("Removing expired session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
```

### **3. Asynchronous Message Processing**

```java
package com.n9.core.thread;

import com.n9.shared.protocol.MessageEnvelope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance asynchronous message processing service.
 * Implements producer-consumer pattern with back-pressure handling.
 */
@Service
public class AsyncMessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageProcessor.class);
    private static final int MAX_QUEUE_SIZE = 10000;
    
    // Message processing queues by priority
    private final BlockingQueue<MessageEnvelope> highPriorityQueue = new LinkedBlockingQueue<>(1000);
    private final BlockingQueue<MessageEnvelope> normalPriorityQueue = new LinkedBlockingQueue<>(5000);
    private final BlockingQueue<MessageEnvelope> lowPriorityQueue = new LinkedBlockingQueue<>(4000);
    
    // Processing metrics
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong droppedMessages = new AtomicLong(0);
    
    private final GameService gameService;
    private final NotificationService notificationService;
    
    public AsyncMessageProcessor(GameService gameService, NotificationService notificationService) {
        this.gameService = gameService;
        this.notificationService = notificationService;
        
        // Start message processing workers
        startMessageProcessingWorkers();
    }
    
    /**
     * Submit message for asynchronous processing.
     */
    public CompletableFuture<MessageEnvelope> submitMessage(MessageEnvelope message) {
        MessagePriority priority = determinePriority(message);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Add to appropriate queue based on priority
                boolean queued = switch (priority) {
                    case HIGH -> highPriorityQueue.offer(message);
                    case NORMAL -> normalPriorityQueue.offer(message);
                    case LOW -> lowPriorityQueue.offer(message);
                };
                
                if (!queued) {
                    droppedMessages.incrementAndGet();
                    logger.warn("Message queue full, dropping message: {}", message.getType());
                    throw new MessageQueueFullException("Message queue is full");
                }
                
                // Return immediate acknowledgment for async processing
                return createAckMessage(message);
                
            } catch (Exception e) {
                logger.error("Error submitting message for processing", e);
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Process game-critical messages synchronously for guaranteed delivery.
     */
    @Async("gamePool")
    public CompletableFuture<MessageEnvelope> processGameMessageSync(MessageEnvelope message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (message.getType()) {
                    case MessageType.GAME_CARD_PLAY_REQUEST:
                        return gameService.processCardPlay(message);
                    case MessageType.GAME_SURRENDER_REQUEST:
                        return gameService.processSurrender(message);
                    case MessageType.GAME_REMATCH_REQUEST:
                        return gameService.processRematch(message);
                    default:
                        return processGenericMessage(message);
                }
            } catch (Exception e) {
                logger.error("Error processing game message", e);
                return createErrorResponse(message, e);
            }
        });
    }
    
    /**
     * Start background workers for message processing.
     */
    private void startMessageProcessingWorkers() {
        // High priority worker
        startWorker("high-priority", highPriorityQueue, 2);
        
        // Normal priority workers
        startWorker("normal-priority", normalPriorityQueue, 4);
        
        // Low priority worker
        startWorker("low-priority", lowPriorityQueue, 1);
    }
    
    /**
     * Start worker threads for specific queue.
     */
    private void startWorker(String name, BlockingQueue<MessageEnvelope> queue, int workerCount) {
        for (int i = 0; i < workerCount; i++) {
            Thread worker = new Thread(() -> processMessagesFromQueue(queue), 
                                     name + "-worker-" + i);
            worker.setDaemon(true);
            worker.start();
        }
    }
    
    /**
     * Worker thread main loop for processing messages.
     */
    private void processMessagesFromQueue(BlockingQueue<MessageEnvelope> queue) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                MessageEnvelope message = queue.take(); // Blocking take
                processMessageInternal(message);
                processedMessages.incrementAndGet();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in message processing worker", e);
            }
        }
    }
    
    /**
     * Internal message processing logic.
     */
    private void processMessageInternal(MessageEnvelope message) {
        try {
            switch (message.getType()) {
                case MessageType.LOBBY_JOIN_REQUEST:
                case MessageType.LOBBY_MATCH_REQUEST:
                    handleLobbyMessage(message);
                    break;
                    
                case MessageType.GAME_CARD_PLAY_REQUEST:
                case MessageType.GAME_SURRENDER_REQUEST:
                    handleGameMessage(message);
                    break;
                    
                case MessageType.SYSTEM_HEARTBEAT:
                case MessageType.SYSTEM_PING:
                    handleSystemMessage(message);
                    break;
                    
                default:
                    logger.warn("Unknown message type for async processing: {}", message.getType());
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message.getType(), e);
        }
    }
    
    /**
     * Determine message priority based on type and content.
     */
    private MessagePriority determinePriority(MessageEnvelope message) {
        return switch (message.getType()) {
            case MessageType.GAME_CARD_PLAY_REQUEST,
                 MessageType.GAME_SURRENDER_REQUEST -> MessagePriority.HIGH;
                 
            case MessageType.LOBBY_JOIN_REQUEST,
                 MessageType.LOBBY_MATCH_REQUEST,
                 MessageType.SYSTEM_HEARTBEAT -> MessagePriority.NORMAL;
                 
            case MessageType.USER_STATISTICS_REQUEST,
                 MessageType.SYSTEM_PING -> MessagePriority.LOW;
                 
            default -> MessagePriority.NORMAL;
        };
    }
    
    /**
     * Get processing statistics for monitoring.
     */
    public ProcessingStats getStats() {
        return ProcessingStats.builder()
            .processedMessages(processedMessages.get())
            .droppedMessages(droppedMessages.get())
            .highPriorityQueueSize(highPriorityQueue.size())
            .normalPriorityQueueSize(normalPriorityQueue.size())
            .lowPriorityQueueSize(lowPriorityQueue.size())
            .totalQueueSize(getTotalQueueSize())
            .build();
    }
    
    private int getTotalQueueSize() {
        return highPriorityQueue.size() + normalPriorityQueue.size() + lowPriorityQueue.size();
    }
    
    private enum MessagePriority {
        HIGH, NORMAL, LOW
    }
}
```

---

## 🔄 **CONCURRENT COLLECTIONS & DATA STRUCTURES**

### **1. Thread-Safe Game State Management**

```java
package com.n9.core.thread;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

/**
 * Thread-safe data structures for high-performance game state management.
 * Uses lock-free algorithms where possible for maximum concurrency.
 */
public class ConcurrentGameState {
    
    // Player connections with atomic updates
    private final ConcurrentHashMap<String, PlayerConnection> playerConnections = new ConcurrentHashMap<>();
    
    // Game sessions with stamped lock for optimistic reading
    private final ConcurrentHashMap<String, GameSessionData> gameSessions = new ConcurrentHashMap<>();
    private final StampedLock gameSessionsLock = new StampedLock();
    
    // Atomic counters for statistics
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger activeGames = new AtomicInteger(0);
    private final AtomicInteger totalMessagesProcessed = new AtomicInteger(0);
    
    // Lock-free queue for pending matches
    private final ConcurrentLinkedQueue<MatchRequest> pendingMatches = new ConcurrentLinkedQueue<>();
    
    /**
     * Add player connection with atomic increment.
     */
    public void addPlayerConnection(String playerId, PlayerConnection connection) {
        PlayerConnection previous = playerConnections.put(playerId, connection);
        if (previous == null) {
            activeConnections.incrementAndGet();
        }
    }
    
    /**
     * Remove player connection with atomic decrement.
     */
    public PlayerConnection removePlayerConnection(String playerId) {
        PlayerConnection removed = playerConnections.remove(playerId);
        if (removed != null) {
            activeConnections.decrementAndGet();
        }
        return removed;
    }
    
    /**
     * Get player connection with concurrent access.
     */
    public PlayerConnection getPlayerConnection(String playerId) {
        return playerConnections.get(playerId);
    }
    
    /**
     * Add game session with optimistic locking.
     */
    public void addGameSession(String gameId, GameSessionData session) {
        long stamp = gameSessionsLock.writeLock();
        try {
            GameSessionData previous = gameSessions.put(gameId, session);
            if (previous == null) {
                activeGames.incrementAndGet();
            }
        } finally {
            gameSessionsLock.unlockWrite(stamp);
        }
    }
    
    /**
     * Get game session with optimistic read lock.
     */
    public GameSessionData getGameSession(String gameId) {
        long stamp = gameSessionsLock.tryOptimisticRead();
        GameSessionData session = gameSessions.get(gameId);
        
        if (!gameSessionsLock.validate(stamp)) {
            // Optimistic read failed, fall back to read lock
            stamp = gameSessionsLock.readLock();
            try {
                session = gameSessions.get(gameId);
            } finally {
                gameSessionsLock.unlockRead(stamp);
            }
        }
        
        return session;
    }
    
    /**
     * Update game session with compare-and-swap semantics.
     */
    public boolean updateGameSession(String gameId, GameSessionData expected, GameSessionData update) {
        long stamp = gameSessionsLock.writeLock();
        try {
            GameSessionData current = gameSessions.get(gameId);
            if (current != null && current.equals(expected)) {
                gameSessions.put(gameId, update);
                return true;
            }
            return false;
        } finally {
            gameSessionsLock.unlockWrite(stamp);
        }
    }
    
    /**
     * Add match request to lock-free queue.
     */
    public void addPendingMatch(MatchRequest request) {
        pendingMatches.offer(request);
    }
    
    /**
     * Poll next match request (returns null if empty).
     */
    public MatchRequest pollPendingMatch() {
        return pendingMatches.poll();
    }
    
    /**
     * Get atomic statistics snapshot.
     */
    public GameStateStats getStats() {
        return GameStateStats.builder()
            .activeConnections(activeConnections.get())
            .activeGames(activeGames.get())
            .totalMessagesProcessed(totalMessagesProcessed.get())
            .pendingMatches(pendingMatches.size())
            .playerConnections(playerConnections.size())
            .gameSessions(gameSessions.size())
            .build();
    }
    
    /**
     * Increment message counter atomically.
     */
    public void incrementMessageCount() {
        totalMessagesProcessed.incrementAndGet();
    }
    
    /**
     * Thread-safe batch operations.
     */
    public void batchUpdateConnections(Map<String, PlayerConnection> updates) {
        updates.forEach((playerId, connection) -> {
            playerConnections.compute(playerId, (key, existing) -> {
                if (existing == null) {
                    activeConnections.incrementAndGet();
                }
                return connection;
            });
        });
    }
}
```

### **2. Lock-Free Performance Monitoring**

```java
package com.n9.core.thread;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lock-free performance monitoring with atomic operations.
 * Provides real-time metrics without contention.
 */
@Component
public class LockFreePerformanceMonitor {
    
    // High-performance counters using LongAdder for better contention handling
    private final LongAdder requestCount = new LongAdder();
    private final LongAdder responseCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();
    
    // Atomic references for timing statistics
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    
    // Per-operation type metrics
    private final ConcurrentHashMap<String, LongAdder> operationCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> operationTimes = new ConcurrentHashMap<>();
    
    /**
     * Record request start.
     */
    public RequestTimer startRequest(String operationType) {
        requestCount.increment();
        operationCounts.computeIfAbsent(operationType, k -> new LongAdder()).increment();
        
        return new RequestTimer(operationType, System.nanoTime());
    }
    
    /**
     * Record successful response.
     */
    public void recordResponse(RequestTimer timer) {
        long duration = System.nanoTime() - timer.startTime;
        responseCount.increment();
        
        // Update timing statistics atomically
        updateResponseTime(duration);
        updateOperationTime(timer.operationType, duration);
    }
    
    /**
     * Record error response.
     */
    public void recordError(RequestTimer timer, Exception error) {
        long duration = System.nanoTime() - timer.startTime;
        errorCount.increment();
        
        updateResponseTime(duration);
        updateOperationTime(timer.operationType, duration);
        
        // Log error details
        logger.warn("Operation {} failed after {}ms: {}", 
                   timer.operationType, duration / 1_000_000, error.getMessage());
    }
    
    /**
     * Update response time statistics atomically.
     */
    private void updateResponseTime(long duration) {
        totalResponseTime.addAndGet(duration);
        
        // Update max response time
        maxResponseTime.updateAndGet(current -> Math.max(current, duration));
        
        // Update min response time
        minResponseTime.updateAndGet(current -> Math.min(current, duration));
    }
    
    /**
     * Update per-operation timing.
     */
    private void updateOperationTime(String operationType, long duration) {
        operationTimes.computeIfAbsent(operationType, k -> new AtomicLong(0))
                     .addAndGet(duration);
    }
    
    /**
     * Get comprehensive performance statistics.
     */
    public PerformanceStats getStats() {
        long requests = requestCount.sum();
        long responses = responseCount.sum();
        long errors = errorCount.sum();
        long totalTime = totalResponseTime.get();
        
        double averageResponseTime = responses > 0 ? (double) totalTime / responses / 1_000_000 : 0;
        double errorRate = requests > 0 ? (double) errors / requests * 100 : 0;
        
        Map<String, OperationStats> operationStats = new HashMap<>();
        operationCounts.forEach((operation, count) -> {
            long operationTime = operationTimes.getOrDefault(operation, new AtomicLong(0)).get();
            double avgTime = count.sum() > 0 ? (double) operationTime / count.sum() / 1_000_000 : 0;
            
            operationStats.put(operation, OperationStats.builder()
                .operationType(operation)
                .requestCount(count.sum())
                .averageTimeMs(avgTime)
                .build());
        });
        
        return PerformanceStats.builder()
            .totalRequests(requests)
            .totalResponses(responses)
            .totalErrors(errors)
            .averageResponseTimeMs(averageResponseTime)
            .maxResponseTimeMs(maxResponseTime.get() / 1_000_000.0)
            .minResponseTimeMs(minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get() / 1_000_000.0)
            .errorRatePercent(errorRate)
            .operationStats(operationStats)
            .build();
    }
    
    /**
     * Reset all statistics (useful for testing or periodic resets).
     */
    public void reset() {
        requestCount.reset();
        responseCount.reset();
        errorCount.reset();
        totalResponseTime.set(0);
        maxResponseTime.set(0);
        minResponseTime.set(Long.MAX_VALUE);
        operationCounts.clear();
        operationTimes.clear();
    }
    
    /**
     * Request timer for tracking operation duration.
     */
    public static class RequestTimer {
        final String operationType;
        final long startTime;
        
        public RequestTimer(String operationType, long startTime) {
            this.operationType = operationType;
            this.startTime = startTime;
        }
    }
}
```

---

## 📊 **MONITORING & DIAGNOSTICS**

### **1. Thread Pool Health Monitoring**

```java
package com.n9.core.thread;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Comprehensive thread pool monitoring and alerting system.
 */
@Component
public class ThreadPoolMonitor {
    
    private final List<ThreadPoolTaskExecutor> monitoredPools;
    private final PerformanceMonitor performanceMonitor;
    
    public ThreadPoolMonitor(@Qualifier("connectionPool") ThreadPoolTaskExecutor connectionPool,
                           @Qualifier("gamePool") ThreadPoolTaskExecutor gamePool,
                           @Qualifier("databasePool") ThreadPoolTaskExecutor databasePool,
                           @Qualifier("notificationPool") ThreadPoolTaskExecutor notificationPool,
                           PerformanceMonitor performanceMonitor) {
        this.monitoredPools = List.of(connectionPool, gamePool, databasePool, notificationPool);
        this.performanceMonitor = performanceMonitor;
    }
    
    /**
     * Periodic health check of all thread pools.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void checkThreadPoolHealth() {
        for (ThreadPoolTaskExecutor pool : monitoredPools) {
            ThreadPoolStats stats = getThreadPoolStats(pool);
            
            // Check for potential issues
            checkPoolSaturation(pool, stats);
            checkQueueBacklog(pool, stats);
            checkThreadUtilization(pool, stats);
            
            // Log statistics
            logger.info("Thread pool {} stats: active={}, queue={}, completed={}", 
                       pool.getThreadNamePrefix(), stats.getActiveCount(), 
                       stats.getQueueSize(), stats.getCompletedTaskCount());
        }
    }
    
    /**
     * Check for thread pool saturation.
     */
    private void checkPoolSaturation(ThreadPoolTaskExecutor pool, ThreadPoolStats stats) {
        double utilizationRate = (double) stats.getActiveCount() / stats.getMaxPoolSize();
        
        if (utilizationRate > 0.9) { // 90% utilization
            logger.warn("Thread pool {} is saturated: {}% utilization", 
                       pool.getThreadNamePrefix(), utilizationRate * 100);
            
            // Alert or auto-scaling logic here
            considerPoolExpansion(pool, stats);
        }
    }
    
    /**
     * Check for queue backlog issues.
     */
    private void checkQueueBacklog(ThreadPoolTaskExecutor pool, ThreadPoolStats stats) {
        double queueUtilization = (double) stats.getQueueSize() / pool.getQueueCapacity();
        
        if (queueUtilization > 0.8) { // 80% queue full
            logger.warn("Thread pool {} queue is backing up: {}% full", 
                       pool.getThreadNamePrefix(), queueUtilization * 100);
            
            // Consider increasing pool size or implementing back-pressure
        }
    }
    
    /**
     * Monitor thread utilization efficiency.
     */
    private void checkThreadUtilization(ThreadPoolTaskExecutor pool, ThreadPoolStats stats) {
        if (stats.getActiveCount() == 0 && stats.getQueueSize() > 0) {
            logger.warn("Thread pool {} has queued tasks but no active threads", 
                       pool.getThreadNamePrefix());
        }
    }
    
    /**
     * Auto-scaling logic for thread pools under high load.
     */
    private void considerPoolExpansion(ThreadPoolTaskExecutor pool, ThreadPoolStats stats) {
        // Implement dynamic scaling logic based on load patterns
        // This could integrate with cloud auto-scaling or container orchestration
    }
    
    /**
     * Get comprehensive thread pool statistics.
     */
    private ThreadPoolStats getThreadPoolStats(ThreadPoolTaskExecutor pool) {
        ThreadPoolExecutor executor = pool.getThreadPoolExecutor();
        
        return ThreadPoolStats.builder()
            .poolName(pool.getThreadNamePrefix())
            .corePoolSize(executor.getCorePoolSize())
            .maxPoolSize(executor.getMaximumPoolSize())
            .activeCount(executor.getActiveCount())
            .queueSize(executor.getQueue().size())
            .completedTaskCount(executor.getCompletedTaskCount())
            .taskCount(executor.getTaskCount())
            .largestPoolSize(executor.getLargestPoolSize())
            .build();
    }
}
```

---

## 🧪 **CONCURRENCY TESTING FRAMEWORK**

### **1. Load Testing for Threading Model**

```java
@Test
public void testConcurrentGameProcessing() throws InterruptedException {
    int numThreads = 50;
    int operationsPerThread = 100;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);
    
    ExecutorService testExecutor = Executors.newFixedThreadPool(numThreads);
    
    // Create concurrent test tasks
    for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        testExecutor.submit(() -> {
            try {
                startLatch.await(); // Wait for all threads to be ready
                
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        String gameId = "test-game-" + threadId + "-" + j;
                        GameAction action = createTestGameAction(gameId);
                        
                        CompletableFuture<GameActionResult> future = 
                            gameSessionManager.processGameActionAsync(gameId, action);
                        
                        GameActionResult result = future.get(1, TimeUnit.SECONDS);
                        
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.error("Test operation failed", e);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completionLatch.countDown();
            }
        });
    }
    
    // Start all threads simultaneously
    startLatch.countDown();
    
    // Wait for completion with timeout
    assertTrue(completionLatch.await(30, TimeUnit.SECONDS));
    
    // Verify results
    int expectedOperations = numThreads * operationsPerThread;
    assertEquals(expectedOperations, successCount.get() + errorCount.get());
    
    // Performance assertions
    assertTrue("Too many errors: " + errorCount.get(), 
               errorCount.get() < expectedOperations * 0.05); // < 5% error rate
    
    testExecutor.shutdown();
}

@Test
public void testThreadPoolPerformanceUnderLoad() {
    // Stress test thread pools with high message volume
    int messageCount = 10000;
    CountDownLatch latch = new CountDownLatch(messageCount);
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < messageCount; i++) {
        MessageEnvelope message = createTestMessage("GAME.CARD_PLAY_REQUEST");
        
        asyncMessageProcessor.submitMessage(message)
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    latch.countDown();
                } else {
                    logger.error("Message processing failed", throwable);
                    latch.countDown();
                }
            });
    }
    
    // Wait for all messages to be processed
    assertTrue(latch.await(60, TimeUnit.SECONDS));
    
    long duration = System.currentTimeMillis() - startTime;
    double messagesPerSecond = (double) messageCount / (duration / 1000.0);
    
    // Performance assertions
    assertTrue("Throughput too low: " + messagesPerSecond + " msg/s", 
               messagesPerSecond > 100); // Minimum 100 messages/second
    
    // Verify thread pool health
    ProcessingStats stats = asyncMessageProcessor.getStats();
    assertEquals(messageCount, stats.getProcessedMessages());
    assertTrue("Too many dropped messages", stats.getDroppedMessages() < messageCount * 0.01);
}
```

---

Đây là comprehensive threading model architecture. Bạn có muốn tôi tiếp tục với **Integration Testing Strategy** hoặc **Performance Optimization Guide** không?