package com.n9.shared.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n9.shared.model.enums.ErrorCode;
import com.n9.shared.util.IdUtils;
import com.n9.shared.util.JsonUtils;

import java.util.Objects;

/**
 * Message Factory
 * 
 * Utility class for creating MessageEnvelope instances with consistent
 * structure and proper defaults.
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class MessageFactory {
    
    private static final ObjectMapper mapper = JsonUtils.getObjectMapper();
    
    // Prevent instantiation
    private MessageFactory() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    // ============================================================================
    // REQUEST MESSAGES
    // ============================================================================
    
    /**
     * Create a request message
     * 
     * @param type Message type
     * @param payload Request payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createRequest(String type, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create a request message with user context
     * 
     * @param type Message type
     * @param userId User ID
     * @param sessionId Session ID
     * @param payload Request payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createRequest(String type, String userId, String sessionId, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(userId)
            .sessionId(sessionId)
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create a request message with full context
     * 
     * @param type Message type
     * @param userId User ID
     * @param sessionId Session ID
     * @param matchId Match ID
     * @param payload Request payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createRequest(
            String type, 
            String userId, 
            String sessionId, 
            String matchId, 
            Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(userId)
            .sessionId(sessionId)
            .matchId(matchId)
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    // ============================================================================
    // RESPONSE MESSAGES
    // ============================================================================
    
    /**
     * Create a response message
     * 
     * @param type Message type
     * @param correlationId Correlation ID from request
     * @param payload Response payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createResponse(String type, String correlationId, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(correlationId)
            .timestamp(System.currentTimeMillis())
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create a response message with user context
     * 
     * @param type Message type
     * @param correlationId Correlation ID from request
     * @param userId User ID
     * @param payload Response payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createResponse(
            String type, 
            String correlationId, 
            String userId, 
            Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(correlationId)
            .timestamp(System.currentTimeMillis())
            .userId(userId)
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create a response message with full context
     * 
     * @param type Message type
     * @param correlationId Correlation ID from request
     * @param userId User ID
     * @param sessionId Session ID
     * @param matchId Match ID
     * @param payload Response payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createResponse(
            String type,
            String correlationId,
            String userId,
            String sessionId,
            String matchId,
            Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(correlationId)
            .timestamp(System.currentTimeMillis())
            .userId(userId)
            .sessionId(sessionId)
            .matchId(matchId)
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    // ============================================================================
    // ERROR MESSAGES
    // ============================================================================
    
    /**
     * Create an error message
     * 
     * @param correlationId Correlation ID from request
     * @param errorInfo Error information
     * @return MessageEnvelope
     */
    public static MessageEnvelope createError(String correlationId, ErrorInfo errorInfo) {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_ERROR)
            .correlationId(correlationId)
            .timestamp(System.currentTimeMillis())
            .error(errorInfo)
            .build();
    }
    
    /**
     * Create an error message with ErrorCode
     * 
     * @param correlationId Correlation ID from request
     * @param errorCode Error code
     * @param message Error message
     * @return MessageEnvelope
     */
    public static MessageEnvelope createError(String correlationId, ErrorCode errorCode, String message) {
        ErrorInfo errorInfo = new ErrorInfo(errorCode, message);
        return createError(correlationId, errorInfo);
    }
    
    /**
     * Create a validation error message
     * 
     * @param correlationId Correlation ID from request
     * @param fieldName Field name
     * @param errorMessage Error message
     * @return MessageEnvelope
     */
    public static MessageEnvelope createValidationError(
            String correlationId, 
            String fieldName, 
            String errorMessage) {
        ErrorInfo errorInfo = ErrorInfo.validationError(fieldName, errorMessage);
        return createError(correlationId, errorInfo);
    }
    
    /**
     * Create an authentication error message
     * 
     * @param correlationId Correlation ID from request
     * @param message Error message
     * @return MessageEnvelope
     */
    public static MessageEnvelope createAuthError(String correlationId, String message) {
        ErrorInfo errorInfo = ErrorInfo.authenticationError(message);
        return createError(correlationId, errorInfo);
    }
    
    /**
     * Create an internal server error message
     * 
     * @param correlationId Correlation ID from request
     * @return MessageEnvelope
     */
    public static MessageEnvelope createInternalError(String correlationId) {
        String traceId = IdUtils.generateCorrelationId(); // Use existing method
        ErrorInfo errorInfo = ErrorInfo.internalError(traceId);
        return createError(correlationId, errorInfo);
    }
    
    // ============================================================================
    // NOTIFICATION MESSAGES (Server-initiated)
    // ============================================================================
    
    /**
     * Create a notification message (server-initiated, no correlation)
     * 
     * @param type Message type
     * @param payload Notification payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createNotification(String type, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create a notification message with user context
     * 
     * @param type Message type
     * @param userId User ID
     * @param payload Notification payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createNotification(String type, String userId, Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(userId)
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create a notification message with full context
     * 
     * @param type Message type
     * @param userId User ID
     * @param sessionId Session ID
     * @param matchId Match ID
     * @param payload Notification payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope createNotification(
            String type,
            String userId,
            String sessionId,
            String matchId,
            Object payload) {
        return MessageEnvelope.builder()
            .type(type)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(userId)
            .sessionId(sessionId)
            .matchId(matchId)
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    // ============================================================================
    // SYSTEM MESSAGES
    // ============================================================================
    
    /**
     * Create a PING message
     * 
     * @return MessageEnvelope
     */
    public static MessageEnvelope createPing() {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_HEARTBEAT)
            .correlationId(IdUtils.generateCorrelationId())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Create a PONG message (response to PING)
     * 
     * @param pingMessage Original ping message
     * @return MessageEnvelope
     */
    public static MessageEnvelope createPong(MessageEnvelope pingMessage) {
        Objects.requireNonNull(pingMessage, "Ping message cannot be null");
        
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_PONG)
            .correlationId(pingMessage.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    // ============================================================================
    // REPLY METHODS (Create response from request)
    // ============================================================================
    
    /**
     * Create a reply to a request message
     * 
     * @param request Original request message
     * @param responseType Response message type
     * @param payload Response payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope reply(MessageEnvelope request, String responseType, Object payload) {
        Objects.requireNonNull(request, "Request message cannot be null");
        
        return MessageEnvelope.builder()
            .type(responseType)
            .correlationId(request.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(request.getUserId())
            .sessionId(request.getSessionId())
            .matchId(request.getMatchId())
            .payload(convertToJsonNode(payload))
            .build();
    }
    
    /**
     * Create an error reply to a request message
     * 
     * @param request Original request message
     * @param errorInfo Error information
     * @return MessageEnvelope
     */
    public static MessageEnvelope replyError(MessageEnvelope request, ErrorInfo errorInfo) {
        Objects.requireNonNull(request, "Request message cannot be null");
        
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_ERROR)
            .correlationId(request.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .userId(request.getUserId())
            .sessionId(request.getSessionId())
            .matchId(request.getMatchId())
            .error(errorInfo)
            .build();
    }
    
    /**
     * Create an error reply with ErrorCode
     * 
     * @param request Original request message
     * @param errorCode Error code
     * @param message Error message
     * @return MessageEnvelope
     */
    public static MessageEnvelope replyError(MessageEnvelope request, ErrorCode errorCode, String message) {
        ErrorInfo errorInfo = new ErrorInfo(errorCode, message);
        return replyError(request, errorInfo);
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Convert payload object to JsonNode
     * 
     * @param payload Payload object
     * @return JsonNode
     */
    private static JsonNode convertToJsonNode(Object payload) {
        if (payload == null) {
            return null;
        }
        
        if (payload instanceof JsonNode) {
            return (JsonNode) payload;
        }
        
        return mapper.valueToTree(payload);
    }
    
    /**
     * Create a copy of message with new type
     * 
     * @param original Original message
     * @param newType New message type
     * @return MessageEnvelope
     */
    public static MessageEnvelope copyWithType(MessageEnvelope original, String newType) {
        Objects.requireNonNull(original, "Original message cannot be null");
        
        return MessageEnvelope.builder()
            .type(newType)
            .correlationId(original.getCorrelationId())
            .timestamp(original.getTimestamp())
            .userId(original.getUserId())
            .sessionId(original.getSessionId())
            .matchId(original.getMatchId())
            .version(original.getVersion())
            .payload(original.getPayload())
            .error(original.getError())
            .build();
    }
    
    /**
     * Create a copy of message with new payload
     * 
     * @param original Original message
     * @param newPayload New payload
     * @return MessageEnvelope
     */
    public static MessageEnvelope copyWithPayload(MessageEnvelope original, Object newPayload) {
        Objects.requireNonNull(original, "Original message cannot be null");
        
        return MessageEnvelope.builder()
            .type(original.getType())
            .correlationId(original.getCorrelationId())
            .timestamp(original.getTimestamp())
            .userId(original.getUserId())
            .sessionId(original.getSessionId())
            .matchId(original.getMatchId())
            .version(original.getVersion())
            .payload(convertToJsonNode(newPayload))
            .error(original.getError())
            .build();
    }
}
