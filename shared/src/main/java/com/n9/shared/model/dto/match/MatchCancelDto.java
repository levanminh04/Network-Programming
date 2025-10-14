package com.n9.shared.model.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Match Cancel DTO
 * 
 * Request to cancel matchmaking (leave queue).
 * 
 * Message Type: MATCH.CANCEL
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchCancelDto {
    
    /**
     * User ID (extracted from auth context)
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Cancel reason (optional)
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public MatchCancelDto() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public MatchCancelDto(String userId) {
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }
    
    public MatchCancelDto(String userId, String reason) {
        this.userId = userId;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static MatchCancelDto userCancelled() {
        return new MatchCancelDto(null, "User cancelled");
    }
    
    public static MatchCancelDto timeout() {
        return new MatchCancelDto(null, "Matchmaking timeout");
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
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
        MatchCancelDto that = (MatchCancelDto) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, timestamp);
    }
    
    @Override
    public String toString() {
        return "MatchCancelDto{" +
               "userId='" + userId + '\'' +
               ", reason='" + reason + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
