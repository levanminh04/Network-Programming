# Game Logic Engine - Enterprise Game Architecture

## 🎯 **GAME ENGINE OVERVIEW**

Game Logic Engine là **trái tim xử lý business logic** của card game, chịu trách nhiệm implement toàn bộ game rules, quản lý game sessions, và đảm bảo game fairness với high performance.

### **Core Responsibilities**
- **Game Session Management**: Quản lý lifecycle của individual games
- **Card Deck Management**: Shuffle, distribute, và track cards
- **Game Rules Engine**: Enforce game rules và validate moves
- **Score Calculation**: Real-time scoring với accurate algorithms
- **Matchmaking Logic**: Pair players dựa trên skill level
- **Game State Synchronization**: Ensure consistent state across clients

### **Performance Requirements**
- **Game Creation**: < 10ms per new game session
- **Card Processing**: < 5ms per card play
- **Score Calculation**: < 1ms per round
- **Concurrent Games**: Support 50+ simultaneous game sessions
- **Memory Efficiency**: < 10MB per active game session

---

## 🏗️ **GAME ARCHITECTURE COMPONENTS**

### **1. GameEngine.java - Central Game Coordinator**

```java
package com.n9.core.game;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.core.model.entity.Game;
import com.n9.core.service.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Central Game Engine coordinating all game operations.
 * Manages game sessions, enforces rules, and handles game lifecycle.
 * 
 * Features:
 * - Concurrent game session management
 * - Automatic game cleanup and timeout handling
 * - Performance monitoring and metrics
 * - Thread-safe operations
 */
@Service
public class GameEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(GameEngine.class);
    
    // Active game sessions indexed by game ID
    private final ConcurrentHashMap<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    
    // Cleanup scheduler for expired games
    private final ScheduledExecutorService cleanupScheduler = 
        Executors.newScheduledThreadPool(2, r -> new Thread(r, "game-cleanup"));
    
    private final GameRepository gameRepository;
    private final GameRules gameRules;
    private final ScoreCalculator scoreCalculator;
    private final NotificationService notificationService;
    
    public GameEngine(GameRepository gameRepository,
                     GameRules gameRules,
                     ScoreCalculator scoreCalculator,
                     NotificationService notificationService) {
        this.gameRepository = gameRepository;
        this.gameRules = gameRules;
        this.scoreCalculator = scoreCalculator;
        this.notificationService = notificationService;
        
        // Start periodic cleanup task
        startCleanupTask();
    }
    
    /**
     * Create a new game session between two players.
     */
    public GameSession createGame(String player1Id, String player2Id, String gameMode) {
        logger.info("Creating new game: {} vs {} in mode {}", player1Id, player2Id, gameMode);
        
        try {
            // Create game entity
            Game gameEntity = Game.builder()
                .id(generateGameId())
                .player1Id(player1Id)
                .player2Id(player2Id)
                .gameMode(gameMode)
                .status(GameStatus.WAITING_TO_START)
                .createdAt(System.currentTimeMillis())
                .build();
            
            // Save to database
            gameEntity = gameRepository.save(gameEntity);
            
            // Create game session
            GameSession session = new GameSession(
                gameEntity, gameRules, scoreCalculator, notificationService);
            
            // Register session
            activeSessions.put(gameEntity.getId(), session);
            
            logger.info("Game {} created successfully", gameEntity.getId());
            return session;
            
        } catch (Exception e) {
            logger.error("Failed to create game for players {} and {}", player1Id, player2Id, e);
            throw new GameException("Failed to create game", e);
        }
    }
    
    /**
     * Get active game session by ID.
     */
    public GameSession getGameSession(String gameId) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            logger.warn("Game session not found: {}", gameId);
            throw new GameNotFoundException("Game session not found: " + gameId);
        }
        return session;
    }
    
    /**
     * Process card play action in a game.
     */
    public MessageEnvelope processCardPlay(MessageEnvelope envelope) {
        try {
            CardPlayRequestDto request = JsonUtils.convertPayload(
                envelope.getPayload(), CardPlayRequestDto.class);
            
            // Validate request
            ValidationResult validation = ValidationUtils.validate(request);
            if (!validation.isValid()) {
                return createErrorResponse(envelope, "INVALID_REQUEST", validation.getErrorMessage());
            }
            
            // Get game session
            GameSession session = getGameSession(request.getMatchId());
            
            // Process card play
            GameMoveResult result = session.playCard(
                envelope.getUserId(), request.getCardIndex(), request.getRoundNumber());
            
            // Handle result
            if (result.isError()) {
                return createErrorResponse(envelope, result.getErrorCode(), result.getErrorMessage());
            }
            
            // Create acknowledgment
            MessageEnvelope ack = MessageEnvelope.builder()
                .type(MessageType.GAME_CARD_PLAY_ACK)
                .correlationId(envelope.getCorrelationId())
                .timestamp(System.currentTimeMillis())
                .userId(envelope.getUserId())
                .matchId(request.getMatchId())
                .payload(result.getAckPayload())
                .build();
            
            // Check if round is complete
            if (session.isRoundComplete()) {
                handleRoundComplete(session);
            }
            
            // Check if game is complete
            if (session.isGameComplete()) {
                handleGameComplete(session);
            }
            
            return ack;
            
        } catch (Exception e) {
            logger.error("Error processing card play", e);
            return createErrorResponse(envelope, "GAME_ERROR", "Failed to process card play");
        }
    }
    
    /**
     * Handle round completion and notify players.
     */
    private void handleRoundComplete(GameSession session) {
        try {
            RoundResult roundResult = session.completeRound();
            
            // Create round reveal message
            MessageEnvelope revealMessage = MessageEnvelope.builder()
                .type(MessageType.GAME_ROUND_REVEAL)
                .correlationId(generateCorrelationId())
                .timestamp(System.currentTimeMillis())
                .matchId(session.getGameId())
                .payload(roundResult.toRevealDto())
                .build();
            
            // Broadcast to both players
            notificationService.broadcastToGame(session.getGameId(), revealMessage);
            
            // Schedule next round if game not complete
            if (!session.isGameComplete()) {
                scheduleNextRound(session);
            }
            
        } catch (Exception e) {
            logger.error("Error handling round completion for game {}", session.getGameId(), e);
        }
    }
    
    /**
     * Handle game completion and cleanup.
     */
    private void handleGameComplete(GameSession session) {
        try {
            GameResult gameResult = session.completeGame();
            
            // Update database
            Game gameEntity = session.getGameEntity();
            gameEntity.setStatus(GameStatus.COMPLETED);
            gameEntity.setCompletedAt(System.currentTimeMillis());
            gameEntity.setWinnerId(gameResult.getWinnerId());
            gameRepository.save(gameEntity);
            
            // Create game end message
            MessageEnvelope endMessage = MessageEnvelope.builder()
                .type(MessageType.GAME_END)
                .correlationId(generateCorrelationId())
                .timestamp(System.currentTimeMillis())
                .matchId(session.getGameId())
                .payload(gameResult.toEndDto())
                .build();
            
            // Broadcast to both players
            notificationService.broadcastToGame(session.getGameId(), endMessage);
            
            // Remove from active sessions
            activeSessions.remove(session.getGameId());
            
            logger.info("Game {} completed. Winner: {}", session.getGameId(), gameResult.getWinnerId());
            
        } catch (Exception e) {
            logger.error("Error handling game completion for game {}", session.getGameId(), e);
        }
    }
    
    /**
     * Schedule next round start with delay.
     */
    private void scheduleNextRound(GameSession session) {
        cleanupScheduler.schedule(() -> {
            try {
                session.startNextRound();
                
                // Create round start message
                MessageEnvelope startMessage = MessageEnvelope.builder()
                    .type(MessageType.GAME_ROUND_START)
                    .correlationId(generateCorrelationId())
                    .timestamp(System.currentTimeMillis())
                    .matchId(session.getGameId())
                    .payload(session.createRoundStartPayload())
                    .build();
                
                // Broadcast to both players
                notificationService.broadcastToGame(session.getGameId(), startMessage);
                
            } catch (Exception e) {
                logger.error("Error starting next round for game {}", session.getGameId(), e);
            }
        }, 3, TimeUnit.SECONDS); // 3-second delay between rounds
    }
    
    /**
     * Start periodic cleanup task for expired games.
     */
    private void startCleanupTask() {
        cleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredGames();
            } catch (Exception e) {
                logger.error("Error in game cleanup task", e);
            }
        }, 1, 1, TimeUnit.MINUTES); // Run every minute
    }
    
    /**
     * Clean up expired and abandoned games.
     */
    private void cleanupExpiredGames() {
        long currentTime = System.currentTimeMillis();
        long gameTimeout = 30 * 60 * 1000; // 30 minutes
        
        activeSessions.entrySet().removeIf(entry -> {
            GameSession session = entry.getValue();
            long gameAge = currentTime - session.getCreatedAt();
            
            if (gameAge > gameTimeout || session.isAbandoned()) {
                logger.info("Cleaning up expired/abandoned game: {}", entry.getKey());
                
                try {
                    // Mark game as abandoned in database
                    Game gameEntity = session.getGameEntity();
                    gameEntity.setStatus(GameStatus.ABANDONED);
                    gameEntity.setCompletedAt(currentTime);
                    gameRepository.save(gameEntity);
                    
                    // Notify players if still connected
                    notificationService.notifyGameAbandoned(session.getGameId());
                    
                } catch (Exception e) {
                    logger.error("Error cleaning up game {}", entry.getKey(), e);
                }
                
                return true; // Remove from active sessions
            }
            
            return false; // Keep in active sessions
        });
    }
    
    /**
     * Get current engine statistics.
     */
    public GameEngineStats getStats() {
        return GameEngineStats.builder()
            .activeGames(activeSessions.size())
            .totalGamesCreated(gameRepository.getTotalGamesCount())
            .averageGameDuration(gameRepository.getAverageGameDuration())
            .gamesCompletedToday(gameRepository.getGamesCompletedToday())
            .build();
    }
    
    /**
     * Shutdown the game engine gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down Game Engine...");
        
        // Complete all active games
        activeSessions.values().forEach(session -> {
            try {
                session.forceComplete("Server shutdown");
            } catch (Exception e) {
                logger.warn("Error force completing game {}", session.getGameId(), e);
            }
        });
        
        // Shutdown cleanup scheduler
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Game Engine shutdown complete");
    }
}
```

