package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.n9.shared.model.enums.GameMode;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Match Request DTO
 * 
 * Request to enter matchmaking queue.
 * MVP: QUICK_MATCH mode only.
 * 
 * Message Type: MATCH.REQUEST
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchRequestDto {
    
    /**
     * Game mode (MVP: QUICK_MATCH only)
     */
    @JsonProperty("gameMode")
    @NotNull(message = "Game mode is required")
    private String gameMode;
    
    /**
     * User ID (extracted from auth context)
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Request timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public MatchRequestDto() {
        this.gameMode = GameMode.QUICK_MATCH.getCode();
        this.timestamp = System.currentTimeMillis();
    }
    
    public MatchRequestDto(String gameMode) {
        this.gameMode = gameMode;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static MatchRequestDto quickMatch() {
        return new MatchRequestDto(GameMode.QUICK_MATCH.getCode());
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getGameMode() {
        return gameMode;
    }
    
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================
    
    public boolean isValid() {
        return gameMode != null && GameMode.isValidCode(gameMode);
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchRequestDto that = (MatchRequestDto) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(gameMode, that.gameMode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, gameMode);
    }
    
    @Override
    public String toString() {
        return "MatchRequestDto{" +
               "gameMode='" + gameMode + '\'' +
               ", userId='" + userId + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
