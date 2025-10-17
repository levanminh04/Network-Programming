package com.n9.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.n9.shared.protocol.MessageEnvelope;

import java.io.IOException;
import java.util.Optional;

/**
 * Centralized JSON processing utility for consistent serialization/deserialization
 * across all system components. Provides type-safe conversion methods with proper
 * error handling and validation.
 * 
 * Features:
 * - Configured ObjectMapper with optimal settings for network protocols
 * - Type-safe conversion methods with validation
 * - Error handling with detailed diagnostics
 * - Support for Java Time API and optional fields
 * - Message envelope serialization utilities
 * 
 * Usage:
 * <pre>
 * // Serialize message envelope
 * String json = JsonUtils.toJson(messageEnvelope);
 * 
 * // Deserialize with validation
 * Optional&lt;MessageEnvelope&gt; envelope = JsonUtils.fromJson(json, MessageEnvelope.class);
 * 
 * // Convert payload objects
 * LoginRequestDto request = JsonUtils.convertPayload(envelope.getPayload(), LoginRequestDto.class);
 * </pre>
 * 
 * @author Network Programming Team
 * @version 1.0.0
 * @since 2024-01-09
 */
public final class JsonUtils {
    
    private static final ObjectMapper OBJECT_MAPPER;
    
    static {
        OBJECT_MAPPER = new ObjectMapper();
        
        // Configure for network protocol usage
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Support Java Time API
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        
        // Disable default typing for security
        OBJECT_MAPPER.deactivateDefaultTyping();
    }
    
    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Get the configured ObjectMapper instance.
     * Use this when you need direct access to Jackson functionality.
     * 
     * @return configured ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
    
    /**
     * Serialize an object to JSON string with error handling.
     * 
     * @param object the object to serialize
     * @return JSON string representation
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(Object object) throws JsonProcessingException {
        if (object == null) {
            return "null";
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonProcessingException("Failed to serialize object of type: " + 
                                           object.getClass().getSimpleName(), e) {};
        }
    }
    
    /**
     * Serialize an object to JSON string with safe error handling.
     * Returns empty Optional if serialization fails.
     * 
     * @param object the object to serialize
     * @return Optional containing JSON string, or empty if serialization fails
     */
    public static Optional<String> toJsonSafe(Object object) {
        try {
            return Optional.of(toJson(object));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Deserialize JSON string to specified type with validation.
     * 
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param targetClass the target class type
     * @return deserialized object
     * @throws IOException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> targetClass) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }
        
