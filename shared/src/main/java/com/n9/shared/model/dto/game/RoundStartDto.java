package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.n9.shared.model.dto.game.CardDto;

import java.util.List;
import java.util.Objects;

/**
 * Round Start DTO
 * 
 * Notification that a new round is starting.
 * Contains player's hand (cards to choose from).
 * 
 * Message Type: GAME.ROUND_START
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundStartDto {
    
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
     * Total rounds (3 for MVP)
     */
    @JsonProperty("totalRounds")
    private Integer totalRounds;
    
    /**
     * Player's hand (cards available to play)
     * MVP: All 36 cards available each round
     */
    @JsonProperty("hand")
    private List<CardDto> hand;
    
    /**
     * Round timeout in seconds (10 for MVP)
     */
    @JsonProperty("timeout")
    private Integer timeout;
    
    /**
     * Round deadline (epoch milliseconds)
     * Player must select card before this time
     */
    @JsonProperty("deadline")
    private Long deadline;
    
    /**
     * Round start timestamp (epoch milliseconds)
     */
    @JsonProperty("startedAt")
    private Long startedAt;
    
    /**
     * Current player score
     */
    @JsonProperty("playerScore")
    private Integer playerScore;
    
    /**
     * Opponent score
     */
    @JsonProperty("opponentScore")
    private Integer opponentScore;
    
    /**
     * Message for player
     */
    @JsonProperty("message")
    private String message;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public RoundStartDto() {
        this.totalRounds = 3;
        this.timeout = 10;
        this.startedAt = System.currentTimeMillis();
        this.deadline = this.startedAt + (this.timeout * 1000L);
        this.playerScore = 0;
        this.opponentScore = 0;
    }
    
    public RoundStartDto(String gameId, Integer roundNumber, List<CardDto> hand) {
        this();
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.hand = hand;
        this.message = "Round " + roundNumber + " - Select your card!";
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String gameId;
        private Integer roundNumber;
        private Integer totalRounds;
        private List<CardDto> hand;
        private Integer timeout;
        private Long deadline;
        private Long startedAt;
        private Integer playerScore;
        private Integer opponentScore;
        private String message;
        
        public Builder gameId(String gameId) {
            this.gameId = gameId;
            return this;
        }
        
        public Builder roundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
            return this;
        }
        
        public Builder totalRounds(Integer totalRounds) {
            this.totalRounds = totalRounds;
            return this;
        }
        
        public Builder hand(List<CardDto> hand) {
            this.hand = hand;
            return this;
        }
        
        public Builder timeout(Integer timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder deadline(Long deadline) {
            this.deadline = deadline;
            return this;
        }
        
        public Builder startedAt(Long startedAt) {
            this.startedAt = startedAt;
            return this;
        }
        
        public Builder playerScore(Integer playerScore) {
            this.playerScore = playerScore;
            return this;
        }
        
        public Builder opponentScore(Integer opponentScore) {
            this.opponentScore = opponentScore;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public RoundStartDto build() {
            RoundStartDto dto = new RoundStartDto();
            dto.gameId = this.gameId;
            dto.roundNumber = this.roundNumber;
            dto.totalRounds = this.totalRounds != null ? this.totalRounds : 3;
            dto.hand = this.hand;
            dto.timeout = this.timeout != null ? this.timeout : 10;
            dto.startedAt = this.startedAt != null ? this.startedAt : System.currentTimeMillis();
            dto.deadline = this.deadline != null ? this.deadline : dto.startedAt + (dto.timeout * 1000L);
            dto.playerScore = this.playerScore != null ? this.playerScore : 0;
            dto.opponentScore = this.opponentScore != null ? this.opponentScore : 0;
            dto.message = this.message != null ? this.message : "Round " + dto.roundNumber + " - Select your card!";
            return dto;
        }
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
    
    public Integer getTotalRounds() {
        return totalRounds;
    }
    
    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }
    
    public List<CardDto> getHand() {
        return hand;
    }
    
    public void setHand(List<CardDto> hand) {
        this.hand = hand;
    }
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    public Long getDeadline() {
        return deadline;
    }
    
    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }
    
    public Long getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }
    
    public Integer getPlayerScore() {
        return playerScore;
    }
    
    public void setPlayerScore(Integer playerScore) {
        this.playerScore = playerScore;
    }
    
    public Integer getOpponentScore() {
        return opponentScore;
    }
    
    public void setOpponentScore(Integer opponentScore) {
        this.opponentScore = opponentScore;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================
    
    /**
     * Get remaining time in seconds
     */
    public long getRemainingSeconds() {
        if (deadline == null) {
            return timeout != null ? timeout : 10;
        }
        long remaining = (deadline - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    /**
     * Check if round has timed out
     */
    public boolean isTimedOut() {
        return deadline != null && System.currentTimeMillis() > deadline;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoundStartDto that = (RoundStartDto) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(roundNumber, that.roundNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameId, roundNumber);
    }
    
    @Override
    public String toString() {
        return "RoundStartDto{" +
               "gameId='" + gameId + '\'' +
               ", roundNumber=" + roundNumber +
               ", totalRounds=" + totalRounds +
               ", handSize=" + (hand != null ? hand.size() : 0) +
               ", timeout=" + timeout +
               ", playerScore=" + playerScore +
               ", opponentScore=" + opponentScore +
               '}';
    }
}
