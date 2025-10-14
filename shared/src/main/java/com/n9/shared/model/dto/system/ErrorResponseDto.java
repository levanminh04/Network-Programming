package com.n9.shared.model.dto.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.n9.shared.model.enums.ErrorCode;

import java.util.Objects;

/**
 * Error Response DTO
 * 
 * Generic error response for all error scenarios.
 * Contains error code, message, and optional details.
 * 
 * Message Type: SYSTEM.ERROR
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {
    
    /**
     * Error code (hierarchical: DOMAIN_SPECIFIC_ERROR)
     */
    @JsonProperty("errorCode")
    private String errorCode;
    
    /**
     * Error message (human-readable)
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Error details (optional, for debugging)
     */
    @JsonProperty("details")
    private String details;
    
    /**
     * Original request correlation ID (for tracing)
     */
    @JsonProperty("correlationId")
    private String correlationId;
    
    /**
     * Error timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * Whether client can retry
     */
    @JsonProperty("retryable")
    private Boolean retryable;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public ErrorResponseDto() {
        this.timestamp = System.currentTimeMillis();
        this.retryable = false;
    }
    
    public ErrorResponseDto(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public ErrorResponseDto(ErrorCode errorCode) {
        this();
        this.errorCode = errorCode.getCode();
        this.message = errorCode.getDefaultMessage();
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static ErrorResponseDto from(ErrorCode errorCode) {
        return new ErrorResponseDto(errorCode);
    }
    
    public static ErrorResponseDto validation(String message) {
        ErrorResponseDto dto = new ErrorResponseDto();
        dto.errorCode = ErrorCode.VALIDATION_FAILED.getCode();
        dto.message = message;
        dto.retryable = true;
        return dto;
    }
    
    public static ErrorResponseDto authentication(String message) {
        ErrorResponseDto dto = new ErrorResponseDto();
        dto.errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS.getCode();
        dto.message = message;
        dto.retryable = false;
        return dto;
    }
    
    public static ErrorResponseDto gameError(String message) {
        ErrorResponseDto dto = new ErrorResponseDto();
        dto.errorCode = "GAME_ERROR";
        dto.message = message;
        dto.retryable = false;
        return dto;
    }
    
    public static ErrorResponseDto internalError() {
        ErrorResponseDto dto = new ErrorResponseDto();
        dto.errorCode = ErrorCode.SYSTEM_INTERNAL_ERROR.getCode();
        dto.message = "Internal server error. Please try again later.";
        dto.retryable = true;
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
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getRetryable() {
        return retryable;
    }
    
    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponseDto that = (ErrorResponseDto) o;
        return Objects.equals(errorCode, that.errorCode) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(errorCode, timestamp);
    }
    
    @Override
    public String toString() {
        return "ErrorResponseDto{" +
               "errorCode='" + errorCode + '\'' +
               ", message='" + message + '\'' +
               ", correlationId='" + correlationId + '\'' +
               ", retryable=" + retryable +
               '}';
    }
}