        try {
            return OBJECT_MAPPER.readValue(json, targetClass);
        } catch (IOException e) {
            throw new IOException("Failed to deserialize JSON to " + 
                                targetClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Deserialize JSON string to specified type with safe error handling.
     * Returns empty Optional if deserialization fails.
     * 
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param targetClass the target class type
     * @return Optional containing deserialized object, or empty if deserialization fails
     */
    public static <T> Optional<T> fromJsonSafe(String json, Class<T> targetClass) {
        try {
            return Optional.of(fromJson(json, targetClass));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Convert a payload object (typically from MessageEnvelope) to specified DTO type.
     * Handles both Map-based payloads and already-typed objects.
     * 
     * @param <T> the target DTO type
     * @param payload the payload object (Map, JsonNode, or typed object)
     * @param targetClass the target DTO class
     * @return converted DTO object
     * @throws IllegalArgumentException if conversion fails
     */
    public static <T> T convertPayload(Object payload, Class<T> targetClass) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
        
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }
        
        // If payload is already the target type, return it
        if (targetClass.isInstance(payload)) {
            return targetClass.cast(payload);
        }
        
        try {
            return OBJECT_MAPPER.convertValue(payload, targetClass);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert payload to " + 
                                             targetClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert a payload object with safe error handling.
     * Returns empty Optional if conversion fails.
     * 
     * @param <T> the target DTO type
     * @param payload the payload object
     * @param targetClass the target DTO class
     * @return Optional containing converted object, or empty if conversion fails
     */
    public static <T> Optional<T> convertPayloadSafe(Object payload, Class<T> targetClass) {
        try {
            return Optional.of(convertPayload(payload, targetClass));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Parse JSON string to JsonNode for flexible processing.
     * Useful when you need to inspect JSON structure before deserialization.
     * 
     * @param json the JSON string to parse
     * @return JsonNode representing the parsed JSON
     * @throws IOException if parsing fails
     */
    public static JsonNode parseToJsonNode(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IOException("Failed to parse JSON string: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse JSON string to JsonNode with safe error handling.
     * 
     * @param json the JSON string to parse
     * @return Optional containing JsonNode, or empty if parsing fails
     */
    public static Optional<JsonNode> parseToJsonNodeSafe(String json) {
        try {
            return Optional.of(parseToJsonNode(json));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Validate if a string is valid JSON format.
     * 
     * @param json the string to validate
     * @return true if the string is valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Create a deep copy of an object using JSON serialization.
     * Useful for creating independent copies of complex objects.
     * 
     * @param <T> the object type
     * @param object the object to copy
     * @param objectClass the class of the object
     * @return deep copy of the object
     * @throws IllegalArgumentException if copying fails
     */
    public static <T> T deepCopy(T object, Class<T> objectClass) {
        if (object == null) {
            return null;
        }
        
        try {
            String json = toJson(object);
            return fromJson(json, objectClass);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to create deep copy of " + 
                                             objectClass.getSimpleName(), e);
        }
    }
    
    /**
     * Serialize MessageEnvelope with additional validation.
     * Ensures all required fields are present before serialization.
     * 
     * @param envelope the message envelope to serialize
     * @return JSON string representation
     * @throws JsonProcessingException if serialization fails
     * @throws IllegalArgumentException if envelope validation fails
     */
    public static String serializeMessageEnvelope(MessageEnvelope envelope) throws JsonProcessingException {
        if (envelope == null) {
            throw new IllegalArgumentException("Message envelope cannot be null");
        }
        
        // Validate required fields
        if (envelope.getType() == null || envelope.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Message type is required");
        }
        
        if (envelope.getCorrelationId() == null || envelope.getCorrelationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID is required");
        }
        
//        if (envelope.getTimestamp() <= 0) {
//            throw new IllegalArgumentException("Timestamp must be positive");
//        }
        
        return toJson(envelope);
    }
    
    /**
     * Deserialize MessageEnvelope with additional validation.
     * Validates envelope structure and required fields after deserialization.
     * 
     * @param json the JSON string to deserialize
     * @return validated MessageEnvelope instance
     * @throws IOException if deserialization or validation fails
     */
    public static MessageEnvelope deserializeMessageEnvelope(String json) throws IOException {
        MessageEnvelope envelope = fromJson(json, MessageEnvelope.class);
        
        // Validate required fields
        if (envelope.getType() == null || envelope.getType().trim().isEmpty()) {
            throw new IOException("Deserialized envelope missing required field: type");
        }
        
        if (envelope.getCorrelationId() == null || envelope.getCorrelationId().trim().isEmpty()) {
            throw new IOException("Deserialized envelope missing required field: correlationId");
        }
        
//        if (envelope.getTimestamp() <= 0) {
//            throw new IOException("Deserialized envelope has invalid timestamp: " + envelope.getTimestamp());
//        }
        
        return envelope;
    }
    
    /**
     * Calculate the approximate size of a JSON string in bytes.
     * Useful for message size validation and network optimization.
     * 
     * @param json the JSON string to measure
     * @return approximate size in bytes (UTF-8 encoding)
     */
    public static int getJsonSizeBytes(String json) {
        if (json == null) {
            return 0;
        }
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
    
    /**
     * Check if an object's JSON representation exceeds the specified size limit.
     * 
     * @param object the object to check
     * @param maxSizeBytes the maximum allowed size in bytes
     * @return true if object size exceeds limit, false otherwise
     */
    public static boolean exceedsSizeLimit(Object object, int maxSizeBytes) {
        try {
            String json = toJson(object);
            return getJsonSizeBytes(json) > maxSizeBytes;
        } catch (JsonProcessingException e) {
            return true; // If we can't serialize, consider it oversized
        }
    }
}