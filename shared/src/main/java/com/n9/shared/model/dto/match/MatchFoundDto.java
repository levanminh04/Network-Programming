package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Match Found DTO
 * 
 * Notification that match has been found.
 * Sent to both players when matchmaking succeeds.
 * 
 * Message Type: MATCH.FOUND
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchFoundDto {
    
    /**
     * Match ID (unique identifier)
     */
    @JsonProperty("matchId")
    private String matchId;
    
    /**
     * Game mode
     */
    @JsonProperty("gameMode")
    private String gameMode;
    
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
     * Opponent display name
     */
    @JsonProperty("opponentDisplayName")
    private String opponentDisplayName;
    
    /**
     * Countdown before game starts (seconds)
     * MVP: 3 seconds
     */
    @JsonProperty("startCountdown")
    private Integer startCountdown;
    
    /**
     * Match found timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public MatchFoundDto() {
        this.timestamp = System.currentTimeMillis();
        this.startCountdown = 3; // Default 3-second countdown
    }
    
    public MatchFoundDto(String matchId, String opponentId, String opponentUsername) {
        this();
        this.matchId = matchId;
        this.opponentId = opponentId;
        this.opponentUsername = opponentUsername;
        this.opponentDisplayName = opponentUsername;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String matchId;
        private String gameMode;
        private String opponentId;
        private String opponentUsername;
        private String opponentDisplayName;
        private Integer startCountdown;
        private Long timestamp;
        
        public Builder matchId(String matchId) {
            this.matchId = matchId;
            return this;
        }
        
        public Builder gameMode(String gameMode) {
            this.gameMode = gameMode;
            return this;
        }
        
        public Builder opponentId(String opponentId) {
            this.opponentId = opponentId;
            return this;
        }
        
        public Builder opponentUsername(String opponentUsername) {
            this.opponentUsername = opponentUsername;
            return this;
        }
        
        public Builder opponentDisplayName(String opponentDisplayName) {
            this.opponentDisplayName = opponentDisplayName;
            return this;
        }
        
        public Builder startCountdown(Integer startCountdown) {
            this.startCountdown = startCountdown;
            return this;
        }
        
        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public MatchFoundDto build() {
            MatchFoundDto dto = new MatchFoundDto();
            dto.matchId = this.matchId;
            dto.gameMode = this.gameMode;
            dto.opponentId = this.opponentId;
            dto.opponentUsername = this.opponentUsername;
            dto.opponentDisplayName = this.opponentDisplayName;
            dto.startCountdown = this.startCountdown != null ? this.startCountdown : 3;
            dto.timestamp = this.timestamp != null ? this.timestamp : System.currentTimeMillis();
            return dto;
        }
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
    
    public String getGameMode() {
        return gameMode;
    }
    
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
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
    
    public String getOpponentDisplayName() {
        return opponentDisplayName;
    }
    
    public void setOpponentDisplayName(String opponentDisplayName) {
        this.opponentDisplayName = opponentDisplayName;
    }
    
    public Integer getStartCountdown() {
        return startCountdown;
    }
    
    public void setStartCountdown(Integer startCountdown) {
        this.startCountdown = startCountdown;
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
        MatchFoundDto that = (MatchFoundDto) o;
        return Objects.equals(matchId, that.matchId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(matchId);
    }
    
    @Override
    public String toString() {
        return "MatchFoundDto{" +
               "matchId='" + matchId + '\'' +
               ", gameMode='" + gameMode + '\'' +
               ", opponentId='" + opponentId + '\'' +
               ", opponentUsername='" + opponentUsername + '\'' +
               ", startCountdown=" + startCountdown +
               '}';
    }
}
