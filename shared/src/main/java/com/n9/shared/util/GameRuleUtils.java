package com.n9.shared.util;

import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.enums.CardRank;
import com.n9.shared.model.enums.MatchResult;
import com.n9.shared.constants.GameConstants;

import java.util.Objects;

/**
 * Game Rule Utilities
 * 
 * Implements game logic and rules:
 * - Card comparison (winner determination)
 * - Round scoring
 * - Game winner calculation
 * - Rule validation
 * 
 * MVP Rules (36-card deck):
 * - 2 players
 * - 3 rounds
 * - Higher card wins the round (by value)
 * - Winner gets 1 point per round
 * - Tie: both get 0 points
 * - Most points after 3 rounds wins
 * - Final tie: DRAW result
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-01-05
 */
public final class GameRuleUtils {
    
    // Prevent instantiation
    private GameRuleUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    // ============================================================================
    // CARD COMPARISON
    // ============================================================================
    
    /**
     * Compare two cards to determine winner
     * 
     * Rules:
     * - Higher value wins
     * - Equal value = tie
     * - Suit is NOT considered (MVP simplification)
     * 
     * @param card1 First card
     * @param card2 Second card
     * @return Negative if card1 < card2, 0 if tie, positive if card1 > card2
     */
    public static int compareCards(CardDto card1, CardDto card2) {
        Objects.requireNonNull(card1, "Card 1 cannot be null");
        Objects.requireNonNull(card2, "Card 2 cannot be null");
        
        return Integer.compare(card1.getValue(), card2.getValue());
    }
    
    /**
     * Determine round winner
     * 
     * @param player1Card Player 1's card
     * @param player2Card Player 2's card
     * @return 1 if player 1 wins, 2 if player 2 wins, 0 if tie
     */
    public static int getRoundWinner(CardDto player1Card, CardDto player2Card) {
        int comparison = compareCards(player1Card, player2Card);
        
        if (comparison > 0) {
            return 1; // Player 1 wins
        } else if (comparison < 0) {
            return 2; // Player 2 wins
        } else {
            return 0; // Tie
        }
    }
    
    /**
     * Check if card1 wins against card2
     * 
     * @param card1 First card
     * @param card2 Second card
     * @return true if card1 wins
     */
    public static boolean winsAgainst(CardDto card1, CardDto card2) {
        return compareCards(card1, card2) > 0;
    }
    
    /**
     * Check if cards are tied (same value)
     * 
     * @param card1 First card
     * @param card2 Second card
     * @return true if tied
     */
    public static boolean isTie(CardDto card1, CardDto card2) {
        return compareCards(card1, card2) == 0;
    }
    

    public static int calculateRoundPoints(CardDto playerCard, CardDto opponentCard) {
        int winner = getRoundWinner(playerCard, opponentCard);
        
        if (winner == 1) {
            return GameConstants.POINTS_PER_WIN; // 1 point
        } else {
            return 0; // Tie or loss
        }
    }
    


    public static int calculateOpponentRoundPoints(CardDto playerCard, CardDto opponentCard) {
        int winner = getRoundWinner(playerCard, opponentCard);
        
        if (winner == 2) {
            return GameConstants.POINTS_PER_WIN; // 1 point
        } else {
            return 0; // Tie or loss
        }
    }
    


    public static MatchResult getRoundResult(CardDto playerCard, CardDto opponentCard) {
        int winner = getRoundWinner(playerCard, opponentCard);
        
        switch (winner) {
            case 1:
                return MatchResult.WIN;
            case 2:
                return MatchResult.LOSS;
            default:
                return MatchResult.DRAW;
        }
    }
    


    public static int getGameWinner(int player1Score, int player2Score) {
        if (player1Score > player2Score) {
            return 1;
        } else if (player2Score > player1Score) {
            return 2;
        } else {
            return 0; // Draw
        }
    }



    public static MatchResult getGameResult(int playerScore, int opponentScore) {
        return MatchResult.fromScores(playerScore, opponentScore);
    }
    



    public static boolean isGameOver(int completedRounds) {
        return completedRounds >= GameConstants.TOTAL_ROUNDS;
    }
    



