package com.n9.shared.constants;

/**
 * Validation Constants for DTOs
 * 
 * Centralized validation rules for consistent input validation
 * across all system components.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class ValidationConstants {
    
    // Prevent instantiation
    private ValidationConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // ============================================================================
    // USERNAME VALIDATION
    // ============================================================================
    
    /**
     * Minimum username length (3 characters)
     */
    public static final int MIN_USERNAME_LENGTH = 3;
    
    /**
     * Maximum username length (50 characters)
     */
    public static final int MAX_USERNAME_LENGTH = 50;
    
    /**
     * Username pattern (alphanumeric, underscore, dash only)
     * Examples: "player01", "user_name", "player-123"
     */
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    
    /**
     * Username pattern description (for error messages)
     */
    public static final String USERNAME_PATTERN_DESC = "Username can only contain letters, numbers, underscore and dash";
    
    // ============================================================================
    // PASSWORD VALIDATION
    // ============================================================================
    
    /**
     * Minimum password length (6 characters)
     * MVP: Plain text, no complexity requirements
     * DEFERRED: Increase to 8+ with complexity requirements
     */
    public static final int MIN_PASSWORD_LENGTH = 6;
    
    /**
     * Maximum password length (100 characters)
     */
    public static final int MAX_PASSWORD_LENGTH = 100;
    
    /**
     * Password pattern (any characters allowed for MVP)
     * DEFERRED: Add complexity requirements (uppercase, lowercase, digit, special char)
     */
    public static final String PASSWORD_PATTERN = ".*"; // Any characters
    
    /**
     * Password pattern description
     */
    public static final String PASSWORD_PATTERN_DESC = "Password must be between 6 and 100 characters";
    
    // ============================================================================
    // EMAIL VALIDATION
    // ============================================================================
    
    /**
     * Maximum email length (255 characters)
     */
    public static final int MAX_EMAIL_LENGTH = 255;
    
    /**
     * Email pattern (basic RFC 5322 compliance)
     */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    /**
     * Email pattern description
     */
    public static final String EMAIL_PATTERN_DESC = "Invalid email format";
    
    // ============================================================================
    // DISPLAY NAME VALIDATION
    // ============================================================================
    
    /**
     * Minimum display name length (1 character)
     */
    public static final int MIN_DISPLAY_NAME_LENGTH = 1;
    
    /**
     * Maximum display name length (100 characters)
     */
    public static final int MAX_DISPLAY_NAME_LENGTH = 100;
    
    /**
     * Display name pattern (allow Unicode, spaces, most special chars)
     * Excludes: <>{}[]|\/
     */
    public static final String DISPLAY_NAME_PATTERN = "^[^<>{}\\[\\]|\\\\]+$";
    
    /**
     * Display name pattern description
     */
    public static final String DISPLAY_NAME_PATTERN_DESC = "Display name cannot contain <>{}[]|\\ characters";
    
    // ============================================================================
    // MESSAGE FIELD VALIDATION
    // ============================================================================
    
    /**
     * Maximum correlation ID length
     */
    public static final int MAX_CORRELATION_ID_LENGTH = 64;
    
    /**
     * Maximum session ID length
     */
    public static final int MAX_SESSION_ID_LENGTH = 64;
    
    /**
     * Maximum user ID length
     */
    public static final int MAX_USER_ID_LENGTH = 64;
    
    /**
     * Maximum match ID length
     */
    public static final int MAX_MATCH_ID_LENGTH = 64;
    
    /**
     * Maximum game ID length
     */
    public static final int MAX_GAME_ID_LENGTH = 64;
    
    /**
     * Maximum message type length
     */
    public static final int MAX_MESSAGE_TYPE_LENGTH = 64;
    
    // ============================================================================
    // NUMERIC VALIDATION
    // ============================================================================
    
    /**
     * Minimum card ID (1)
     */
    public static final int MIN_CARD_ID = 1;
    
    /**
     * Maximum card ID (36 for MVP)
     */
    public static final int MAX_CARD_ID = 36;
    
    /**
     * Minimum round number (1)
     */
    public static final int MIN_ROUND_NUMBER = 1;
    
    /**
     * Maximum round number (3 for MVP)
     */
    public static final int MAX_ROUND_NUMBER = 3;
    
    /**
     * Minimum score (0)
     */
    public static final int MIN_SCORE = 0;
    
    /**
     * Maximum score per game (27 for MVP - 3 rounds × 9 max value)
     */
    public static final int MAX_SCORE = 27;
    
    /**
     * Minimum leaderboard limit
     */
    public static final int MIN_LEADERBOARD_LIMIT = 1;
    
    /**
     * Maximum leaderboard limit
     */
    public static final int MAX_LEADERBOARD_LIMIT = 500;
    
    // ============================================================================
    // TIMESTAMP VALIDATION
    // ============================================================================
    
    /**
     * Minimum valid timestamp (2020-01-01 00:00:00 UTC)
     * Used to detect invalid/garbage timestamps
     */
    public static final long MIN_VALID_TIMESTAMP = 1577836800000L;
    
    /**
     * Maximum valid timestamp (2100-01-01 00:00:00 UTC)
     * Used to detect invalid/future timestamps
     */
    public static final long MAX_VALID_TIMESTAMP = 4102444800000L;
    
    /**
     * Maximum clock skew allowed (5 minutes)
     * Allows for minor time differences between client/server
     */
    public static final long MAX_CLOCK_SKEW_MS = 300000L;
    
    // ============================================================================
    // STRING VALIDATION
    // ============================================================================
    
    /**
     * Maximum error message length
     */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    
    /**
     * Maximum log message length
     */
    public static final int MAX_LOG_MESSAGE_LENGTH = 1000;
    
    /**
     * Maximum JSON string length
     */
    public static final int MAX_JSON_STRING_LENGTH = 1048576; // 1 MB
    
    // ============================================================================
    // ERROR MESSAGES
    // ============================================================================
    
    /**
     * Username validation error message
     */
    public static final String ERR_USERNAME_REQUIRED = "Username is required";
    public static final String ERR_USERNAME_LENGTH = "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters";
    public static final String ERR_USERNAME_PATTERN = USERNAME_PATTERN_DESC;
    
    /**
     * Password validation error message
     */
    public static final String ERR_PASSWORD_REQUIRED = "Password is required";
    public static final String ERR_PASSWORD_LENGTH = "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters";
    
    /**
     * Email validation error message
     */
    public static final String ERR_EMAIL_REQUIRED = "Email is required";
    public static final String ERR_EMAIL_INVALID = EMAIL_PATTERN_DESC;
    public static final String ERR_EMAIL_LENGTH = "Email must not exceed " + MAX_EMAIL_LENGTH + " characters";
    
    /**
     * Display name validation error message
     */
    public static final String ERR_DISPLAY_NAME_LENGTH = "Display name must be between " + MIN_DISPLAY_NAME_LENGTH + " and " + MAX_DISPLAY_NAME_LENGTH + " characters";
    public static final String ERR_DISPLAY_NAME_PATTERN = DISPLAY_NAME_PATTERN_DESC;
    
    /**
     * Card validation error message
     */
    public static final String ERR_CARD_ID_INVALID = "Card ID must be between " + MIN_CARD_ID + " and " + MAX_CARD_ID;
    
    /**
     * Round validation error message
     */
    public static final String ERR_ROUND_NUMBER_INVALID = "Round number must be between " + MIN_ROUND_NUMBER + " and " + MAX_ROUND_NUMBER;
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if username is valid
     * 
     * @param username Username to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        int length = username.trim().length();
        return length >= MIN_USERNAME_LENGTH && 
               length <= MAX_USERNAME_LENGTH && 
               username.matches(USERNAME_PATTERN);
    }
    
    /**
     * Check if password is valid
     * 
     * @param password Password to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        int length = password.length();
        return length >= MIN_PASSWORD_LENGTH && length <= MAX_PASSWORD_LENGTH;
    }
    
    /**
     * Check if email is valid
     * 
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return email.length() <= MAX_EMAIL_LENGTH && 
               email.matches(EMAIL_PATTERN);
    }
    
    /**
     * Check if timestamp is valid
     * 
     * @param timestamp Timestamp to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTimestamp(long timestamp) {
        return timestamp >= MIN_VALID_TIMESTAMP && timestamp <= MAX_VALID_TIMESTAMP;
    }
    
    /**
     * Check if timestamp is within acceptable clock skew
     * 
     * @param timestamp Timestamp to check
     * @param currentTime Current server time
     * @return true if within skew, false otherwise
     */
    public static boolean isWithinClockSkew(long timestamp, long currentTime) {
        return Math.abs(timestamp - currentTime) <= MAX_CLOCK_SKEW_MS;
    }
}
