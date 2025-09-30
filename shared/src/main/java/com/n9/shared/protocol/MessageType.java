package com.n9.shared.protocol;

/**
 * Centralized Message Type Registry
 * 
 * This class defines all message types used across the card game system.
 * Following a hierarchical naming convention: DOMAIN.ACTION[.MODIFIER]
 * 
 * Design Principles:
 * - Hierarchical organization by functional domain
 * - Clear action semantics (REQUEST/RESPONSE/EVENT/NOTIFICATION)
 * - Future-proof naming that can scale to multiple game types
 * - Self-documenting through consistent patterns
 * 
 * Naming Convention:
 * - DOMAIN: Functional area (AUTH, GAME, LOBBY, SYSTEM)
 * - ACTION: What happens (LOGIN, PLAY, JOIN, ERROR)
 * - MODIFIER: Optional qualifier (REQUEST, RESPONSE, SUCCESS, FAILURE)
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public final class MessageType {
    
    // Prevent instantiation
    private MessageType() {}
    
    // ============================================================================
    // AUTHENTICATION DOMAIN - User login, logout, registration
    // ============================================================================
    
    /** User authentication request with credentials */
    public static final String AUTH_LOGIN_REQUEST = "AUTH.LOGIN_REQUEST";
    
    /** Successful authentication response with user info and token */
    public static final String AUTH_LOGIN_SUCCESS = "AUTH.LOGIN_SUCCESS";
    
    /** Failed authentication response with error details */
    public static final String AUTH_LOGIN_FAILURE = "AUTH.LOGIN_FAILURE";
    
    /** User logout request */
    public static final String AUTH_LOGOUT_REQUEST = "AUTH.LOGOUT_REQUEST";
    
    /** Successful logout confirmation */
    public static final String AUTH_LOGOUT_SUCCESS = "AUTH.LOGOUT_SUCCESS";
    
    /** User registration request with account details */
    public static final String AUTH_REGISTER_REQUEST = "AUTH.REGISTER_REQUEST";
    
    /** Successful registration response */
    public static final String AUTH_REGISTER_SUCCESS = "AUTH.REGISTER_SUCCESS";
    
    /** Failed registration response with validation errors */
    public static final String AUTH_REGISTER_FAILURE = "AUTH.REGISTER_FAILURE";
    
    /** Token refresh request for session extension */
    public static final String AUTH_TOKEN_REFRESH = "AUTH.TOKEN_REFRESH";
    
    /** Session validation check */
    public static final String AUTH_SESSION_VALIDATE = "AUTH.SESSION_VALIDATE";
    
    // ============================================================================
    // LOBBY DOMAIN - Player matchmaking, room management
    // ============================================================================
    
    /** Player joins the game lobby */
    public static final String LOBBY_JOIN_REQUEST = "LOBBY.JOIN_REQUEST";
    
    /** Successful lobby join confirmation */
    public static final String LOBBY_JOIN_SUCCESS = "LOBBY.JOIN_SUCCESS";
    
    /** Player leaves the lobby */
    public static final String LOBBY_LEAVE_REQUEST = "LOBBY.LEAVE_REQUEST";
    
    /** Updated list of players in lobby (broadcast) */
    public static final String LOBBY_PLAYER_LIST_UPDATE = "LOBBY.PLAYER_LIST_UPDATE";
    
    /** Player status change notification (online/offline/in-game) */
    public static final String LOBBY_PLAYER_STATUS_UPDATE = "LOBBY.PLAYER_STATUS_UPDATE";
    
    /** Request to find a match with another player */
    public static final String LOBBY_MATCH_REQUEST = "LOBBY.MATCH_REQUEST";
    
    /** Cancel pending match request */
    public static final String LOBBY_MATCH_CANCEL = "LOBBY.MATCH_CANCEL";
    
    /** Direct invitation to specific player */
    public static final String LOBBY_INVITE_SEND = "LOBBY.INVITE_SEND";
    
    /** Response to received invitation */
    public static final String LOBBY_INVITE_RESPONSE = "LOBBY.INVITE_RESPONSE";
    
    /** Request leaderboard data */
    public static final String LOBBY_LEADERBOARD_REQUEST = "LOBBY.LEADERBOARD_REQUEST";
    
    /** Leaderboard data response */
    public static final String LOBBY_LEADERBOARD_RESPONSE = "LOBBY.LEADERBOARD_RESPONSE";
    
    // ============================================================================
    // GAME DOMAIN - Core gameplay mechanics
    // ============================================================================
    
    /** Match found and players paired */
    public static final String GAME_MATCH_FOUND = "GAME.MATCH_FOUND";
    
    /** Game session starts */
    public static final String GAME_START = "GAME.START";
    
    /** New round begins */
    public static final String GAME_ROUND_START = "GAME.ROUND_START";
    
    /** Player plays a card */
    public static final String GAME_CARD_PLAY_REQUEST = "GAME.CARD_PLAY_REQUEST";
    
    /** Card play acknowledged by server */
    public static final String GAME_CARD_PLAY_ACK = "GAME.CARD_PLAY_ACK";
    
    /** Card play rejected (invalid move) */
    public static final String GAME_CARD_PLAY_NACK = "GAME.CARD_PLAY_NACK";
    
    /** Opponent has made their card selection */
    public static final String GAME_OPPONENT_READY = "GAME.OPPONENT_READY";
    
    /** Round results revealed (both cards shown) */
    public static final String GAME_ROUND_REVEAL = "GAME.ROUND_REVEAL";
    
    /** Round ends with scoring update */
    public static final String GAME_ROUND_END = "GAME.ROUND_END";
    
    /** Current game score update */
    public static final String GAME_SCORE_UPDATE = "GAME.SCORE_UPDATE";
    
    /** Game ends with final results */
    public static final String GAME_END = "GAME.END";
    
    /** Game results with winner and statistics */
    public static final String GAME_RESULT = "GAME.RESULT";
    
    /** Request to play another game */
    public static final String GAME_REMATCH_REQUEST = "GAME.REMATCH_REQUEST";
    
    /** Response to rematch request */
    public static final String GAME_REMATCH_RESPONSE = "GAME.REMATCH_RESPONSE";
    
    /** Player forfeits the game */
    public static final String GAME_FORFEIT = "GAME.FORFEIT";
    
    /** Opponent disconnected notification */
    public static final String GAME_OPPONENT_DISCONNECTED = "GAME.OPPONENT_DISCONNECTED";
    
    /** Game state synchronization request */
    public static final String GAME_STATE_SYNC = "GAME.STATE_SYNC";
    
    /** Game pause request */
    public static final String GAME_PAUSE_REQUEST = "GAME.PAUSE_REQUEST";
    
    /** Game resume notification */
    public static final String GAME_RESUME = "GAME.RESUME";
    
    // ============================================================================
    // SYSTEM DOMAIN - Infrastructure, monitoring, control
    // ============================================================================
    
    /** Client connection established welcome message */
    public static final String SYSTEM_WELCOME = "SYSTEM.WELCOME";
    
    /** Heartbeat/ping message to maintain connection */
    public static final String SYSTEM_HEARTBEAT = "SYSTEM.HEARTBEAT";
    
    /** Pong response to heartbeat */
    public static final String SYSTEM_PONG = "SYSTEM.PONG";
    
    /** Generic error notification */
    public static final String SYSTEM_ERROR = "SYSTEM.ERROR";
    
    /** Server maintenance notification */
    public static final String SYSTEM_MAINTENANCE = "SYSTEM.MAINTENANCE";
    
    /** Server shutdown warning */
    public static final String SYSTEM_SHUTDOWN = "SYSTEM.SHUTDOWN";
    
    /** Protocol version mismatch */
    public static final String SYSTEM_VERSION_MISMATCH = "SYSTEM.VERSION_MISMATCH";
    
    /** Rate limiting exceeded warning */
    public static final String SYSTEM_RATE_LIMIT = "SYSTEM.RATE_LIMIT";
    
    /** Connection about to be closed */
    public static final String SYSTEM_DISCONNECT_WARNING = "SYSTEM.DISCONNECT_WARNING";
    
    /** Server status update */
    public static final String SYSTEM_STATUS_UPDATE = "SYSTEM.STATUS_UPDATE";
    
    /** Debug information (development only) */
    public static final String SYSTEM_DEBUG = "SYSTEM.DEBUG";
    
    // ============================================================================
    // ADMIN DOMAIN - Administrative operations (future extension)
    // ============================================================================
    
    /** Admin authentication */
    public static final String ADMIN_LOGIN = "ADMIN.LOGIN";
    
    /** Server statistics request */
    public static final String ADMIN_STATS_REQUEST = "ADMIN.STATS_REQUEST";
    
    /** User management operation */
    public static final String ADMIN_USER_MANAGEMENT = "ADMIN.USER_MANAGEMENT";
    
    /** Game monitoring request */
    public static final String ADMIN_GAME_MONITOR = "ADMIN.GAME_MONITOR";
    
    /** Server configuration update */
    public static final String ADMIN_CONFIG_UPDATE = "ADMIN.CONFIG_UPDATE";
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Validate if message type follows naming convention
     * 
     * @param messageType The message type to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidMessageType(String messageType) {
        if (messageType == null || messageType.isEmpty()) {
            return false;
        }
        
        // Check format: DOMAIN.ACTION[.MODIFIER]
        String[] parts = messageType.split("\\.");
        return parts.length >= 2 && parts.length <= 3 &&
               parts[0].matches("[A-Z_]+") &&
               parts[1].matches("[A-Z_]+") &&
               (parts.length == 2 || parts[2].matches("[A-Z_]+"));
    }
    
    /**
     * Extract domain from message type
     * 
     * @param messageType The message type
     * @return The domain part (e.g., "AUTH" from "AUTH.LOGIN_REQUEST")
     */
    public static String getDomain(String messageType) {
        if (messageType == null || messageType.isEmpty()) {
            return null;
        }
        
        int dotIndex = messageType.indexOf('.');
        return dotIndex > 0 ? messageType.substring(0, dotIndex) : null;
    }
    
    /**
     * Extract action from message type
     * 
     * @param messageType The message type
     * @return The action part (e.g., "LOGIN_REQUEST" from "AUTH.LOGIN_REQUEST")
     */
    public static String getAction(String messageType) {
        if (messageType == null || messageType.isEmpty()) {
            return null;
        }
        
        int firstDot = messageType.indexOf('.');
        if (firstDot < 0) return null;
        
        return messageType.substring(firstDot + 1);
    }
    
    /**
     * Check if message type is a request
     * 
     * @param messageType The message type to check
     * @return true if it's a request message
     */
    public static boolean isRequest(String messageType) {
        return messageType != null && messageType.endsWith("_REQUEST");
    }
    
    /**
     * Check if message type is a response
     * 
     * @param messageType The message type to check
     * @return true if it's a response message
     */
    public static boolean isResponse(String messageType) {
        return messageType != null && 
               (messageType.endsWith("_SUCCESS") || 
                messageType.endsWith("_FAILURE") || 
                messageType.endsWith("_RESPONSE"));
    }
    
    /**
     * Check if message type is an event/notification
     * 
     * @param messageType The message type to check
     * @return true if it's an event/notification message
     */
    public static boolean isEvent(String messageType) {
        return messageType != null && 
               !isRequest(messageType) && 
               !isResponse(messageType);
    }
}