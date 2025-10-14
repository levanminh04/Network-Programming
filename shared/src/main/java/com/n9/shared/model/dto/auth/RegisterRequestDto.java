package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.*;
import java.util.Objects;

/**
 * Register Request DTO
 * 
 * Request payload for user registration.
 * Creates new player account with username, email, and password.
 * 
 * Message Type: AUTH.REGISTER_REQUEST
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequestDto {
    
    /**
     * Username (unique identifier for login)
     * Must be 3-50 characters, alphanumeric/underscore/dash only
     */
    @JsonProperty("username")
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscore and dash")
    private String username;
    
    /**
     * Email address (must be valid email format)
     */
    @JsonProperty("email")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    /**
     * Password (plain text in MVP - academic project)
     * DEFERRED: Hash password in production
     */
    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
    private String password;
    
    /**
     * Display name (optional, defaults to username)
     * Shown in UI and leaderboard
     */
    @JsonProperty("displayName")
    @Size(min = 1, max = 100, message = "Display name must be 1-100 characters")
    @Pattern(regexp = "^[^<>{}\\[\\]|\\\\]+$", message = "Display name contains invalid characters")
    private String displayName;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public RegisterRequestDto() {
        // Default constructor for JSON deserialization
    }
    
    public RegisterRequestDto(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = username; // Default to username
    }
    
    public RegisterRequestDto(String username, String email, String password, String displayName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName.trim() : null;
    }
    
    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================
    
    /**
     * Validate DTO fields
     * 
     * @return true if all fields are valid
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               password != null && !password.isEmpty() &&
               username.length() >= 3 && username.length() <= 50 &&
               password.length() >= 6 && password.length() <= 100 &&
               email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") &&
               username.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * Clear sensitive data (password)
     * Should be called after authentication
     */
    public void clearSensitiveData() {
        this.password = null;
    }
    
    /**
     * Get display name or fallback to username
     * 
     * @return Display name if set, otherwise username
     */
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() 
               ? displayName 
               : username;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRequestDto that = (RegisterRequestDto) o;
        return Objects.equals(username, that.username) &&
               Objects.equals(email, that.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(username, email);
    }
    
    @Override
    public String toString() {
        return "RegisterRequestDto{" +
               "username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", displayName='" + displayName + '\'' +
               ", password='[PROTECTED]'" +
               '}';
    }
}
