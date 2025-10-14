package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Play Card Acknowledgement DTO
 * 
 * Confirmation that card selection was accepted.
 * Sent immediately after valid card selection.
 * 
 * Message Type: GAME.PICK_ACK
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayCardAckDto {
    
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
     * Selected card ID
     */
    @JsonProperty("cardId")
    private Integer cardId;
    
    /**
     * Whether waiting for opponent to select
     */
    @JsonProperty("waitingForOpponent")
    private Boolean waitingForOpponent;
    
    /**
     * Acknowledgement timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * Message for player
     */
    @JsonProperty("message")
    private String message;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public PlayCardAckDto() {
        this.timestamp = System.currentTimeMillis();
        this.waitingForOpponent = true;
        this.message = "Card selected. Waiting for opponent...";
    }
    
    public PlayCardAckDto(String gameId, Integer roundNumber, Integer cardId) {
        this();
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.cardId = cardId;
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
    
    public Boolean getWaitingForOpponent() {
        return waitingForOpponent;
    }
    
    public void setWaitingForOpponent(Boolean waitingForOpponent) {
        this.waitingForOpponent = waitingForOpponent;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayCardAckDto that = (PlayCardAckDto) o;
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
        return "PlayCardAckDto{" +
               "gameId='" + gameId + '\'' +
               ", roundNumber=" + roundNumber +
               ", cardId=" + cardId +
               ", waitingForOpponent=" + waitingForOpponent +
               '}';
    }
}
