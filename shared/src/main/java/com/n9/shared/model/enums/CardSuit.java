package com.n9.shared.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Card Suit Enumeration
 * 
 * Represents the four suits in a standard deck of cards.
 * Used for card representation and game logic.
 * 
 * @author N9 Team
 * @version 1.0.0
 */
public enum CardSuit {
    
    /** Hearts - Cơ */
    HEARTS("H", "♥", "Hearts", "Cơ", "red"),
    
    /** Diamonds - Rô */
    DIAMONDS("D", "♦", "Diamonds", "Rô", "red"),
    
    /** Clubs - Chuồn */
    CLUBS("C", "♣", "Clubs", "Chuồn", "black"),
    
    /** Spades - Bích */
    SPADES("S", "♠", "Spades", "Bích", "black");
    
    private final String code;
    private final String symbol;
    private final String englishName;
    private final String vietnameseName;
    private final String color;
    
    CardSuit(String code, String symbol, String englishName, String vietnameseName, String color) {
        this.code = code;
        this.symbol = symbol;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.color = color;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    public String getVietnameseName() {
        return vietnameseName;
    }
    
    public String getColor() {
        return color;
    }
    
    public boolean isRed() {
        return "red".equals(color);
    }
    
    public boolean isBlack() {
        return "black".equals(color);
    }
    
    /**
     * Parse from code (H, D, C, S)
     */
    public static CardSuit fromCode(String code) {
        if (code == null) return null;
        
        for (CardSuit suit : values()) {
            if (suit.code.equalsIgnoreCase(code)) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Unknown card suit code: " + code);
    }
    
    /**
     * Parse from symbol (♥, ♦, ♣, ♠)
     */
    public static CardSuit fromSymbol(String symbol) {
        if (symbol == null) return null;
        
        for (CardSuit suit : values()) {
            if (suit.symbol.equals(symbol)) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Unknown card suit symbol: " + symbol);
    }
}