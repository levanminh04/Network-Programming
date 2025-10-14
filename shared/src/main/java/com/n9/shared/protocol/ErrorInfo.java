package com.n9.shared.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.n9.shared.model.enums.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Error Information Container
 * 
 * Provides detailed error context for failed operations.
 * Used in MessageEnvelope.error field.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfo {
    
    /**
     * Error code (hierarchical format: DOMAIN_CATEGORY_ERROR)
     */
    @JsonProperty("code")
    private String code;
    
    /**
     * Human-readable error message
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Detailed error description (optional, for debugging)
     */
    @JsonProperty("details")
    private String details;
    
    /**
     * Field-specific errors (for validation failures)
     * Key: field name, Value: error message
     */
    @JsonProperty("fieldErrors")
    private Map<String, String> fieldErrors;
    
    /**
     * Timestamp when error occurred (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private long timestamp;
    
    /**
     * Whether the operation can be retried
     */
    @JsonProperty("retryable")
    private boolean retryable;
    
    /**
     * Suggested action for user/client
     */
    @JsonProperty("suggestedAction")
    private String suggestedAction;
    
    /**
     * Internal error trace ID (for support/debugging)
     */
    @JsonProperty("traceId")
    private String traceId;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    /**
     * Default constructor (for Jackson)
     */
    public ErrorInfo() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor with ErrorCode
     * 
     * @param errorCode Error code enum
     */
    public ErrorInfo(ErrorCode errorCode) {
        this();
        this.code = errorCode.getCode();
        this.message = errorCode.getDefaultMessage();
    }
    
    /**
     * Constructor with ErrorCode and custom message
     * 
     * @param errorCode Error code enum
     * @param message Custom error message
     */
    public ErrorInfo(ErrorCode errorCode, String message) {
        this();
        this.code = errorCode.getCode();
        this.message = message != null ? message : errorCode.getDefaultMessage();
    }
    
    /**
     * Constructor with ErrorCode, message, and details
     * 
     * @param errorCode Error code enum
     * @param message Custom error message
     * @param details Detailed error description
     */
    public ErrorInfo(ErrorCode errorCode, String message, String details) {
        this(errorCode, message);
        this.details = details;
    }
    
    /**
     * Constructor with code and message strings
     * 
     * @param code Error code string
     * @param message Error message
     */
    public ErrorInfo(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    /**
     * Create validation error with field errors
     * 
     * @param fieldErrors Map of field name to error message
     * @return ErrorInfo instance
     */
    public static ErrorInfo validationError(Map<String, String> fieldErrors) {
        ErrorInfo error = new ErrorInfo(ErrorCode.VALIDATION_FAILED);
        error.fieldErrors = new HashMap<>(fieldErrors);
        error.retryable = true;
        error.suggestedAction = "Please correct the highlighted fields and try again";
        return error;
    }
    
    /**
     * Create validation error for single field
     * 
     * @param fieldName Field name
     * @param errorMessage Error message
     * @return ErrorInfo instance
     */
    public static ErrorInfo validationError(String fieldName, String errorMessage) {
        Map<String, String> errors = new HashMap<>();
        errors.put(fieldName, errorMessage);
        return validationError(errors);
    }
    
    /**
     * Create authentication error
     * 
     * @param message Error message
     * @return ErrorInfo instance
     */
    public static ErrorInfo authenticationError(String message) {
        ErrorInfo error = new ErrorInfo(ErrorCode.AUTH_INVALID_CREDENTIALS, message);
        error.retryable = true;
        error.suggestedAction = "Please check your credentials and try again";
        return error;
    }
    
    /**
     * Create internal server error
     * 
     * @param traceId Trace ID for support
     * @return ErrorInfo instance
     */
    public static ErrorInfo internalError(String traceId) {
        ErrorInfo error = new ErrorInfo(ErrorCode.SYSTEM_INTERNAL_ERROR);
        error.traceId = traceId;
        error.retryable = true;
        error.suggestedAction = "Please try again later. If problem persists, contact support with trace ID: " + traceId;
        return error;
    }
    
    /**
     * Create game logic error
     * 
     * @param errorCode Error code
     * @param message Error message
     * @param retryable Whether operation can be retried
     * @return ErrorInfo instance
     */
    public static ErrorInfo gameError(ErrorCode errorCode, String message, boolean retryable) {
        ErrorInfo error = new ErrorInfo(errorCode, message);
        error.retryable = retryable;
        return error;
    }
    
    /**
     * Create network error
     * 
     * @param message Error message
     * @return ErrorInfo instance
     */
    public static ErrorInfo networkError(String message) {
        ErrorInfo error = new ErrorInfo(ErrorCode.NETWORK_CONNECTION_FAILED, message);
        error.retryable = true;
        error.suggestedAction = "Please check your internet connection and try again";
        return error;
    }
    
    // ============================================================================
    // BUILDER
    // ============================================================================
    
    public static class Builder {
        private final ErrorInfo errorInfo;
        
        public Builder(ErrorCode errorCode) {
            this.errorInfo = new ErrorInfo(errorCode);
        }
        
        public Builder(String code, String message) {
            this.errorInfo = new ErrorInfo(code, message);
        }
        
        public Builder details(String details) {
            errorInfo.details = details;
            return this;
        }
        
        public Builder fieldErrors(Map<String, String> fieldErrors) {
            errorInfo.fieldErrors = new HashMap<>(fieldErrors);
            return this;
        }
        
        public Builder addFieldError(String field, String error) {
            if (errorInfo.fieldErrors == null) {
                errorInfo.fieldErrors = new HashMap<>();
            }
            errorInfo.fieldErrors.put(field, error);
            return this;
        }
        
        public Builder retryable(boolean retryable) {
            errorInfo.retryable = retryable;
            return this;
        }
        
        public Builder suggestedAction(String action) {
            errorInfo.suggestedAction = action;
            return this;
        }
        
        public Builder traceId(String traceId) {
            errorInfo.traceId = traceId;
            return this;
        }
        
        public ErrorInfo build() {
            return errorInfo;
        }
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Add field error
     * 
     * @param fieldName Field name
     * @param errorMessage Error message
     */
    public void addFieldError(String fieldName, String errorMessage) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new HashMap<>();
        }
        this.fieldErrors.put(fieldName, errorMessage);
    }
    
    /**
     * Check if this is a validation error
     * 
     * @return true if validation error
     */
    public boolean isValidationError() {
        return code != null && (
            code.startsWith("VALIDATION_") || 
            (fieldErrors != null && !fieldErrors.isEmpty())
        );
    }
    
    /**
     * Check if this is an authentication error
     * 
     * @return true if auth error
     */
    public boolean isAuthenticationError() {
        return code != null && code.startsWith("AUTH_");
    }
    
    /**
     * Check if this is a system error
     * 
     * @return true if system error
     */
    public boolean isSystemError() {
        return code != null && code.startsWith("SYSTEM_");
    }
    
    /**
     * Get error domain (AUTH, VALIDATION, GAME, etc.)
     * 
     * @return Error domain
     */
    public String getErrorDomain() {
        if (code == null) return "UNKNOWN";
        int index = code.indexOf('_');
        return index > 0 ? code.substring(0, index) : code;
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
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
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
    
    public String getSuggestedAction() {
        return suggestedAction;
    }
    
    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorInfo errorInfo = (ErrorInfo) o;
        return Objects.equals(code, errorInfo.code) &&
               Objects.equals(message, errorInfo.message);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }
    
    @Override
    public String toString() {
        return "ErrorInfo{" +
               "code='" + code + '\'' +
               ", message='" + message + '\'' +
               (details != null ? ", details='" + details + '\'' : "") +
               (traceId != null ? ", traceId='" + traceId + '\'' : "") +
               ", retryable=" + retryable +
               '}';
    }
}
