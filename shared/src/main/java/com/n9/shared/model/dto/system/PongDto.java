package com.n9.shared.model.dto.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Pong DTO
 * 
 * Server-to-client heartbeat/pong response.
 * Sent in response to PING message.
 * 
 * Message Type: SYSTEM.PONG
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PongDto {
    
    /**
     * Original client timestamp from PING
     */
    @JsonProperty("clientTimestamp")
    private Long clientTimestamp;
    
    /**
     * Server timestamp when pong was sent (epoch milliseconds)
     */
    @JsonProperty("serverTimestamp")
    private Long serverTimestamp;
    
    /**
     * Original sequence number from PING
     */
    @JsonProperty("sequence")
    private Long sequence;
    
    /**
     * Round-trip time in milliseconds (optional)
     */
    @JsonProperty("rtt")
    private Long rtt;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public PongDto() {
        this.serverTimestamp = System.currentTimeMillis();
    }
    
    public PongDto(Long clientTimestamp, Long sequence) {
        this();
        this.clientTimestamp = clientTimestamp;
        this.sequence = sequence;
        if (clientTimestamp != null) {
            this.rtt = this.serverTimestamp - clientTimestamp;
        }
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static PongDto from(PingDto ping) {
        return new PongDto(ping.getClientTimestamp(), ping.getSequence());
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
    
    public Long getServerTimestamp() {
        return serverTimestamp;
    }
    
    public void setServerTimestamp(Long serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
    
    public Long getSequence() {
        return sequence;
    }
    
    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
    
    public Long getRtt() {
        return rtt;
    }
    
    public void setRtt(Long rtt) {
        this.rtt = rtt;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PongDto pongDto = (PongDto) o;
        return Objects.equals(sequence, pongDto.sequence);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sequence);
    }
    
    @Override
    public String toString() {
        return "PongDto{" +
               "clientTimestamp=" + clientTimestamp +
               ", serverTimestamp=" + serverTimestamp +
               ", sequence=" + sequence +
               ", rtt=" + rtt +
               '}';
    }
}
