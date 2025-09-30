package com.n9.shared.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating unique identifiers, correlation IDs, and other
 * protocol-specific ID values. Provides thread-safe generation methods optimized
 * for network protocols and distributed systems.
 * 
 * Features:
 * - UUID-based correlation ID generation
 * - Match ID generation with timestamp prefixes
 * - Session ID generation with security considerations
 * - Short ID generation for temporary references
 * - Timestamp-based ID generation for ordering
 * 
 * Usage:
 * <pre>
 * // Generate correlation ID for message tracking
 * String correlationId = IdUtils.generateCorrelationId();
 * 
 * // Generate match ID for game sessions
 * String matchId = IdUtils.generateMatchId();
 * 
 * // Generate session ID for user connections
 * String sessionId = IdUtils.generateSessionId();
 * </pre>
 * 
 * @author Network Programming Team
 * @version 1.0.0
 * @since 2024-01-09
 */
public final class IdUtils {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] ALPHANUMERIC = 
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] NUMERIC = "0123456789".toCharArray();
    
    // ID prefixes for different types
    private static final String CORRELATION_PREFIX = "cor";
    private static final String MATCH_PREFIX = "match";
    private static final String SESSION_PREFIX = "sess";
    private static final String USER_PREFIX = "user";
    private static final String GAME_PREFIX = "game";
    
    // ID length constants
    private static final int SHORT_ID_LENGTH = 8;
    private static final int MEDIUM_ID_LENGTH = 16;
    private static final int LONG_ID_LENGTH = 32;
    
    private IdUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Generate a unique correlation ID for message tracking.
     * Format: cor-{timestamp}-{random}
     * Example: cor-1736412000-abc123def
     * 
     * @return unique correlation ID string
     */
    public static String generateCorrelationId() {
        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
        String randomPart = generateRandomString(9, ALPHANUMERIC);
        return String.format("%s-%d-%s", CORRELATION_PREFIX, timestamp, randomPart);
    }
    
    /**
     * Generate a unique match ID for game sessions.
     * Format: match-{timestamp}-{random}
     * Example: match-1736412000-xyz789
     * 
     * @return unique match ID string
     */
    public static String generateMatchId() {
        long timestamp = System.currentTimeMillis() / 1000;
        String randomPart = generateRandomString(6, ALPHANUMERIC);
        return String.format("%s-%d-%s", MATCH_PREFIX, timestamp, randomPart);
    }
    
    /**
     * Generate a unique session ID for user connections.
     * Format: sess-{uuid-without-dashes}
     * Example: sess-abc123def456ghi789jkl012
     * 
     * @return unique session ID string
     */
    public static String generateSessionId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return SESSION_PREFIX + "-" + uuid;
    }
    
    /**
     * Generate a unique user ID.
     * Format: user-{timestamp}-{random}
     * Example: user-1736412000-xyz123
     * 
     * @return unique user ID string
     */
    public static String generateUserId() {
        long timestamp = System.currentTimeMillis() / 1000;
        String randomPart = generateRandomString(6, ALPHANUMERIC);
        return String.format("%s-%d-%s", USER_PREFIX, timestamp, randomPart);
    }
    
    /**
     * Generate a unique game instance ID.
     * Format: game-{timestamp}-{random}
     * Example: game-1736412000-abc789
     * 
     * @return unique game ID string
     */
    public static String generateGameId() {
        long timestamp = System.currentTimeMillis() / 1000;
        String randomPart = generateRandomString(6, ALPHANUMERIC);
        return String.format("%s-%d-%s", GAME_PREFIX, timestamp, randomPart);
    }
    
    /**
     * Generate a short random ID for temporary references.
     * Useful for short-lived identifiers that don't need global uniqueness.
     * 
     * @return random 8-character alphanumeric string
     */
    public static String generateShortId() {
        return generateRandomString(SHORT_ID_LENGTH, ALPHANUMERIC);
    }
    
    /**
     * Generate a medium random ID for moderate-duration references.
     * 
     * @return random 16-character alphanumeric string
     */
    public static String generateMediumId() {
        return generateRandomString(MEDIUM_ID_LENGTH, ALPHANUMERIC);
    }
    
    /**
     * Generate a long random ID for high-security or long-duration references.
     * 
     * @return random 32-character alphanumeric string
     */
    public static String generateLongId() {
        return generateRandomString(LONG_ID_LENGTH, ALPHANUMERIC);
    }
    
    /**
     * Generate a numeric-only ID of specified length.
     * Useful for invite codes, room codes, etc.
     * 
     * @param length the desired length of the numeric ID
     * @return random numeric string of specified length
     */
    public static String generateNumericId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        return generateRandomString(length, NUMERIC);
    }
    
    /**
     * Generate a 6-digit numeric invite code.
     * Commonly used for room invitations and short-term access codes.
     * 
     * @return 6-digit numeric string
     */
    public static String generateInviteCode() {
        return generateNumericId(6);
    }
    
    /**
     * Generate a UUID-based ID without dashes.
     * Provides maximum uniqueness guarantee with compact format.
     * 
     * @return UUID string without dashes (32 characters)
     */
    public static String generateUuidId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a UUID-based ID with custom prefix.
     * 
     * @param prefix the prefix to add to the UUID
     * @return prefixed UUID string without dashes
     */
    public static String generatePrefixedUuidId(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + "-" + uuid;
    }
    
    /**
     * Generate a timestamp-based ID for chronological ordering.
     * Format: {timestamp}-{random}
     * Useful when you need IDs that maintain creation order.
     * 
     * @return timestamp-based ID string
     */
    public static String generateTimestampId() {
        long timestamp = System.currentTimeMillis();
        String randomPart = generateRandomString(8, ALPHANUMERIC);
        return timestamp + "-" + randomPart;
    }
    
    /**
     * Generate a human-readable timestamp ID with date formatting.
     * Format: {yyyyMMdd-HHmmss}-{random}
     * Example: 20240109-143022-abc123
     * 
     * @return human-readable timestamp ID
     */
    public static String generateReadableTimestampId() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String randomPart = generateRandomString(6, ALPHANUMERIC);
        return timestamp + "-" + randomPart;
    }
    
    /**
     * Extract timestamp from a timestamp-based ID.
     * Works with IDs generated by generateTimestampId().
     * 
     * @param timestampId the timestamp-based ID
     * @return timestamp in milliseconds, or -1 if extraction fails
     */
    public static long extractTimestamp(String timestampId) {
        if (timestampId == null || timestampId.trim().isEmpty()) {
            return -1;
        }
        
        int dashIndex = timestampId.indexOf('-');
        if (dashIndex == -1) {
            return -1;
        }
        
        try {
            return Long.parseLong(timestampId.substring(0, dashIndex));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Extract prefix from a prefixed ID.
     * Works with IDs that follow the pattern: prefix-{rest}
     * 
     * @param prefixedId the prefixed ID
     * @return the prefix part, or null if no prefix found
     */
    public static String extractPrefix(String prefixedId) {
        if (prefixedId == null || prefixedId.trim().isEmpty()) {
            return null;
        }
        
        int dashIndex = prefixedId.indexOf('-');
        if (dashIndex == -1) {
            return null;
        }
        
        return prefixedId.substring(0, dashIndex);
    }
    
    /**
     * Validate if an ID matches the expected pattern for correlation IDs.
     * 
     * @param correlationId the ID to validate
     * @return true if ID matches correlation ID pattern, false otherwise
     */
    public static boolean isValidCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return false;
        }
        
        return correlationId.startsWith(CORRELATION_PREFIX + "-") &&
               correlationId.length() >= 15 && // Minimum reasonable length
               correlationId.length() <= 50;   // Maximum reasonable length
    }
    
    /**
     * Validate if an ID matches the expected pattern for match IDs.
     * 
     * @param matchId the ID to validate
     * @return true if ID matches match ID pattern, false otherwise
     */
    public static boolean isValidMatchId(String matchId) {
        if (matchId == null || matchId.trim().isEmpty()) {
            return false;
        }
        
        return matchId.startsWith(MATCH_PREFIX + "-") &&
               matchId.length() >= 12 &&
               matchId.length() <= 50;
    }
    
    /**
     * Validate if an ID matches the expected pattern for session IDs.
     * 
     * @param sessionId the ID to validate
     * @return true if ID matches session ID pattern, false otherwise
     */
    public static boolean isValidSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        return sessionId.startsWith(SESSION_PREFIX + "-") &&
               sessionId.length() >= 36; // UUID length + prefix
    }
    
    /**
     * Generate a secure random string of specified length using the given character set.
     * 
     * @param length the desired length of the random string
     * @param charSet the character set to use for generation
     * @return random string of specified length
     */
    private static String generateRandomString(int length, char[] charSet) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(charSet.length);
            sb.append(charSet[randomIndex]);
        }
        return sb.toString();
    }
    
    /**
     * Generate a random string with mixed case and numbers (high entropy).
     * Uses cryptographically secure random number generation.
     * 
     * @param length the desired length
     * @return secure random string
     */
    public static String generateSecureRandomString(int length) {
        return generateRandomString(length, ALPHANUMERIC);
    }
    
    /**
     * Generate a random string optimized for human readability.
     * Excludes confusing characters like 0/O, 1/l/I.
     * 
     * @param length the desired length
     * @return human-readable random string
     */
    public static String generateReadableRandomString(int length) {
        char[] readableChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
        return generateRandomString(length, readableChars);
    }
    
    /**
     * Check if two IDs were likely generated around the same time.
     * Useful for detecting related operations or potential duplicates.
     * 
     * @param id1 first timestamp-based ID
     * @param id2 second timestamp-based ID
     * @param toleranceMs tolerance in milliseconds
     * @return true if IDs were generated within tolerance of each other
     */
    public static boolean areTimestampsClose(String id1, String id2, long toleranceMs) {
        long timestamp1 = extractTimestamp(id1);
        long timestamp2 = extractTimestamp(id2);
        
        if (timestamp1 == -1 || timestamp2 == -1) {
            return false;
        }
        
        return Math.abs(timestamp1 - timestamp2) <= toleranceMs;
    }
    
    /**
     * Get the age of a timestamp-based ID in milliseconds.
     * 
     * @param timestampId the timestamp-based ID
     * @return age in milliseconds, or -1 if timestamp extraction fails
     */
    public static long getIdAge(String timestampId) {
        long timestamp = extractTimestamp(timestampId);
        if (timestamp == -1) {
            return -1;
        }
        
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Format timestamp from ID as human-readable string.
     * 
     * @param timestampId the timestamp-based ID
     * @return formatted timestamp string, or null if extraction fails
     */
    public static String formatIdTimestamp(String timestampId) {
        long timestamp = extractTimestamp(timestampId);
        if (timestamp == -1) {
            return null;
        }
        
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}