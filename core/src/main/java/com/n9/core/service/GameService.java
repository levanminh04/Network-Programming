package com.n9.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.constants.GameConstants;
import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.PlayCardAckDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.CardUtils;
import com.n9.shared.util.GameRuleUtils;
import com.n9.shared.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Game Service - Core business logic for card game.
 * Implements "Shared Deck" logic where players pick from a common pool.
 *
 * @version 1.1.0 (Refactored for Shared Deck)
 */
public class GameService {

    private final ConcurrentHashMap<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Lock> gameLocks = new ConcurrentHashMap<>();

    public GameService(ConcurrentHashMap<String, ClientConnectionHandler> activeConnections, ScheduledExecutorService scheduler) {
        this.activeConnections = activeConnections;
        this.scheduler = scheduler;
    }

    /**
     * Game state for a single match (Shared Deck version).
     */
    public static class GameState {
        private final String matchId;
        private final String player1Id;
        private final String player2Id;
        private int player1Score = 0;
        private int player2Score = 0;
        private int currentRound = 0;
        private final List<RoundRevealDto> roundHistory = new ArrayList<>();
        private boolean isComplete = false;
        private List<CardDto> availableCards; // Bộ bài chung
        private CardDto player1PlayedCard = null;
        private CardDto player2PlayedCard = null;
        private boolean player1AutoPicked = false;
        private boolean player2AutoPicked = false;

        public GameState(String matchId, String player1Id, String player2Id) {
            this.matchId = matchId;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
        }

        // Getters & Setters đầy đủ cho tất cả các trường
        public String getMatchId() {
            return matchId;
        }

        public String getPlayer1Id() {
            return player1Id;
        }

        public String getPlayer2Id() {
            return player2Id;
        }

        public List<CardDto> getAvailableCards() {
            return availableCards;
        }

        public int getPlayer1Score() {
            return player1Score;
        }

        public int getPlayer2Score() {
            return player2Score;
        }

        public int getCurrentRound() {
            return currentRound;
        }

        public List<RoundRevealDto> getRoundHistory() {
            return roundHistory;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public CardDto getPlayer1PlayedCard() {
            return player1PlayedCard;
        }

        public CardDto getPlayer2PlayedCard() {
            return player2PlayedCard;
        }

        public boolean isPlayer1AutoPicked() {
            return player1AutoPicked;
        }

        public boolean isPlayer2AutoPicked() {
            return player2AutoPicked;
        }

        public void setAvailableCards(List<CardDto> cards) {
            this.availableCards = cards;
        }

        public void setPlayer1Score(int score) {
            this.player1Score = score;
        }

        public void setPlayer2Score(int score) {
            this.player2Score = score;
        }

        public void setCurrentRound(int round) {
            this.currentRound = round;
        }

        public void setComplete(boolean complete) {
            this.isComplete = complete;
        }

        public void setPlayer1PlayedCard(CardDto card) {
            this.player1PlayedCard = card;
        }

        public void setPlayer2PlayedCard(CardDto card) {
            this.player2PlayedCard = card;
        }

        public void setPlayer1AutoPicked(boolean autoPicked) {
            this.player1AutoPicked = autoPicked;
        }

        public void setPlayer2AutoPicked(boolean autoPicked) {
            this.player2AutoPicked = autoPicked;
        }

        public void addRoundResult(RoundRevealDto reveal) {
            roundHistory.add(reveal);
        }
    }


    /**
     * Khởi tạo trận đấu với bộ bài chung. Được gọi bởi MatchmakingService.
     */
    public GameState initializeGame(String matchId, String player1Id, String player2Id) {
        System.out.println("🚀 Initializing game (Shared Deck) for match: " + matchId);
        List<CardDto> fullShuffledDeck = CardUtils.generateDeck();
        CardUtils.shuffle(fullShuffledDeck);
        GameState game = new GameState(matchId, player1Id, player2Id);
        game.setAvailableCards(new ArrayList<>(fullShuffledDeck));
        game.setCurrentRound(0);
        activeGames.put(matchId, game);
        gameLocks.put(matchId, new ReentrantLock());
        System.out.println("   Created lock for match: " + matchId);
        Object payload1 = createGameStartPayload_SharedDeck(game, player1Id, "Opponent"); // Cần lấy username đối thủ
        Object payload2 = createGameStartPayload_SharedDeck(game, player2Id, "Opponent");
        notifyPlayer(player1Id, MessageProtocol.Type.GAME_START, payload1);
        notifyPlayer(player2Id, MessageProtocol.Type.GAME_START, payload2);
        System.out.println("   Sent GAME_START notifications.");
        startNextRound(matchId); // Bắt đầu Round 1
        return game;
    }

