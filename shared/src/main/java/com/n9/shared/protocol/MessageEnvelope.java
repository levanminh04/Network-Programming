package com.n9.shared.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

/**
 * Standard Message Envelope for All Communications
 * 
 * This class provides a consistent wrapper for all messages exchanged between
 * Frontend ↔ Gateway ↔ Core components. It ensures message traceability,
 * proper routing, and standardized error handling.
 * 
 * Design Principles:
 * - Immutable after creation (defensive programming)
 * - JSON-serializable for network transmission
 * - Correlation support for request/response tracking
 * - Extensible through payload field
 * - Version-aware for protocol evolution
 * 
 * Message Flow:
 * 1. Client creates envelope with request
 * 2. Server processes and responds with same correlationId
 * 3. Client matches response to original request
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class MessageEnvelope {
    
    /** Message type identifier - required for routing */
    @JsonProperty("type")
    private final String type;
    
    /** Unique correlation ID for request/response matching - required */
    @JsonProperty("correlationId")
    private final String correlationId;
    
    /** Message timestamp in epoch milliseconds - required */
    @JsonProperty("timestamp")
    private final long timestamp;
    
    /** User identifier - set after authentication */
    @JsonProperty("userId")
    private final String userId;
    
    /** Session identifier - WebSocket session ID */
    @JsonProperty("sessionId")
    private final String sessionId;
    
    /** Match identifier - set during game sessions */
    @JsonProperty("matchId")
    private final String matchId;
    
    /** Protocol version for compatibility checking */
    @JsonProperty("version")
    private final String version;
    
    /** Message-specific data payload */
    @JsonProperty("payload")
    private final JsonNode payload;
    
    /** Error information if message represents an error */
    @JsonProperty("error")
    private final ErrorInfo error;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    /**
     * Full constructor for complete message envelope
     */
    public MessageEnvelope(String type, 
                          String correlationId, 
                          long timestamp, 
                          String userId, 
                          String sessionId, 
                          String matchId, 
                          String version, 
                          JsonNode payload, 
                          ErrorInfo error) {
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
        this.correlationId = Objects.requireNonNull(correlationId, "Correlation ID cannot be null");
        this.timestamp = timestamp;
        this.userId = userId;
        this.sessionId = sessionId;
        this.matchId = matchId;
        this.version = version != null ? version : ProtocolVersion.CURRENT;
        this.payload = payload;
        this.error = error;
    }
    
    /**
     * Constructor for request messages (no error)
     */
    public MessageEnvelope(String type, 
                          String correlationId, 
                          long timestamp, 
                          String userId, 
                          String sessionId, 
                          JsonNode payload) {
        this(type, correlationId, timestamp, userId, sessionId, null, null, payload, null);
    }
    
    /**
     * Constructor for basic messages
     */
    public MessageEnvelope(String type, 
                          String correlationId, 
                          JsonNode payload) {
        this(type, correlationId, Instant.now().toEpochMilli(), null, null, null, null, payload, null);
    }
    
    /**
     * Constructor for error messages
     */
    public MessageEnvelope(String type, 
                          String correlationId, 
                          ErrorInfo error) {
        this(type, correlationId, Instant.now().toEpochMilli(), null, null, null, null, null, error);
    }
    
    // ============================================================================
    // BUILDER PATTERN for easy construction
    // ============================================================================
    
    public static class Builder {
        private String type;
        private String correlationId;
        private long timestamp = Instant.now().toEpochMilli();
        private String userId;
        private String sessionId;
        private String matchId;
        private String version = ProtocolVersion.CURRENT;
        private JsonNode payload;
        private ErrorInfo error;
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder matchId(String matchId) {
            this.matchId = matchId;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder payload(JsonNode payload) {
            this.payload = payload;
            return this;
        }
        
        public Builder error(ErrorInfo error) {
            this.error = error;
            return this;
        }
        
        public MessageEnvelope build() {
            return new MessageEnvelope(type, correlationId, timestamp, 
                                     userId, sessionId, matchId, version, 
                                     payload, error);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // ============================================================================
    // ACCESSORS
    // ============================================================================
    
    public String getType() { return type; }
    public String getCorrelationId() { return correlationId; }
    public long getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public String getMatchId() { return matchId; }
    public String getVersion() { return version; }
    public JsonNode getPayload() { return payload; }
    public ErrorInfo getError() { return error; }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if this is an error message
     */
    public boolean isError() {
        return error != null || MessageType.SYSTEM_ERROR.equals(type);
    }
    
    /**
     * Check if this is a request message
     */
    public boolean isRequest() {
        return MessageType.isRequest(type);
    }
    
    /**
     * Check if this is a response message
     */
    public boolean isResponse() {
        return MessageType.isResponse(type);
    }
    
    /**
     * Check if this is an event/notification message
     */
    public boolean isEvent() {
        return MessageType.isEvent(type);
    }
    
    /**
     * Get message domain (AUTH, GAME, LOBBY, SYSTEM)
     */
    public String getDomain() {
        return MessageType.getDomain(type);
    }
    
    /**
     * Check if message has required authentication context
     */
    public boolean isAuthenticated() {
        return userId != null && !userId.isEmpty();
    }
    
    /**
     * Check if message is part of a game session
     */
    public boolean isInGameContext() {
        return matchId != null && !matchId.isEmpty();
    }
    
    /**
     * Create a response envelope for this message
     */
    public Builder createResponse(String responseType) {
        return builder()
                .type(responseType)
                .correlationId(this.correlationId)
                .userId(this.userId)
                .sessionId(this.sessionId)
                .matchId(this.matchId)
                .version(this.version);
    }
    
    /**
     * Create an error response for this message
     */
    public MessageEnvelope createErrorResponse(String errorCode, String errorMessage) {
        return createResponse(MessageType.SYSTEM_ERROR)
                .error(new ErrorInfo(errorCode, errorMessage))
                .build();
    }
    
    // ============================================================================
    // EQUALS, HASHCODE, TOSTRING
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEnvelope that = (MessageEnvelope) o;
        return timestamp == that.timestamp &&
               Objects.equals(type, that.type) &&
               Objects.equals(correlationId, that.correlationId) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(matchId, that.matchId) &&
               Objects.equals(version, that.version) &&
               Objects.equals(payload, that.payload) &&
               Objects.equals(error, that.error);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, correlationId, timestamp, userId, sessionId, 
                           matchId, version, payload, error);
    }
    
    @Override
    public String toString() {
        return "MessageEnvelope{" +
               "type='" + type + '\'' +
               ", correlationId='" + correlationId + '\'' +
               ", timestamp=" + timestamp +
               ", userId='" + userId + '\'' +
               ", sessionId='" + sessionId + '\'' +
               ", matchId='" + matchId + '\'' +
               ", version='" + version + '\'' +
               ", hasPayload=" + (payload != null) +
               ", hasError=" + (error != null) +
               '}';
    }
    
    // ============================================================================
    // INNER CLASSES
    // ============================================================================
    
    /**
     * Error information structure
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        @JsonProperty("code")
        private final String code;
        
        @JsonProperty("message")
        private final String message;
        
        @JsonProperty("details")
        private final JsonNode details;
        
        public ErrorInfo(String code, String message) {
            this(code, message, null);
        }
        
        public ErrorInfo(String code, String message, JsonNode details) {
            this.code = Objects.requireNonNull(code, "Error code cannot be null");
            this.message = Objects.requireNonNull(message, "Error message cannot be null");
            this.details = details;
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public JsonNode getDetails() { return details; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ErrorInfo errorInfo = (ErrorInfo) o;
            return Objects.equals(code, errorInfo.code) &&
                   Objects.equals(message, errorInfo.message) &&
                   Objects.equals(details, errorInfo.details);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(code, message, details);
        }
        
        @Override
        public String toString() {
            return "ErrorInfo{" +
                   "code='" + code + '\'' +
                   ", message='" + message + '\'' +
                   ", hasDetails=" + (details != null) +
                   '}';
        }
    }
}