### **2. GameSession.java - Individual Game Instance**

```java
package com.n9.core.game;

import com.n9.shared.model.Card;
import com.n9.shared.model.enums.GameState;
import com.n9.core.model.entity.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an individual game session between two players.
 * Manages game state, player moves, and round progression.
 * 
 * Thread-safe implementation with proper synchronization.
 */
public class GameSession {
    
    private static final Logger logger = LoggerFactory.getLogger(GameSession.class);
    
    private final Game gameEntity;
    private final GameRules gameRules;
    private final ScoreCalculator scoreCalculator;
    private final NotificationService notificationService;
    
    // Game state
    private volatile GameState currentState = GameState.WAITING_TO_START;
    private final AtomicInteger currentRound = new AtomicInteger(0);
    private final long createdAt = System.currentTimeMillis();
    
    // Card management
    private final CardDeck deck;
    private final Map<String, List<Card>> playerCards = new ConcurrentHashMap<>();
    
    // Round management
    private final Map<Integer, RoundData> rounds = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();
    
    // Player moves for current round
    private final Map<String, PlayerMove> currentRoundMoves = new ConcurrentHashMap<>();
    
    // Synchronization
    private final ReentrantLock gameLock = new ReentrantLock();
    
    public GameSession(Game gameEntity, GameRules gameRules, 
                      ScoreCalculator scoreCalculator, NotificationService notificationService) {
        this.gameEntity = gameEntity;
        this.gameRules = gameRules;
        this.scoreCalculator = scoreCalculator;
        this.notificationService = notificationService;
        
        // Initialize deck and player data
        this.deck = new CardDeck();
        this.playerScores.put(gameEntity.getPlayer1Id(), 0);
        this.playerScores.put(gameEntity.getPlayer2Id(), 0);
        
        logger.info("Game session created: {}", gameEntity.getId());
    }
    
    /**
     * Start the game and first round.
     */
    public void startGame() {
        gameLock.lock();
        try {
            if (currentState != GameState.WAITING_TO_START) {
                throw new IllegalStateException("Game already started");
            }
            
            currentState = GameState.IN_PROGRESS;
            startNextRound();
            
            logger.info("Game {} started", gameEntity.getId());
            
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Start the next round.
     */
    public void startNextRound() {
        gameLock.lock();
        try {
            int roundNumber = currentRound.incrementAndGet();
            
            if (roundNumber > gameRules.getMaxRounds()) {
                completeGame();
                return;
            }
            
            // Clear previous round moves
            currentRoundMoves.clear();
            
            // Create round data
            RoundData roundData = new RoundData(roundNumber, System.currentTimeMillis());
            rounds.put(roundNumber, roundData);
            
            currentState = GameState.ROUND_IN_PROGRESS;
            
            logger.info("Round {} started for game {}", roundNumber, gameEntity.getId());
            
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Process a card play from a player.
     */
    public GameMoveResult playCard(String playerId, int cardIndex, int roundNumber) {
        gameLock.lock();
        try {
            // Validate game state
            if (currentState != GameState.ROUND_IN_PROGRESS) {
                return GameMoveResult.error("INVALID_GAME_STATE", "Game is not in progress");
            }
            
            // Validate round number
            if (roundNumber != currentRound.get()) {
                return GameMoveResult.error("INVALID_ROUND", "Invalid round number");
            }
            
            // Validate player
            if (!isValidPlayer(playerId)) {
                return GameMoveResult.error("INVALID_PLAYER", "Player not in this game");
            }
            
            // Check if player already moved this round
            if (currentRoundMoves.containsKey(playerId)) {
                return GameMoveResult.error("ALREADY_MOVED", "Player already moved this round");
            }
            
            // Validate card index
            if (!ValidationUtils.isValidCardIndex(cardIndex)) {
                return GameMoveResult.error("INVALID_CARD", "Invalid card index");
            }
            
            // Get the card
            Card selectedCard = deck.getCard(cardIndex);
            if (selectedCard == null) {
                return GameMoveResult.error("CARD_NOT_FOUND", "Card not found");
            }
            
            // Record player move
            PlayerMove move = new PlayerMove(playerId, selectedCard, System.currentTimeMillis());
            currentRoundMoves.put(playerId, move);
            
            // Update round data
            RoundData roundData = rounds.get(roundNumber);
            roundData.addPlayerMove(move);
            
            logger.info("Player {} played card {} in round {} of game {}", 
                       playerId, selectedCard.getDisplayName(), roundNumber, gameEntity.getId());
            
            return GameMoveResult.success(selectedCard, isRoundComplete());
            
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Complete the current round and calculate scores.
     */
    public RoundResult completeRound() {
        gameLock.lock();
        try {
            int roundNumber = currentRound.get();
            RoundData roundData = rounds.get(roundNumber);
            
            if (currentRoundMoves.size() != 2) {
                throw new IllegalStateException("Round not complete");
            }
            
            // Get player moves
            PlayerMove player1Move = currentRoundMoves.get(gameEntity.getPlayer1Id());
            PlayerMove player2Move = currentRoundMoves.get(gameEntity.getPlayer2Id());
            
            // Calculate round winner and scores
            RoundWinner winner = scoreCalculator.calculateRoundWinner(
                player1Move.getCard(), player2Move.getCard());
            
            // Update round data
            roundData.setPlayer1Card(player1Move.getCard());
            roundData.setPlayer2Card(player2Move.getCard());
            roundData.setWinner(winner.getWinnerId());
            roundData.setPlayer1Score(winner.getPlayer1Score());
            roundData.setPlayer2Score(winner.getPlayer2Score());
            roundData.setCompletedAt(System.currentTimeMillis());
            
            // Update total scores
            playerScores.put(gameEntity.getPlayer1Id(), 
                           playerScores.get(gameEntity.getPlayer1Id()) + winner.getPlayer1Score());
            playerScores.put(gameEntity.getPlayer2Id(), 
                           playerScores.get(gameEntity.getPlayer2Id()) + winner.getPlayer2Score());
            
            currentState = GameState.ROUND_COMPLETE;
            
            logger.info("Round {} completed for game {}. Winner: {}", 
                       roundNumber, gameEntity.getId(), winner.getWinnerId());
            
            return new RoundResult(roundData, playerScores);
            
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Complete the entire game.
     */
    public GameResult completeGame() {
        gameLock.lock();
        try {
            currentState = GameState.COMPLETED;
            
            // Calculate final winner
            String winnerId = scoreCalculator.calculateGameWinner(playerScores);
            
            // Calculate game statistics
            long duration = System.currentTimeMillis() - createdAt;
            GameStats stats = calculateGameStats();
            
            logger.info("Game {} completed. Winner: {}, Duration: {}ms", 
                       gameEntity.getId(), winnerId, duration);
            
            return new GameResult(winnerId, playerScores, duration, stats, rounds.values());
            
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Check if current round is complete (both players moved).
     */
    public boolean isRoundComplete() {
        return currentRoundMoves.size() == 2;
    }
    
    /**
     * Check if game is complete (all rounds played).
     */
    public boolean isGameComplete() {
        return currentRound.get() >= gameRules.getMaxRounds() || 
               currentState == GameState.COMPLETED;
    }
    
    /**
     * Check if game is abandoned (inactive for too long).
     */
    public boolean isAbandoned() {
        long inactiveTime = System.currentTimeMillis() - getLastActivityTime();
        return inactiveTime > gameRules.getGameTimeoutMs();
    }
    
    /**
     * Force complete the game (e.g., server shutdown).
     */
    public void forceComplete(String reason) {
        gameLock.lock();
        try {
            currentState = GameState.ABANDONED;
            logger.info("Game {} force completed: {}", gameEntity.getId(), reason);
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Create payload for round start message.
     */
    public Map<String, Object> createRoundStartPayload() {
        return Map.of(
            "matchId", gameEntity.getId(),
            "roundNumber", currentRound.get(),
            "deadline", System.currentTimeMillis() + gameRules.getRoundTimeoutMs(),
            "availableCards", deck.getAvailableCards(),
            "roundScore", playerScores
        );
    }
    
    /**
     * Get game statistics for monitoring.
     */
    private GameStats calculateGameStats() {
        long totalRoundTime = rounds.values().stream()
            .filter(round -> round.getCompletedAt() > 0)
            .mapToLong(round -> round.getCompletedAt() - round.getStartedAt())
            .sum();
        
        double averageRoundTime = rounds.isEmpty() ? 0 : 
            (double) totalRoundTime / rounds.size();
        
        return GameStats.builder()
            .totalRounds(rounds.size())
            .averageRoundTime(averageRoundTime)
            .totalGameTime(System.currentTimeMillis() - createdAt)
            .build();
    }
    
    /**
     * Get last activity timestamp.
     */
    private long getLastActivityTime() {
        return currentRoundMoves.values().stream()
            .mapToLong(PlayerMove::getTimestamp)
            .max()
            .orElse(createdAt);
    }
    
    /**
     * Validate if player belongs to this game.
     */
    private boolean isValidPlayer(String playerId) {
        return gameEntity.getPlayer1Id().equals(playerId) || 
               gameEntity.getPlayer2Id().equals(playerId);
    }
    
    // Getters
    public String getGameId() { return gameEntity.getId(); }
    public Game getGameEntity() { return gameEntity; }
    public GameState getCurrentState() { return currentState; }
    public int getCurrentRound() { return currentRound.get(); }
    public long getCreatedAt() { return createdAt; }
    public Map<String, Integer> getPlayerScores() { return new HashMap<>(playerScores); }
}
```

