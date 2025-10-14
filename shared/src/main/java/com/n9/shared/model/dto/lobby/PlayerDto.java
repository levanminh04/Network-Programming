package com.n9.shared.model.dto.lobby;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Player DTO
 * 
 * Player information for lobby/game display.
 * Contains stats and current status.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDto {
    
    /**
     * User ID
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Username
     */
    @JsonProperty("username")
    private String username;
    
    /**
     * Display name
     */
    @JsonProperty("displayName")
    private String displayName;
    
    /**
     * Player status (ONLINE, IN_QUEUE, IN_GAME, etc.)
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * Total games played
     */
    @JsonProperty("gamesPlayed")
    private Integer gamesPlayed;
    
    /**
     * Total games won
     */
    @JsonProperty("gamesWon")
    private Integer gamesWon;
    
    /**
     * Current score/rating
     */
    @JsonProperty("score")
    private Integer score;
    
    /**
     * Leaderboard rank (optional)
     */
    @JsonProperty("rank")
    private Integer rank;
    
    /**
     * Last login timestamp (epoch milliseconds)
     */
    @JsonProperty("lastLogin")
    private Long lastLogin;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public PlayerDto() {
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.score = 0;
    }
    
    public PlayerDto(String userId, String username, String displayName) {
        this();
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String userId;
        private String username;
        private String displayName;
        private String status;
        private Integer gamesPlayed;
        private Integer gamesWon;
        private Integer score;
        private Integer rank;
        private Long lastLogin;
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder gamesPlayed(Integer gamesPlayed) {
            this.gamesPlayed = gamesPlayed;
            return this;
        }
        
        public Builder gamesWon(Integer gamesWon) {
            this.gamesWon = gamesWon;
            return this;
        }
        
        public Builder score(Integer score) {
            this.score = score;
            return this;
        }
        
        public Builder rank(Integer rank) {
            this.rank = rank;
            return this;
        }
        
        public Builder lastLogin(Long lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }
        
        public PlayerDto build() {
            PlayerDto dto = new PlayerDto();
            dto.userId = this.userId;
            dto.username = this.username;
            dto.displayName = this.displayName;
            dto.status = this.status;
            dto.gamesPlayed = this.gamesPlayed != null ? this.gamesPlayed : 0;
            dto.gamesWon = this.gamesWon != null ? this.gamesWon : 0;
            dto.score = this.score != null ? this.score : 0;
            dto.rank = this.rank;
            dto.lastLogin = this.lastLogin;
            return dto;
        }
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getGamesPlayed() {
        return gamesPlayed;
    }
    
    public void setGamesPlayed(Integer gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
    
    public Integer getGamesWon() {
        return gamesWon;
    }
    
    public void setGamesWon(Integer gamesWon) {
        this.gamesWon = gamesWon;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public Long getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================
    
    /**
     * Get win rate percentage (0-100)
     */
    public double getWinRate() {
        if (gamesPlayed == null || gamesPlayed == 0) {
            return 0.0;
        }
        return (gamesWon != null ? gamesWon : 0) * 100.0 / gamesPlayed;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerDto playerDto = (PlayerDto) o;
        return Objects.equals(userId, playerDto.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "PlayerDto{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", displayName='" + displayName + '\'' +
               ", status='" + status + '\'' +
               ", gamesPlayed=" + gamesPlayed +
               ", gamesWon=" + gamesWon +
               ", score=" + score +
               ", rank=" + rank +
               '}';
    }
}
