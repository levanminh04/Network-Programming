package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Card Rank Enumeration (MVP: 36-card deck)
 * 
 * Represents the 9 ranks in the MVP 36-card deck (A, 2-9).
 * Face cards (J, Q, K) are DEFERRED to future versions.
 * 
 * Database Reference: cards table
 * - rank: VARCHAR(2) ('A', '2'-'9')
 * - value: INT (1-9)
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public enum CardRank {
    /**
     * Ace (value: 1)
     * Lowest card in MVP
     */
    ACE("A", "Ace", "Át", 1),
    
    /**
     * Two (value: 2)
     */
    TWO("2", "Two", "Hai", 2),
    
    /**
     * Three (value: 3)
     */
    THREE("3", "Three", "Ba", 3),
    
    /**
     * Four (value: 4)
     */
    FOUR("4", "Four", "Bốn", 4),
    
    /**
     * Five (value: 5)
     */
    FIVE("5", "Five", "Năm", 5),
    
    /**
     * Six (value: 6)
     */
    SIX("6", "Six", "Sáu", 6),
    
    /**
     * Seven (value: 7)
     */
    SEVEN("7", "Seven", "Bảy", 7),
    
    /**
     * Eight (value: 8)
     */
    EIGHT("8", "Eight", "Tám", 8),
    
    /**
     * Nine (value: 9)
     * Highest card in MVP
     */
    NINE("9", "Nine", "Chín", 9);
    
    // DEFERRED: Face cards for full 52-card deck
    // JACK("J", "Jack", "Bồi", 10),
    // QUEEN("Q", "Queen", "Đầm", 11),
    // KING("K", "King", "Già", 12);
    
    /**
     * Rank code (A, 2-9)
     * Stored in database
     */
    private final String code;
    
    /**
     * English name
     */
    private final String englishName;
    
    /**
     * Vietnamese name
     */
    private final String vietnameseName;
    
    /**
     * Numeric value for comparison and scoring (1-9)
     */
    private final int value;
    
    /**
     * Constructor
     * 
     * @param code Rank code (A, 2-9)
     * @param englishName English name
     * @param vietnameseName Vietnamese name
     * @param value Numeric value (1-9)
     */
    CardRank(String code, String englishName, String vietnameseName, int value) {
        this.code = code;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.value = value;
    }
    
    /**
     * Get rank code (for JSON serialization)
     * 
     * @return Rank code (A, 2-9)
     */
    @JsonValue
    public String getCode() {
        return code;
    }
    
    /**
     * Get English name
     * 
     * @return English name (e.g., "Ace", "Nine")
     */
    public String getEnglishName() {
        return englishName;
    }
    
    /**
     * Get Vietnamese name
     * 
     * @return Vietnamese name (e.g., "Át", "Chín")
     */
    public String getVietnameseName() {
        return vietnameseName;
    }
    
    /**
     * Get numeric value
     * 
     * @return Numeric value (1-9)
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Get display name in specified language
     * 
     * @param language Language code ("en" or "vi")
     * @return Display name in specified language
     */
    public String getDisplayName(String language) {
        if ("vi".equalsIgnoreCase(language)) {
            return vietnameseName;
        }
        return englishName;
    }
    
    /**
     * Parse rank from code string
     * 
     * @param code Rank code (A, 2-9)
     * @return CardRank enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static CardRank fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Rank code cannot be null or empty");
        }
        
        for (CardRank rank : values()) {
            if (rank.code.equalsIgnoreCase(code)) {
                return rank;
            }
        }
        
        throw new IllegalArgumentException("Invalid rank code: " + code + 
                " (must be A, 2, 3, 4, 5, 6, 7, 8, or 9)");
    }
    
    /**
     * Parse rank from value
     * 
     * @param value Numeric value (1-9)
     * @return CardRank enum
     * @throws IllegalArgumentException if value is invalid
     */
    public static CardRank fromValue(int value) {
        for (CardRank rank : values()) {
            if (rank.value == value) {
                return rank;
            }
        }
        
        throw new IllegalArgumentException("Invalid rank value: " + value + 
                " (must be 1-9)");
    }
    
    /**
     * Check if rank code is valid
     * 
     * @param code Rank code to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        for (CardRank rank : values()) {
            if (rank.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if value is valid
     * 
     * @param value Value to check (1-9)
     * @return true if valid, false otherwise
     */
    public static boolean isValidValue(int value) {
        return value >= 1 && value <= 9;
    }
    
    /**
     * Compare rank values (for game logic)
     * Note: Use enum's natural compareTo() for ordering, this is for value comparison
     * 
     * @param other Rank to compare with
     * @return Negative if this < other, 0 if equal, positive if this > other
     */
    public int compareValue(CardRank other) {
        return Integer.compare(this.value, other.value);
    }
    
    /**
     * Check if this rank is higher than another
     * 
     * @param other Rank to compare with
     * @return true if this rank is higher
     */
    public boolean isHigherThan(CardRank other) {
        return this.value > other.value;
    }
    
    /**
     * Check if this rank is lower than another
     * 
     * @param other Rank to compare with
     * @return true if this rank is lower
     */
    public boolean isLowerThan(CardRank other) {
        return this.value < other.value;
    }
    
    /**
     * Get total number of ranks in deck
     * 
     * @return Number of ranks (9 for MVP)
     */
    public static int getRankCount() {
        return values().length;
    }
    
    @Override
    public String toString() {
        return code + " (" + englishName + "/" + vietnameseName + ", value=" + value + ")";
    }
}