### **3. CardDeck.java - Card Management**

```java
package com.n9.core.game;

import com.n9.shared.model.Card;
import com.n9.shared.model.enums.CardSuit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages a standard 52-card deck for game sessions.
 * Provides shuffling, card distribution, and deck state management.
 */
public class CardDeck {
    
    private static final Logger logger = LoggerFactory.getLogger(CardDeck.class);
    
    private final List<Card> cards;
    private final Set<Integer> usedCardIndices = new HashSet<>();
    
    public CardDeck() {
        this.cards = createStandardDeck();
        shuffleDeck();
        logger.debug("New card deck created and shuffled");
    }
    
    /**
     * Create a standard 52-card deck.
     */
    private List<Card> createStandardDeck() {
        List<Card> deck = new ArrayList<>(52);
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        
        for (CardSuit suit : CardSuit.values()) {
            for (int i = 0; i < ranks.length; i++) {
                Card card = Card.builder()
                    .index(deck.size())
                    .rank(ranks[i])
                    .suit(suit)
                    .value(values[i])
                    .displayName(ranks[i] + suit.getSymbol())
                    .build();
                deck.add(card);
            }
        }
        
        return Collections.unmodifiableList(deck);
    }
    
    /**
     * Shuffle the deck using Fisher-Yates algorithm.
     */
    private void shuffleDeck() {
        // Create mutable copy for shuffling
        List<Card> shuffledCards = new ArrayList<>(cards);
        
        for (int i = shuffledCards.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            Collections.swap(shuffledCards, i, j);
        }
        
        // Update indices after shuffle
        for (int i = 0; i < shuffledCards.size(); i++) {
            Card original = shuffledCards.get(i);
            Card updated = original.toBuilder().index(i).build();
            shuffledCards.set(i, updated);
        }
        
        this.cards = Collections.unmodifiableList(shuffledCards);
    }
    
    /**
     * Get card by index.
     */
    public Card getCard(int index) {
        if (index < 0 || index >= cards.size()) {
            return null;
        }
        return cards.get(index);
    }
    
    /**
     * Get available cards (not yet used in game).
     */
    public List<Card> getAvailableCards() {
        return cards.stream()
            .filter(card -> !usedCardIndices.contains(card.getIndex()))
            .collect(Collectors.toList());
    }
    
    /**
     * Mark card as used.
     */
    public void markCardUsed(int index) {
        usedCardIndices.add(index);
    }
    
    /**
     * Get total cards in deck.
     */
    public int getTotalCards() {
        return cards.size();
    }
    
    /**
     * Get number of remaining cards.
     */
    public int getRemainingCards() {
        return cards.size() - usedCardIndices.size();
    }
    
    /**
     * Check if deck has available cards.
     */
    public boolean hasAvailableCards() {
        return getRemainingCards() > 0;
    }
    
    /**
     * Reset deck (clear used cards).
     */
    public void reset() {
        usedCardIndices.clear();
        shuffleDeck();
    }
}
```

