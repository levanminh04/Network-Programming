package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Play Card Negative Acknowledgement DTO
 * 
 * Rejection of card selection (invalid card, timeout, etc.).
 * Sent when card selection is rejected.
 * 
 * Message Type: GAME.PICK_NACK
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayCardNackDto {
    
    /**
     * Game ID
     */
    @JsonProperty("gameId")
    private String gameId;
    
    /**
     * Round number (1-3)
     */
    @JsonProperty("roundNumber")
    private Integer roundNumber;
    
    /**
     * Attempted card ID
     */
    @JsonProperty("cardId")
    private Integer cardId;
    
    /**
     * Error code
     */
    @JsonProperty("errorCode")
    private String errorCode;
    
    /**
     * Rejection reason
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Whether player can retry
     */
    @JsonProperty("canRetry")
    private Boolean canRetry;
    
    /**
     * Timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public PlayCardNackDto() {
        this.timestamp = System.currentTimeMillis();
        this.canRetry = true;
    }
    
    public PlayCardNackDto(String gameId, Integer roundNumber, Integer cardId, String reason) {
        this();
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.cardId = cardId;
        this.reason = reason;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static PlayCardNackDto invalidCard(String gameId, Integer roundNumber, Integer cardId) {
        PlayCardNackDto dto = new PlayCardNackDto();
        dto.gameId = gameId;
        dto.roundNumber = roundNumber;
        dto.cardId = cardId;
        dto.errorCode = "GAME_INVALID_CARD";
        dto.reason = "Invalid card ID";
        dto.canRetry = true;
        return dto;
    }
    
    public static PlayCardNackDto timeout(String gameId, Integer roundNumber) {
        PlayCardNackDto dto = new PlayCardNackDto();
        dto.gameId = gameId;
        dto.roundNumber = roundNumber;
        dto.errorCode = "GAME_TIMEOUT";
        dto.reason = "Round timed out. Card auto-selected.";
        dto.canRetry = false;
        return dto;
    }
    
    public static PlayCardNackDto alreadyPlayed(String gameId, Integer roundNumber) {
        PlayCardNackDto dto = new PlayCardNackDto();
        dto.gameId = gameId;
        dto.roundNumber = roundNumber;
        dto.errorCode = "GAME_ALREADY_PLAYED";
        dto.reason = "Card already selected for this round";
        dto.canRetry = false;
        return dto;
    }
    
    public static PlayCardNackDto invalidRound(String gameId, Integer roundNumber) {
        PlayCardNackDto dto = new PlayCardNackDto();
        dto.gameId = gameId;
        dto.roundNumber = roundNumber;
        dto.errorCode = "GAME_INVALID_ROUND";
        dto.reason = "Invalid round number or round not active";
        dto.canRetry = false;
        return dto;
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
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Boolean getCanRetry() {
        return canRetry;
    }
    
    public void setCanRetry(Boolean canRetry) {
        this.canRetry = canRetry;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayCardNackDto that = (PlayCardNackDto) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(roundNumber, that.roundNumber) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameId, roundNumber, timestamp);
    }
    
    @Override
    public String toString() {
        return "PlayCardNackDto{" +
               "gameId='" + gameId + '\'' +
               ", roundNumber=" + roundNumber +
               ", cardId=" + cardId +
               ", errorCode='" + errorCode + '\'' +
               ", reason='" + reason + '\'' +
               ", canRetry=" + canRetry +
               '}';
    }
}
