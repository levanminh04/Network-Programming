package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.n9.shared.model.enums.CardSuit;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.Objects;

/**
 * Card Data Transfer Object
 * 
 * Represents a playing card with rank, suit, and value.
 * Used in game logic and UI display.
 * 
 * @author N9 Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardDto {
    
    /** Card rank (2, 3, 4, 5, 6, 7, 8, 9, J, Q, K, A) */
    @JsonProperty("rank")
    @NotNull(message = "Card rank is required")
    private String rank;
    
    /** Card suit (Hearts, Diamonds, Clubs, Spades) */
    @JsonProperty("suit")
    @NotNull(message = "Card suit is required")
    private CardSuit suit;
    
    /** Numeric value for game calculations */
    @JsonProperty("value")
    @Min(value = 1, message = "Card value must be at least 1")
    @Max(value = 14, message = "Card value cannot exceed 14")
    private Integer value;
    
    /** Card index in deck (0-51) */
    @JsonProperty("index")
    @Min(value = 0, message = "Card index must be non-negative")
    @Max(value = 51, message = "Card index cannot exceed 51")
    private Integer index;
    
    /** Whether card is face up or down */
    @JsonProperty("faceUp")
    private Boolean faceUp;
    
    /** Card display name (for UI) */
    @JsonProperty("displayName")
    private String displayName;
    
    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================
    
    public CardDto() {
        // Default constructor for JSON deserialization
        this.faceUp = true; // Default to face up
    }
    
    public CardDto(String rank, CardSuit suit, Integer value) {
        this.rank = rank;
        this.suit = suit;
        this.value = value;
        this.faceUp = true;
        this.displayName = generateDisplayName();
    }
    
    public CardDto(String rank, CardSuit suit, Integer value, Integer index) {
        this(rank, suit, value);
        this.index = index;
    }
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    /**
     * Create a standard playing card
     */
    public static CardDto create(String rank, CardSuit suit) {
        int value = calculateValue(rank);
        CardDto card = new CardDto(rank, suit, value);
        card.displayName = generateDisplayName(rank, suit);
        return card;
    }
    
    /**
     * Create card with specific index
     */
    public static CardDto create(String rank, CardSuit suit, int index) {
        CardDto card = create(rank, suit);
        card.index = index;
        return card;
    }
    
    /**
     * Create a face-down card (hidden)
     */
    public static CardDto createFaceDown() {
        CardDto card = new CardDto();
        card.faceUp = false;
        card.displayName = "Hidden Card";
        return card;
    }
    
    // ============================================================================
    // ACCESSORS
    // ============================================================================
    
    public String getRank() { return rank; }
    public void setRank(String rank) { 
        this.rank = rank;
        this.value = calculateValue(rank);
        this.displayName = generateDisplayName();
    }
    
    public CardSuit getSuit() { return suit; }
    public void setSuit(CardSuit suit) { 
        this.suit = suit;
        this.displayName = generateDisplayName();
    }
    
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    
    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }
    
    public Boolean getFaceUp() { return faceUp; }
    public void setFaceUp(Boolean faceUp) { this.faceUp = faceUp; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Calculate numeric value from rank
     */
    private static int calculateValue(String rank) {
        if (rank == null) return 0;
        
        switch (rank.toUpperCase()) {
            case "A": return 1;  // Ace low in this game
            case "2": return 2;
            case "3": return 3;
            case "4": return 4;
            case "5": return 5;
            case "6": return 6;
            case "7": return 7;
            case "8": return 8;
            case "9": return 9;
            case "J": return 11; // Jack
            case "Q": return 12; // Queen
            case "K": return 13; // King
            default: 
                // Try to parse as number for ranks like "10"
                try {
                    return Integer.parseInt(rank);
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }
    
    /**
     * Generate display name from rank and suit
     */
    private String generateDisplayName() {
        return generateDisplayName(this.rank, this.suit);
    }
    
    private static String generateDisplayName(String rank, CardSuit suit) {
        if (rank == null || suit == null) return "Unknown Card";
        return rank + suit.getSymbol();
    }
    
    /**
     * Check if card is a face card (J, Q, K)
     */
    public boolean isFaceCard() {
        return "J".equals(rank) || "Q".equals(rank) || "K".equals(rank);
    }
    
    /**
     * Check if card is an Ace
     */
    public boolean isAce() {
        return "A".equals(rank);
    }
    
    /**
     * Check if card is red (Hearts or Diamonds)
     */
    public boolean isRed() {
        return suit != null && suit.isRed();
    }
    
    /**
     * Check if card is black (Clubs or Spades)
     */
    public boolean isBlack() {
        return suit != null && suit.isBlack();
    }
    
    /**
     * Get card color as string
     */
    public String getColor() {
        return suit != null ? suit.getColor() : "unknown";
    }
    
    /**
     * Compare card values
     */
    public int compareValue(CardDto other) {
        if (other == null || other.value == null) return 1;
        if (this.value == null) return -1;
        return Integer.compare(this.value, other.value);
    }
    
    /**
     * Check if this card beats another card (higher value)
     */
    public boolean beats(CardDto other) {
        return compareValue(other) > 0;
    }
    
    // ============================================================================
    // EQUALS, HASHCODE, TOSTRING
    // ============================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardDto cardDto = (CardDto) o;
        return Objects.equals(rank, cardDto.rank) &&
               suit == cardDto.suit &&
               Objects.equals(value, cardDto.value) &&
               Objects.equals(index, cardDto.index);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(rank, suit, value, index);
    }
    
    @Override
    public String toString() {
        if (Boolean.FALSE.equals(faceUp)) {
            return "CardDto{faceDown}";
        }
        return "CardDto{" +
               "rank='" + rank + '\'' +
               ", suit=" + suit +
               ", value=" + value +
               ", index=" + index +
               ", displayName='" + displayName + '\'' +
               '}';
    }
}