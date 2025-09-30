package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Game State Enumeration
 * 
 * Represents all possible states during a card game session.
 * Used for state management, validation, and UI updates.
 * 
 * State Transitions:
 * WAITING_FOR_PLAYERS → STARTING → IN_PROGRESS → PAUSED ⇄ IN_PROGRESS → COMPLETED
 *                    ↘                                                  ↗
 *                     CANCELLED ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
 * 
 * @author N9 Team
 * @version 1.0.0
 */
public enum GameState {
    
    /** Waiting for second player to join */
    WAITING_FOR_PLAYERS("waiting_for_players", "Đang chờ người chơi"),
    
    /** Game is starting, initializing deck and players */
    STARTING("starting", "Đang bắt đầu"),
    
    /** Game is actively in progress */
    IN_PROGRESS("in_progress", "Đang chơi"),
    
    /** Game is temporarily paused */
    PAUSED("paused", "Tạm dừng"),
    
    /** Game completed normally */
    COMPLETED("completed", "Hoàn thành"),
    
    /** Game cancelled or aborted */
    CANCELLED("cancelled", "Đã hủy"),
    
    /** Game abandoned due to disconnection */
    ABANDONED("abandoned", "Bị bỏ"),
    
    /** Error occurred during game */
    ERROR("error", "Lỗi");
    
    private final String value;
    private final String displayName;
    
    GameState(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if game is in playable state
     */
    public boolean isPlayable() {
        return this == IN_PROGRESS;
    }
    
    /**
     * Check if game is finished (completed, cancelled, abandoned, error)
     */
    public boolean isFinished() {
        return this == COMPLETED || this == CANCELLED || this == ABANDONED || this == ERROR;
    }
    
    /**
     * Check if game can be resumed
     */
    public boolean canResume() {
        return this == PAUSED;
    }
    
    /**
     * Check if game can be cancelled
     */
    public boolean canCancel() {
        return this == WAITING_FOR_PLAYERS || this == STARTING || this == IN_PROGRESS || this == PAUSED;
    }
    
    /**
     * Get valid next states from current state
     */
    public GameState[] getValidNextStates() {
        switch (this) {
            case WAITING_FOR_PLAYERS:
                return new GameState[]{STARTING, CANCELLED};
            case STARTING:
                return new GameState[]{IN_PROGRESS, CANCELLED, ERROR};
            case IN_PROGRESS:
                return new GameState[]{PAUSED, COMPLETED, ABANDONED, ERROR};
            case PAUSED:
                return new GameState[]{IN_PROGRESS, CANCELLED, ABANDONED};
            case COMPLETED:
            case CANCELLED:
            case ABANDONED:
            case ERROR:
                return new GameState[]{}; // Terminal states
            default:
                return new GameState[]{};
        }
    }
    
    /**
     * Validate state transition
     */
    public boolean canTransitionTo(GameState newState) {
        if (newState == null) return false;
        
        for (GameState validState : getValidNextStates()) {
            if (validState == newState) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parse from string value
     */
    public static GameState fromValue(String value) {
        if (value == null) return null;
        
        for (GameState state : values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown game state: " + value);
    }
}