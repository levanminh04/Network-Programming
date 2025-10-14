package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Game Result DTO
 * 
 * Final game result after all 3 rounds complete.
 * Contains winner, scores, and round-by-round breakdown.
 * 
 * Message Type: GAME.RESULT
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameResultDto {
    
    /**
     * Game ID
     */
    @JsonProperty("gameId")
    private String gameId;
    
    /**
     * Match ID
     */
    @JsonProperty("matchId")
    private String matchId;
    
    /**
     * Game result for player (WIN, LOSS, DRAW)
     */
    @JsonProperty("result")
    private String result;
    
    /**
     * Player's final total score
     */
    @JsonProperty("playerScore")
    private Integer playerScore;
    
    /**
     * Opponent's final total score
     */
    @JsonProperty("opponentScore")
    private Integer opponentScore;
    
    /**
     * Winner user ID (null for draw)
     */
    @JsonProperty("winnerId")
    private String winnerId;
    
    /**
     * Winner username
     */
    @JsonProperty("winnerUsername")
    private String winnerUsername;
    
    /**
     * Round-by-round results
     */
    @JsonProperty("rounds")
    private List<RoundSummaryDto> rounds;
    
    /**
     * Game duration in seconds
     */
    @JsonProperty("duration")
    private Integer duration;
    
    /**
     * Game ended timestamp (epoch milliseconds)
     */
    @JsonProperty("endedAt")
    private Long endedAt;
    
    /**
     * Whether result counts for statistics
     */
    @JsonProperty("countsForStats")
    private Boolean countsForStats;
    
    /**
     * Message for player
     */
    @JsonProperty("message")
    private String message;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public GameResultDto() {
        this.endedAt = System.currentTimeMillis();
        this.countsForStats = true;
    }
    
    public GameResultDto(String gameId, String result, Integer playerScore, Integer opponentScore) {
        this();
        this.gameId = gameId;
        this.result = result;
        this.playerScore = playerScore;
        this.opponentScore = opponentScore;
        this.message = generateMessage(result);
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String gameId;
        private String matchId;
        private String result;
        private Integer playerScore;
        private Integer opponentScore;
        private String winnerId;
        private String winnerUsername;
        private List<RoundSummaryDto> rounds;
        private Integer duration;
        private Long endedAt;
        private Boolean countsForStats;
        private String message;
        
        public Builder gameId(String gameId) {
            this.gameId = gameId;
            return this;
        }
        
        public Builder matchId(String matchId) {
            this.matchId = matchId;
            return this;
        }
        
        public Builder result(String result) {
            this.result = result;
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
        
        public Builder winnerId(String winnerId) {
            this.winnerId = winnerId;
            return this;
        }
        
        public Builder winnerUsername(String winnerUsername) {
            this.winnerUsername = winnerUsername;
            return this;
        }
        
        public Builder rounds(List<RoundSummaryDto> rounds) {
            this.rounds = rounds;
            return this;
        }
        
        public Builder duration(Integer duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder endedAt(Long endedAt) {
            this.endedAt = endedAt;
            return this;
        }
        
        public Builder countsForStats(Boolean countsForStats) {
            this.countsForStats = countsForStats;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public GameResultDto build() {
            GameResultDto dto = new GameResultDto();
            dto.gameId = this.gameId;
            dto.matchId = this.matchId;
            dto.result = this.result;
            dto.playerScore = this.playerScore;
            dto.opponentScore = this.opponentScore;
            dto.winnerId = this.winnerId;
            dto.winnerUsername = this.winnerUsername;
            dto.rounds = this.rounds;
            dto.duration = this.duration;
            dto.endedAt = this.endedAt != null ? this.endedAt : System.currentTimeMillis();
            dto.countsForStats = this.countsForStats != null ? this.countsForStats : true;
            dto.message = this.message != null ? this.message : dto.generateMessage(this.result);
            return dto;
        }
    }
    
    // ============================================================================
    // NESTED DTO
    // ============================================================================
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoundSummaryDto {
        @JsonProperty("roundNumber")
        private Integer roundNumber;
        
        @JsonProperty("playerCardId")
        private Integer playerCardId;
        
        @JsonProperty("opponentCardId")
        private Integer opponentCardId;
        
        @JsonProperty("result")
        private String result;
        
        @JsonProperty("pointsEarned")
        private Integer pointsEarned;
        
        public RoundSummaryDto() {}
        
        public RoundSummaryDto(Integer roundNumber, Integer playerCardId, Integer opponentCardId, String result, Integer pointsEarned) {
            this.roundNumber = roundNumber;
            this.playerCardId = playerCardId;
            this.opponentCardId = opponentCardId;
            this.result = result;
            this.pointsEarned = pointsEarned;
        }
        
        // Getters & Setters
        public Integer getRoundNumber() { return roundNumber; }
        public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
        public Integer getPlayerCardId() { return playerCardId; }
        public void setPlayerCardId(Integer playerCardId) { this.playerCardId = playerCardId; }
        public Integer getOpponentCardId() { return opponentCardId; }
        public void setOpponentCardId(Integer opponentCardId) { this.opponentCardId = opponentCardId; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public Integer getPointsEarned() { return pointsEarned; }
        public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
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
    
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
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
    
    public String getWinnerId() {
        return winnerId;
    }
    
    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }
    
    public String getWinnerUsername() {
        return winnerUsername;
    }
    
    public void setWinnerUsername(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }
    
    public List<RoundSummaryDto> getRounds() {
        return rounds;
    }
    
    public void setRounds(List<RoundSummaryDto> rounds) {
        this.rounds = rounds;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Long getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(Long endedAt) {
        this.endedAt = endedAt;
    }
    
    public Boolean getCountsForStats() {
        return countsForStats;
    }
    
    public void setCountsForStats(Boolean countsForStats) {
        this.countsForStats = countsForStats;
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
    
    private String generateMessage(String result) {
        if (result == null) {
            return "Game over";
        }
        
        switch (result.toUpperCase()) {
            case "WIN":
                return "Congratulations! You won the game!";
            case "LOSS":
                return "You lost the game. Better luck next time!";
            case "DRAW":
                return "Game is a draw!";
            default:
                return "Game over";
        }
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameResultDto that = (GameResultDto) o;
        return Objects.equals(gameId, that.gameId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameId);
    }
    
    @Override
    public String toString() {
        return "GameResultDto{" +
               "gameId='" + gameId + '\'' +
               ", result='" + result + '\'' +
               ", playerScore=" + playerScore +
               ", opponentScore=" + opponentScore +
               ", winnerId='" + winnerId + '\'' +
               ", duration=" + duration +
               '}';
    }
}
