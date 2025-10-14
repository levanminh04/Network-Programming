package com.n9.core.service;

import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.model.enums.MatchResult;
import com.n9.shared.util.CardUtils;
import com.n9.shared.util.GameRuleUtils;
import com.n9.shared.constants.GameConstants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game Service - Core business logic for card game
 * 
 * Uses shared utilities:
 * - CardUtils: Deck generation, shuffle, deal, format
 * - GameRuleUtils: Card comparison, scoring, winner determination
 * 
 * Manages:
 * - Game state per match
 * - Player hands (3 cards each)
 * - Round execution (card play, auto-pick, reveal)
 * - Score tracking and winner determination
 * 
 * Thread-safe using ConcurrentHashMap for multi-player games
 * 
 * @version 1.0.0 (MVP)
 */
public class GameService {

    /**
     * Game state for a single match
     */
    public static class GameState {
        private final String matchId;
        private final String player1Id;
        private final String player2Id;
        private List<CardDto> player1Hand;
        private List<CardDto> player2Hand;
        private int player1Score = 0;
        private int player2Score = 0;
        private int currentRound = 0;
        private final List<RoundRevealDto> roundHistory = new ArrayList<>();
        private boolean isComplete = false;
        
        public GameState(String matchId, String player1Id, String player2Id) {
            this.matchId = matchId;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
        }
        
        // Getters
        public String getMatchId() { return matchId; }
        public String getPlayer1Id() { return player1Id; }
        public String getPlayer2Id() { return player2Id; }
        public List<CardDto> getPlayer1Hand() { return player1Hand; }
        public List<CardDto> getPlayer2Hand() { return player2Hand; }
        public int getPlayer1Score() { return player1Score; }
        public int getPlayer2Score() { return player2Score; }
        public int getCurrentRound() { return currentRound; }
        public List<RoundRevealDto> getRoundHistory() { return roundHistory; }
        public boolean isComplete() { return isComplete; }
        
        // Setters
        public void setPlayer1Hand(List<CardDto> hand) { this.player1Hand = hand; }
        public void setPlayer2Hand(List<CardDto> hand) { this.player2Hand = hand; }
        public void setPlayer1Score(int score) { this.player1Score = score; }
        public void setPlayer2Score(int score) { this.player2Score = score; }
        public void setCurrentRound(int round) { this.currentRound = round; }
        public void setComplete(boolean complete) { this.isComplete = complete; }
        
        public void addRoundResult(RoundRevealDto reveal) {
            roundHistory.add(reveal);
        }
    }
    
    // Active games: matchId -> GameState
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    
    /**
     * Initialize a new game
     * 
     * @param matchId Unique match identifier
     * @param player1Id First player ID
     * @param player2Id Second player ID
     * @return GameState with dealt hands
     */
    public GameState initializeGame(String matchId, String player1Id, String player2Id) {
        // Generate and shuffle deck
        List<CardDto> deck = CardUtils.generateDeck();
        CardUtils.shuffle(deck);
        
        // Deal cards (2 players × 3 cards each)
        List<CardDto>[] hands = CardUtils.dealForGame(deck);
        
        // Create game state
        GameState game = new GameState(matchId, player1Id, player2Id);
        game.setPlayer1Hand(new ArrayList<>(hands[0]));
        game.setPlayer2Hand(new ArrayList<>(hands[1]));
        game.setCurrentRound(1);
        
        // Store in active games
        activeGames.put(matchId, game);
        
        return game;
    }
    
