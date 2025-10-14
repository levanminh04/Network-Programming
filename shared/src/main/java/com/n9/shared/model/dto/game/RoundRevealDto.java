package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Round Reveal DTO
 * 
 * Reveals both players' cards and round result.
 * Sent when both players have selected (or timeout).
 * 
 * Message Type: GAME.ROUND_REVEAL
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundRevealDto {
    
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
     * Player's selected card
     */
    @JsonProperty("playerCard")
    private CardDto playerCard;
    
    /**
     * Opponent's selected card
     */
    @JsonProperty("opponentCard")
    private CardDto opponentCard;
    
    /**
     * Round result for player (WIN, LOSS, DRAW)
     */
    @JsonProperty("result")
    private String result;
    
    /**
     * Points earned this round
     */
    @JsonProperty("pointsEarned")
    private Integer pointsEarned;
    
    /**
     * Player's total score after this round
     */
    @JsonProperty("playerScore")
    private Integer playerScore;
    
    /**
     * Opponent's total score after this round
     */
    @JsonProperty("opponentScore")
    private Integer opponentScore;
    
    /**
     * Whether player card was auto-picked due to timeout
     */
    @JsonProperty("playerAutoPicked")
    private Boolean playerAutoPicked;
    
    /**
     * Whether opponent card was auto-picked due to timeout
     */
    @JsonProperty("opponentAutoPicked")
    private Boolean opponentAutoPicked;
    
    /**
     * Reveal timestamp (epoch milliseconds)
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
    
    public RoundRevealDto() {
        this.timestamp = System.currentTimeMillis();
        this.playerAutoPicked = false;
        this.opponentAutoPicked = false;
    }
    
    public RoundRevealDto(String gameId, Integer roundNumber, CardDto playerCard, CardDto opponentCard, String result) {
        this();
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.playerCard = playerCard;
        this.opponentCard = opponentCard;
        this.result = result;
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
        private Integer roundNumber;
        private CardDto playerCard;
        private CardDto opponentCard;
        private String result;
        private Integer pointsEarned;
        private Integer playerScore;
        private Integer opponentScore;
        private Boolean playerAutoPicked;
        private Boolean opponentAutoPicked;
        private Long timestamp;
        private String message;
        
        public Builder gameId(String gameId) {
            this.gameId = gameId;
            return this;
        }
        
        public Builder roundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
            return this;
        }
        
        public Builder playerCard(CardDto playerCard) {
            this.playerCard = playerCard;
            return this;
        }
        
        public Builder opponentCard(CardDto opponentCard) {
            this.opponentCard = opponentCard;
            return this;
        }
        
        public Builder result(String result) {
            this.result = result;
            return this;
        }
        
        public Builder pointsEarned(Integer pointsEarned) {
            this.pointsEarned = pointsEarned;
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
        
        public Builder playerAutoPicked(Boolean playerAutoPicked) {
            this.playerAutoPicked = playerAutoPicked;
            return this;
        }
        
        public Builder opponentAutoPicked(Boolean opponentAutoPicked) {
            this.opponentAutoPicked = opponentAutoPicked;
            return this;
        }
        
        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public RoundRevealDto build() {
            RoundRevealDto dto = new RoundRevealDto();
            dto.gameId = this.gameId;
            dto.roundNumber = this.roundNumber;
            dto.playerCard = this.playerCard;
            dto.opponentCard = this.opponentCard;
            dto.result = this.result;
            dto.pointsEarned = this.pointsEarned;
            dto.playerScore = this.playerScore;
            dto.opponentScore = this.opponentScore;
            dto.playerAutoPicked = this.playerAutoPicked != null ? this.playerAutoPicked : false;
            dto.opponentAutoPicked = this.opponentAutoPicked != null ? this.opponentAutoPicked : false;
            dto.timestamp = this.timestamp != null ? this.timestamp : System.currentTimeMillis();
            dto.message = this.message != null ? this.message : dto.generateMessage(this.result);
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
    
    public CardDto getPlayerCard() {
        return playerCard;
    }
    
    public void setPlayerCard(CardDto playerCard) {
        this.playerCard = playerCard;
    }
    
    public CardDto getOpponentCard() {
        return opponentCard;
    }
    
    public void setOpponentCard(CardDto opponentCard) {
        this.opponentCard = opponentCard;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public Integer getPointsEarned() {
        return pointsEarned;
    }
    
    public void setPointsEarned(Integer pointsEarned) {
        this.pointsEarned = pointsEarned;
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
    
    public Boolean getPlayerAutoPicked() {
        return playerAutoPicked;
    }
    
    public void setPlayerAutoPicked(Boolean playerAutoPicked) {
        this.playerAutoPicked = playerAutoPicked;
    }
    
    public Boolean getOpponentAutoPicked() {
        return opponentAutoPicked;
    }
    
    public void setOpponentAutoPicked(Boolean opponentAutoPicked) {
        this.opponentAutoPicked = opponentAutoPicked;
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
    // BUSINESS METHODS
    // ============================================================================
    
    private String generateMessage(String result) {
        if (result == null) {
            return "Round complete";
        }
        
        switch (result.toUpperCase()) {
            case "WIN":
                return "You won this round!";
            case "LOSS":
                return "You lost this round";
            case "DRAW":
                return "Round is a draw";
            default:
                return "Round complete";
        }
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoundRevealDto that = (RoundRevealDto) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(roundNumber, that.roundNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gameId, roundNumber);
    }
    
    @Override
    public String toString() {
        return "RoundRevealDto{" +
               "gameId='" + gameId + '\'' +
               ", roundNumber=" + roundNumber +
               ", result='" + result + '\'' +
               ", pointsEarned=" + pointsEarned +
               ", playerScore=" + playerScore +
               ", opponentScore=" + opponentScore +
               ", playerAutoPicked=" + playerAutoPicked +
               ", opponentAutoPicked=" + opponentAutoPicked +
               '}';
    }
}