---

## 🎯 **GAME RULES ENGINE**

### **GameRules.java - Configurable Rule System**

```java
package com.n9.core.game;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable game rules and settings.
 * All game parameters can be customized via configuration.
 */
@Component
@ConfigurationProperties(prefix = "game.rules")
public class GameRules {
    
    private int maxRounds = 3;
    private long roundTimeoutMs = 30000; // 30 seconds
    private long gameTimeoutMs = 1800000; // 30 minutes
    private boolean allowRematch = true;
    private boolean allowSurrender = true;
    private int minPlayersPerGame = 2;
    private int maxPlayersPerGame = 2;
    
    // Card value rules
    private boolean aceHighValue = false; // Ace = 1 or 14
    private boolean allowTies = true;
    
    // Scoring rules
    private int winRoundPoints = 1;
    private int tieRoundPoints = 0;
    private int loseRoundPoints = 0;
    private double gameWinMultiplier = 1.5;
    
    // Validation methods
    public boolean isValidRound(int roundNumber) {
        return roundNumber >= 1 && roundNumber <= maxRounds;
    }
    
    public boolean isGameTimedOut(long gameStartTime) {
        return System.currentTimeMillis() - gameStartTime > gameTimeoutMs;
    }
    
    public boolean isRoundTimedOut(long roundStartTime) {
        return System.currentTimeMillis() - roundStartTime > roundTimeoutMs;
    }
    
    // Getters and setters
    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }
    
    public long getRoundTimeoutMs() { return roundTimeoutMs; }
    public void setRoundTimeoutMs(long roundTimeoutMs) { this.roundTimeoutMs = roundTimeoutMs; }
    
    public long getGameTimeoutMs() { return gameTimeoutMs; }
    public void setGameTimeoutMs(long gameTimeoutMs) { this.gameTimeoutMs = gameTimeoutMs; }
    
    public boolean isAllowRematch() { return allowRematch; }
    public void setAllowRematch(boolean allowRematch) { this.allowRematch = allowRematch; }
    
    public boolean isAllowSurrender() { return allowSurrender; }
    public void setAllowSurrender(boolean allowSurrender) { this.allowSurrender = allowSurrender; }
    
    public int getWinRoundPoints() { return winRoundPoints; }
    public int getTieRoundPoints() { return tieRoundPoints; }
    public int getLoseRoundPoints() { return loseRoundPoints; }
    public double getGameWinMultiplier() { return gameWinMultiplier; }
}
```

