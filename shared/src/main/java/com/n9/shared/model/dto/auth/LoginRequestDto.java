package com.n9.shared.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import java.util.Objects;

/**
 * Login Request Data Transfer Object
 * 
 * Contains user credentials for authentication.
 * Includes validation annotations for input sanitization.
 * 
 * @author N9 Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRequestDto {
    
    /** Username for authentication */
    @JsonProperty("username")
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscore and dash")
    private String username;
    
    /** Password for authentication */
    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
    
    /** Client version information */
    @JsonProperty("clientVersion")
    private String clientVersion;
    
    /** Remember login session */
    @JsonProperty("rememberMe")
    private Boolean rememberMe;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LoginRequestDto() {
        // Default constructor for JSON deserialization
    }
    
    public LoginRequestDto(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public LoginRequestDto(String username, String password, String clientVersion, Boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.clientVersion = clientVersion;
        this.rememberMe = rememberMe;
    }
    
    // ============================================================================
    // ACCESSORS
    // ============================================================================
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
    
    public Boolean getRememberMe() { return rememberMe; }
    public void setRememberMe(Boolean rememberMe) { this.rememberMe = rememberMe; }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Clear sensitive data (password) from memory
     */
    public void clearSensitiveData() {
        this.password = null;
    }
    
    /**
     * Validate required fields
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.trim().isEmpty() &&
               username.length() >= 3 && username.length() <= 50 &&
               password.length() >= 6 && password.length() <= 100;
    }
    
    // ============================================================================
    // EQUALS, HASHCODE, TOSTRING
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequestDto that = (LoginRequestDto) o;
        return Objects.equals(username, that.username) &&
               Objects.equals(password, that.password) &&
               Objects.equals(clientVersion, that.clientVersion) &&
               Objects.equals(rememberMe, that.rememberMe);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(username, password, clientVersion, rememberMe);
    }
    
    @Override
    public String toString() {
        return "LoginRequestDto{" +
               "username='" + username + '\'' +
               ", password='[HIDDEN]'" +
               ", clientVersion='" + clientVersion + '\'' +
               ", rememberMe=" + rememberMe +
               '}';
    }
}