package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Round Phase Enumeration
 * 
 * Represents the current phase/stage within a single round.
 * Used for UI state management and game flow control.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public enum RoundPhase {
    /**
     * Round is waiting to start (countdown/preparation)
     */
    WAITING("WAITING", "Waiting", "Đang chờ"),
    
    /**
     * Players are selecting their cards (10-second timer active)
     */
    CARD_SELECTION("CARD_SELECTION", "Card Selection", "Chọn bài"),
    
    /**
     * Waiting for opponent to select card
     * (current player has selected but opponent hasn't)
     */
    WAITING_FOR_OPPONENT("WAITING_FOR_OPPONENT", "Waiting for Opponent", "Đợi đối thủ"),
    
    /**
     * Both players have selected, revealing cards
     */
    REVEALING("REVEALING", "Revealing", "Lật bài"),
    
    /**
     * Cards revealed, showing round result
     */
    RESULT("RESULT", "Result", "Kết quả"),
    
    /**
     * Round completed, transitioning to next round
     */
    COMPLETED("COMPLETED", "Completed", "Hoàn thành");
    
    /**
     * Phase code (for JSON serialization)
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
     * Constructor
     * 
     * @param code Phase code
     * @param englishName English display name
     * @param vietnameseName Vietnamese display name
     */
    RoundPhase(String code, String englishName, String vietnameseName) {
        this.code = code;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
    }
    
    /**
     * Get phase code (for JSON serialization)
     * 
     * @return Phase code
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
     * Parse phase from code string
     * 
     * @param code Phase code
     * @return RoundPhase enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static RoundPhase fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Phase code cannot be null or empty");
        }
        
        for (RoundPhase phase : values()) {
            if (phase.code.equalsIgnoreCase(code)) {
                return phase;
            }
        }
        
        throw new IllegalArgumentException("Invalid phase code: " + code);
    }
    
    /**
     * Check if phase code is valid
     * 
     * @param code Phase code to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        for (RoundPhase phase : values()) {
            if (phase.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if player can select card in this phase
     * 
     * @return true if card selection allowed
     */
    public boolean isCardSelectionAllowed() {
        return this == CARD_SELECTION;
    }
    
    /**
     * Check if round is active (not completed)
     * 
     * @return true if active
     */
    public boolean isActive() {
        return this != COMPLETED;
    }
    
    /**
     * Check if round is in progress (past waiting phase)
     * 
     * @return true if in progress
     */
    public boolean isInProgress() {
        return this != WAITING && this != COMPLETED;
    }
    
    /**
     * Get next phase in sequence
     * 
     * @return Next phase, or null if already at COMPLETED
     */
    public RoundPhase getNextPhase() {
        switch (this) {
            case WAITING:
                return CARD_SELECTION;
            case CARD_SELECTION:
                return WAITING_FOR_OPPONENT;
            case WAITING_FOR_OPPONENT:
                return REVEALING;
            case REVEALING:
                return RESULT;
            case RESULT:
                return COMPLETED;
            case COMPLETED:
                return null; // No next phase
            default:
                return null;
        }
    }
    
    /**
     * Check if phase is a terminal phase (round ending)
     * 
     * @return true if terminal phase
     */
    public boolean isTerminalPhase() {
        return this == RESULT || this == COMPLETED;
    }
    
    @Override
    public String toString() {
        return code + " (" + englishName + "/" + vietnameseName + ")";
    }
}
