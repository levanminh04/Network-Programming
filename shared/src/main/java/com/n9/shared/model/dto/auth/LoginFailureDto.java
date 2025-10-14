package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.n9.shared.model.enums.ErrorCode;

import java.util.Objects;

/**
 * Login Failure DTO
 * 
 * Response payload when login fails.
 * Contains error details and reason for failure.
 * 
 * Message Type: AUTH.LOGIN_FAIL
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginFailureDto {
    
    /**
     * Error code
     */
    @JsonProperty("errorCode")
    private String errorCode;
    
    /**
     * Error message (human-readable)
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Failure reason (INVALID_CREDENTIALS, USER_NOT_FOUND, ACCOUNT_LOCKED, etc.)
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Timestamp of failure (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * Retry allowed flag
     */
    @JsonProperty("retryAllowed")
    private Boolean retryAllowed;
    
    /**
     * Retry after (seconds) - for rate limiting
     */
    @JsonProperty("retryAfter")
    private Integer retryAfter;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LoginFailureDto() {
        this.timestamp = System.currentTimeMillis();
        this.retryAllowed = true;
    }
    
    public LoginFailureDto(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.retryAllowed = true;
    }
    
    public LoginFailureDto(ErrorCode errorCode) {
        this.errorCode = errorCode.getCode();
        this.message = errorCode.getDefaultMessage();
        this.timestamp = System.currentTimeMillis();
        this.retryAllowed = true;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static LoginFailureDto invalidCredentials() {
        LoginFailureDto dto = new LoginFailureDto();
        dto.errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS.getCode();
        dto.message = "Invalid username or password";
        dto.reason = "INVALID_CREDENTIALS";
        dto.retryAllowed = true;
        return dto;
    }
    
    public static LoginFailureDto userNotFound() {
        LoginFailureDto dto = new LoginFailureDto();
        dto.errorCode = ErrorCode.AUTH_USER_NOT_FOUND.getCode();
        dto.message = "User not found";
        dto.reason = "USER_NOT_FOUND";
        dto.retryAllowed = true;
        return dto;
    }
    
    public static LoginFailureDto accountLocked(int retryAfter) {
        LoginFailureDto dto = new LoginFailureDto();
        dto.errorCode = "AUTH_ACCOUNT_LOCKED";
        dto.message = "Account locked due to too many failed attempts";
        dto.reason = "ACCOUNT_LOCKED";
        dto.retryAllowed = false;
        dto.retryAfter = retryAfter;
        return dto;
    }
    
    public static LoginFailureDto sessionConflict() {
        LoginFailureDto dto = new LoginFailureDto();
        dto.errorCode = "AUTH_SESSION_CONFLICT";
        dto.message = "User already logged in from another location";
        dto.reason = "SESSION_CONFLICT";
        dto.retryAllowed = false;
        return dto;
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
    
    public Boolean getRetryAllowed() {
        return retryAllowed;
    }
    
    public void setRetryAllowed(Boolean retryAllowed) {
        this.retryAllowed = retryAllowed;
    }
    
    public Integer getRetryAfter() {
        return retryAfter;
    }
    
    public void setRetryAfter(Integer retryAfter) {
        this.retryAfter = retryAfter;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginFailureDto that = (LoginFailureDto) o;
        return Objects.equals(errorCode, that.errorCode) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(errorCode, timestamp);
    }
    
    @Override
    public String toString() {
        return "LoginFailureDto{" +
               "errorCode='" + errorCode + '\'' +
               ", message='" + message + '\'' +
               ", reason='" + reason + '\'' +
               ", retryAllowed=" + retryAllowed +
               ", retryAfter=" + retryAfter +
               '}';
    }
}