---

## 📊 **SCORE CALCULATION SYSTEM**

### **ScoreCalculator.java - Advanced Scoring Logic**

```java
package com.n9.core.game;

import com.n9.shared.model.Card;
import org.springframework.stereotype.Component;

/**
 * Advanced score calculation system for card games.
 * Handles round scoring, game scoring, and winner determination.
 */
@Component
public class ScoreCalculator {
    
    private final GameRules gameRules;
    
    public ScoreCalculator(GameRules gameRules) {
        this.gameRules = gameRules;
    }
    
    /**
     * Calculate winner of a single round.
     */
    public RoundWinner calculateRoundWinner(Card player1Card, Card player2Card) {
        int player1Value = getCardValue(player1Card);
        int player2Value = getCardValue(player2Card);
        
        String winnerId;
        int player1Score;
        int player2Score;
        
        if (player1Value > player2Value) {
            // Player 1 wins
            winnerId = "player1";
            player1Score = gameRules.getWinRoundPoints();
            player2Score = gameRules.getLoseRoundPoints();
        } else if (player2Value > player1Value) {
            // Player 2 wins
            winnerId = "player2";
            player1Score = gameRules.getLoseRoundPoints();
            player2Score = gameRules.getWinRoundPoints();
        } else {
            // Tie
            winnerId = gameRules.isAllowTies() ? "tie" : "player1"; // Default to player1 if ties not allowed
            player1Score = gameRules.getTieRoundPoints();
            player2Score = gameRules.getTieRoundPoints();
        }
        
        return RoundWinner.builder()
            .winnerId(winnerId)
            .player1Score(player1Score)
            .player2Score(player2Score)
            .player1Card(player1Card)
            .player2Card(player2Card)
            .build();
    }
    
    /**
     * Calculate overall game winner.
     */
    public String calculateGameWinner(Map<String, Integer> playerScores) {
        // Find player with highest score
        return playerScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("tie");
    }
    
    /**
     * Calculate rating points earned/lost for ranked games.
     */
    public RatingChange calculateRatingChange(String winnerId, String loserId, 
                                            double winnerRating, double loserRating) {
        // ELO-style rating calculation
        double expectedWinnerScore = 1.0 / (1.0 + Math.pow(10, (loserRating - winnerRating) / 400));
        double expectedLoserScore = 1.0 - expectedWinnerScore;
        
        int kFactor = calculateKFactor(winnerRating);
        
        double winnerNewRating = winnerRating + kFactor * (1.0 - expectedWinnerScore);
        double loserNewRating = loserRating + kFactor * (0.0 - expectedLoserScore);
        
        return RatingChange.builder()
            .winnerId(winnerId)
            .loserId(loserId)
            .winnerOldRating(winnerRating)
            .winnerNewRating(winnerNewRating)
            .loserOldRating(loserRating)
            .loserNewRating(loserNewRating)
            .winnerChange(winnerNewRating - winnerRating)
            .loserChange(loserNewRating - loserRating)
            .build();
    }
    
    /**
     * Get effective card value considering game rules.
     */
    private int getCardValue(Card card) {
        int baseValue = card.getValue();
        
        // Handle Ace high/low rule
        if ("A".equals(card.getRank()) && gameRules.isAceHighValue()) {
            return 14; // Ace high
        }
        
        return baseValue;
    }
    
    /**
     * Calculate K-factor for ELO rating based on current rating.
     */
    private int calculateKFactor(double rating) {
        if (rating < 1200) return 40;
        if (rating < 1800) return 20;
        return 10;
    }
}
```

