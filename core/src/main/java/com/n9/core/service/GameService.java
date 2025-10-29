package com.n9.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.constants.GameConstants;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.PlayCardAckDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.model.enums.MatchResult; // Giữ lại nếu dùng
import com.n9.shared.util.CardUtils;
import com.n9.shared.util.GameRuleUtils;
import com.n9.shared.util.JsonUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Game Service - Core business logic for card game.
 * Implements "Shared Deck" logic where players pick from a common pool.
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

        // --- THAY THẾ BẰNG BỘ BÀI CHUNG ---
        private List<CardDto> availableCards; // Danh sách các lá bài còn lại có thể chọn
        // ------------------------------------

        // --- CÁC TRƯỜNG CHO ROUND HIỆN TẠI ---
        private CardDto player1PlayedCard = null;
        private CardDto player2PlayedCard = null;
        private boolean player1AutoPicked = false;
        private boolean player2AutoPicked = false;
        // ------------------------------------

        public GameState(String matchId, String player1Id, String player2Id) {
            this.matchId = matchId;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
        }

        // Getters (Thêm/Sửa)
        public String getMatchId() { return matchId; }
        public String getPlayer1Id() { return player1Id; }
        public String getPlayer2Id() { return player2Id; }
        public List<CardDto> getAvailableCards() { return availableCards; } // <-- Get bộ bài chung
        public int getPlayer1Score() { return player1Score; }
        public int getPlayer2Score() { return player2Score; }
        public int getCurrentRound() { return currentRound; }
        public List<RoundRevealDto> getRoundHistory() { return roundHistory; }
        public boolean isComplete() { return isComplete; }
        public CardDto getPlayer1PlayedCard() { return player1PlayedCard; }
        public CardDto getPlayer2PlayedCard() { return player2PlayedCard; }
        public boolean isPlayer1AutoPicked() { return player1AutoPicked; }
        public boolean isPlayer2AutoPicked() { return player2AutoPicked; }

        // Setters (Thêm/Sửa)
        public void setAvailableCards(List<CardDto> cards) { this.availableCards = cards; } // <-- Set bộ bài chung
        public void setPlayer1Score(int score) { this.player1Score = score; }
        public void setPlayer2Score(int score) { this.player2Score = score; }
        public void setCurrentRound(int round) { this.currentRound = round; }
        public void setComplete(boolean complete) { this.isComplete = complete; }
        public void setPlayer1PlayedCard(CardDto card) { this.player1PlayedCard = card; }
        public void setPlayer2PlayedCard(CardDto card) { this.player2PlayedCard = card; }
        public void setPlayer1AutoPicked(boolean autoPicked) { this.player1AutoPicked = autoPicked; }
        public void setPlayer2AutoPicked(boolean autoPicked) { this.player2AutoPicked = autoPicked; }

        public void addRoundResult(RoundRevealDto reveal) { roundHistory.add(reveal); }
    }


    /**
     * Khởi tạo trận đấu với bộ bài chung.
     */
    public GameState initializeGame(String matchId, String player1Id, String player2Id) {
        System.out.println("🚀 Initializing game (Shared Deck) for match: " + matchId);

        List<CardDto> fullShuffledDeck = CardUtils.generateDeck();
        CardUtils.shuffle(fullShuffledDeck);

        GameState game = new GameState(matchId, player1Id, player2Id);
        // THAY ĐỔI: Lưu bộ bài chung
        game.setAvailableCards(new ArrayList<>(fullShuffledDeck));
        game.setCurrentRound(0);

        activeGames.put(matchId, game);
        gameLocks.put(matchId, new ReentrantLock());
        System.out.println("   Created lock for match: " + matchId);

        // THAY ĐỔI: Payload GAME_START không còn 'yourHand'
        Object payload1 = createGameStartPayload_SharedDeck(game, player1Id, "Player 2"); // Tạm
        Object payload2 = createGameStartPayload_SharedDeck(game, player2Id, "Player 1");

        notifyPlayer(player1Id, MessageProtocol.Type.GAME_START, payload1);
        notifyPlayer(player2Id, MessageProtocol.Type.GAME_START, payload2);
        System.out.println("   Sent GAME_START notifications (Shared Deck).");

        startNextRound(matchId);
        return game;
    }

    private Object createGameStartPayload_SharedDeck(GameState game, String targetPlayerId, String opponentUsername) {
        boolean isPlayer1 = targetPlayerId.equals(game.getPlayer1Id());
        String opponentId = isPlayer1 ? game.getPlayer2Id() : game.getPlayer1Id();

        Map<String, Object> opponentInfo = new HashMap<>();
        opponentInfo.put("userId", opponentId);
        opponentInfo.put("username", opponentUsername);

        Map<String, Object> payload = new HashMap<>();
        payload.put("matchId", game.getMatchId());
        // Có thể gửi danh sách bài ban đầu nếu Frontend cần để hiển thị
        // payload.put("initialAvailableCards", new ArrayList<>(game.getAvailableCards()));
        payload.put("opponent", opponentInfo);
        payload.put("yourPosition", isPlayer1 ? 1 : 2);
        return payload;
    }

    public void startNextRound(String matchId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) return;

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
            // THAY ĐỔI: Gửi danh sách bài còn lại cho client cập nhật UI
            payload.put("availableCards", new ArrayList<>(game.getAvailableCards())); // Gửi bản copy

            notifyPlayer(game.getPlayer1Id(), MessageProtocol.Type.GAME_ROUND_START, payload);
            notifyPlayer(game.getPlayer2Id(), MessageProtocol.Type.GAME_ROUND_START, payload);

            scheduler.schedule(() -> handleRoundTimeout(matchId, nextRound), timeoutMillis, TimeUnit.MILLISECONDS);
            System.out.println("   Scheduled timeout for round " + nextRound + " in " + timeoutMillis + " ms.");

        } finally {
            lock.unlock();
        }
    }

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
                // THAY ĐỔI: Gọi autoPick từ availableCards
                pickedCard = autoPickCardInternal_SharedDeck(game);
                if(pickedCard != null) {
                    game.setPlayer1PlayedCard(pickedCard);
                    game.setPlayer1AutoPicked(true);
                    System.out.println("   Auto-picked for player " + game.getPlayer1Id() + ": " + formatCard(pickedCard));
                } else { System.err.println("   ERROR: Failed to auto-pick for player " + game.getPlayer1Id()); }
            }
            // Đảm bảo không auto-pick trùng lá P1 vừa auto-pick
            if (p2Card == null) {
                pickedCard = autoPickCardInternal_SharedDeck(game);
                if(pickedCard != null) {
                    game.setPlayer2PlayedCard(pickedCard);
                    game.setPlayer2AutoPicked(true);
                    System.out.println("   Auto-picked for player " + game.getPlayer2Id() + ": " + formatCard(pickedCard));
                } else { System.err.println("   ERROR: Failed to auto-pick for player " + game.getPlayer2Id()); }
            }

            if(game.getPlayer1PlayedCard() != null && game.getPlayer2PlayedCard() != null) {
                triggerReveal = true;
            }

        } finally {
            lock.unlock();
        }

        if (triggerReveal) {
            executeRoundRevealAndProceed(matchId);
        }
    }

    // THAY ĐỔI: Hàm autoPick nội bộ cho Shared Deck
    private CardDto autoPickCardInternal_SharedDeck(GameState game) {
        List<CardDto> available = game.getAvailableCards();
        if (CardUtils.isEmpty(available)) return null;

        CardDto pickedCard = CardUtils.pickRandomCard(available);
        if (pickedCard != null) {
            // Xóa lá bài đã chọn khỏi availableCards
            CardDto removedCard = CardUtils.removeCard(available, pickedCard.getCardId());
            if (removedCard == null) {
                System.err.println("CRITICAL ERROR: Failed to remove auto-picked card " + pickedCard.getCardId() + " from available deck.");
                return null;
            }
        }
        return pickedCard;
    }

    // THAY ĐỔI: Hàm playCard cho Shared Deck
    public CardDto playCard(String matchId, String playerId, int cardId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) throw new IllegalArgumentException("Game not found or ended: " + matchId);

        CardDto playedCard = null;
        boolean triggerReveal = false;
        boolean isPlayer1 = false; // Biến cục bộ để dùng ngoài try-finally

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) throw new IllegalArgumentException("Game not found or ended: " + matchId);
            if (game.getCurrentRound() == 0 || game.getCurrentRound() > GameConstants.TOTAL_ROUNDS) throw new IllegalArgumentException("Cannot play outside active rounds.");

            isPlayer1 = playerId.equals(game.getPlayer1Id()); // Gán giá trị

            // Kiểm tra đã chơi round này chưa
            if ((isPlayer1 && game.getPlayer1PlayedCard() != null) || (!isPlayer1 && game.getPlayer2PlayedCard() != null)) {
                throw new IllegalArgumentException("Already played this round.");
            }

            // Kiểm tra và xóa bài khỏi availableCards
            // Sử dụng findAndRemoveCard cho gọn
            playedCard = CardUtils.findAndRemoveCard(game.getAvailableCards(), cardId);
            if (playedCard == null) {
                throw new IllegalArgumentException("Card " + cardId + " is not available or already played.");
            }

            System.out.println("🃏 Player " + playerId + " played available card " + formatCard(playedCard) + " in round " + game.getCurrentRound());

            // Lưu lựa chọn
            if (isPlayer1) {
                game.setPlayer1PlayedCard(playedCard);
                game.setPlayer1AutoPicked(false);
            } else {
                game.setPlayer2PlayedCard(playedCard);
                game.setPlayer2AutoPicked(false);
            }

            // Kiểm tra đối thủ
            CardDto opponentCard = isPlayer1 ? game.getPlayer2PlayedCard() : game.getPlayer1PlayedCard();
            if (opponentCard != null) {
                triggerReveal = true;
            }

        } finally {
            lock.unlock();
        }

        // --- Hành động I/O bên ngoài Lock ---
        PlayCardAckDto ackDto = new PlayCardAckDto();
        ackDto.setGameId(matchId);
        ackDto.setCardId(playedCard.getCardId());
        // THAY ĐỔI: Gửi kèm danh sách bài còn lại để client cập nhật
        GameState currentGameState = activeGames.get(matchId); // Lấy lại state mới nhất (có thể null)
        if (currentGameState != null) {
            ackDto.setAvailableCards(new ArrayList<>(currentGameState.getAvailableCards())); // Gửi bản copy
        }
        notifyPlayer(playerId, MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS, ackDto);

        if (triggerReveal) {
            executeRoundRevealAndProceed(matchId);
        } else {
            GameState game = activeGames.get(matchId);
            if (game != null) {
                String opponentId = playerId.equals(game.getPlayer1Id()) ? game.getPlayer2Id() : game.getPlayer1Id();
                // THAY ĐỔI: Gửi kèm lá bài vừa chơi để client kia vô hiệu hóa
                Map<String, Object> opponentReadyPayload = new HashMap<>();
                opponentReadyPayload.put("status", "READY");
                opponentReadyPayload.put("playedCardId", playedCard.getCardId()); // Gửi ID lá bài đã chơi
                if (currentGameState != null) { // Gửi cả available cards cập nhật
                    opponentReadyPayload.put("availableCards", new ArrayList<>(currentGameState.getAvailableCards()));
                }
                notifyPlayer(opponentId, MessageProtocol.Type.GAME_OPPONENT_READY, opponentReadyPayload);
            }
        }

        return playedCard;
    }


    private void executeRoundRevealAndProceed(String matchId) {
        // ... (Giữ nguyên logic tính điểm, gửi reveal, kiểm tra game end, start next round) ...
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;
        RoundRevealDto revealPayloadP1 = null, revealPayloadP2 = null;
        boolean gameOver = false;
        GameState gameSnapshotForEnd = null;
        String player1Id = null, player2Id = null;
        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete() || game.getPlayer1PlayedCard() == null || game.getPlayer2PlayedCard() == null) return;
            player1Id = game.getPlayer1Id(); player2Id = game.getPlayer2Id();
            System.out.println("✨ Revealing round " + game.getCurrentRound() + " for match " + matchId);
            CardDto p1Card = game.getPlayer1PlayedCard(); CardDto p2Card = game.getPlayer2PlayedCard();
            boolean p1Auto = game.isPlayer1AutoPicked(); boolean p2Auto = game.isPlayer2AutoPicked();
            int p1RoundScore = GameRuleUtils.calculateRoundPoints(p1Card, p2Card); int p2RoundScore = GameRuleUtils.calculateRoundPoints(p2Card, p1Card);
            game.setPlayer1Score(game.getPlayer1Score() + p1RoundScore); game.setPlayer2Score(game.getPlayer2Score() + p2RoundScore);
            revealPayloadP1 = RoundRevealDto.builder() /* ... */ .build();
            game.addRoundResult(revealPayloadP1);
            revealPayloadP2 = createRevealForPlayer2(revealPayloadP1, game.getPlayer1Score(), game.getPlayer2Score(), p2RoundScore);
            if (game.getCurrentRound() >= GameConstants.TOTAL_ROUNDS) {
                game.setComplete(true); gameOver = true; gameSnapshotForEnd = cloneGameState(game);
                System.out.println("🏁 Game " + matchId + " completed...");
            }
        } finally { lock.unlock(); }
        if (revealPayloadP1 != null && revealPayloadP2 != null && player1Id != null && player2Id != null) {
            // THAY ĐỔI: Gửi kèm availableCards trong reveal nếu cần
            // revealPayloadP1.setAvailableCards(getAvailableCardsSnapshot(matchId)); // Cần hàm helper
            // revealPayloadP2.setAvailableCards(getAvailableCardsSnapshot(matchId));
            notifyPlayer(player1Id, MessageProtocol.Type.GAME_ROUND_REVEAL, revealPayloadP1);
            notifyPlayer(player2Id, MessageProtocol.Type.GAME_ROUND_REVEAL, revealPayloadP2);
        }
        if (gameOver && gameSnapshotForEnd != null) {
            handleGameEnd(gameSnapshotForEnd); cleanupGame(matchId);
        } else if (!gameOver && player1Id != null) {
            scheduler.schedule(() -> startNextRound(matchId), 3, TimeUnit.SECONDS);
        }
    }

    // Hàm clone GameState (cần thiết)
    private GameState cloneGameState(GameState original) { /* ... */ return null;} // Giữ nguyên

    private RoundRevealDto createRevealForPlayer2(RoundRevealDto p1Reveal, int finalP1Score, int finalP2Score, int p2PointsEarned) { /*...*/ return null; } // Giữ nguyên

    private void handleGameEnd(GameState completedGame) { /*...*/ } // Giữ nguyên

    private void notifyPlayer(String userId, String messageType, Object payload) { /*...*/ } // Giữ nguyên

    // Các hàm còn lại...
    public void cleanupGame(String matchId) { activeGames.remove(matchId); gameLocks.remove(matchId); System.out.println("🧹 Cleaned up game state for match " + matchId);}
    public GameState getGameState(String matchId) { return activeGames.get(matchId); }
    public String formatCard(CardDto card) { return CardUtils.formatCard(card); }

}