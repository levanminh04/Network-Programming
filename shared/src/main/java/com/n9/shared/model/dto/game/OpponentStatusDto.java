package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Opponent Status DTO
 * 
 * Real-time status update about opponent during game.
 * Shows if opponent has selected card, is waiting, etc.
 * 
 * Message Type: GAME.OPPONENT_STATUS
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpponentStatusDto {
    
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
     * Opponent user ID
     */
    @JsonProperty("opponentId")
    private String opponentId;
    
    /**
     * Opponent status (SELECTING, SELECTED, WAITING, etc.)
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * Whether opponent has selected card
     */
    @JsonProperty("hasSelected")
    private Boolean hasSelected;
    
    /**
     * Timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public OpponentStatusDto() {
        this.timestamp = System.currentTimeMillis();
        this.hasSelected = false;
    }
    
    public OpponentStatusDto(String gameId, Integer roundNumber, String status, Boolean hasSelected) {
        this();
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.status = status;
        this.hasSelected = hasSelected;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static OpponentStatusDto selecting(String gameId, Integer roundNumber, String opponentId) {
        return new OpponentStatusDto(gameId, roundNumber, "SELECTING", false);
    }
    
    public static OpponentStatusDto selected(String gameId, Integer roundNumber, String opponentId) {
        return new OpponentStatusDto(gameId, roundNumber, "SELECTED", true);
    }
    
    public static OpponentStatusDto waiting(String gameId, Integer roundNumber, String opponentId) {
        return new OpponentStatusDto(gameId, roundNumber, "WAITING", true);
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
    
    public String getOpponentId() {
        return opponentId;
    }
    
    public void setOpponentId(String opponentId) {
        this.opponentId = opponentId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getHasSelected() {
        return hasSelected;
    }
    
    public void setHasSelected(Boolean hasSelected) {
        this.hasSelected = hasSelected;
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
        OpponentStatusDto that = (OpponentStatusDto) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(roundNumber, that.roundNumber) &&
               Objects.equals(opponentId, that.opponentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameId, roundNumber, opponentId);
    }
    
    @Override
    public String toString() {
        return "OpponentStatusDto{" +
               "gameId='" + gameId + '\'' +
               ", roundNumber=" + roundNumber +
               ", status='" + status + '\'' +
               ", hasSelected=" + hasSelected +
               '}';
    }
}
