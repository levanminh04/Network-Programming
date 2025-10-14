package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Match Result Enumeration
 * 
 * Represents the final outcome of a completed match.
 * Stored in database and used for statistics/leaderboard.
 * 
 * Database Reference: games table
 * - winner_id: INT (NULL for draw)
 * - result: ENUM('WIN', 'LOSS', 'DRAW')
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public enum MatchResult {
    /**
     * Player won the match (total score > opponent score)
     */
    WIN("WIN", "Win", "Thắng", true, false, false),
    
    /**
     * Player lost the match (total score < opponent score)
     */
    LOSS("LOSS", "Loss", "Thua", false, true, false),
    
    /**
     * Match ended in a draw (total score = opponent score)
     */
    DRAW("DRAW", "Draw", "Hòa", false, false, true),
    
    /**
     * Match was abandoned (player disconnected)
     */
    ABANDONED("ABANDONED", "Abandoned", "Bỏ cuộc", false, true, false),
    
    /**
     * Match was cancelled (before completion)
     */
    CANCELLED("CANCELLED", "Cancelled", "Đã hủy", false, false, false);
    
    /**
     * Result code (for JSON serialization and database storage)
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
     * Whether this result counts as a win for statistics
     */
    private final boolean isWin;
    
    /**
     * Whether this result counts as a loss for statistics
     */
    private final boolean isLoss;
    
    /**
     * Whether this result counts as a draw for statistics
     */
    private final boolean isDraw;
    
    /**
     * Constructor
     * 
     * @param code Result code
     * @param englishName English display name
     * @param vietnameseName Vietnamese display name
     * @param isWin Whether this is a win
     * @param isLoss Whether this is a loss
     * @param isDraw Whether this is a draw
     */
    MatchResult(String code, String englishName, String vietnameseName, 
                boolean isWin, boolean isLoss, boolean isDraw) {
        this.code = code;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.isWin = isWin;
        this.isLoss = isLoss;
        this.isDraw = isDraw;
    }
    
    /**
     * Get result code (for JSON serialization)
     * 
     * @return Result code
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
     * Check if this result is a win
     * 
     * @return true if win
     */
    public boolean isWin() {
        return isWin;
    }
    
    /**
     * Check if this result is a loss
     * 
     * @return true if loss
     */
    public boolean isLoss() {
        return isLoss;
    }
    
    /**
     * Check if this result is a draw
     * 
     * @return true if draw
     */
    public boolean isDraw() {
        return isDraw;
    }
    
    /**
     * Check if this result counts for statistics
     * (Excludes CANCELLED)
     * 
     * @return true if counts for stats
     */
    public boolean countsForStatistics() {
        return this != CANCELLED;
    }
    
    /**
     * Check if this result counts as games played
     * (Excludes CANCELLED and ABANDONED)
     * 
     * @return true if counts as games played
     */
    public boolean countsAsGamesPlayed() {
        return this != CANCELLED && this != ABANDONED;
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
     * Parse result from code string
     * 
     * @param code Result code
     * @return MatchResult enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static MatchResult fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Result code cannot be null or empty");
        }
        
        for (MatchResult result : values()) {
            if (result.code.equalsIgnoreCase(code)) {
                return result;
            }
        }
        
        throw new IllegalArgumentException("Invalid result code: " + code);
    }
    
    /**
     * Check if result code is valid
     * 
     * @param code Result code to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        for (MatchResult result : values()) {
            if (result.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get opposite result (WIN ↔ LOSS, DRAW stays same)
     * Used to get opponent's result
     * 
     * @return Opposite result
     */
    public MatchResult getOppositeResult() {
        switch (this) {
            case WIN:
                return LOSS;
            case LOSS:
                return WIN;
            case DRAW:
                return DRAW;
            case ABANDONED:
                return WIN; // Opponent wins if player abandoned
            case CANCELLED:
                return CANCELLED;
            default:
                return CANCELLED;
        }
    }
    
    /**
     * Determine result based on scores
     * 
     * @param playerScore Player's total score
     * @param opponentScore Opponent's total score
     * @return Match result for player
     */
    public static MatchResult fromScores(int playerScore, int opponentScore) {
        if (playerScore > opponentScore) {
            return WIN;
        } else if (playerScore < opponentScore) {
            return LOSS;
        } else {
            return DRAW;
        }
    }
    
    @Override
    public String toString() {
        return code + " (" + englishName + "/" + vietnameseName + ")";
    }
}
