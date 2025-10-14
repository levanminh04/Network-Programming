package com.n9.shared.constants;

/**
 * Network Protocol Constants
 * 
 * Defines protocol version, message size limits, timeouts, and other
 * protocol-level configuration shared across all components.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class ProtocolConstants {
    
    // Prevent instantiation
    private ProtocolConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // ============================================================================
    // PROTOCOL VERSION
    // ============================================================================
    
    /**
     * Current protocol version (Semantic Versioning: MAJOR.MINOR.PATCH)
     * - MAJOR: Breaking changes
     * - MINOR: New features, backward compatible
     * - PATCH: Bug fixes
     */
    public static final String PROTOCOL_VERSION = "1.0.0";
    
    /**
     * Protocol version major number
     */
    public static final int PROTOCOL_VERSION_MAJOR = 1;
    
    /**
     * Protocol version minor number
     */
    public static final int PROTOCOL_VERSION_MINOR = 0;
    
    /**
     * Protocol version patch number
     */
    public static final int PROTOCOL_VERSION_PATCH = 0;
    
    // ============================================================================
    // MESSAGE SIZE LIMITS
    // ============================================================================
    
    /**
     * Maximum message size in bytes (1 MB)
     * Prevents memory exhaustion attacks
     */
    public static final int MAX_MESSAGE_SIZE_BYTES = 1024 * 1024; // 1 MB
    
    /**
     * Maximum payload size in bytes (512 KB)
     */
    public static final int MAX_PAYLOAD_SIZE_BYTES = 512 * 1024; // 512 KB
    
    /**
     * Maximum JSON message size (same as message size)
     */
    public static final int MAX_JSON_SIZE_BYTES = MAX_MESSAGE_SIZE_BYTES;
    
    /**
     * Warning threshold for message size (768 KB)
     * Log warning if message exceeds this
     */
    public static final int MESSAGE_SIZE_WARNING_THRESHOLD = 768 * 1024;
    
    // ============================================================================
    // CONNECTION TIMEOUTS
    // ============================================================================
    
    /**
     * Connection establishment timeout (30 seconds)
     */
    public static final int CONNECTION_TIMEOUT_MS = 30000;
    
    /**
     * Socket read timeout (60 seconds)
     */
    public static final int READ_TIMEOUT_MS = 60000;
    
    /**
     * Socket write timeout (30 seconds)
     */
    public static final int WRITE_TIMEOUT_MS = 30000;
    
    /**
     * Idle connection timeout before considering disconnected (120 seconds)
     */
    public static final int IDLE_TIMEOUT_MS = 120000;
    
    // ============================================================================
    // HEARTBEAT / PING CONFIGURATION
    // ============================================================================
    
    /**
     * Heartbeat interval in seconds (30 seconds)
     * Client should send PING every 30 seconds
     */
    public static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    
    /**
     * Heartbeat interval in milliseconds
     */
    public static final int HEARTBEAT_INTERVAL_MS = HEARTBEAT_INTERVAL_SECONDS * 1000;
    
    /**
     * Heartbeat timeout (3 × interval = 90 seconds)
     * Server considers client dead if no heartbeat for 90 seconds
     */
    public static final int HEARTBEAT_TIMEOUT_SECONDS = HEARTBEAT_INTERVAL_SECONDS * 3;
    
    /**
     * Heartbeat timeout in milliseconds
     */
    public static final int HEARTBEAT_TIMEOUT_MS = HEARTBEAT_TIMEOUT_SECONDS * 1000;
    
    /**
     * Maximum missed heartbeats before disconnect
     */
    public static final int MAX_MISSED_HEARTBEATS = 3;
    
    // ============================================================================
    // RECONNECTION CONFIGURATION
    // ============================================================================
    
    /**
     * Maximum reconnection attempts (5 attempts)
     */
    public static final int MAX_RECONNECT_ATTEMPTS = 5;
    
    /**
     * Initial reconnection delay (2 seconds)
     */
    public static final int RECONNECT_DELAY_MS = 2000;
    
    /**
     * Maximum reconnection delay with exponential backoff (30 seconds)
     */
    public static final int RECONNECT_MAX_DELAY_MS = 30000;
    
    /**
     * Reconnection backoff multiplier (2x each attempt)
     */
    public static final double RECONNECT_BACKOFF_MULTIPLIER = 2.0;
    
    /**
     * Reconnection jitter (±500ms random)
     * Prevents thundering herd
     */
    public static final int RECONNECT_JITTER_MS = 500;
    
    // ============================================================================
    // RATE LIMITING
    // ============================================================================
    
    /**
     * Maximum requests per second per connection (10 req/s)
     */
    public static final int MAX_REQUESTS_PER_SECOND = 10;
    
    /**
     * Maximum requests per minute per connection (300 req/min)
     */
    public static final int MAX_REQUESTS_PER_MINUTE = 300;
    
    /**
     * Maximum requests per hour per connection (5000 req/hr)
     */
    public static final int MAX_REQUESTS_PER_HOUR = 5000;
    
    /**
     * Rate limit window size in milliseconds (1 second)
     */
    public static final int RATE_LIMIT_WINDOW_MS = 1000;
    
    /**
     * Rate limit penalty duration (60 seconds)
     * Client is throttled for this duration if exceeding rate limit
     */
    public static final int RATE_LIMIT_PENALTY_MS = 60000;
    
    // ============================================================================
    // WEBSOCKET SPECIFIC (for Gateway)
    // ============================================================================
    
    /**
     * WebSocket endpoint path
     */
    public static final String WEBSOCKET_ENDPOINT = "/ws/game";
    
    /**
     * WebSocket subprotocol (optional)
     */
    public static final String WEBSOCKET_SUBPROTOCOL = "cardgame-v1";
    
    /**
     * Maximum WebSocket frame size (1 MB)
     */
    public static final int MAX_WEBSOCKET_FRAME_SIZE = 1024 * 1024;
    
    /**
     * WebSocket ping interval (same as heartbeat)
     */
    public static final int WEBSOCKET_PING_INTERVAL_MS = HEARTBEAT_INTERVAL_MS;
    
    // ============================================================================
    // TCP SPECIFIC (for Core)
    // ============================================================================
    
    /**
     * TCP server port (Core server)
     */
    public static final int TCP_SERVER_PORT = 9999;
    
    /**
     * TCP backlog size (max pending connections)
     */
    public static final int TCP_BACKLOG_SIZE = 50;
    
    /**
     * TCP keep-alive enabled
     */
    public static final boolean TCP_KEEP_ALIVE = true;
    
    /**
     * TCP no-delay (Nagle's algorithm disabled)
     */
    public static final boolean TCP_NO_DELAY = true;
    
    // ============================================================================
    // CORRELATION ID
    // ============================================================================
    
    /**
     * Maximum correlation ID length
     */
    public static final int MAX_CORRELATION_ID_LENGTH = 64;
    
    /**
     * Correlation ID prefix for client-generated IDs
     */
    public static final String CORRELATION_ID_PREFIX = "cor-";
    
    /**
     * Correlation ID timeout (5 minutes)
     * Remove correlation mapping after this time
     */
    public static final int CORRELATION_TIMEOUT_MS = 300000;
    
    // ============================================================================
    // BUFFER SIZES
    // ============================================================================
    
    /**
     * Default buffer size for I/O operations (8 KB)
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    
    /**
     * Large buffer size for batch operations (64 KB)
     */
    public static final int LARGE_BUFFER_SIZE = 65536;
    
    /**
     * String builder initial capacity
     */
    public static final int STRING_BUILDER_CAPACITY = 256;
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if protocol version is compatible
     * MVP: Exact match only
     * Future: Can implement semantic versioning compatibility
     * 
     * @param version Version string to check (e.g., "1.0.0")
     * @return true if compatible, false otherwise
     */
    public static boolean isCompatibleVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        // MVP: Exact match only
        return PROTOCOL_VERSION.equals(version);
        
        // Future: Semantic versioning compatibility
        // Compare major versions - compatible if major version matches
    }
    
    /**
     * Get major version from version string
     * 
     * @param version Version string (e.g., "1.0.0")
     * @return Major version number
     */
    public static int getMajorVersion(String version) {
        if (version == null || version.isEmpty()) {
            return 0;
        }
        
        String[] parts = version.split("\\.");
        if (parts.length < 1) {
            return 0;
        }
        
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Calculate reconnection delay with exponential backoff
     * 
     * @param attemptNumber Reconnection attempt number (0-based)
     * @return Delay in milliseconds
     */
    public static long calculateReconnectDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            return RECONNECT_DELAY_MS;
        }
        
        long delay = (long) (RECONNECT_DELAY_MS * Math.pow(RECONNECT_BACKOFF_MULTIPLIER, attemptNumber));
        
        // Cap at maximum
        return Math.min(delay, RECONNECT_MAX_DELAY_MS);
    }
    
    /**
     * Check if message size is within limits
     * 
     * @param messageSize Size in bytes
     * @return true if within limits, false otherwise
     */
    public static boolean isValidMessageSize(int messageSize) {
        return messageSize > 0 && messageSize <= MAX_MESSAGE_SIZE_BYTES;
    }
}
