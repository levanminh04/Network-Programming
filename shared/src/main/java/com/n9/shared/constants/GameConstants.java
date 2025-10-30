package com.n9.shared.constants;

/**
 * Game Rule Constants (MVP)
 * 
 * These values MUST match the database schema and frontend logic.
 * Any changes require coordination across all modules.
 * 
 * Database Reference: core/docs/database/V1__cardgame_mvp.sql
 * - cards table: 36 cards (4 suits × 9 ranks)
 * - games table: total_rounds = 3
 * - game_rounds table: round_number 1-3
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class GameConstants {
    
    // Prevent instantiation
    private GameConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // ============================================================================
    // CARD DECK CONSTANTS (MVP: 36-card deck)
    // ============================================================================
    
    /**
     * Total number of cards in deck (MVP uses 36-card deck)
     * 4 suits × 9 ranks (A, 2-9) = 36 cards
     * DEFERRED: 52 cards for full deck (would include J, Q, K)
     */
    public static final int DECK_SIZE = 36;
    
    /**
     * Number of suits in deck (Hearts, Diamonds, Clubs, Spades)
     */
    public static final int SUITS_COUNT = 4;
    
    /**
     * Number of ranks in deck (A, 2, 3, 4, 5, 6, 7, 8, 9)
     * DEFERRED: 13 ranks for full deck (would include J, Q, K)
     */
    public static final int RANKS_COUNT = 9;
    
    /**
     * Minimum card value (Ace = 1)
     */
    public static final int MIN_CARD_VALUE = 1;
    
    /**
     * Maximum card value (9 in MVP)
     * DEFERRED: 13 for full deck (King)
     */
    public static final int MAX_CARD_VALUE = 9;
    
    // ============================================================================
    // GAME RULES CONSTANTS
    // ============================================================================
    
    /**
     * Total number of rounds per game (FIXED for MVP)
     * Database: games.total_rounds = 3
     */
    public static final int TOTAL_ROUNDS = 3;
    
    /**
     * Minimum number of rounds (cannot be less)
     */
    public static final int MIN_ROUNDS = 3;
    
    /**
     * Maximum number of rounds (cannot be more in MVP)
     */
    public static final int MAX_ROUNDS = 3;
    
    /**
     * Round timeout in seconds (player has 10 seconds to select card)
     * After timeout, server auto-picks random card
     */
    public static final int ROUND_TIMEOUT_SECONDS = 10;
    
    /**
     * Round timeout in milliseconds (for JavaScript/frontend)
     */
    public static final int ROUND_TIMEOUT_MS = ROUND_TIMEOUT_SECONDS * 1000;
    
    /**
     * Grace period before auto-pick (milliseconds)
     * Allows for network latency
     */
    public static final int AUTO_PICK_GRACE_PERIOD_MS = 100;
    
    // ============================================================================
    // PLAYER CONSTANTS
    // ============================================================================
    
    /**
     * Number of players per game (1v1 only in MVP)
     */
    public static final int PLAYERS_PER_GAME = 2;
    
    /**
     * Minimum players required to start game
     */
    public static final int MIN_PLAYERS = 2;
    
    /**
     * Maximum players allowed in game
     */
    public static final int MAX_PLAYERS = 2;
    
    /**
     * Player 1 index (zero-based)
     */
    public static final int PLAYER1_INDEX = 0;
    
    /**
     * Player 2 index (zero-based)
     */
    public static final int PLAYER2_INDEX = 1;
    
    // ============================================================================
    // SCORING CONSTANTS
    // ============================================================================
    
    /**
     * Points awarded for winning a round (simplified scoring: 1 point per win)
     */
    public static final int POINTS_PER_WIN = 1;
    
    /**
     * Points awarded for winning a round (card value)
     */
    public static final int ROUND_WIN_POINTS_IS_CARD_VALUE = 0; // Placeholder - use actual card value
    
    /**
     * Points awarded for draw (both players get 0)
     */
    public static final int ROUND_DRAW_POINTS = 0;
    
    /**
     * Initial score at game start
     */
    public static final int INITIAL_SCORE = 0;
    
    /**
     * Minimum possible score
     */
    public static final int MIN_SCORE = 0;
    
    /**
     * Maximum possible score per game (if winning all 3 rounds with 9s)
     */
    public static final int MAX_SCORE_PER_GAME = MAX_CARD_VALUE * TOTAL_ROUNDS; // 27
    
    // ============================================================================
    // LEADERBOARD CONSTANTS
    // ============================================================================
    
    /**
     * Default number of entries to show in leaderboard
     */
    public static final int DEFAULT_LEADERBOARD_LIMIT = 100;
    
    /**
     * Maximum number of entries allowed in leaderboard request
     */
    public static final int MAX_LEADERBOARD_LIMIT = 500;
    
    /**
     * Minimum leaderboard limit
     */
    public static final int MIN_LEADERBOARD_LIMIT = 10;
    
    // ============================================================================
    // MATCHMAKING CONSTANTS
    // ============================================================================
    
    /**
     * Maximum time to wait for matchmaking (seconds)
     */
    public static final int MATCHMAKING_TIMEOUT_SECONDS = 60;
    
    /**
     * Time to wait before starting game after match found (seconds)
     */
    public static final int GAME_START_COUNTDOWN_SECONDS = 3;
    
    /**
     * Game start countdown in milliseconds
     */
    public static final int GAME_START_COUNTDOWN_MS = GAME_START_COUNTDOWN_SECONDS * 1000;
    
    // ============================================================================
    // GAME MODE CONSTANTS (MVP: QUICK_MATCH only)
    // ============================================================================
    
    /**
     * Quick match mode (default for MVP)
     */
    public static final String GAME_MODE_QUICK_MATCH = "QUICK_MATCH";
    
    /**
     * Ranked mode (DEFERRED)
     */
    public static final String GAME_MODE_RANKED = "RANKED";
    
    /**
     * Custom match mode (DEFERRED)
     */
    public static final String GAME_MODE_CUSTOM = "CUSTOM";
    /**
     * Kích thước tin nhắn tối đa (bytes) cho phép từ Gateway.
     * 65536 bytes = 64KB. Giúp chống lại tấn công OutOfMemoryError.
     */
    public static final int MAX_MESSAGE_SIZE = 65536;
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if round number is valid
     * 
     * @param roundNumber The round number to check (1-3)
     * @return true if valid, false otherwise
     */
    public static boolean isValidRoundNumber(int roundNumber) {
        return roundNumber >= 1 && roundNumber <= TOTAL_ROUNDS;
    }
    
    /**
     * Check if card value is valid
     * 
     * @param cardValue The card value to check (1-9)
     * @return true if valid, false otherwise
     */
    public static boolean isValidCardValue(int cardValue) {
        return cardValue >= MIN_CARD_VALUE && cardValue <= MAX_CARD_VALUE;
    }
    
    /**
     * Check if card ID is valid (1-36)
     * 
     * @param cardId The card ID to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCardId(int cardId) {
        return cardId >= 1 && cardId <= DECK_SIZE;
    }
    
    /**
     * Check if player count is valid
     * 
     * @param playerCount The number of players
     * @return true if valid, false otherwise
     */
    public static boolean isValidPlayerCount(int playerCount) {
        return playerCount >= MIN_PLAYERS && playerCount <= MAX_PLAYERS;
    }
    
    /**
     * Get round timeout in milliseconds
     * 
     * @return Round timeout in milliseconds
     */
    public static long getRoundTimeoutMs() {
        return ROUND_TIMEOUT_MS;
    }
    
    /**
     * Calculate deadline epoch milliseconds for a round
     * 
     * @param startTime Round start time in epoch milliseconds
     * @return Deadline epoch milliseconds
     */
    public static long calculateRoundDeadline(long startTime) {
        return startTime + ROUND_TIMEOUT_MS;
    }
    
    /**
     * Check if time is past deadline
     * 
     * @param currentTime Current time in epoch milliseconds
     * @param deadline Deadline in epoch milliseconds
     * @return true if past deadline, false otherwise
     */
    public static boolean isPastDeadline(long currentTime, long deadline) {
        return currentTime > deadline;
    }
}
