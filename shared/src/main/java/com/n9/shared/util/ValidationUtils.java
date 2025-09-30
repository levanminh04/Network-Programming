package com.n9.shared.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Comprehensive validation utilities for message protocols and DTOs.
 * Provides both Bean Validation (JSR-303) integration and custom validation
 * methods optimized for network protocols.
 * 
 * Features:
 * - Bean Validation integration with custom error messages
 * - Username/password validation with security best practices
 * - Message size and format validation
 * - Network protocol specific validations
 * - Rate limiting and security checks
 * 
 * Usage:
 * <pre>
 * // Validate DTO with annotations
 * ValidationResult result = ValidationUtils.validate(loginRequest);
 * if (!result.isValid()) {
 *     throw new ValidationException(result.getErrorMessage());
 * }
 * 
 * // Custom validation methods
 * if (!ValidationUtils.isValidUsername("player123")) {
 *     throw new IllegalArgumentException("Invalid username format");
 * }
 * </pre>
 * 
 * @author Network Programming Team
 * @version 1.0.0
 * @since 2024-01-09
 */
public final class ValidationUtils {
    
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    
    // Validation patterns
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern MATCH_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-]{10,50}$");
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]{5,100}$");
    
    // Security constraints
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 100;
    private static final int MAX_MESSAGE_SIZE_BYTES = 64 * 1024; // 64KB
    private static final int MAX_PAYLOAD_SIZE_BYTES = 32 * 1024; // 32KB
    private static final int MAX_STRING_FIELD_LENGTH = 1000;
    
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Validation result container with error details.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Set<String> fieldErrors;
        
        private ValidationResult(boolean valid, String errorMessage, Set<String> fieldErrors) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.fieldErrors = fieldErrors;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null, Set.of());
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, Set.of());
        }
        
        public static ValidationResult failure(String errorMessage, Set<String> fieldErrors) {
            return new ValidationResult(false, errorMessage, fieldErrors);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public Set<String> getFieldErrors() { return fieldErrors; }
        
        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult{valid=true}";
            }
            return String.format("ValidationResult{valid=false, error='%s', fieldErrors=%s}", 
                               errorMessage, fieldErrors);
        }
    }
    
    /**
     * Validate an object using Bean Validation annotations.
     * 
     * @param object the object to validate
     * @return validation result with error details
     */
    public static ValidationResult validate(Object object) {
        if (object == null) {
            return ValidationResult.failure("Object cannot be null");
        }
        
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object);
        
        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        
        Set<String> fieldErrors = violations.stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toSet());
        
        String errorMessage = "Validation failed: " + String.join(", ", fieldErrors);
        
        return ValidationResult.failure(errorMessage, fieldErrors);
    }
    
    /**
     * Validate username format and security requirements.
     * Rules: 3-50 characters, alphanumeric + underscore/dash only.
     * 
     * @param username the username to validate
     * @return true if username is valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }
    
    /**
     * Validate password strength and security requirements.
     * Rules: 6-100 characters, no whitespace only passwords.
     * 
     * @param password the password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }
        
        // Check if password is all whitespace
        if (password.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get detailed password validation errors for user feedback.
     * 
     * @param password the password to validate
     * @return validation result with specific error messages
     */
    public static ValidationResult validatePasswordDetailed(String password) {
        if (password == null) {
            return ValidationResult.failure("Password cannot be null");
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.failure(
                String.format("Password must be at least %d characters long", MIN_PASSWORD_LENGTH));
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return ValidationResult.failure(
                String.format("Password cannot exceed %d characters", MAX_PASSWORD_LENGTH));
        }
        
        if (password.trim().isEmpty()) {
            return ValidationResult.failure("Password cannot be empty or whitespace only");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate email address format.
     * 
     * @param email the email to validate
     * @return true if email format is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate match ID format for game sessions.
     * Rules: 10-50 characters, alphanumeric + dash only.
     * 
     * @param matchId the match ID to validate
     * @return true if match ID is valid, false otherwise
     */
    public static boolean isValidMatchId(String matchId) {
        if (matchId == null || matchId.trim().isEmpty()) {
            return false;
        }
        
        return MATCH_ID_PATTERN.matcher(matchId.trim()).matches();
    }
    
    /**
     * Validate correlation ID format for message tracking.
     * Rules: 5-100 characters, alphanumeric + dash/underscore only.
     * 
     * @param correlationId the correlation ID to validate
     * @return true if correlation ID is valid, false otherwise
     */
    public static boolean isValidCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return false;
        }
        
        return CORRELATION_ID_PATTERN.matcher(correlationId.trim()).matches();
    }
    
    /**
     * Validate card index for game mechanics.
     * Rules: Must be between 0 and 51 (standard 52-card deck).
     * 
     * @param cardIndex the card index to validate
     * @return true if card index is valid, false otherwise
     */
    public static boolean isValidCardIndex(int cardIndex) {
        return cardIndex >= 0 && cardIndex <= 51;
    }
    
    /**
     * Validate round number for game mechanics.
     * Rules: Must be between 1 and 3 (standard 3-round game).
     * 
     * @param roundNumber the round number to validate
     * @return true if round number is valid, false otherwise
     */
    public static boolean isValidRoundNumber(int roundNumber) {
        return roundNumber >= 1 && roundNumber <= 3;
    }
    
    /**
     * Validate timestamp is within reasonable bounds.
     * Rules: Must be positive and not too far in the future (1 hour max).
     * 
     * @param timestamp the timestamp to validate (epoch milliseconds)
     * @return true if timestamp is valid, false otherwise
     */
    public static boolean isValidTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long oneHourFromNow = currentTime + (60 * 60 * 1000); // 1 hour in milliseconds
        
        // Timestamp should not be too far in the future
        return timestamp <= oneHourFromNow;
    }
    
    /**
     * Validate string field length for network protocols.
     * Rules: Non-null, not empty, within max length limit.
     * 
     * @param value the string value to validate
     * @param fieldName the field name for error messages
     * @return validation result with error details
     */
    public static ValidationResult validateStringField(String value, String fieldName) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        
        if (value.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }
        
        if (value.length() > MAX_STRING_FIELD_LENGTH) {
            return ValidationResult.failure(
                String.format("%s cannot exceed %d characters", fieldName, MAX_STRING_FIELD_LENGTH));
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate string field length with custom limits.
     * 
     * @param value the string value to validate
     * @param fieldName the field name for error messages
     * @param minLength minimum required length
     * @param maxLength maximum allowed length
     * @return validation result with error details
     */
    public static ValidationResult validateStringField(String value, String fieldName, 
                                                     int minLength, int maxLength) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        
        if (value.length() < minLength) {
            return ValidationResult.failure(
                String.format("%s must be at least %d characters long", fieldName, minLength));
        }
        
        if (value.length() > maxLength) {
            return ValidationResult.failure(
                String.format("%s cannot exceed %d characters", fieldName, maxLength));
        }
        
        if (minLength > 0 && value.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate message size for network transmission.
     * Rules: Total message should not exceed protocol limits.
     * 
     * @param messageJson the JSON message string
     * @return validation result with size information
     */
    public static ValidationResult validateMessageSize(String messageJson) {
        if (messageJson == null) {
            return ValidationResult.failure("Message cannot be null");
        }
        
        int sizeBytes = messageJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        
        if (sizeBytes > MAX_MESSAGE_SIZE_BYTES) {
            return ValidationResult.failure(
                String.format("Message size (%d bytes) exceeds maximum allowed (%d bytes)", 
                             sizeBytes, MAX_MESSAGE_SIZE_BYTES));
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate payload size for network transmission.
     * Rules: Payload should not exceed protocol limits.
     * 
     * @param payloadJson the JSON payload string
     * @return validation result with size information
     */
    public static ValidationResult validatePayloadSize(String payloadJson) {
        if (payloadJson == null) {
            return ValidationResult.success(); // Null payload is allowed
        }
        
        int sizeBytes = payloadJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        
        if (sizeBytes > MAX_PAYLOAD_SIZE_BYTES) {
            return ValidationResult.failure(
                String.format("Payload size (%d bytes) exceeds maximum allowed (%d bytes)", 
                             sizeBytes, MAX_PAYLOAD_SIZE_BYTES));
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Check if a user ID has valid format.
     * Rules: Non-null, non-empty, reasonable length.
     * 
     * @param userId the user ID to validate
     * @return true if user ID is valid, false otherwise
     */
    public static boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        return userId.length() >= 3 && userId.length() <= 50;
    }
    
    /**
     * Validate session ID format.
     * Rules: Non-null, non-empty, reasonable length.
     * 
     * @param sessionId the session ID to validate
     * @return true if session ID is valid, false otherwise
     */
    public static boolean isValidSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        return sessionId.length() >= 5 && sessionId.length() <= 100;
    }
    
    /**
     * Validate game mode string.
     * Rules: Must be one of the supported game modes.
     * 
     * @param gameMode the game mode to validate
     * @return true if game mode is valid, false otherwise
     */
    public static boolean isValidGameMode(String gameMode) {
        if (gameMode == null || gameMode.trim().isEmpty()) {
            return false;
        }
        
        String mode = gameMode.trim().toUpperCase();
        return mode.equals("QUICK") || mode.equals("RANKED") || mode.equals("CUSTOM");
    }
    
    /**
     * Validate score value for game mechanics.
     * Rules: Must be non-negative and within reasonable bounds.
     * 
     * @param score the score to validate
     * @return true if score is valid, false otherwise
     */
    public static boolean isValidScore(double score) {
        return score >= 0.0 && score <= 10000.0 && !Double.isNaN(score) && !Double.isInfinite(score);
    }
    
    /**
     * Perform comprehensive validation of a username and password combination.
     * Used for authentication requests.
     * 
     * @param username the username to validate
     * @param password the password to validate
     * @return validation result with detailed error information
     */
    public static ValidationResult validateCredentials(String username, String password) {
        if (!isValidUsername(username)) {
            return ValidationResult.failure(
                "Username must be 3-50 characters long and contain only letters, numbers, underscore, and dash");
        }
        
        ValidationResult passwordResult = validatePasswordDetailed(password);
        if (!passwordResult.isValid()) {
            return passwordResult;
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Get the configured maximum message size in bytes.
     * 
     * @return maximum message size in bytes
     */
    public static int getMaxMessageSizeBytes() {
        return MAX_MESSAGE_SIZE_BYTES;
    }
    
    /**
     * Get the configured maximum payload size in bytes.
     * 
     * @return maximum payload size in bytes
     */
    public static int getMaxPayloadSizeBytes() {
        return MAX_PAYLOAD_SIZE_BYTES;
    }
    
    /**
     * Get the configured maximum string field length.
     * 
     * @return maximum string field length in characters
     */
    public static int getMaxStringFieldLength() {
        return MAX_STRING_FIELD_LENGTH;
    }
}