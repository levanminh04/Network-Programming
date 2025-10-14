package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Player Status Enumeration
 * 
 * Represents the current status of a player in the system.
 * Used for presence tracking, matchmaking, and game state management.
 * 
 * Database Reference: players table (online column)
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public enum PlayerStatus {
    /**
     * Player is offline (not connected)
     */
    OFFLINE("OFFLINE", "Offline", "Ngoại tuyến", false),
    
    /**
     * Player is online and idle in main menu
     */
    ONLINE("ONLINE", "Online", "Trực tuyến", true),
    
    /**
     * Player is in matchmaking queue
     */
    IN_QUEUE("IN_QUEUE", "In Queue", "Đang tìm trận", true),
    
    /**
     * Player is in active game
     */
    IN_GAME("IN_GAME", "In Game", "Đang chơi", true),
    
    /**
     * Player is away (connected but inactive)
     */
    AWAY("AWAY", "Away", "Vắng mặt", true),
    
    /**
     * Player connection is unstable/reconnecting
     */
    RECONNECTING("RECONNECTING", "Reconnecting", "Đang kết nối lại", true),
    
    /**
     * Player has disconnected during game
     */
    DISCONNECTED("DISCONNECTED", "Disconnected", "Mất kết nối", false);
    
    /**
     * Status code (for JSON serialization)
     */
    private final String code;
    
    /**
     * English display name
     */
    private final String englishName;
    
    /**
     * Vietnamese display name
     */
    private final String vietnameseName;
    
    /**
     * Whether player is considered "online" for presence
     * False for OFFLINE and DISCONNECTED
     */
    private final boolean online;
    
    /**
     * Constructor
     * 
     * @param code Status code
     * @param englishName English display name
     * @param vietnameseName Vietnamese display name
     * @param online Whether player is online
     */
    PlayerStatus(String code, String englishName, String vietnameseName, boolean online) {
        this.code = code;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.online = online;
    }
    
    /**
     * Get status code (for JSON serialization)
     * 
     * @return Status code
     */
    @JsonValue
    public String getCode() {
        return code;
    }
    
    /**
     * Get English display name
     * 
     * @return English display name
     */
    public String getEnglishName() {
        return englishName;
    }
    
    /**
     * Get Vietnamese display name
     * 
     * @return Vietnamese display name
     */
    public String getVietnameseName() {
        return vietnameseName;
    }
    
    /**
     * Check if player is online
     * 
     * @return true if online, false if offline/disconnected
     */
    public boolean isOnline() {
        return online;
    }
    
    /**
     * Get display name in specified language
     * 
     * @param language Language code ("en" or "vi")
     * @return Display name in specified language
     */
    public String getDisplayName(String language) {
        if ("vi".equalsIgnoreCase(language)) {
            return vietnameseName;
        }
        return englishName;
    }
    
    /**
     * Parse status from code string
     * 
     * @param code Status code
     * @return PlayerStatus enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static PlayerStatus fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Status code cannot be null or empty");
        }
        
        for (PlayerStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Invalid status code: " + code);
    }
    
    /**
     * Check if status code is valid
     * 
     * @param code Status code to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        for (PlayerStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if player can enter matchmaking queue
     * 
     * @return true if player can enter queue
     */
    public boolean canEnterQueue() {
        return this == ONLINE;
    }
    
    /**
     * Check if player is available for matchmaking
     * 
     * @return true if available
     */
    public boolean isAvailableForMatch() {
        return this == ONLINE || this == IN_QUEUE;
    }
    
    /**
     * Check if player is currently in a game
     * 
     * @return true if in game
     */
    public boolean isInGame() {
        return this == IN_GAME;
    }
    
    /**
     * Check if player is in queue
     * 
     * @return true if in queue
     */
    public boolean isInQueue() {
        return this == IN_QUEUE;
    }
    
    /**
     * Check if player has unstable connection
     * 
     * @return true if reconnecting or disconnected
     */
    public boolean hasConnectionIssue() {
        return this == RECONNECTING || this == DISCONNECTED;
    }
    
    @Override
    public String toString() {
        return code + " (" + englishName + "/" + vietnameseName + ", online=" + online + ")";
    }
}
