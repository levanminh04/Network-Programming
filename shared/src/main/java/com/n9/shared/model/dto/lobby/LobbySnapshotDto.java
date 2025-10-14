package com.n9.shared.model.dto.lobby;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Lobby Snapshot DTO
 * 
 * Complete snapshot of current lobby/main menu state.
 * Sent when player enters lobby or requests refresh.
 * 
 * Message Type: LOBBY.SNAPSHOT
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LobbySnapshotDto {
    
    /**
     * Current player info
     */
    @JsonProperty("player")
    private PlayerDto player;
    
    /**
     * Total online players count
     */
    @JsonProperty("onlinePlayers")
    private Integer onlinePlayers;
    
    /**
     * Players currently in matchmaking queue
     */
    @JsonProperty("inQueueCount")
    private Integer inQueueCount;
    
    /**
     * Active games count
     */
    @JsonProperty("activeGames")
    private Integer activeGames;
    
    /**
     * Server message/announcement (optional)
     */
    @JsonProperty("serverMessage")
    private String serverMessage;
    
    /**
     * Snapshot timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LobbySnapshotDto() {
        this.timestamp = System.currentTimeMillis();
        this.onlinePlayers = 0;
        this.inQueueCount = 0;
        this.activeGames = 0;
    }
    
    public LobbySnapshotDto(PlayerDto player) {
        this();
        this.player = player;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private PlayerDto player;
        private Integer onlinePlayers;
        private Integer inQueueCount;
        private Integer activeGames;
        private String serverMessage;
        private Long timestamp;
        
        public Builder player(PlayerDto player) {
            this.player = player;
            return this;
        }
        
        public Builder onlinePlayers(Integer onlinePlayers) {
            this.onlinePlayers = onlinePlayers;
            return this;
        }
        
        public Builder inQueueCount(Integer inQueueCount) {
            this.inQueueCount = inQueueCount;
            return this;
        }
        
        public Builder activeGames(Integer activeGames) {
            this.activeGames = activeGames;
            return this;
        }
        
        public Builder serverMessage(String serverMessage) {
            this.serverMessage = serverMessage;
            return this;
        }
        
        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public LobbySnapshotDto build() {
            LobbySnapshotDto dto = new LobbySnapshotDto();
            dto.player = this.player;
            dto.onlinePlayers = this.onlinePlayers != null ? this.onlinePlayers : 0;
            dto.inQueueCount = this.inQueueCount != null ? this.inQueueCount : 0;
            dto.activeGames = this.activeGames != null ? this.activeGames : 0;
            dto.serverMessage = this.serverMessage;
            dto.timestamp = this.timestamp != null ? this.timestamp : System.currentTimeMillis();
            return dto;
        }
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public PlayerDto getPlayer() {
        return player;
    }
    
    public void setPlayer(PlayerDto player) {
        this.player = player;
    }
    
    public Integer getOnlinePlayers() {
        return onlinePlayers;
    }
    
    public void setOnlinePlayers(Integer onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }
    
    public Integer getInQueueCount() {
        return inQueueCount;
    }
    
    public void setInQueueCount(Integer inQueueCount) {
        this.inQueueCount = inQueueCount;
    }
    
    public Integer getActiveGames() {
        return activeGames;
    }
    
    public void setActiveGames(Integer activeGames) {
        this.activeGames = activeGames;
    }
    
    public String getServerMessage() {
        return serverMessage;
    }
    
    public void setServerMessage(String serverMessage) {
        this.serverMessage = serverMessage;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LobbySnapshotDto that = (LobbySnapshotDto) o;
        return Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }
    
    @Override
    public String toString() {
        return "LobbySnapshotDto{" +
               "player=" + player +
               ", onlinePlayers=" + onlinePlayers +
               ", inQueueCount=" + inQueueCount +
               ", activeGames=" + activeGames +
               '}';
    }
}
