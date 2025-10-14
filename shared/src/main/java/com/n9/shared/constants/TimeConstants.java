package com.n9.shared.constants;

/**
 * Time-related Constants
 * 
 * Centralized time values for consistency.
 * All values in milliseconds unless otherwise specified.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class TimeConstants {
    
    // Prevent instantiation
    private TimeConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // ============================================================================
    // SECOND/MINUTE/HOUR CONVERSIONS
    // ============================================================================
    
    /**
     * Milliseconds in 1 second
     */
    public static final long SECOND_MS = 1000L;
    
    /**
     * Milliseconds in 1 minute
     */
    public static final long MINUTE_MS = 60 * SECOND_MS;
    
    /**
     * Milliseconds in 1 hour
     */
    public static final long HOUR_MS = 60 * MINUTE_MS;
    
    /**
     * Milliseconds in 1 day
     */
    public static final long DAY_MS = 24 * HOUR_MS;
    
    // ============================================================================
    // GAME TIMEOUTS
    // ============================================================================
    
    /**
     * Round timeout (10 seconds in MVP)
     */
    public static final long ROUND_TIMEOUT_MS = 10 * SECOND_MS;
    
    /**
     * Grace period before auto-pick (100ms for network latency)
     */
    public static final long AUTO_PICK_GRACE_PERIOD_MS = 100L;
    
    /**
     * Game start countdown (3 seconds)
     */
    public static final long GAME_START_COUNTDOWN_MS = 3 * SECOND_MS;
    
    /**
     * Matchmaking timeout (60 seconds)
     */
    public static final long MATCHMAKING_TIMEOUT_MS = 60 * SECOND_MS;
    
    /**
     * Maximum game duration (10 minutes - safety limit)
     */
    public static final long MAX_GAME_DURATION_MS = 10 * MINUTE_MS;
    
    // ============================================================================
    // SESSION TIMEOUTS
    // ============================================================================
    
    /**
     * Session expiration time (24 hours)
     */
    public static final long SESSION_EXPIRATION_MS = 24 * HOUR_MS;
    
    /**
     * Session idle timeout (30 minutes of inactivity)
     */
    public static final long SESSION_IDLE_TIMEOUT_MS = 30 * MINUTE_MS;
    
    /**
     * Session refresh threshold (1 hour before expiration)
     */
    public static final long SESSION_REFRESH_THRESHOLD_MS = 1 * HOUR_MS;
    
    // ============================================================================
    // HEARTBEAT / PING
    // ============================================================================
    
    /**
     * Heartbeat interval (30 seconds)
     */
    public static final long HEARTBEAT_INTERVAL_MS = 30 * SECOND_MS;
    
    /**
     * Heartbeat timeout (90 seconds = 3 × interval)
     */
    public static final long HEARTBEAT_TIMEOUT_MS = 3 * HEARTBEAT_INTERVAL_MS;
    
    /**
     * Ping interval (same as heartbeat)
     */
    public static final long PING_INTERVAL_MS = HEARTBEAT_INTERVAL_MS;
    
    /**
     * Ping timeout (5 seconds)
     */
    public static final long PING_TIMEOUT_MS = 5 * SECOND_MS;
    
    // ============================================================================
    // CONNECTION TIMEOUTS
    // ============================================================================
    
    /**
     * Connection establishment timeout (30 seconds)
     */
    public static final long CONNECTION_TIMEOUT_MS = 30 * SECOND_MS;
    
    /**
     * Socket read timeout (60 seconds)
     */
    public static final long READ_TIMEOUT_MS = 60 * SECOND_MS;
    
    /**
     * Socket write timeout (30 seconds)
     */
    public static final long WRITE_TIMEOUT_MS = 30 * SECOND_MS;
    
    /**
     * Idle connection timeout (120 seconds)
     */
    public static final long IDLE_TIMEOUT_MS = 120 * SECOND_MS;
    
    // ============================================================================
    // RECONNECTION
    // ============================================================================
    
    /**
     * Initial reconnection delay (2 seconds)
     */
    public static final long RECONNECT_DELAY_MS = 2 * SECOND_MS;
    
    /**
     * Maximum reconnection delay (30 seconds with exponential backoff)
     */
    public static final long RECONNECT_MAX_DELAY_MS = 30 * SECOND_MS;
    
    /**
     * Reconnection window (player has 2 minutes to reconnect)
     */
    public static final long RECONNECT_WINDOW_MS = 2 * MINUTE_MS;
    
    // ============================================================================
    // RATE LIMITING
    // ============================================================================
    
    /**
     * Rate limit window size (1 second)
     */
    public static final long RATE_LIMIT_WINDOW_MS = 1 * SECOND_MS;
    
    /**
     * Rate limit penalty duration (60 seconds)
     */
    public static final long RATE_LIMIT_PENALTY_MS = 60 * SECOND_MS;
    
    /**
     * Rate limit reset interval (1 minute)
     */
    public static final long RATE_LIMIT_RESET_MS = 1 * MINUTE_MS;
    
    // ============================================================================
    // CACHE / CLEANUP
    // ============================================================================
    
    /**
     * Correlation ID cache timeout (5 minutes)
     */
    public static final long CORRELATION_CACHE_TIMEOUT_MS = 5 * MINUTE_MS;
    
    /**
     * Completed game cleanup delay (1 hour)
     */
    public static final long GAME_CLEANUP_DELAY_MS = 1 * HOUR_MS;
    
    /**
     * Abandoned game cleanup delay (10 minutes)
     */
    public static final long ABANDONED_GAME_CLEANUP_MS = 10 * MINUTE_MS;
    
    /**
     * Statistics cache TTL (5 minutes)
     */
    public static final long STATS_CACHE_TTL_MS = 5 * MINUTE_MS;
    
    /**
     * Leaderboard cache TTL (1 minute)
     */
    public static final long LEADERBOARD_CACHE_TTL_MS = 1 * MINUTE_MS;
    
    // ============================================================================
    // DELAYS / THROTTLING
    // ============================================================================
    
    /**
     * UI update throttle (100ms - max 10 updates/second)
     */
    public static final long UI_UPDATE_THROTTLE_MS = 100L;
    
    /**
     * Lobby refresh interval (5 seconds)
     */
    public static final long LOBBY_REFRESH_INTERVAL_MS = 5 * SECOND_MS;
    
    /**
     * Leaderboard refresh interval (30 seconds)
     */
    public static final long LEADERBOARD_REFRESH_INTERVAL_MS = 30 * SECOND_MS;
    
    /**
     * Minimum delay between actions (500ms debounce)
     */
    public static final long ACTION_DEBOUNCE_MS = 500L;
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Convert seconds to milliseconds
     * 
     * @param seconds Seconds
     * @return Milliseconds
     */
    public static long toMillis(int seconds) {
        return seconds * SECOND_MS;
    }
    
    /**
     * Convert milliseconds to seconds
     * 
     * @param millis Milliseconds
     * @return Seconds
     */
    public static long toSeconds(long millis) {
        return millis / SECOND_MS;
    }
    
    /**
     * Get current epoch milliseconds
     * 
     * @return Current timestamp
     */
    public static long now() {
        return System.currentTimeMillis();
    }
    
    /**
     * Calculate deadline from now
     * 
     * @param durationMs Duration in milliseconds
     * @return Deadline timestamp
     */
    public static long deadline(long durationMs) {
        return now() + durationMs;
    }
    
    /**
     * Check if deadline has passed
     * 
     * @param deadline Deadline timestamp
     * @return true if past deadline
     */
    public static boolean isPast(long deadline) {
        return now() > deadline;
    }
    
    /**
     * Get remaining time until deadline
     * 
     * @param deadline Deadline timestamp
     * @return Remaining milliseconds (0 if past deadline)
     */
    public static long remaining(long deadline) {
        return Math.max(0, deadline - now());
    }
}
