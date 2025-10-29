package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Register Response DTO (Success)
 * 
 * Response payload when registration succeeds.
 * Contains basic user info but NOT auth token (user must login separately).
 * 
 * Message Type: AUTH.REGISTER_SUCCESS
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponseDto {
    
    /**
     * Newly created user ID
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Username (as registered)
     */
    @JsonProperty("username")
    private String username;
    
    /**
     * Email (as registered)
     */
    @JsonProperty("email")
    private String email;
    
    /**
     * Display name (effective display name)
     */
    @JsonProperty("displayName")
    private String displayName;
    
    /**
     * Registration timestamp (epoch milliseconds)
     */
    @JsonProperty("registeredAt")
    private Long registeredAt;
    
    /**
     * Success message
     */
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private Long timestamp;
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public RegisterResponseDto() {
        // Default constructor for JSON deserialization
    }
    
    public RegisterResponseDto(String userId, String username, String email, String displayName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.registeredAt = System.currentTimeMillis();
        this.message = "Registration successful. Please login to continue.";
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
        private String email;
        private String displayName;
        private Long registeredAt;
        private String message;
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder registeredAt(Long registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public RegisterResponseDto build() {
            RegisterResponseDto dto = new RegisterResponseDto();
            dto.userId = this.userId;
            dto.username = this.username;
            dto.email = this.email;
            dto.displayName = this.displayName;
            dto.registeredAt = this.registeredAt != null ? this.registeredAt : System.currentTimeMillis();
            dto.message = this.message != null ? this.message : "Registration successful";
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public Long getRegisteredAt() {
        return registeredAt;
    }
    
    public void setRegisteredAt(Long registeredAt) {
        this.registeredAt = registeredAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterResponseDto that = (RegisterResponseDto) o;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "RegisterResponseDto{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", displayName='" + displayName + '\'' +
               ", registeredAt=" + registeredAt +
               ", message='" + message + '\'' +
               '}';
    }
}
