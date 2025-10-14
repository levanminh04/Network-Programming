package com.n9.shared.model.dto.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Ping DTO
 * 
 * Client-to-server heartbeat/ping message.
 * Sent every 30 seconds to maintain connection.
 * 
 * Message Type: SYSTEM.PING
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PingDto {
    
    /**
     * Client timestamp when ping was sent (epoch milliseconds)
     */
    @JsonProperty("clientTimestamp")
    private Long clientTimestamp;
    
    /**
     * Sequence number (incremental)
     */
    @JsonProperty("sequence")
    private Long sequence;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public PingDto() {
        this.clientTimestamp = System.currentTimeMillis();
    }
    
    public PingDto(Long sequence) {
        this();
        this.sequence = sequence;
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public Long getClientTimestamp() {
        return clientTimestamp;
    }
    
    public void setClientTimestamp(Long clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }
    
    public Long getSequence() {
        return sequence;
    }
    
    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PingDto pingDto = (PingDto) o;
        return Objects.equals(sequence, pingDto.sequence);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sequence);
    }
    
    @Override
    public String toString() {
        return "PingDto{" +
               "clientTimestamp=" + clientTimestamp +
               ", sequence=" + sequence +
               '}';
    }
}
