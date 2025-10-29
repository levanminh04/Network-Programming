package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;

/**
 * Login Success Response Data Transfer Object
 * 
 * Contains authentication result and user information.
 * Returned when login is successful.
 * 
 * @author N9 Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginSuccessDto {
    
    /** Unique user identifier */
    @JsonProperty("userId")
    private String userId;
    
    /** Username */
    @JsonProperty("username")
    private String username;
    
    /** Display name */
    @JsonProperty("displayName")
    private String displayName;
    
    /** JWT authentication token */
    @JsonProperty("token")
    private String token;
    
    /** Token expiration time (epoch milliseconds) */
    @JsonProperty("expiresAt")
    private Long expiresAt;
    
    /** Refresh token for session extension */
    @JsonProperty("refreshToken")
    private String refreshToken;
    
    /** User's current score/rating */
    @JsonProperty("score")
    private Double score;
    
    /** Total games played */
    @JsonProperty("gamesPlayed")
    private Integer gamesPlayed;
    
    /** Total games won */
    @JsonProperty("gamesWon")
    private Integer gamesWon;
    
    /** User's current rank/level */
    @JsonProperty("rank")
    private String rank;
    
    /** Last login timestamp */
    @JsonProperty("lastLogin")
    private Long lastLogin;
    // THÊM: Email nếu AuthService trả về
    @JsonProperty("email")
    private String email;

    // THÊM: Timestamp nếu AuthService trả về
    @JsonProperty("timestamp")
    private Long timestamp;
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LoginSuccessDto() {
        // Default constructor for JSON deserialization
    }
    
    public LoginSuccessDto(String userId, String username, String token, Long expiresAt) {
        this.userId = userId;
        this.username = username;
        this.token = token;
        this.expiresAt = expiresAt;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static class Builder {
        private String userId;
        private String username;
        private String displayName;
        private String token;
        private Long expiresAt;
        private String refreshToken;
        private Double score;
        private Integer gamesPlayed;
        private Integer gamesWon;
        private String rank;
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
        
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        public Builder expiresAt(Long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public Builder score(Double score) {
            this.score = score;
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
        
        public Builder rank(String rank) {
            this.rank = rank;
            return this;
        }
        
        public Builder lastLogin(Long lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }
        
        public LoginSuccessDto build() {
            LoginSuccessDto dto = new LoginSuccessDto();
            dto.userId = this.userId;
            dto.username = this.username;
            dto.displayName = this.displayName;
            dto.token = this.token;
            dto.expiresAt = this.expiresAt;
            dto.refreshToken = this.refreshToken;
            dto.score = this.score;
            dto.gamesPlayed = this.gamesPlayed;
            dto.gamesWon = this.gamesWon;
            dto.rank = this.rank;
            dto.lastLogin = this.lastLogin;
            return dto;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // ============================================================================
    // ACCESSORS
    // ============================================================================
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // THÊM SETTER CHO TIMESTAMP
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    
    public Integer getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(Integer gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    
    public Integer getGamesWon() { return gamesWon; }
    public void setGamesWon(Integer gamesWon) { this.gamesWon = gamesWon; }
    
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
    
    public Long getLastLogin() { return lastLogin; }
    public void setLastLogin(Long lastLogin) { this.lastLogin = lastLogin; }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired() {
        return expiresAt != null && Instant.now().toEpochMilli() >= expiresAt;
    }
    
    /**
     * Get token expiration in seconds from now
     */
    public long getTokenExpirationSeconds() {
        if (expiresAt == null) return 0;
        long secondsUntilExpiry = (expiresAt - Instant.now().toEpochMilli()) / 1000;
        return Math.max(0, secondsUntilExpiry);
    }
    
    /**
     * Calculate win rate percentage
     */
    public double getWinRate() {
        if (gamesPlayed == null || gamesPlayed == 0) return 0.0;
        if (gamesWon == null) return 0.0;
        return (double) gamesWon / gamesPlayed * 100.0;
    }
    
    /**
     * Get effective display name (fallback to username)
     */
    public String getEffectiveDisplayName() {
        return displayName != null ? displayName : username;
    }
    
    // ============================================================================
    // EQUALS, HASHCODE, TOSTRING
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginSuccessDto that = (LoginSuccessDto) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(username, that.username) &&
               Objects.equals(displayName, that.displayName) &&
               Objects.equals(token, that.token) &&
               Objects.equals(expiresAt, that.expiresAt) &&
               Objects.equals(refreshToken, that.refreshToken) &&
               Objects.equals(score, that.score) &&
               Objects.equals(gamesPlayed, that.gamesPlayed) &&
               Objects.equals(gamesWon, that.gamesWon) &&
               Objects.equals(rank, that.rank) &&
               Objects.equals(lastLogin, that.lastLogin);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, username, displayName, token, expiresAt, 
                           refreshToken, score, gamesPlayed, gamesWon, rank, lastLogin);
    }
    
    @Override
    public String toString() {
        return "LoginSuccessDto{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", displayName='" + displayName + '\'' +
               ", token='[HIDDEN]'" +
               ", expiresAt=" + expiresAt +
               ", refreshToken='[HIDDEN]'" +
               ", score=" + score +
               ", gamesPlayed=" + gamesPlayed +
               ", gamesWon=" + gamesWon +
               ", rank='" + rank + '\'' +
               ", lastLogin=" + lastLogin +
               '}';
    }
}