package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Standardized Error Codes
 * 
 * Centralized error code definitions for consistent error handling
 * across all system components. Error codes are hierarchical and
 * follow a domain-based categorization.
 * 
 * Format: DOMAIN_CATEGORY_SPECIFIC_ERROR
 * 
 * @author N9 Team
 * @version 1.0.0
 */
public enum ErrorCode {
    
    // ============================================================================
    // AUTHENTICATION ERRORS (AUTH_*)
    // ============================================================================
    
    /** Invalid username or password */
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Invalid username or password"),
    
    /** User not found */
    AUTH_USER_NOT_FOUND("AUTH_USER_NOT_FOUND", "User not found"),
    
    /** User account is locked */
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "Account is temporarily locked"),
    
    /** Authentication token is invalid or expired */
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "Authentication token is invalid"),
    
    /** Authentication token has expired */
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "Authentication token has expired"),
    
    /** User is not authenticated */
    AUTH_NOT_AUTHENTICATED("AUTH_NOT_AUTHENTICATED", "Authentication required"),
    
    /** User does not have required permissions */
    AUTH_INSUFFICIENT_PERMISSIONS("AUTH_INSUFFICIENT_PERMISSIONS", "Insufficient permissions"),
    
    /** User already exists during registration */
    AUTH_USER_ALREADY_EXISTS("AUTH_USER_ALREADY_EXISTS", "Username already exists"),
    
    /** Registration data validation failed */
    AUTH_REGISTRATION_INVALID("AUTH_REGISTRATION_INVALID", "Invalid registration data"),
    
    // ============================================================================
    // VALIDATION ERRORS (VALIDATION_*)
    // ============================================================================
    
    /** General validation failed */
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    
    /** Input data validation failed */
    VALIDATION_INVALID_INPUT("VALIDATION_INVALID_INPUT", "Input validation failed"),
    
    /** Required field is missing */
    VALIDATION_MISSING_FIELD("VALIDATION_MISSING_FIELD", "Required field is missing"),
    
    /** Field value is out of allowed range */
    VALIDATION_VALUE_OUT_OF_RANGE("VALIDATION_VALUE_OUT_OF_RANGE", "Value is out of allowed range"),
    
    /** Invalid data format */
    VALIDATION_INVALID_FORMAT("VALIDATION_INVALID_FORMAT", "Invalid data format"),
    
    /** Message too large */
    VALIDATION_MESSAGE_TOO_LARGE("VALIDATION_MESSAGE_TOO_LARGE", "Message exceeds size limit"),
    
    /** Invalid message type */
    VALIDATION_INVALID_MESSAGE_TYPE("VALIDATION_INVALID_MESSAGE_TYPE", "Unknown message type"),
    
    /** Invalid JSON format */
    VALIDATION_INVALID_JSON("VALIDATION_INVALID_JSON", "Invalid JSON format"),
    
    // ============================================================================
    // GAME LOGIC ERRORS (GAME_*)
    // ============================================================================
    
    /** Game session not found */
    GAME_SESSION_NOT_FOUND("GAME_SESSION_NOT_FOUND", "Game session not found"),
    
    /** Game is not in playable state */
    GAME_INVALID_STATE("GAME_INVALID_STATE", "Game is not in a valid state for this action"),
    
    /** Invalid card play */
    GAME_INVALID_MOVE("GAME_INVALID_MOVE", "Invalid card play"),
    
    /** Card not available (already played) */
    GAME_CARD_NOT_AVAILABLE("GAME_CARD_NOT_AVAILABLE", "Selected card is not available"),
    
    /** Not player's turn */
    GAME_NOT_PLAYER_TURN("GAME_NOT_PLAYER_TURN", "It's not your turn"),
    
    /** Player not in game */
    GAME_PLAYER_NOT_IN_GAME("GAME_PLAYER_NOT_IN_GAME", "Player is not in this game"),
    
    /** Game is full */
    GAME_ROOM_FULL("GAME_ROOM_FULL", "Game room is full"),
    
    /** Game already started */
    GAME_ALREADY_STARTED("GAME_ALREADY_STARTED", "Game has already started"),
    
    /** Game timeout */
    GAME_TIMEOUT("GAME_TIMEOUT", "Game action timed out"),
    
    // ============================================================================
    // LOBBY ERRORS (LOBBY_*)
    // ============================================================================
    
    /** Player not found in lobby */
    LOBBY_PLAYER_NOT_FOUND("LOBBY_PLAYER_NOT_FOUND", "Player not found in lobby"),
    
    /** Player already in game */
    LOBBY_PLAYER_BUSY("LOBBY_PLAYER_BUSY", "Player is already in a game"),
    
    /** Matchmaking failed */
    LOBBY_MATCHMAKING_FAILED("LOBBY_MATCHMAKING_FAILED", "Failed to find a match"),
    
    /** Invalid invitation */
    LOBBY_INVALID_INVITATION("LOBBY_INVALID_INVITATION", "Invalid game invitation"),
    
    /** Invitation expired */
    LOBBY_INVITATION_EXPIRED("LOBBY_INVITATION_EXPIRED", "Game invitation has expired"),
    
    /** Cannot invite self */
    LOBBY_CANNOT_INVITE_SELF("LOBBY_CANNOT_INVITE_SELF", "Cannot invite yourself"),
    
    // ============================================================================
    // SYSTEM ERRORS (SYSTEM_*)
    // ============================================================================
    
    /** Internal server error */
    SYSTEM_INTERNAL_ERROR("SYSTEM_INTERNAL_ERROR", "Internal server error"),
    
    /** Service temporarily unavailable */
    SYSTEM_SERVICE_UNAVAILABLE("SYSTEM_SERVICE_UNAVAILABLE", "Service temporarily unavailable"),
    
    /** Rate limit exceeded */
    SYSTEM_RATE_LIMIT_EXCEEDED("SYSTEM_RATE_LIMIT_EXCEEDED", "Too many requests"),
    
    /** Connection lost */
    SYSTEM_CONNECTION_LOST("SYSTEM_CONNECTION_LOST", "Connection to server lost"),
    
    /** Protocol version mismatch */
    SYSTEM_VERSION_MISMATCH("SYSTEM_VERSION_MISMATCH", "Protocol version mismatch"),
    
    /** Database error */
    SYSTEM_DATABASE_ERROR("SYSTEM_DATABASE_ERROR", "Database operation failed"),
    
    /** Network timeout */
    SYSTEM_TIMEOUT("SYSTEM_TIMEOUT", "Operation timed out"),
    
    /** Configuration error */
    SYSTEM_CONFIG_ERROR("SYSTEM_CONFIG_ERROR", "System configuration error"),
    
    /** Resource not found */
    SYSTEM_RESOURCE_NOT_FOUND("SYSTEM_RESOURCE_NOT_FOUND", "Requested resource not found"),
    
    // ============================================================================
    // NETWORK ERRORS (NETWORK_*)
    // ============================================================================
    
    /** Connection failed */
    NETWORK_CONNECTION_FAILED("NETWORK_CONNECTION_FAILED", "Failed to establish connection"),
    
    /** Connection timeout */
    NETWORK_CONNECTION_TIMEOUT("NETWORK_CONNECTION_TIMEOUT", "Connection timed out"),
    
    /** Message delivery failed */
    NETWORK_MESSAGE_DELIVERY_FAILED("NETWORK_MESSAGE_DELIVERY_FAILED", "Failed to deliver message"),
    
    /** Invalid message format */
    NETWORK_INVALID_MESSAGE_FORMAT("NETWORK_INVALID_MESSAGE_FORMAT", "Invalid message format"),
    
    /** Connection closed unexpectedly */
    NETWORK_CONNECTION_CLOSED("NETWORK_CONNECTION_CLOSED", "Connection closed unexpectedly"),
    
    // ============================================================================
    // UNKNOWN ERROR
    // ============================================================================
    
    /** Unknown error */
    UNKNOWN_ERROR("UNKNOWN_ERROR", "An unknown error occurred");
    
    private final String code;
    private final String defaultMessage;
    
    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    /**
     * Get error domain (AUTH, VALIDATION, GAME, etc.)
     */
    public String getDomain() {
        int underscoreIndex = code.indexOf('_');
        return underscoreIndex > 0 ? code.substring(0, underscoreIndex) : "UNKNOWN";
    }
    
    /**
     * Check if error is authentication related
     */
    public boolean isAuthError() {
        return "AUTH".equals(getDomain());
    }
    
    /**
     * Check if error is validation related
     */
    public boolean isValidationError() {
        return "VALIDATION".equals(getDomain());
    }
    
    /**
     * Check if error is game logic related
     */
    public boolean isGameError() {
        return "GAME".equals(getDomain());
    }
    
    /**
     * Check if error is system related
     */
    public boolean isSystemError() {
        return "SYSTEM".equals(getDomain());
    }
    
    /**
     * Check if error is network related
     */
    public boolean isNetworkError() {
        return "NETWORK".equals(getDomain());
    }
    
    /**
     * Check if error is retryable
     */
    public boolean isRetryable() {
        switch (this) {
            case SYSTEM_SERVICE_UNAVAILABLE:
            case SYSTEM_TIMEOUT:
            case NETWORK_CONNECTION_TIMEOUT:
            case NETWORK_MESSAGE_DELIVERY_FAILED:
            case NETWORK_CONNECTION_FAILED:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Parse from code string
     */
    public static ErrorCode fromCode(String code) {
        if (code == null) return UNKNOWN_ERROR;
        
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }
}