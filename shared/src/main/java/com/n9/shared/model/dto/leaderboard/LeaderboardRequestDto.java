package com.n9.shared.model.dto.leaderboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.Objects;

/**
 * Leaderboard Request DTO
 * 
 * Request to fetch leaderboard data.
 * Supports pagination for large leaderboards.
 * 
 * Message Type: LEADERBOARD.REQUEST
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardRequestDto {
    
    /**
     * Number of entries to fetch (default: 100, max: 500)
     */
    @JsonProperty("limit")
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 500, message = "Limit cannot exceed 500")
    private Integer limit;
    
    /**
     * Offset for pagination (default: 0)
     */
    @JsonProperty("offset")
    @Min(value = 0, message = "Offset must be non-negative")
    private Integer offset;
    
    /**
     * Sort field (MVP: total_wins only)
     */
    @JsonProperty("sortBy")
    private String sortBy;
    
    /**
     * Sort direction (ASC or DESC)
     */
    @JsonProperty("sortOrder")
    private String sortOrder;
    
    /**
     * Request timestamp (epoch milliseconds)
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public LeaderboardRequestDto() {
        this.limit = 100;
        this.offset = 0;
        this.sortBy = "total_wins";
        this.sortOrder = "DESC";
        this.timestamp = System.currentTimeMillis();
    }
    
    public LeaderboardRequestDto(Integer limit, Integer offset) {
        this();
        this.limit = limit != null ? limit : 100;
        this.offset = offset != null ? offset : 0;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    public static LeaderboardRequestDto top100() {
        return new LeaderboardRequestDto(100, 0);
    }
    
    public static LeaderboardRequestDto withLimit(int limit) {
        return new LeaderboardRequestDto(limit, 0);
    }
    
    public static LeaderboardRequestDto page(int limit, int offset) {
        return new LeaderboardRequestDto(limit, offset);
    }
    
    // ============================================================================
    // GETTERS & SETTERS
    // ============================================================================
    
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
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
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
        LeaderboardRequestDto that = (LeaderboardRequestDto) o;
        return Objects.equals(limit, that.limit) &&
               Objects.equals(offset, that.offset) &&
               Objects.equals(sortBy, that.sortBy);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(limit, offset, sortBy);
    }
    
    @Override
    public String toString() {
        return "LeaderboardRequestDto{" +
               "limit=" + limit +
               ", offset=" + offset +
               ", sortBy='" + sortBy + '\'' +
               ", sortOrder='" + sortOrder + '\'' +
               '}';
    }
}
