package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Opponent Left DTO
 * 
 * Notification that opponent has disconnected/left the game.
 * Current player wins by default.
 * 
 * Message Type: MATCH.OPPONENT_LEFT
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpponentLeftDto {
    
    /**
     * Match ID
     */
    @JsonProperty("matchId")
    private String matchId;
    
    /**
     * Game ID
     */
    @JsonProperty("gameId")
    private String gameId;
    
    /**
     * Opponent user ID
     */
    @JsonProperty("opponentId")
    private String opponentId;
    
    /**
     * Opponent username
     */
    @JsonProperty("opponentUsername")
    private String opponentUsername;
    
    /**
     * Reason for leaving (DISCONNECTED, TIMEOUT, ABANDONED, etc.)
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Current round number when opponent left
     */
    @JsonProperty("currentRound")
    private Integer currentRound;
    
    /**
     * Whether current player wins by forfeit
     */
    @JsonProperty("winByForfeit")
    private Boolean winByForfeit;
    
    /**
     * Timestamp (epoch milliseconds)
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
    
    public OpponentLeftDto() {
        this.timestamp = System.currentTimeMillis();
        this.winByForfeit = true;
    }
    
    public OpponentLeftDto(String matchId, String opponentId, String reason) {
        this();
        this.matchId = matchId;
        this.opponentId = opponentId;
        this.reason = reason;
        this.message = "Opponent has left the game. You win!";
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static OpponentLeftDto disconnected(String matchId, String opponentId, String opponentUsername) {
        OpponentLeftDto dto = new OpponentLeftDto();
        dto.matchId = matchId;
        dto.opponentId = opponentId;
        dto.opponentUsername = opponentUsername;
        dto.reason = "DISCONNECTED";
        dto.message = opponentUsername + " has disconnected. You win by forfeit!";
        return dto;
    }
    
    public static OpponentLeftDto abandoned(String matchId, String opponentId, String opponentUsername) {
        OpponentLeftDto dto = new OpponentLeftDto();
        dto.matchId = matchId;
        dto.opponentId = opponentId;
        dto.opponentUsername = opponentUsername;
        dto.reason = "ABANDONED";
        dto.message = opponentUsername + " has abandoned the game. You win!";
        return dto;
    }
    
    public static OpponentLeftDto timeout(String matchId, String opponentId, String opponentUsername) {
        OpponentLeftDto dto = new OpponentLeftDto();
        dto.matchId = matchId;
        dto.opponentId = opponentId;
        dto.opponentUsername = opponentUsername;
        dto.reason = "TIMEOUT";
        dto.message = opponentUsername + " timed out. You win!";
        return dto;
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    public String getOpponentId() {
        return opponentId;
    }
    
    public void setOpponentId(String opponentId) {
        this.opponentId = opponentId;
    }
    
    public String getOpponentUsername() {
        return opponentUsername;
    }
    
    public void setOpponentUsername(String opponentUsername) {
        this.opponentUsername = opponentUsername;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Integer getCurrentRound() {
        return currentRound;
    }
    
    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }
    
    public Boolean getWinByForfeit() {
        return winByForfeit;
    }
    
    public void setWinByForfeit(Boolean winByForfeit) {
        this.winByForfeit = winByForfeit;
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
        OpponentLeftDto that = (OpponentLeftDto) o;
        return Objects.equals(matchId, that.matchId) &&
               Objects.equals(opponentId, that.opponentId) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(matchId, opponentId, timestamp);
    }
    
    @Override
    public String toString() {
        return "OpponentLeftDto{" +
               "matchId='" + matchId + '\'' +
               ", opponentId='" + opponentId + '\'' +
               ", reason='" + reason + '\'' +
               ", currentRound=" + currentRound +
               ", winByForfeit=" + winByForfeit +
               '}';
    }
}
