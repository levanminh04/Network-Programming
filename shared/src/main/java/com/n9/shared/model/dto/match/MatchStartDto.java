package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Match Start DTO
 * 
 * Notification that game is starting.
 * Sent to both players after countdown completes.
 * 
 * Message Type: MATCH.START
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchStartDto {
    
    /**
     * Match ID
     */
    @JsonProperty("matchId")
    private String matchId;
    
    /**
     * Game ID (same as match ID for MVP)
     */
    @JsonProperty("gameId")
    private String gameId;
    
    /**
     * Game mode
     */
    @JsonProperty("gameMode")
    private String gameMode;
    
    /**
     * Player position (1 or 2)
     */
    @JsonProperty("playerPosition")
    private Integer playerPosition;
    
    /**
     * Total rounds (3 for MVP)
     */
    @JsonProperty("totalRounds")
    private Integer totalRounds;
    
    /**
     * Round timeout in seconds (10 for MVP)
     */
    @JsonProperty("roundTimeout")
    private Integer roundTimeout;
    
    /**
     * Game start timestamp (epoch milliseconds)
     */
    @JsonProperty("startedAt")
    private Long startedAt;
    
    /**
     * Message for player
     */
    @JsonProperty("message")
    private String message;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public MatchStartDto() {
        this.startedAt = System.currentTimeMillis();
        this.totalRounds = 3;
        this.roundTimeout = 10;
        this.message = "Game is starting!";
    }
    
    public MatchStartDto(String matchId, String gameId) {
        this();
        this.matchId = matchId;
        this.gameId = gameId;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String matchId;
        private String gameId;
        private String gameMode;
        private Integer playerPosition;
        private Integer totalRounds;
        private Integer roundTimeout;
        private Long startedAt;
        private String message;
        
        public Builder matchId(String matchId) {
            this.matchId = matchId;
            return this;
        }
        
        public Builder gameId(String gameId) {
            this.gameId = gameId;
            return this;
        }
        
        public Builder gameMode(String gameMode) {
            this.gameMode = gameMode;
            return this;
        }
        
        public Builder playerPosition(Integer playerPosition) {
            this.playerPosition = playerPosition;
            return this;
        }
        
        public Builder totalRounds(Integer totalRounds) {
            this.totalRounds = totalRounds;
            return this;
        }
        
        public Builder roundTimeout(Integer roundTimeout) {
            this.roundTimeout = roundTimeout;
            return this;
        }
        
        public Builder startedAt(Long startedAt) {
            this.startedAt = startedAt;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public MatchStartDto build() {
            MatchStartDto dto = new MatchStartDto();
            dto.matchId = this.matchId;
            dto.gameId = this.gameId;
            dto.gameMode = this.gameMode;
            dto.playerPosition = this.playerPosition;
            dto.totalRounds = this.totalRounds != null ? this.totalRounds : 3;
            dto.roundTimeout = this.roundTimeout != null ? this.roundTimeout : 10;
            dto.startedAt = this.startedAt != null ? this.startedAt : System.currentTimeMillis();
            dto.message = this.message != null ? this.message : "Game is starting!";
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
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    public String getGameMode() {
        return gameMode;
    }
    
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
    
    public Integer getPlayerPosition() {
        return playerPosition;
    }
    
    public void setPlayerPosition(Integer playerPosition) {
        this.playerPosition = playerPosition;
    }
    
    public Integer getTotalRounds() {
        return totalRounds;
    }
    
    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }
    
    public Integer getRoundTimeout() {
        return roundTimeout;
    }
    
    public void setRoundTimeout(Integer roundTimeout) {
        this.roundTimeout = roundTimeout;
    }
    
    public Long getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
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
        MatchStartDto that = (MatchStartDto) o;
        return Objects.equals(matchId, that.matchId) &&
               Objects.equals(gameId, that.gameId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(matchId, gameId);
    }
    
    @Override
    public String toString() {
        return "MatchStartDto{" +
               "matchId='" + matchId + '\'' +
               ", gameId='" + gameId + '\'' +
               ", playerPosition=" + playerPosition +
               ", totalRounds=" + totalRounds +
               ", roundTimeout=" + roundTimeout +
               '}';
    }
}
