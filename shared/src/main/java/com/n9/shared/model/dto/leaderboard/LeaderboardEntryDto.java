package com.n9.shared.model.dto.leaderboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Leaderboard Entry DTO
 * 
 * Single entry in leaderboard.
 * MVP: Sorted by total_wins (games won).
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardEntryDto {
    
    /**
     * Rank position (1 = first place)
     */
    @JsonProperty("rank")
    private Integer rank;
    
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
     * Total games won (primary sort field in MVP)
     */
    @JsonProperty("totalWins")
    private Integer totalWins;
    
    /**
     * Total games played
     */
    @JsonProperty("totalGames")
    private Integer totalGames;
    
    /**
     * Current score/rating
     */
    @JsonProperty("score")
    private Integer score;
    
    /**
     * Win rate percentage (0-100)
     */
    @JsonProperty("winRate")
    private Double winRate;
    
    /**
     * Last game played timestamp (epoch milliseconds)
     */
    @JsonProperty("lastPlayed")
    private Long lastPlayed;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LeaderboardEntryDto() {
        this.totalWins = 0;
        this.totalGames = 0;
        this.score = 0;
        this.winRate = 0.0;
    }
    
    public LeaderboardEntryDto(Integer rank, String userId, String username, Integer totalWins, Integer totalGames) {
        this();
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.totalWins = totalWins;
        this.totalGames = totalGames;
        this.winRate = calculateWinRate(totalWins, totalGames);
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Integer rank;
        private String userId;
        private String username;
        private String displayName;
        private Integer totalWins;
        private Integer totalGames;
        private Integer score;
        private Double winRate;
        private Long lastPlayed;
        
        public Builder rank(Integer rank) {
            this.rank = rank;
            return this;
        }
        
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
        
        public Builder totalWins(Integer totalWins) {
            this.totalWins = totalWins;
            return this;
        }
        
        public Builder totalGames(Integer totalGames) {
            this.totalGames = totalGames;
            return this;
        }
        
        public Builder score(Integer score) {
            this.score = score;
            return this;
        }
        
        public Builder winRate(Double winRate) {
            this.winRate = winRate;
            return this;
        }
        
        public Builder lastPlayed(Long lastPlayed) {
            this.lastPlayed = lastPlayed;
            return this;
        }
        
        public LeaderboardEntryDto build() {
            LeaderboardEntryDto dto = new LeaderboardEntryDto();
            dto.rank = this.rank;
            dto.userId = this.userId;
            dto.username = this.username;
            dto.displayName = this.displayName;
            dto.totalWins = this.totalWins != null ? this.totalWins : 0;
            dto.totalGames = this.totalGames != null ? this.totalGames : 0;
            dto.score = this.score != null ? this.score : 0;
            dto.winRate = this.winRate != null ? this.winRate : calculateWinRate(dto.totalWins, dto.totalGames);
            dto.lastPlayed = this.lastPlayed;
            return dto;
        }
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
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
    
    public Integer getTotalWins() {
        return totalWins;
    }
    
    public void setTotalWins(Integer totalWins) {
        this.totalWins = totalWins;
        this.winRate = calculateWinRate(totalWins, totalGames);
    }
    
    public Integer getTotalGames() {
        return totalGames;
    }
    
    public void setTotalGames(Integer totalGames) {
        this.totalGames = totalGames;
        this.winRate = calculateWinRate(totalWins, totalGames);
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public Double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }
    
    public Long getLastPlayed() {
        return lastPlayed;
    }
    
    public void setLastPlayed(Long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }
    
    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================
    
    private static double calculateWinRate(Integer wins, Integer games) {
        if (games == null || games == 0) {
            return 0.0;
        }
        int w = wins != null ? wins : 0;
        return Math.round((w * 100.0 / games) * 100.0) / 100.0; // 2 decimal places
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderboardEntryDto that = (LeaderboardEntryDto) o;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "LeaderboardEntryDto{" +
               "rank=" + rank +
               ", userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", totalWins=" + totalWins +
               ", totalGames=" + totalGames +
               ", winRate=" + winRate +
               '}';
    }
}