    /**
     * Get player's hand for a match
     * 
     * @param matchId Match identifier
     * @param playerId Player identifier
     * @return List of cards in player's hand (max 3)
     */
    public List<CardDto> getPlayerHand(String matchId, String playerId) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + matchId);
        }
        
        if (playerId.equals(game.getPlayer1Id())) {
            return new ArrayList<>(game.getPlayer1Hand());
        } else if (playerId.equals(game.getPlayer2Id())) {
            return new ArrayList<>(game.getPlayer2Hand());
        } else {
            throw new IllegalArgumentException("Player not in game: " + playerId);
        }
    }
    
    /**
     * Play a card for a player
     * 
     * @param matchId Match identifier
     * @param playerId Player identifier
     * @param cardId Card ID to play (1-36)
     * @return The played card
     * @throws IllegalArgumentException if card not in hand or invalid
     */
    public CardDto playCard(String matchId, String playerId, int cardId) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + matchId);
        }
        
        List<CardDto> hand;
        if (playerId.equals(game.getPlayer1Id())) {
            hand = game.getPlayer1Hand();
        } else if (playerId.equals(game.getPlayer2Id())) {
            hand = game.getPlayer2Hand();
        } else {
            throw new IllegalArgumentException("Player not in game: " + playerId);
        }
        
        // Find and remove card from hand
        CardDto playedCard = CardUtils.findCard(hand, cardId);
        if (playedCard == null) {
            throw new IllegalArgumentException(
                "Card " + cardId + " not in player's hand"
            );
        }
        
        CardUtils.removeCard(hand, cardId);
        return playedCard;
    }
    
    /**
     * Auto-pick a card for a player (timeout scenario)
     * Uses random strategy for MVP
     * 
     * @param matchId Match identifier
     * @param playerId Player identifier
     * @return The auto-picked card
     */
    public CardDto autoPickCard(String matchId, String playerId) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + matchId);
        }
        
        List<CardDto> hand;
        if (playerId.equals(game.getPlayer1Id())) {
            hand = game.getPlayer1Hand();
        } else if (playerId.equals(game.getPlayer2Id())) {
            hand = game.getPlayer2Hand();
        } else {
            throw new IllegalArgumentException("Player not in game: " + playerId);
        }
        
        // Auto-pick using random strategy
        CardDto picked = CardUtils.pickRandomCard(hand);
        if (picked != null) {
            CardUtils.removeCard(hand, picked.getCardId());
        }
        
        return picked;
    }
    
    /**
     * Execute a round with both players' cards
     * 
     * @param matchId Match identifier
     * @param player1Card Player 1's card
     * @param player2Card Player 2's card
     * @param player1AutoPicked Whether P1's card was auto-picked
     * @param player2AutoPicked Whether P2's card was auto-picked
     * @return Round reveal DTO with results
     */
    public RoundRevealDto executeRound(
        String matchId,
        CardDto player1Card,
        CardDto player2Card,
        boolean player1AutoPicked,
        boolean player2AutoPicked
    ) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + matchId);
        }
        
        // Determine round winner (1, 2, or 0 for tie)
        int roundWinner = GameRuleUtils.getRoundWinner(player1Card, player2Card);
        
        // Calculate round scores (1 point per win)
        int player1RoundScore = GameRuleUtils.calculateRoundPoints(player1Card, player2Card);
        int player2RoundScore = GameRuleUtils.calculateRoundPoints(player2Card, player1Card);
        
        // Update total scores
        game.setPlayer1Score(game.getPlayer1Score() + player1RoundScore);
        game.setPlayer2Score(game.getPlayer2Score() + player2RoundScore);
        
        // Determine round winner ID
        String roundWinnerId = null;
        if (roundWinner == 1) {
            roundWinnerId = game.getPlayer1Id();
        } else if (roundWinner == 2) {
            roundWinnerId = game.getPlayer2Id();
        }
        
        // Create round reveal DTO (from Player 1's perspective)
        // Note: This is server-side model. Gateway will convert to player-centric view
        RoundRevealDto reveal = RoundRevealDto.builder()
            .gameId(matchId)
            .roundNumber(game.getCurrentRound())
            .playerCard(player1Card)  // From P1's perspective
            .opponentCard(player2Card)
            .playerAutoPicked(player1AutoPicked)
            .opponentAutoPicked(player2AutoPicked)
            .pointsEarned(player1RoundScore)
            .playerScore(game.getPlayer1Score())
            .opponentScore(game.getPlayer2Score())
            .result(player1RoundScore > 0 ? "WIN" : (player2RoundScore > 0 ? "LOSS" : "DRAW"))
            .build();
        
        // Store round result
        game.addRoundResult(reveal);
        
        // Increment round counter
        game.setCurrentRound(game.getCurrentRound() + 1);
        
        // Check if game is complete
        if (game.getCurrentRound() > GameConstants.TOTAL_ROUNDS) {
            game.setComplete(true);
        }
        
        return reveal;
    }
    
    /**
     * Get game winner
     * 
     * @param matchId Match identifier
     * @return Winner player ID or null if tie
     */
    public String getGameWinner(String matchId) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + matchId);
        }
        
        int winner = GameRuleUtils.getGameWinner(
            game.getPlayer1Score(),
            game.getPlayer2Score()
        );
        
        if (winner == 1) {
            return game.getPlayer1Id();
        } else if (winner == 2) {
            return game.getPlayer2Id();
        } else {
            return null; // Tie
        }
    }
    
    /**
     * Get game result from player's perspective
     * 
     * @param matchId Match identifier
     * @param playerId Player identifier
     * @return MatchResult enum (WIN, LOSE, TIE)
     */
    public MatchResult getGameResult(String matchId, String playerId) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + matchId);
        }
        
        int playerScore;
        int opponentScore;
        
        if (playerId.equals(game.getPlayer1Id())) {
            playerScore = game.getPlayer1Score();
            opponentScore = game.getPlayer2Score();
        } else if (playerId.equals(game.getPlayer2Id())) {
            playerScore = game.getPlayer2Score();
            opponentScore = game.getPlayer1Score();
        } else {
            throw new IllegalArgumentException("Player not in game: " + playerId);
        }
        
        return GameRuleUtils.getGameResult(playerScore, opponentScore);
    }
    
    /**
     * Check if game is over
     * 
     * @param matchId Match identifier
     * @return true if 3 rounds complete
     */
    public boolean isGameOver(String matchId) {
        GameState game = activeGames.get(matchId);
        if (game == null) {
            return false;
        }
        return game.isComplete();
    }
    
    /**
     * Get game state
     * 
     * @param matchId Match identifier
     * @return GameState or null if not found
     */
    public GameState getGameState(String matchId) {
        return activeGames.get(matchId);
    }
    
    /**
     * Remove completed game from active games
     * 
     * @param matchId Match identifier
     */
    public void cleanupGame(String matchId) {
        activeGames.remove(matchId);
    }
    
    /**
     * Get formatted card for display
     * 
     * @param card Card to format
     * @return Formatted string (e.g., "A♥", "7♠")
     */
    public String formatCard(CardDto card) {
        return CardUtils.formatCard(card);
    }
    
    /**
     * Get detailed round result description
     * 
     * @param playerCard Player's card
     * @param opponentCard Opponent's card
     * @return Description (e.g., "Your A♥ vs Opponent's 7♠ - You lose!")
     */
    public String getRoundDescription(CardDto playerCard, CardDto opponentCard) {
        return GameRuleUtils.getDetailedResultDescription(playerCard, opponentCard);
    }
}