    /**
     * Tạo payload cho GAME_START (Shared Deck).
     */
    private Object createGameStartPayload_SharedDeck(GameState game, String targetPlayerId, String opponentUsername) {
        boolean isPlayer1 = targetPlayerId.equals(game.getPlayer1Id());
        String opponentId = isPlayer1 ? game.getPlayer2Id() : game.getPlayer1Id();
        Map<String, Object> opponentInfo = new HashMap<>();
        opponentInfo.put("userId", opponentId);
        opponentInfo.put("username", opponentUsername);
        Map<String, Object> payload = new HashMap<>();
        payload.put("matchId", game.getMatchId());
        payload.put("initialAvailableCards", new ArrayList<>(game.getAvailableCards())); // Gửi bộ bài ban đầu
        payload.put("opponent", opponentInfo);
        payload.put("yourPosition", isPlayer1 ? 1 : 2);
        return payload;
    }

    /**
     * Bắt đầu một round mới.
     */
    public void startNextRound(String matchId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) {
            System.err.println("⚠️ Lock not found for startNextRound: " + matchId);
            return;
        }
        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) {
                System.out.println("🏁 Game already ended or not found for startNextRound: " + matchId);
                return;
            }
            int nextRound = game.getCurrentRound() + 1;
            game.setCurrentRound(nextRound);
            game.setPlayer1PlayedCard(null);
            game.setPlayer2PlayedCard(null);
            game.setPlayer1AutoPicked(false);
            game.setPlayer2AutoPicked(false);
            System.out.println("⏱️ Starting Round " + nextRound + " for Match " + matchId);
            long timeoutMillis = GameConstants.ROUND_TIMEOUT_SECONDS * 1000L;
            long deadlineTimestamp = System.currentTimeMillis() + timeoutMillis;
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId);
            payload.put("roundNumber", nextRound);
            payload.put("deadlineTimestamp", deadlineTimestamp);
            payload.put("durationMs", timeoutMillis);
            payload.put("availableCards", new ArrayList<>(game.getAvailableCards())); // Gửi bài còn lại
            notifyPlayer(game.getPlayer1Id(), MessageProtocol.Type.GAME_ROUND_START, payload);
            notifyPlayer(game.getPlayer2Id(), MessageProtocol.Type.GAME_ROUND_START, payload);
            scheduler.schedule(() -> handleRoundTimeout(matchId, nextRound), timeoutMillis, TimeUnit.MILLISECONDS);
            System.out.println("   Scheduled timeout for round " + nextRound + " in " + timeoutMillis + " ms.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý khi hết giờ chọn bài.
     */
    private void handleRoundTimeout(String matchId, int roundNumber) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;
        boolean triggerReveal = false;
        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete() || game.getCurrentRound() != roundNumber) return;
            System.out.println("⏰ Timeout triggered for round " + roundNumber + " in match " + matchId);
            CardDto p1Card = game.getPlayer1PlayedCard();
            CardDto p2Card = game.getPlayer2PlayedCard();
            CardDto pickedCard;
            if (p1Card == null) {
                pickedCard = autoPickCardInternal_SharedDeck(game); // Gọi hàm auto pick
                if (pickedCard != null) {
                    game.setPlayer1PlayedCard(pickedCard);
                    game.setPlayer1AutoPicked(true);
                    System.out.println("   Auto-picked P1: " + formatCard(pickedCard));
                } else {
                    System.err.println("   ERROR: Auto-pick failed P1");
                }
            }
            if (p2Card == null) {
                pickedCard = autoPickCardInternal_SharedDeck(game); // Gọi hàm auto pick
                if (pickedCard != null) {
                    game.setPlayer2PlayedCard(pickedCard);
                    game.setPlayer2AutoPicked(true);
                    System.out.println("   Auto-picked P2: " + formatCard(pickedCard));
                } else {
                    System.err.println("   ERROR: Auto-pick failed P2");
                }
            }
            if (game.getPlayer1PlayedCard() != null && game.getPlayer2PlayedCard() != null) triggerReveal = true;
        } finally {
            lock.unlock();
        }
        if (triggerReveal) executeRoundRevealAndProceed(matchId);
    }

    /**
     * Chọn và xóa một lá bài ngẫu nhiên từ bộ bài chung. Được gọi bên trong lock.
     */
    private CardDto autoPickCardInternal_SharedDeck(GameState game) {
        List<CardDto> available = game.getAvailableCards();
        if (CardUtils.isEmpty(available)) return null;
        CardDto pickedCard = CardUtils.pickRandomCard(available);
        if (pickedCard != null) {
            CardDto removedCard = CardUtils.removeCard(available, pickedCard.getCardId()); // Xóa khỏi available
            if (removedCard == null) return null; // Lỗi
        }
        return pickedCard;
    }

    /**
     * Xử lý khi người chơi đánh bài.
     */
    public CardDto playCard(String matchId, String playerId, int cardId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) throw new IllegalArgumentException("Game not found or ended: " + matchId);
        CardDto playedCard = null;
        boolean triggerReveal = false;
        boolean isPlayer1 = false;
        List<CardDto> currentAvailableCards = null; // Lưu lại để gửi đi

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete())
                throw new IllegalArgumentException("Game not found or ended: " + matchId);
            if (game.getCurrentRound() == 0 || game.getCurrentRound() > GameConstants.TOTAL_ROUNDS)
                throw new IllegalArgumentException("Cannot play outside active rounds.");
            isPlayer1 = playerId.equals(game.getPlayer1Id());
            if ((isPlayer1 && game.getPlayer1PlayedCard() != null) || (!isPlayer1 && game.getPlayer2PlayedCard() != null))
                throw new IllegalArgumentException("Already played this round.");

            // Dùng findAndRemoveCard cho an toàn và gọn
            playedCard = CardUtils.findAndRemoveCard(game.getAvailableCards(), cardId);
            if (playedCard == null)
                throw new IllegalArgumentException("Card " + cardId + " is not available or already played.");

            System.out.println("🃏 Player " + playerId + " played: " + formatCard(playedCard));
            if (isPlayer1) {
                game.setPlayer1PlayedCard(playedCard);
                game.setPlayer1AutoPicked(false);
            } else {
                game.setPlayer2PlayedCard(playedCard);
                game.setPlayer2AutoPicked(false);
            }

            // Lưu lại danh sách bài còn lại sau khi xóa
            currentAvailableCards = new ArrayList<>(game.getAvailableCards());

            CardDto opponentCard = isPlayer1 ? game.getPlayer2PlayedCard() : game.getPlayer1PlayedCard();
            if (opponentCard != null) triggerReveal = true;
        } finally {
            lock.unlock();
        }

        // --- Hành động I/O bên ngoài Lock ---
        PlayCardAckDto ackDto = new PlayCardAckDto();
        ackDto.setGameId(matchId);
        ackDto.setCardId(playedCard.getCardId());
        if (currentAvailableCards != null) {
            ackDto.setAvailableCards(currentAvailableCards); // Gửi danh sách mới nhất
        }
        notifyPlayer(playerId, MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS, ackDto);

        if (triggerReveal) {
            executeRoundRevealAndProceed(matchId);
        } else {
            GameState game = activeGames.get(matchId); // Chỉ để lấy opponentId
            if (game != null) {
                String opponentId = playerId.equals(game.getPlayer1Id()) ? game.getPlayer2Id() : game.getPlayer1Id();
                Map<String, Object> opponentReadyPayload = new HashMap<>();
                opponentReadyPayload.put("status", "READY");
                opponentReadyPayload.put("playedCardId", playedCard.getCardId());
                if (currentAvailableCards != null) { // Gửi cả available cards cho đối thủ
                    opponentReadyPayload.put("availableCards", currentAvailableCards);
                }
                notifyPlayer(opponentId, MessageProtocol.Type.GAME_OPPONENT_READY, opponentReadyPayload);
            }
        }
        return playedCard;
    }

    /**
     * Thực thi lật bài, tính điểm, chuyển round hoặc kết thúc game.
     */
    private void executeRoundRevealAndProceed(String matchId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;
        RoundRevealDto revealPayloadP1 = null, revealPayloadP2 = null;
        boolean gameOver = false;
        GameState gameSnapshotForEnd = null;
        String player1Id = null, player2Id = null;
        List<CardDto> finalAvailableCards = null; // Lưu lại để gửi đi

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete() || game.getPlayer1PlayedCard() == null || game.getPlayer2PlayedCard() == null)
                return;
            player1Id = game.getPlayer1Id();
            player2Id = game.getPlayer2Id();
            System.out.println("✨ Revealing round " + game.getCurrentRound() + " for match " + matchId);
            CardDto p1Card = game.getPlayer1PlayedCard();
            CardDto p2Card = game.getPlayer2PlayedCard();
            boolean p1Auto = game.isPlayer1AutoPicked();
            boolean p2Auto = game.isPlayer2AutoPicked();
            int p1RoundScore = GameRuleUtils.calculateRoundPoints(p1Card, p2Card);
            int p2RoundScore = GameRuleUtils.calculateRoundPoints(p2Card, p1Card);
            game.setPlayer1Score(game.getPlayer1Score() + p1RoundScore);
            game.setPlayer2Score(game.getPlayer2Score() + p2RoundScore);
            revealPayloadP1 = RoundRevealDto.builder()
                    .gameId(matchId).roundNumber(game.getCurrentRound())
                    .playerCard(p1Card).opponentCard(p2Card)
                    .playerAutoPicked(p1Auto).opponentAutoPicked(p2Auto)
                    .pointsEarned(p1RoundScore).playerScore(game.getPlayer1Score()).opponentScore(game.getPlayer2Score())
                    .result(p1RoundScore > p2RoundScore ? "WIN" : (p2RoundScore > p1RoundScore ? "LOSS" : "DRAW")).build();
            game.addRoundResult(revealPayloadP1);
            revealPayloadP2 = createRevealForPlayer2(revealPayloadP1, game.getPlayer1Score(), game.getPlayer2Score(), p2RoundScore);

            // Lưu lại danh sách bài còn lại TRƯỚC khi kiểm tra game over
            finalAvailableCards = new ArrayList<>(game.getAvailableCards());

            if (game.getCurrentRound() >= GameConstants.TOTAL_ROUNDS) {
                game.setComplete(true);
                gameOver = true;
                gameSnapshotForEnd = cloneGameState(game);
                System.out.println("🏁 Game " + matchId + " completed...");
            }
        } finally {
            lock.unlock();
        }

        if (revealPayloadP1 != null && revealPayloadP2 != null && player1Id != null && player2Id != null) {
            // Gửi kèm availableCards nếu cần thiết
            // revealPayloadP1.setAvailableCards(finalAvailableCards);
            // revealPayloadP2.setAvailableCards(finalAvailableCards);
            notifyPlayer(player1Id, MessageProtocol.Type.GAME_ROUND_REVEAL, revealPayloadP1);
            notifyPlayer(player2Id, MessageProtocol.Type.GAME_ROUND_REVEAL, revealPayloadP2);
        }
        if (gameOver && gameSnapshotForEnd != null) {
            handleGameEnd(gameSnapshotForEnd);
            cleanupGame(matchId);
        } else if (!gameOver && player1Id != null) {
            scheduler.schedule(() -> startNextRound(matchId), 3, TimeUnit.SECONDS);
        } // Delay 3s
    }

    /**
     * Tạo payload reveal cho Player 2.
     */
    private RoundRevealDto createRevealForPlayer2(RoundRevealDto p1Reveal, int finalP1Score, int finalP2Score, int p2PointsEarned) {
        if (p1Reveal == null) return null;
        String p2Result = p1Reveal.getResult().equals("WIN") ? "LOSS" : (p1Reveal.getResult().equals("LOSS") ? "WIN" : "DRAW");
        return RoundRevealDto.builder()
                .gameId(p1Reveal.getGameId()).roundNumber(p1Reveal.getRoundNumber())
                .playerCard(p1Reveal.getOpponentCard()).opponentCard(p1Reveal.getPlayerCard())
                .playerAutoPicked(p1Reveal.getOpponentAutoPicked()) // Đã sửa
                .opponentAutoPicked(p1Reveal.getPlayerAutoPicked()) // Đã sửa
                .pointsEarned(p2PointsEarned).playerScore(finalP2Score).opponentScore(finalP1Score)
                .result(p2Result).build();
    }

    /**
     * Sao chép GameState để xử lý bất đồng bộ.
     */
    private GameState cloneGameState(GameState original) {
        if (original == null) return null;
        GameState copy = new GameState(original.getMatchId(), original.getPlayer1Id(), original.getPlayer2Id());
        copy.setPlayer1Score(original.getPlayer1Score());
        copy.setPlayer2Score(original.getPlayer2Score());
        copy.setComplete(original.isComplete());
        // Copy thêm history nếu cần
        // copy.roundHistory.addAll(original.getRoundHistory().stream().map(dto -> cloneRoundRevealDto(dto)).toList());
        return copy;
    }

    /**
     * Xử lý kết thúc game.
     */
    private void handleGameEnd(GameState completedGame) {
        System.out.println("Handling game end for match " + completedGame.getMatchId());
        String winnerId = getGameWinner(completedGame.getMatchId());
        // TODO: Gọi DB Stored Procedure update_user_stats_after_game(completedGame.getMatchId());
        Map<String, Object> gameEndPayload = new HashMap<>();
        gameEndPayload.put("matchId", completedGame.getMatchId());
        gameEndPayload.put("player1Score", completedGame.getPlayer1Score());
        gameEndPayload.put("player2Score", completedGame.getPlayer2Score());
        gameEndPayload.put("winnerId", winnerId);
        notifyPlayer(completedGame.getPlayer1Id(), MessageProtocol.Type.GAME_END, gameEndPayload);
        notifyPlayer(completedGame.getPlayer2Id(), MessageProtocol.Type.GAME_END, gameEndPayload);
    }

    /**
     * Gửi thông báo cho người chơi.
     */
    private void notifyPlayer(String userId, String messageType, Object payload) {
        ClientConnectionHandler handler = activeConnections.get(userId);
        if (handler != null) {
            try {
                MessageEnvelope envelope = MessageFactory.createNotification(messageType, payload);
                handler.sendMessage(JsonUtils.toJson(envelope));
            } catch (JsonProcessingException e) {
                System.err.println("❌ Error serializing notification [" + messageType + "]");
            }
        } else {
            System.err.println("⚠️ Cannot notify player " + userId + ": Handler not found.");
        }
    }

    /**
     * Dọn dẹp game khỏi bộ nhớ.
     */
    public void cleanupGame(String matchId) {
        activeGames.remove(matchId);
        gameLocks.remove(matchId); // Quan trọng: Xóa Lock
        System.out.println("🧹 Cleaned up game state for match " + matchId);
    }

    // --- Các hàm getter/helper khác ---
    public GameState getGameState(String matchId) {
        return activeGames.get(matchId);
    }

    public String formatCard(CardDto card) {
        return CardUtils.formatCard(card);
    }

    public String getGameWinner(String matchId) {
        GameState game = activeGames.get(matchId);
        if (game == null) return null;
        int winner = GameRuleUtils.getGameWinner(game.getPlayer1Score(), game.getPlayer2Score());
        return winner == 1 ? game.getPlayer1Id() : (winner == 2 ? game.getPlayer2Id() : null);
    }

    public boolean isGameOver(String matchId) {
        GameState game = activeGames.get(matchId);
        return game != null && game.isComplete();
    }

}