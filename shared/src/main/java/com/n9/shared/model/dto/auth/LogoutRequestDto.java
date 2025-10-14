package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Logout Request DTO
 * 
 * Request to logout current session.
 * Invalidates session token and disconnects player.
 * 
 * Message Type: AUTH.LOGOUT_REQUEST
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogoutRequestDto {
    
    /**
     * Session ID to logout (optional - defaults to current session)
     */
    @JsonProperty("sessionId")
    private String sessionId;
    
    /**
     * User ID (optional - extracted from auth context)
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * Logout reason (optional)
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Whether to logout all sessions (false = current session only)
     */
    @JsonProperty("logoutAllSessions")
    private Boolean logoutAllSessions;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LogoutRequestDto() {
        this.logoutAllSessions = false;
    }
    
    public LogoutRequestDto(String sessionId) {
        this.sessionId = sessionId;
        this.logoutAllSessions = false;
    }
    
    public LogoutRequestDto(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.logoutAllSessions = false;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static LogoutRequestDto currentSession() {
        return new LogoutRequestDto();
    }
    
    public static LogoutRequestDto allSessions() {
        LogoutRequestDto dto = new LogoutRequestDto();
        dto.logoutAllSessions = true;
        return dto;
    }
    
    public static LogoutRequestDto withReason(String reason) {
        LogoutRequestDto dto = new LogoutRequestDto();
        dto.reason = reason;
        return dto;
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Boolean getLogoutAllSessions() {
        return logoutAllSessions;
    }
    
    public void setLogoutAllSessions(Boolean logoutAllSessions) {
        this.logoutAllSessions = logoutAllSessions;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogoutRequestDto that = (LogoutRequestDto) o;
        return Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId, userId);
    }
    
    @Override
    public String toString() {
        return "LogoutRequestDto{" +
               "sessionId='" + sessionId + '\'' +
               ", userId='" + userId + '\'' +
               ", reason='" + reason + '\'' +
               ", logoutAllSessions=" + logoutAllSessions +
               '}';
    }
}
