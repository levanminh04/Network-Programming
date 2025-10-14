package com.n9.shared.model.dto.leaderboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Leaderboard Response DTO
 * 
 * Leaderboard data with rankings.
 * MVP: Sorted by total wins only.
 * 
 * Message Type: LEADERBOARD.RESPONSE
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardResponseDto {
    
    /**
     * Leaderboard entries
     */
    @JsonProperty("entries")
    private List<LeaderboardEntryDto> entries;
    
    /**
     * Total entries in leaderboard
     */
    @JsonProperty("totalEntries")
    private Integer totalEntries;
    
    /**
     * Current page limit
     */
    @JsonProperty("limit")
    private Integer limit;
    
    /**
     * Current page offset
     */
    @JsonProperty("offset")
    private Integer offset;
    
    /**
     * Current player's entry (if in leaderboard)
     */
    @JsonProperty("currentPlayerEntry")
    private LeaderboardEntryDto currentPlayerEntry;
    
    /**
     * Response timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LeaderboardResponseDto() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public LeaderboardResponseDto(List<LeaderboardEntryDto> entries, Integer totalEntries) {
        this();
        this.entries = entries;
        this.totalEntries = totalEntries;
    }
    
    // ============================================================================
    // BUILDER PATTERN
    // ============================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<LeaderboardEntryDto> entries;
        private Integer totalEntries;
        private Integer limit;
        private Integer offset;
        private LeaderboardEntryDto currentPlayerEntry;
        private Long timestamp;
        
        public Builder entries(List<LeaderboardEntryDto> entries) {
            this.entries = entries;
            return this;
        }
        
        public Builder totalEntries(Integer totalEntries) {
            this.totalEntries = totalEntries;
            return this;
        }
        
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }
        
        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }
        
        public Builder currentPlayerEntry(LeaderboardEntryDto currentPlayerEntry) {
            this.currentPlayerEntry = currentPlayerEntry;
            return this;
        }
        
        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public LeaderboardResponseDto build() {
            LeaderboardResponseDto dto = new LeaderboardResponseDto();
            dto.entries = this.entries;
            dto.totalEntries = this.totalEntries;
            dto.limit = this.limit;
            dto.offset = this.offset;
            dto.currentPlayerEntry = this.currentPlayerEntry;
            dto.timestamp = this.timestamp != null ? this.timestamp : System.currentTimeMillis();
            return dto;
        }
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
    public List<LeaderboardEntryDto> getEntries() {
        return entries;
    }
    
    public void setEntries(List<LeaderboardEntryDto> entries) {
        this.entries = entries;
    }
    
    public Integer getTotalEntries() {
        return totalEntries;
    }
    
    public void setTotalEntries(Integer totalEntries) {
        this.totalEntries = totalEntries;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Integer getOffset() {
        return offset;
    }
    
    public void setOffset(Integer offset) {
        this.offset = offset;
    }
    
    public LeaderboardEntryDto getCurrentPlayerEntry() {
        return currentPlayerEntry;
    }
    
    public void setCurrentPlayerEntry(LeaderboardEntryDto currentPlayerEntry) {
        this.currentPlayerEntry = currentPlayerEntry;
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
        LeaderboardResponseDto that = (LeaderboardResponseDto) o;
        return Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }
    
    @Override
    public String toString() {
        return "LeaderboardResponseDto{" +
               "entriesCount=" + (entries != null ? entries.size() : 0) +
               ", totalEntries=" + totalEntries +
               ", limit=" + limit +
               ", offset=" + offset +
               '}';
    }
}
