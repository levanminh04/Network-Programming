package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Session DTO
 * 
 * Represents an active user session.
 * Contains session metadata and validity info.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionDto {
    
    /**
     * Session ID (unique identifier)
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * User ID
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Session token (JWT or opaque token)
     */
    @JsonProperty("token")
    private String token;
    
    /**
     * Session creation timestamp (epoch milliseconds)
     */
    @JsonProperty("createdAt")
    private Long createdAt;
    
    /**
     * Last activity timestamp (epoch milliseconds)
     */
    @JsonProperty("lastActivityAt")
    private Long lastActivityAt;
    
    /**
     * Session expiration timestamp (epoch milliseconds)
     */
    @JsonProperty("expiresAt")
    private Long expiresAt;
    
    /**
     * Whether session is still valid
     */
    @JsonProperty("valid")
    private Boolean valid;
    
    /**
     * Client IP address (for security tracking)
     */
    @JsonProperty("ipAddress")
    private String ipAddress;
    
    /**
     * User agent (for security tracking)
     */
    @JsonProperty("userAgent")
    private String userAgent;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public SessionDto() {
        this.valid = true;
        this.createdAt = System.currentTimeMillis();
        this.lastActivityAt = this.createdAt;
    }
    
    public SessionDto(String sessionId, String userId, String token, Long expiresAt) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String sessionId;
        private String userId;
        private String token;
        private Long createdAt;
        private Long lastActivityAt;
        private Long expiresAt;
        private Boolean valid;
        private String ipAddress;
        private String userAgent;
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        public Builder createdAt(Long createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder lastActivityAt(Long lastActivityAt) {
            this.lastActivityAt = lastActivityAt;
            return this;
        }
        
        public Builder expiresAt(Long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public Builder valid(Boolean valid) {
            this.valid = valid;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public SessionDto build() {
            SessionDto dto = new SessionDto();
            dto.sessionId = this.sessionId;
            dto.userId = this.userId;
            dto.token = this.token;
            dto.createdAt = this.createdAt != null ? this.createdAt : System.currentTimeMillis();
            dto.lastActivityAt = this.lastActivityAt != null ? this.lastActivityAt : dto.createdAt;
            dto.expiresAt = this.expiresAt;
            dto.valid = this.valid != null ? this.valid : true;
            dto.ipAddress = this.ipAddress;
            dto.userAgent = this.userAgent;
            return dto;
        }
    }
    
    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================
    
    /**
     * Check if session is expired
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Update last activity timestamp
     */
    public void updateActivity() {
        this.lastActivityAt = System.currentTimeMillis();
    }
    
    /**
     * Invalidate session
     */
    public void invalidate() {
        this.valid = false;
    }
    
    /**
     * Get remaining time in seconds
     * 
     * @return Remaining seconds, or -1 if expired
     */
    public long getRemainingSeconds() {
        if (expiresAt == null) {
            return Long.MAX_VALUE;
        }
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(Long lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Boolean getValid() {
        return valid;
    }
    
    public void setValid(Boolean valid) {
        this.valid = valid;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionDto that = (SessionDto) o;
        return Objects.equals(sessionId, that.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
    
    @Override
    public String toString() {
        return "SessionDto{" +
               "sessionId='" + sessionId + '\'' +
               ", userId='" + userId + '\'' +
               ", createdAt=" + createdAt +
               ", lastActivityAt=" + lastActivityAt +
               ", expiresAt=" + expiresAt +
               ", valid=" + valid +
               ", ipAddress='" + ipAddress + '\'' +
               '}';
    }
}