    public static boolean isEarlyWin(int player1Score, int player2Score, int roundsPlayed) {
        int roundsRemaining = GameConstants.TOTAL_ROUNDS - roundsPlayed;
        int scoreDifference = Math.abs(player1Score - player2Score);
        
        // If score difference is greater than rounds remaining, game is decided
        return scoreDifference > roundsRemaining;
    }
    


    public static boolean isValidRoundNumber(int roundNumber) {
        return GameConstants.isValidRoundNumber(roundNumber);
    }
    


    public static boolean isValidScore(int score, int maxRounds) {
        return score >= 0 && score <= maxRounds;
    }
    


    public static boolean areValidFinalScores(int player1Score, int player2Score) {
        return isValidScore(player1Score, GameConstants.TOTAL_ROUNDS) &&
               isValidScore(player2Score, GameConstants.TOTAL_ROUNDS);
    }


    public static double calculateWinRate(int wins, int totalGames) {
        if (totalGames == 0) {
            return 0.0;
        }
        
        return (wins * 100.0) / totalGames;
    }
    


    public static double calculateWinRate(int wins, int totalGames, int decimalPlaces) {
        double winRate = calculateWinRate(wins, totalGames);
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.round(winRate * multiplier) / multiplier;
    }
    


    public static int getWinsNeededForWinRate(int currentWins, int currentGames, double targetWinRate) {
        if (targetWinRate < 0 || targetWinRate > 100) {
            throw new IllegalArgumentException("Target win rate must be between 0 and 100");
        }
        
        // If already achieved target
        if (calculateWinRate(currentWins, currentGames) >= targetWinRate) {
            return 0;
        }
        
        // Calculate wins needed
        // winRate = (currentWins + x) / (currentGames + x) * 100 = targetWinRate
        // Solve for x
        double targetRatio = targetWinRate / 100.0;
        double numerator = targetRatio * currentGames - currentWins;
        double denominator = 1 - targetRatio;
        
        if (denominator == 0) {
            return Integer.MAX_VALUE; // Impossible (100% win rate)
        }
        
        return (int) Math.ceil(numerator / denominator);
    }
    


    public static CardRank getRankFromValue(int value) {
        return CardRank.fromValue(value);
    }
    


    public static boolean isValidCardValue(int value) {
        return GameConstants.isValidCardValue(value);
    }
    


    public static int getMaxScore() {
        return GameConstants.TOTAL_ROUNDS;
    }
    
    /**
     * Get minimum score needed to guarantee win
     * 
     * In 3-round game: need at least 2 points to guarantee win
     * (opponent can have at most 1 point)
     * 
     * @return Minimum winning score
     */
    public static int getMinWinningScore() {
        return (GameConstants.TOTAL_ROUNDS / 2) + 1; // 2 for 3 rounds
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    /**
     * Format score display (e.g., "2 - 1")
     * 
     * @param player1Score Player 1's score
     * @param player2Score Player 2's score
     * @return Formatted score string
     */
    public static String formatScore(int player1Score, int player2Score) {
        return player1Score + " - " + player2Score;
    }
    
    /**
     * Get round description
     * 
     * @param roundNumber Round number
     * @return Description (e.g., "Round 1 of 3")
     */
    public static String getRoundDescription(int roundNumber) {
        return String.format("Round %d of %d", roundNumber, GameConstants.TOTAL_ROUNDS);
    }
    


    public static String getResultDescription(CardDto playerCard, CardDto opponentCard) {
        MatchResult result = getRoundResult(playerCard, opponentCard);
        
        switch (result) {
            case WIN:
                return "You win!";
            case LOSS:
                return "You lose!";
            case DRAW:
                return "Tie!";
            default:
                return "Unknown result";
        }
    }
    


    public static String getDetailedResultDescription(CardDto playerCard, CardDto opponentCard) {
        String playerCardStr = CardUtils.formatCard(playerCard);
        String opponentCardStr = CardUtils.formatCard(opponentCard);
        String result = getResultDescription(playerCard, opponentCard);
        
        return String.format("Your %s vs Opponent's %s - %s", 
            playerCardStr, opponentCardStr, result);
    }
}
