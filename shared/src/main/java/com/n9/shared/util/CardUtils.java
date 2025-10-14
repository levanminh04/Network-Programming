package com.n9.shared.util;

import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.enums.CardRank;
import com.n9.shared.model.enums.CardSuit;
import com.n9.shared.constants.GameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Card Utilities
 * 
 * Provides utility methods for card operations:
 * - Deck generation (36-card deck for MVP)
 * - Shuffling and dealing
 * - Card validation
 * - Card formatting and display
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class CardUtils {
    
    private static final Random random = new Random();
    
    // Prevent instantiation
    private CardUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    // ============================================================================
    // DECK GENERATION
    // ============================================================================
    
    /**
     * Generate a standard 36-card deck (MVP)
     * 
     * 4 suits × 9 ranks (Ace through 9) = 36 cards
     * Card IDs: 1-36
     * 
     * @return List of 36 cards
     */
    public static List<CardDto> generateDeck() {
        List<CardDto> deck = new ArrayList<>(GameConstants.DECK_SIZE);
        int cardId = 1;
        
        for (CardSuit suit : CardSuit.values()) {
            for (CardRank rank : CardRank.values()) {
                CardDto card = new CardDto();
                card.setCardId(cardId);
                card.setSuit(suit);
                card.setRank(rank.getCode());
                card.setValue(rank.getValue());
                card.setIndex(cardId - 1); // 0-based index
                
                deck.add(card);
                cardId++;
            }
        }
        
        return deck;
    }
    
    /**
     * Generate a deck with specific seed (for testing)
     * 
     * @param seed Random seed
     * @return List of 36 cards
     */
    public static List<CardDto> generateDeck(long seed) {
        return generateDeck(); // Deck generation is deterministic
    }
    
    // ============================================================================
    // SHUFFLING
    // ============================================================================
    
    /**
     * Shuffle a deck of cards (Fisher-Yates algorithm)
     * 
     * @param deck Deck to shuffle (modified in place)
     */
    public static void shuffle(List<CardDto> deck) {
        if (deck == null || deck.size() <= 1) {
            return;
        }
        
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(deck, i, j);
        }
    }
    
    /**
     * Shuffle a deck with specific seed (for testing)
     * 
     * @param deck Deck to shuffle
     * @param seed Random seed
     */
    public static void shuffle(List<CardDto> deck, long seed) {
        if (deck == null || deck.size() <= 1) {
            return;
        }
        
        Random seededRandom = new Random(seed);
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = seededRandom.nextInt(i + 1);
            Collections.swap(deck, i, j);
        }
    }
    
    /**
     * Create a shuffled copy of a deck
     * 
     * @param deck Original deck
     * @return Shuffled copy
     */
    public static List<CardDto> shuffledCopy(List<CardDto> deck) {
        List<CardDto> copy = new ArrayList<>(deck);
        shuffle(copy);
        return copy;
    }
    
    // ============================================================================
    // DEALING
    // ============================================================================
    
    /**
     * Deal cards to multiple players
     * 
     * @param deck Deck to deal from
     * @param numPlayers Number of players
     * @param cardsPerPlayer Cards per player
     * @return List of hands (one per player)
     * @throws IllegalArgumentException if not enough cards
     */
    public static List<List<CardDto>> dealCards(List<CardDto> deck, int numPlayers, int cardsPerPlayer) {
        int totalCardsNeeded = numPlayers * cardsPerPlayer;
        
        if (deck.size() < totalCardsNeeded) {
            throw new IllegalArgumentException(
                String.format("Not enough cards. Need %d, have %d", totalCardsNeeded, deck.size())
            );
        }
        
        List<List<CardDto>> hands = new ArrayList<>(numPlayers);
        
        for (int player = 0; player < numPlayers; player++) {
            List<CardDto> hand = new ArrayList<>(cardsPerPlayer);
            
            for (int card = 0; card < cardsPerPlayer; card++) {
                int index = player * cardsPerPlayer + card;
                hand.add(deck.get(index));
            }
            
            hands.add(hand);
        }
        
        return hands;
    }
    
    /**
     * Deal cards for a standard game (2 players, 3 cards each for MVP)
     * 
     * @param deck Shuffled deck
     * @return Array of 2 hands
     */
    public static List<CardDto>[] dealForGame(List<CardDto> deck) {
        @SuppressWarnings("unchecked")
        List<CardDto>[] hands = new List[GameConstants.PLAYERS_PER_GAME];
        
        List<List<CardDto>> dealt = dealCards(
            deck, 
            GameConstants.PLAYERS_PER_GAME, 
            GameConstants.TOTAL_ROUNDS
        );
        
        hands[0] = dealt.get(0);
        hands[1] = dealt.get(1);
        
        return hands;
    }
    
    /**
     * Deal a single hand
     * 
     * @param deck Deck to deal from
     * @param numCards Number of cards to deal
     * @return Hand of cards
     */
    public static List<CardDto> dealHand(List<CardDto> deck, int numCards) {
        if (deck.size() < numCards) {
            throw new IllegalArgumentException(
                String.format("Not enough cards. Need %d, have %d", numCards, deck.size())
            );
        }
        
        return new ArrayList<>(deck.subList(0, numCards));
    }
    
    // ============================================================================
    // CARD VALIDATION
    // ============================================================================
    
    /**
     * Check if card ID is valid (1-36 for MVP)
     * 
     * @param cardId Card ID
     * @return true if valid
     */
    public static boolean isValidCardId(int cardId) {
        return GameConstants.isValidCardId(cardId);
    }
    
    /**
     * Check if card exists in hand
     * 
     * @param hand Player's hand
     * @param cardId Card ID to check
     * @return true if card is in hand
     */
    public static boolean hasCard(List<CardDto> hand, int cardId) {
        if (hand == null || hand.isEmpty()) {
            return false;
        }
        
        return hand.stream()
            .anyMatch(card -> card.getCardId() != null && card.getCardId() == cardId);
    }
    
    /**
     * Find card in hand by ID
     * 
     * @param hand Player's hand
     * @param cardId Card ID
     * @return CardDto or null if not found
     */
    public static CardDto findCard(List<CardDto> hand, int cardId) {
        if (hand == null || hand.isEmpty()) {
            return null;
        }
        
        return hand.stream()
            .filter(card -> card.getCardId() != null && card.getCardId() == cardId)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Remove card from hand
     * 
     * @param hand Player's hand
     * @param cardId Card ID to remove
     * @return Removed card, or null if not found
     */
    public static CardDto removeCard(List<CardDto> hand, int cardId) {
        if (hand == null || hand.isEmpty()) {
            return null;
        }
        
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardId() != null && hand.get(i).getCardId() == cardId) {
                return hand.remove(i);
            }
        }
        
        return null;
    }
    
    // ============================================================================
    // CARD FORMATTING
    // ============================================================================
    
    /**
     * Format card for display (e.g., "A♥", "7♠")
     * 
     * @param card Card to format
     * @return Formatted string
     */
    public static String formatCard(CardDto card) {
        if (card == null) {
            return "??";
        }
        
        CardRank rank = CardRank.fromCode(card.getRank());
        CardSuit suit = card.getSuit();
        
        return rank.getCode() + suit.getSymbol();
    }
    
    /**
     * Format card with Vietnamese name (e.g., "Át Cơ", "Bảy Rô")
     * 
     * @param card Card to format
     * @return Vietnamese formatted string
     */
    public static String formatCardVietnamese(CardDto card) {
        if (card == null) {
            return "??";
        }
        
        CardRank rank = CardRank.fromCode(card.getRank());
        CardSuit suit = card.getSuit();
        
        return rank.getVietnameseName() + " " + suit.getVietnameseName();
    }
    
    /**
     * Format hand for display (e.g., "[A♥, 7♠, 5♦]")
     * 
     * @param hand Hand of cards
     * @return Formatted string
     */
    public static String formatHand(List<CardDto> hand) {
        if (hand == null || hand.isEmpty()) {
            return "[]";
        }
        
        return hand.stream()
            .map(CardUtils::formatCard)
            .collect(Collectors.joining(", ", "[", "]"));
    }
    
    /**
     * Get card description (e.g., "Ace of Hearts")
     * 
     * @param card Card
     * @return Description
     */
    public static String getCardDescription(CardDto card) {
        if (card == null) {
            return "Unknown card";
        }
        
        CardRank rank = CardRank.fromCode(card.getRank());
        CardSuit suit = card.getSuit();
        
        return rank.getEnglishName() + " of " + suit.getEnglishName();
    }
    
    // ============================================================================
    // CARD SELECTION
    // ============================================================================
    
    /**
     * Pick a random card from hand
     * 
     * @param hand Player's hand
     * @return Random card, or null if hand is empty
     */
    public static CardDto pickRandomCard(List<CardDto> hand) {
        if (hand == null || hand.isEmpty()) {
            return null;
        }
        
        int index = random.nextInt(hand.size());
        return hand.get(index);
    }
    
    /**
     * Pick the highest value card from hand
     * 
     * @param hand Player's hand
     * @return Highest card, or null if hand is empty
     */
    public static CardDto pickHighestCard(List<CardDto> hand) {
        if (hand == null || hand.isEmpty()) {
            return null;
        }
        
        return hand.stream()
            .max((c1, c2) -> Integer.compare(c1.getValue(), c2.getValue()))
            .orElse(null);
    }
    
    /**
     * Pick the lowest value card from hand
     * 
     * @param hand Player's hand
     * @return Lowest card, or null if hand is empty
     */
    public static CardDto pickLowestCard(List<CardDto> hand) {
        if (hand == null || hand.isEmpty()) {
            return null;
        }
        
        return hand.stream()
            .min((c1, c2) -> Integer.compare(c1.getValue(), c2.getValue()))
            .orElse(null);
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Sort hand by value (ascending)
     * 
     * @param hand Hand to sort (modified in place)
     */
    public static void sortByValue(List<CardDto> hand) {
        if (hand == null || hand.size() <= 1) {
            return;
        }
        
        hand.sort((c1, c2) -> Integer.compare(c1.getValue(), c2.getValue()));
    }
    
    /**
     * Sort hand by suit, then by value
     * 
     * @param hand Hand to sort (modified in place)
     */
    public static void sortBySuit(List<CardDto> hand) {
        if (hand == null || hand.size() <= 1) {
            return;
        }
        
        hand.sort((c1, c2) -> {
            int suitCompare = c1.getSuit().compareTo(c2.getSuit());
            if (suitCompare != 0) {
                return suitCompare;
            }
            return Integer.compare(c1.getValue(), c2.getValue());
        });
    }
    
    /**
     * Create a copy of a hand
     * 
     * @param hand Original hand
     * @return Copy of hand
     */
    public static List<CardDto> copyHand(List<CardDto> hand) {
        if (hand == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(hand);
    }
    
    /**
     * Get card IDs from hand
     * 
     * @param hand Hand of cards
     * @return List of card IDs
     */
    public static List<Integer> getCardIds(List<CardDto> hand) {
        if (hand == null || hand.isEmpty()) {
            return new ArrayList<>();
        }
        
        return hand.stream()
            .map(CardDto::getCardId)
            .collect(Collectors.toList());
    }
    
    /**
     * Count cards in hand
     * 
     * @param hand Hand of cards
     * @return Number of cards
     */
    public static int countCards(List<CardDto> hand) {
        return hand != null ? hand.size() : 0;
    }
    
    /**
     * Check if hand is empty
     * 
     * @param hand Hand of cards
     * @return true if empty or null
     */
    public static boolean isEmpty(List<CardDto> hand) {
        return hand == null || hand.isEmpty();
    }
}