---

## 🧪 **TESTING STRATEGY**

### **Game Logic Testing Framework**

```java
@Test
public void testCompleteGameFlow() {
    // Create test players
    String player1Id = "test-player-1";
    String player2Id = "test-player-2";
    
    // Create game session
    GameSession session = gameEngine.createGame(player1Id, player2Id, "QUICK");
    assertNotNull(session);
    
    // Start game
    session.startGame();
    assertEquals(GameState.IN_PROGRESS, session.getCurrentState());
    
    // Play 3 rounds
    for (int round = 1; round <= 3; round++) {
        // Both players play cards
        GameMoveResult result1 = session.playCard(player1Id, round * 10, round);
        assertTrue(result1.isSuccess());
        
        GameMoveResult result2 = session.playCard(player2Id, round * 10 + 5, round);
        assertTrue(result2.isSuccess());
        
        // Verify round completion
        assertTrue(session.isRoundComplete());
        
        if (round < 3) {
            session.startNextRound();
        }
    }
    
    // Complete game
    GameResult gameResult = session.completeGame();
    assertNotNull(gameResult);
    assertEquals(GameState.COMPLETED, session.getCurrentState());
    
    // Verify game statistics
    assertTrue(gameResult.getDuration() > 0);
    assertNotNull(gameResult.getWinnerId());
}

@Test
public void testConcurrentCardPlays() {
    String player1Id = "player1";
    String player2Id = "player2";
    
    GameSession session = gameEngine.createGame(player1Id, player2Id, "QUICK");
    session.startGame();
    
    CountDownLatch latch = new CountDownLatch(2);
    AtomicInteger successCount = new AtomicInteger(0);
    
    // Simulate concurrent card plays
    executor.submit(() -> {
        try {
            GameMoveResult result = session.playCard(player1Id, 10, 1);
            if (result.isSuccess()) {
                successCount.incrementAndGet();
            }
        } finally {
            latch.countDown();
        }
    });
    
    executor.submit(() -> {
        try {
            GameMoveResult result = session.playCard(player2Id, 20, 1);
            if (result.isSuccess()) {
                successCount.incrementAndGet();
            }
        } finally {
            latch.countDown();
        }
    });
    
    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(2, successCount.get()); // Both plays should succeed
    assertTrue(session.isRoundComplete());
}
```

---

Đây là foundation của Game Logic Engine. Bạn có muốn tôi tiếp tục với **Database Design** hoặc **Threading Model** không?