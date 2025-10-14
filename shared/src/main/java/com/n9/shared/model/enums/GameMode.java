package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Game Mode Enumeration
 * 
 * Represents different game modes available in the system.
 * MVP only implements QUICK_MATCH; others are DEFERRED.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public enum GameMode {
    /**
     * Quick Match Mode (MVP)
     * - Auto-matchmaking with random opponent
     * - Fixed rules (3 rounds, 10-second timeout)
     * - No rank restrictions
     */
    QUICK_MATCH("QUICK_MATCH", "Quick Match", "Nhanh", true, false, false),
    
    /**
     * Ranked Match Mode (DEFERRED)
     * - Matchmaking based on rank/MMR
     * - Affects player rating/leaderboard
     * - May have stricter rules
     */
    RANKED("RANKED", "Ranked", "Xếp hạng", false, true, false),
    
    /**
     * Custom Match Mode (DEFERRED)
     * - Player creates room with custom settings
     * - Invite-only or public room
     * - Custom rules (rounds, timeout, etc.)
     */
    CUSTOM("CUSTOM", "Custom", "Tùy chỉnh", false, false, true);
    
    /**
     * Mode code (for JSON serialization and database storage)
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
     * Whether this mode is implemented in MVP
     */
    private final boolean isMvpMode;
    
    /**
     * Whether this mode affects player ranking
     */
    private final boolean isRanked;
    
    /**
     * Whether this mode allows custom rules
     */
    private final boolean allowsCustomRules;
    
    /**
     * Constructor
     * 
     * @param code Mode code
     * @param englishName English display name
     * @param vietnameseName Vietnamese display name
     * @param isMvpMode Whether implemented in MVP
     * @param isRanked Whether affects ranking
     * @param allowsCustomRules Whether allows custom rules
     */
    GameMode(String code, String englishName, String vietnameseName, 
             boolean isMvpMode, boolean isRanked, boolean allowsCustomRules) {
        this.code = code;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.isMvpMode = isMvpMode;
        this.isRanked = isRanked;
        this.allowsCustomRules = allowsCustomRules;
    }
    
    /**
     * Get mode code (for JSON serialization)
     * 
     * @return Mode code
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
     * Check if this mode is implemented in MVP
     * 
     * @return true if MVP mode
     */
    public boolean isMvpMode() {
        return isMvpMode;
    }
    
    /**
     * Check if this mode affects player ranking
     * 
     * @return true if ranked
     */
    public boolean isRanked() {
        return isRanked;
    }
    
    /**
     * Check if this mode allows custom rules
     * 
     * @return true if allows custom rules
     */
    public boolean allowsCustomRules() {
        return allowsCustomRules;
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
     * Parse mode from code string
     * 
     * @param code Mode code
     * @return GameMode enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static GameMode fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Mode code cannot be null or empty");
        }
        
        for (GameMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        
        throw new IllegalArgumentException("Invalid mode code: " + code);
    }
    
    /**
     * Check if mode code is valid
     * 
     * @param code Mode code to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        for (GameMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get default mode for MVP
     * 
     * @return QUICK_MATCH (default MVP mode)
     */
    public static GameMode getDefaultMode() {
        return QUICK_MATCH;
    }
    
    /**
     * Get all MVP-supported modes
     * 
     * @return Array of MVP modes
     */
    public static GameMode[] getMvpModes() {
        return new GameMode[] { QUICK_MATCH };
    }
    
    /**
     * Check if mode is supported in current version
     * 
     * @return true if supported
     */
    public boolean isSupported() {
        return isMvpMode; // In MVP, only MVP modes are supported
    }
    
    @Override
    public String toString() {
        return code + " (" + englishName + "/" + vietnameseName + 
               ", MVP=" + isMvpMode + ", ranked=" + isRanked + ")";
    }
}
