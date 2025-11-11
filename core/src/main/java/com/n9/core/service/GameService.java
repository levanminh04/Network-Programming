package com.n9.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.database.DatabaseManager;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.constants.GameConstants;
import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.PlayCardAckDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.model.enums.MatchResult;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.CardUtils;
import com.n9.shared.util.GameRuleUtils;
import com.n9.shared.util.JsonUtils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class GameService {

    private final DatabaseManager dbManager;
    private final ConcurrentHashMap<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Lock> gameLocks = new ConcurrentHashMap<>();

    private final SessionManager sessionManager;

    public GameService(DatabaseManager dbManager,
                       ConcurrentHashMap<String, ClientConnectionHandler> activeConnections,
                       ScheduledExecutorService scheduler,
                       SessionManager sessionManager) {
        this.dbManager = dbManager;
        this.activeConnections = activeConnections;
        this.scheduler = scheduler;
        this.sessionManager = sessionManager; // Thêm
    }


    public static class GameState {

        private final String matchId;
        private final String player1Id;
        private final String player2Id;
        private int player1Score = 0, player2Score = 0, currentRound = 0;
        private final List<RoundRevealDto> roundHistory = new ArrayList<>();
        private boolean isComplete = false;
        private List<CardDto> availableCards;
        private CardDto player1PlayedCard = null, player2PlayedCard = null;
        private boolean player1AutoPicked = false, player2AutoPicked = false;

        public GameState(String matchId, String p1, String p2) {
            this.matchId = matchId;
            this.player1Id = p1;
            this.player2Id = p2;
        }

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
     * Khởi tạo trận đấu (cả trong bộ nhớ và DB).
     */
    public GameState initializeGame(String matchId, String player1Id, String player2Id) {

        List<CardDto> fullShuffledDeck = CardUtils.generateDeck();
        CardUtils.shuffle(fullShuffledDeck);
        GameState game = new GameState(matchId, player1Id, player2Id);
        game.setAvailableCards(new ArrayList<>(fullShuffledDeck));
        game.setCurrentRound(0);
        activeGames.put(matchId, game);
        gameLocks.put(matchId, new ReentrantLock());
        try {
            persistNewGame(game);
        } catch (SQLException e) {
            return null;
        }
        Object payload1 = createGameStartPayload_SharedDeck(game, player1Id, getUsernameForId(player2Id));
        Object payload2 = createGameStartPayload_SharedDeck(game, player2Id, getUsernameForId(player1Id));
        notifyPlayer(player1Id, MessageProtocol.Type.GAME_START, payload1);
        notifyPlayer(player2Id, MessageProtocol.Type.GAME_START, payload2);
        startNextRound(matchId);
        return game;
    }

    /**
     * Tạo payload cho GAME_START.
     */
    private Object createGameStartPayload_SharedDeck(GameState game, String targetPlayerId, String opponentUsername) {
        boolean isPlayer1 = targetPlayerId.equals(game.getPlayer1Id());
        String opponentId = isPlayer1 ? game.getPlayer2Id() : game.getPlayer1Id();
        Map<String, Object> opponentInfo = new HashMap<>();
        opponentInfo.put("userId", opponentId);
        opponentInfo.put("username", opponentUsername);
        Map<String, Object> payload = new HashMap<>();
        payload.put("matchId", game.getMatchId());
        payload.put("initialAvailableCards", new ArrayList<>(game.getAvailableCards()));
        payload.put("opponent", opponentInfo);
        payload.put("yourPosition", isPlayer1 ? 1 : 2);
        return payload;
    }

    /**
     * Lock đảm bảo không một luồng nào khác (ví dụ: luồng playCard hoặc luồng handleTimeout của hiệp trước)
     * có thể xen vào giữa lúc bạn đang dọn dẹp và thiết lập các giá trị
     */
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

            long timeoutMillis = GameConstants.ROUND_TIMEOUT_SECONDS * 1000L;
            long deadlineTimestamp = System.currentTimeMillis() + timeoutMillis;
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId);
            payload.put("roundNumber", nextRound);
            payload.put("deadlineTimestamp", deadlineTimestamp);
            payload.put("durationMs", timeoutMillis);
            payload.put("availableCards", new ArrayList<>(game.getAvailableCards()));
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

            if (game == null || game.isComplete() || game.getCurrentRound() != roundNumber) {
                return;
            }

            CardDto p1Card = game.getPlayer1PlayedCard();
            CardDto p2Card = game.getPlayer2PlayedCard();
            CardDto pickedCard;
            // timeout XẢY RA không có nghĩa là CẢ 2 player CHƯA chọn bài
            if (p1Card == null) {
                pickedCard = autoPickCardInternal_SharedDeck(game);
                if (pickedCard != null) {
                    game.setPlayer1PlayedCard(pickedCard);
                    game.setPlayer1AutoPicked(true);
                }
            }
            if (p2Card == null) {
                pickedCard = autoPickCardInternal_SharedDeck(game);
                if (pickedCard != null) {
                    game.setPlayer2PlayedCard(pickedCard);
                    game.setPlayer2AutoPicked(true);
                }
            }
            if (game.getPlayer1PlayedCard() != null
                    && game.getPlayer2PlayedCard() != null)
            {
                triggerReveal = true;
            }
        } finally {
            lock.unlock();
        }
        if (triggerReveal) {
            executeRoundRevealAndProceed(matchId);
        }
    }


    // Chọn và xóa một lá bài ngẫu nhiên (bên trong lock).
    private CardDto autoPickCardInternal_SharedDeck(GameState game) {
        List<CardDto> available = game.getAvailableCards();
        if (CardUtils.isEmpty(available)) return null;
        CardDto pickedCard = CardUtils.pickRandomCard(available);
        if (pickedCard != null) {
            CardDto removedCard = CardUtils.removeCard(available, pickedCard.getCardId());
            if (removedCard == null) return null;
        }
        return pickedCard;
    }

    /**
     * Xử lý khi người chơi đánh bài.
     */
    public CardDto playCard(String matchId, String playerId, int cardId) throws Exception {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) throw new IllegalArgumentException("Game not found or ended: " + matchId);
        CardDto playedCard = null;
        boolean triggerReveal = false;
        boolean isPlayer1 = false;
        List<CardDto> currentAvailableCards = null;
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

            // ✅ FIX: Kiểm tra lá bài có trong availableCards TRƯỚC KHI xóa
            CardDto cardToCheck = CardUtils.findCard(game.getAvailableCards(), cardId);
            if (cardToCheck == null) {
                // Lá bài không tồn tại hoặc đã bị player khác chọn
                throw new IllegalArgumentException("Card " + cardId + " is not available or already played.");
            }

            // Bây giờ mới xóa (đảm bảo card tồn tại)
            playedCard = CardUtils.findAndRemoveCard(game.getAvailableCards(), cardId);
            if (playedCard == null) {
                // Defensive programming: Không bao giờ xảy ra vì đã check ở trên
                throw new IllegalStateException("Unexpected error: Card validation passed but removal failed.");
            }

            if (isPlayer1) {
                game.setPlayer1PlayedCard(playedCard);
                game.setPlayer1AutoPicked(false);
            } else {
                game.setPlayer2PlayedCard(playedCard);
                game.setPlayer2AutoPicked(false);
            }
            currentAvailableCards = new ArrayList<>(game.getAvailableCards()); // -1 card
            CardDto opponentCard = isPlayer1 ? game.getPlayer2PlayedCard() : game.getPlayer1PlayedCard();

            if (opponentCard != null) {
                triggerReveal = true;
            }
        } finally {
            lock.unlock();
        }

        PlayCardAckDto ackDto = new PlayCardAckDto();
        ackDto.setGameId(matchId);
        ackDto.setCardId(playedCard.getCardId());
        ackDto.setAvailableCards(currentAvailableCards);


        notifyPlayer(playerId, MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS, ackDto);
        if (triggerReveal) { // thằng pick cuối thì thằng đấy kích hoạt  executeRoundRevealAndProceed
            executeRoundRevealAndProceed(matchId);
        } else {
            GameState game = activeGames.get(matchId);
            if (game != null) {
                String opponentId = playerId.equals(game.getPlayer1Id()) ? game.getPlayer2Id() : game.getPlayer1Id();
                Map<String, Object> opponentReadyPayload = new HashMap<>();
                opponentReadyPayload.put("status", "READY");
                opponentReadyPayload.put("playedCardId", playedCard.getCardId());
                if (currentAvailableCards != null) opponentReadyPayload.put("availableCards", currentAvailableCards);
                notifyPlayer(opponentId, MessageProtocol.Type.GAME_OPPONENT_READY, opponentReadyPayload);
            }
        }
        return playedCard;
    }

    /**Thực thi lật bài, tính điểm, chuyển round hoặc kết thúc game.*/
    private void executeRoundRevealAndProceed(String matchId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;
        RoundRevealDto revealPayloadP1 = null, revealPayloadP2 = null;
        boolean gameOver = false;
        GameState gameSnapshotForEnd = null;
        String player1Id = null, player2Id = null;

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete() || game.getPlayer1PlayedCard() == null || game.getPlayer2PlayedCard() == null)
                return;
            player1Id = game.getPlayer1Id();
            player2Id = game.getPlayer2Id();
            CardDto p1Card = game.getPlayer1PlayedCard();
            CardDto p2Card = game.getPlayer2PlayedCard();
            boolean p1Auto = game.isPlayer1AutoPicked();
            boolean p2Auto = game.isPlayer2AutoPicked();
            int p1RoundScore = GameRuleUtils.calculateRoundPoints(p1Card, p2Card);
            int p2RoundScore = GameRuleUtils.calculateRoundPoints(p2Card, p1Card);
            game.setPlayer1Score(game.getPlayer1Score() + p1RoundScore);
            game.setPlayer2Score(game.getPlayer2Score() + p2RoundScore);
            try {
                persistRoundResult(game, p1Card, p2Card, p1RoundScore, p2RoundScore);
            } catch (SQLException e) {
            }
            revealPayloadP1 = RoundRevealDto.builder().
                    gameId(matchId).
                    roundNumber(game.getCurrentRound())
                    .playerCard(p1Card)
                    .opponentCard(p2Card)
                    .playerAutoPicked(p1Auto)
                    .opponentAutoPicked(p2Auto)
                    .pointsEarned(p1RoundScore)
                    .playerScore(game.getPlayer1Score())
                    .opponentScore(game.getPlayer2Score())
                    .result(p1RoundScore > p2RoundScore ? "WIN" :
                            (p2RoundScore > p1RoundScore ? "LOSS" : "DRAW"))
                    .build();

            game.addRoundResult(revealPayloadP1);

            revealPayloadP2 = RoundRevealDto.builder()
                    .gameId(matchId)
                    .roundNumber(game.getCurrentRound())
                    .playerCard(p2Card)          // Player 2 nhìn thấy bài của mình
                    .opponentCard(p1Card)        // và bài của đối thủ
                    .playerAutoPicked(p2Auto)
                    .opponentAutoPicked(p1Auto)
                    .pointsEarned(p2RoundScore)
                    .playerScore(game.getPlayer2Score())
                    .opponentScore(game.getPlayer1Score())
                    .result(p2RoundScore > p1RoundScore ? "WIN" :
                            (p1RoundScore > p2RoundScore ? "LOSS" : "DRAW"))
                    .build();

            if (game.getCurrentRound() >= GameConstants.TOTAL_ROUNDS) {
                game.setComplete(true);
                gameOver = true;
                gameSnapshotForEnd = cloneGameState(game);
            }
        } finally {
            lock.unlock();
        }
        if (revealPayloadP1 != null && revealPayloadP2 != null && player1Id != null && player2Id != null) {
            notifyPlayer(player1Id, MessageProtocol.Type.GAME_ROUND_REVEAL, revealPayloadP1);
            notifyPlayer(player2Id, MessageProtocol.Type.GAME_ROUND_REVEAL, revealPayloadP2);
        }
        if (gameOver && gameSnapshotForEnd != null) {
            handleGameEnd(gameSnapshotForEnd);
            cleanupGame(matchId);
        } else if (!gameOver && player1Id != null) {
            scheduler.schedule(() -> startNextRound(matchId), 3, TimeUnit.SECONDS);
        }
    }



    /* Sao chép GameState để xử lý bất đồng bộ. */
    private GameState cloneGameState(GameState original) {
        if (original == null) return null;
        GameState copy = new GameState(original.getMatchId(), original.getPlayer1Id(), original.getPlayer2Id());
        copy.setPlayer1Score(original.getPlayer1Score());
        copy.setPlayer2Score(original.getPlayer2Score());
        copy.setComplete(original.isComplete());
        return copy;
    }

    /* Xử lý kết thúc game (đã cập nhật logic DB). */
    private void handleGameEnd(GameState completedGame) {
        System.out.println("Handling game end for match " + completedGame.getMatchId());
        String winnerId = getGameWinner(completedGame.getMatchId());
        try (Connection conn = dbManager.getConnection()) {
            String sqlUpdate = "UPDATE games SET status = 'COMPLETED', winner_id = ?, player1_score = ?, player2_score = ?, completed_rounds = ?, completed_at = NOW() WHERE match_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                if (winnerId != null) stmt.setInt(1, Integer.parseInt(winnerId));
                else stmt.setNull(1, java.sql.Types.INTEGER);
                stmt.setInt(2, completedGame.getPlayer1Score());
                stmt.setInt(3, completedGame.getPlayer2Score());
                stmt.setInt(4, completedGame.getCurrentRound());
                stmt.setString(5, completedGame.getMatchId());
                stmt.executeUpdate();
            }
            String sqlCall = "{CALL update_user_stats_after_game(?)}";
            try (CallableStatement cstmt = conn.prepareCall(sqlCall)) {
                cstmt.setString(1, completedGame.getMatchId());
                cstmt.execute();
            }
            System.out.println("   Persisted final game result to DB for match: " + completedGame.getMatchId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Map<String, Object> gameEndPayload = new HashMap<>();
        gameEndPayload.put("matchId", completedGame.getMatchId());
        gameEndPayload.put("player1Score", completedGame.getPlayer1Score());
        gameEndPayload.put("player2Score", completedGame.getPlayer2Score());
        gameEndPayload.put("winnerId", winnerId);
        notifyPlayer(completedGame.getPlayer1Id(), MessageProtocol.Type.GAME_END, gameEndPayload);
        notifyPlayer(completedGame.getPlayer2Id(), MessageProtocol.Type.GAME_END, gameEndPayload);
    }


    /* Xử lý khi một người chơi bỏ cuộc (mất kết nối). */
    public void handleForfeit(String matchId, String forfeitingPlayerId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;
        GameState gameSnapshotForEnd = null;
        String winningPlayerId = null;
        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) return;
            System.out.println("Player " + forfeitingPlayerId + " forfeited match " + matchId);
            game.setComplete(true);
            winningPlayerId = forfeitingPlayerId.equals(game.getPlayer1Id()) ? game.getPlayer2Id() : game.getPlayer1Id();
            try (Connection conn = dbManager.getConnection()) {
                String sqlUpdate = "UPDATE games SET status = 'ABANDONED', winner_id = ?, completed_at = NOW() WHERE match_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setInt(1, Integer.parseInt(winningPlayerId));
                    stmt.setString(2, matchId);
                    stmt.executeUpdate();
                }
                String sqlCall = "{CALL update_user_stats_after_game(?)}";
                try (CallableStatement cstmt = conn.prepareCall(sqlCall)) {
                    cstmt.setString(1, matchId);
                    cstmt.execute();
                }
                System.out.println("   Persisted forfeit game result to DB for match: " + matchId);
            } catch (SQLException e) {

            }
            gameSnapshotForEnd = cloneGameState(game);
        } finally {
            lock.unlock();
        }

        if (winningPlayerId != null && gameSnapshotForEnd != null) {
            Map<String, Object> gameEndPayload = new HashMap<>();
            gameEndPayload.put("matchId", matchId);
            gameEndPayload.put("winnerId", winningPlayerId);
            gameEndPayload.put("forfeited", true);
            gameEndPayload.put("player1Score", gameSnapshotForEnd.getPlayer1Score());
            gameEndPayload.put("player2Score", gameSnapshotForEnd.getPlayer2Score());
            // CHỈ GỬI ĐẾN NGƯỜI THẮNG winningPlayerId (vì người kia đã disconnect)
            notifyPlayer(winningPlayerId, MessageProtocol.Type.GAME_END, gameEndPayload);
        }
        cleanupGame(matchId);
    }

    /* Gửi thông báo cho người chơi. */
    private void notifyPlayer(String userId, String messageType, Object payload) {
        ClientConnectionHandler handler = activeConnections.get(userId);
        if (handler != null) {
            try {
                MessageEnvelope envelope = MessageFactory.createNotification(messageType, payload);
                SessionManager.SessionContext context = sessionManager.getSessionByUserId(userId); // Cần hàm này
                if (context != null) {
                    envelope.setSessionId(context.getSessionId());
                }
                handler.sendMessage(JsonUtils.toJson(envelope));
            } catch (JsonProcessingException e) {

            }
        }
    }

    /* Dọn dẹp game khỏi bộ nhớ. */
    public void cleanupGame(String matchId) {
        // [1] Get game state BEFORE removing (to extract player IDs)
        GameState game = activeGames.get(matchId);
        
        // [2] Remove game state and lock
        activeGames.remove(matchId);
        gameLocks.remove(matchId);
        
        // [3] Clear currentMatchId from both players' SessionContext
        if (game != null) {
            String player1Id = game.getPlayer1Id();
            String player2Id = game.getPlayer2Id();
            
            if (player1Id != null) {
                SessionManager.SessionContext ctx1 = sessionManager.getSessionByUserId(player1Id);
                if (ctx1 != null) {
                    ctx1.setCurrentMatchId(null);
                    System.out.println("   ✅ Cleared matchId for player1: " + player1Id);
                }
            }
            
            if (player2Id != null) {
                SessionManager.SessionContext ctx2 = sessionManager.getSessionByUserId(player2Id);
                if (ctx2 != null) {
                    ctx2.setCurrentMatchId(null);
                    System.out.println("   ✅ Cleared matchId for player2: " + player2Id);
                }
            }
        }
        
        System.out.println("🧹 Cleaned up game state for match " + matchId);
    }

    /*  Lưu game mới vào DB. */
    private void persistNewGame(GameState game) throws SQLException {
        String sql = "INSERT INTO games (match_id, player1_id, player2_id, game_mode, total_rounds, status, started_at) " +
                "VALUES (?, ?, ?, 'QUICK', ?, 'IN_PROGRESS', NOW())";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, game.getMatchId());
            stmt.setInt(2, Integer.parseInt(game.getPlayer1Id()));
            stmt.setInt(3, Integer.parseInt(game.getPlayer2Id()));
            stmt.setInt(4, GameConstants.TOTAL_ROUNDS);
            stmt.executeUpdate();
        }
    }

    /* Lưu kết quả round vào DB. */
    private void persistRoundResult(GameState game, CardDto p1Card, CardDto p2Card, int p1RoundScore, int p2RoundScore) throws SQLException {
        String sql = "INSERT INTO game_rounds (match_id, round_number, " +
                "player1_card_id, player1_card_value, player1_is_auto_picked, " +
                "player2_card_id, player2_card_value, player2_is_auto_picked, " +
                "round_winner_id, player1_round_score, player2_round_score, completed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        String roundWinnerId = null;
        if (p1RoundScore > p2RoundScore) roundWinnerId = game.getPlayer1Id();
        else if (p2RoundScore > p1RoundScore) roundWinnerId = game.getPlayer2Id();
        try (Connection conn = dbManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, game.getMatchId());
            stmt.setInt(2, game.getCurrentRound());
            stmt.setInt(3, p1Card.getCardId());
            stmt.setInt(4, p1Card.getValue());
            stmt.setBoolean(5, game.isPlayer1AutoPicked());
            stmt.setInt(6, p2Card.getCardId());
            stmt.setInt(7, p2Card.getValue());
            stmt.setBoolean(8, game.isPlayer2AutoPicked());
            if (roundWinnerId != null) stmt.setInt(9, Integer.parseInt(roundWinnerId));
            else stmt.setNull(9, java.sql.Types.INTEGER);
            stmt.setInt(10, p1RoundScore);
            stmt.setInt(11, p2RoundScore);
            stmt.executeUpdate();
        }
    }

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

    public List<CardDto> getPlayerHand(String matchId, String playerId) {
        return Collections.emptyList();
    }

    public MatchResult getGameResult(String matchId, String playerId) { /* Logic cũ OK */
        return null;
    }

    private String getUsernameForId(String userId) {
        if (sessionManager == null) return "Unknown";
        // Cần hàm tra cứu ngược từ SessionManager
        SessionManager.SessionContext ctx = sessionManager.getSessionByUserId(userId);
        if (ctx != null) {
            return ctx.getUsername();
        }
        return "Unknown";
    }
}

