package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.Objects;

/**
 * Play Card Request DTO
 * 
 * Player's card selection for current round.
 * Must be sent before round timeout.
 * 
 * Message Type: GAME.PLAY_CARD
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayCardRequestDto {
    
    /**
     * Game ID
     */
    @JsonProperty("gameId")
    @NotNull(message = "Game ID is required")
    private String gameId;
    
    /**
     * Round number (1-3)
     */
    @JsonProperty("roundNumber")
    @NotNull(message = "Round number is required")
    @Min(value = 1, message = "Round number must be at least 1")
    @Max(value = 3, message = "Round number cannot exceed 3")
    private Integer roundNumber;
    
    /**
     * Selected card ID (1-36)
     */
    @JsonProperty("cardId")
    @NotNull(message = "Card ID is required")
    @Min(value = 1, message = "Card ID must be at least 1")
    @Max(value = 36, message = "Card ID cannot exceed 36")
    private Integer cardId;
    
    /**
     * Player user ID (extracted from auth context)
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Selection timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public PlayCardRequestDto() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public PlayCardRequestDto(String gameId, Integer roundNumber, Integer cardId) {
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.cardId = cardId;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    public Integer getRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    public Integer getCardId() {
        return cardId;
    }
    
    public void setCardId(Integer cardId) {
        this.cardId = cardId;
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
    
    /**
     * Validate DTO fields
     */
    public boolean isValid() {
        return gameId != null && !gameId.isEmpty() &&
               roundNumber != null && roundNumber >= 1 && roundNumber <= 3 &&
               cardId != null && cardId >= 1 && cardId <= 36;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayCardRequestDto that = (PlayCardRequestDto) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(roundNumber, that.roundNumber) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameId, roundNumber, userId);
    }
    
    @Override
    public String toString() {
        return "PlayCardRequestDto{" +
               "gameId='" + gameId + '\'' +
               ", roundNumber=" + roundNumber +
               ", cardId=" + cardId +
               ", userId='" + userId + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
