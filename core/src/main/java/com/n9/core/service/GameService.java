package com.n9.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.PlayCardAckDto;
import com.n9.shared.model.dto.game.RoundRevealDto;
import com.n9.shared.model.enums.MatchResult;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.CardUtils;
import com.n9.shared.util.GameRuleUtils;
import com.n9.shared.constants.GameConstants;
import com.n9.shared.util.JsonUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private final ConcurrentHashMap<String, GameState> activeGames = new ConcurrentHashMap<>();

    // THÊM: Dependencies được inject từ CoreServer
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private final ScheduledExecutorService scheduler;

    // THÊM: Map để quản lý Lock cho từng trận đấu -> Đồng bộ hóa chi tiết hơn
    private final ConcurrentHashMap<String, Lock> gameLocks = new ConcurrentHashMap<>();

    // THAY ĐỔI: Constructor để nhận dependencies
    public GameService(ConcurrentHashMap<String, ClientConnectionHandler> activeConnections, ScheduledExecutorService scheduler) {
        this.activeConnections = activeConnections;
        this.scheduler = scheduler;
    }



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

        // --- CÁC TRƯỜNG CHO ROUND HIỆN TẠI ---
        private CardDto player1PlayedCard = null;
        private CardDto player2PlayedCard = null;
        private boolean player1AutoPicked = false;
        private boolean player2AutoPicked = false;


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

        public CardDto getPlayer1PlayedCard() {
            return player1PlayedCard;
        }

        public void setPlayer1PlayedCard(CardDto player1PlayedCard) {
            this.player1PlayedCard = player1PlayedCard;
        }

        public CardDto getPlayer2PlayedCard() {
            return player2PlayedCard;
        }

        public void setPlayer2PlayedCard(CardDto player2PlayedCard) {
            this.player2PlayedCard = player2PlayedCard;
        }

        public boolean isPlayer1AutoPicked() {
            return player1AutoPicked;
        }

        public void setPlayer1AutoPicked(boolean player1AutoPicked) {
            this.player1AutoPicked = player1AutoPicked;
        }

        public boolean isPlayer2AutoPicked() {
            return player2AutoPicked;
        }

        public void setPlayer2AutoPicked(boolean player2AutoPicked) {
            this.player2AutoPicked = player2AutoPicked;
        }
    }
    

    /**
     * Initialize a new game
     * 
     * @param matchId Unique match identifier
     * @param player1Id First player ID
     * @param player2Id Second player ID
     * @return GameState with dealt hands
     */
    public GameState initializeGame(String matchId, String player1Id, String player2Id) {
        System.out.println("🚀 Initializing game for match: " + matchId + " between " + player1Id + " and " + player2Id);

        // 1. Xáo bài và chia bài
        List<CardDto> deck = CardUtils.generateDeck();
        CardUtils.shuffle(deck);
        List<CardDto>[] hands = CardUtils.dealForGame(deck); // Chia 3 lá cho mỗi người

        // 2. Tạo đối tượng trạng thái game (GameState)
        GameState game = new GameState(matchId, player1Id, player2Id);
        game.setPlayer1Hand(new ArrayList<>(hands[0]));
        game.setPlayer2Hand(new ArrayList<>(hands[1]));
        game.setCurrentRound(0); // Sẽ tăng lên 1 khi bắt đầu round đầu tiên

        // 3. Lưu GameState vào bộ nhớ
        activeGames.put(matchId, game);

        // 4. Tạo Lock riêng cho trận đấu này để đảm bảo thread-safety
        gameLocks.put(matchId, new ReentrantLock());
        System.out.println("   Created lock for match: " + matchId);

        // 5. Chuẩn bị và gửi tin nhắn GAME_START cho từng người chơi
        // Giả sử bạn có thông tin username từ MatchmakingService hoặc SessionManager
        // String player1Username = getUsernameForId(player1Id); // Hàm helper giả định
        // String player2Username = getUsernameForId(player2Id);
        Object payload1 = createGameStartPayload(game, player1Id, "Player 2"); // Tạm dùng tên mặc định
        Object payload2 = createGameStartPayload(game, player2Id, "Player 1"); // Tạm dùng tên mặc định

        notifyPlayer(player1Id, MessageProtocol.Type.GAME_START, payload1);
        notifyPlayer(player2Id, MessageProtocol.Type.GAME_START, payload2);
        System.out.println("   Sent GAME_START notifications to both players.");

        // 6. Bắt đầu Round 1 ngay lập tức
        startNextRound(matchId); // Hàm này sẽ gửi GAME_ROUND_START

        return game;
    }

    private Object createGameStartPayload(GameState game, String targetPlayerId, String opponentUsername) {
        boolean isPlayer1 = targetPlayerId.equals(game.getPlayer1Id());

        List<CardDto> yourHand = isPlayer1 ? game.getPlayer1Hand() : game.getPlayer2Hand();
        String opponentId = isPlayer1 ? game.getPlayer2Id() : game.getPlayer1Id();

        // Tạo thông tin đối thủ
        Map<String, Object> opponentInfo = new HashMap<>();
        opponentInfo.put("userId", opponentId);
        opponentInfo.put("username", opponentUsername); // Sử dụng username được truyền vào
        // opponentInfo.put("score", getOpponentScore(opponentId)); // Lấy score từ DB nếu cần

        // Tạo payload cuối cùng
        Map<String, Object> payload = new HashMap<>();
        payload.put("matchId", game.getMatchId());
        payload.put("yourHand", yourHand);
        payload.put("opponent", opponentInfo);
        payload.put("yourPosition", isPlayer1 ? 1 : 2); // Cho client biết họ là P1 hay P2

        return payload;
    }


    private void notifyPlayer(String userId, String messageType, Object payload) {
        ClientConnectionHandler handler = activeConnections.get(userId);
        if (handler != null) {
            try {
                MessageEnvelope envelope = MessageFactory.createNotification(messageType, payload);
                // Gắn userId hoặc sessionId nếu cần thiết ở tầng Gateway/Client
                // envelope.setUserId(userId); // Gắn userId để client biết tin này là của mình (nếu cần)
                handler.sendMessage(JsonUtils.toJson(envelope));
            } catch (JsonProcessingException e) {
                System.err.println("❌ Error serializing notification [" + messageType + "] for user " + userId + ": " + e.getMessage());
            }
        } else {
            System.err.println("⚠️ Cannot notify player " + userId + " [" + messageType + "]: Handler not found (disconnected?).");
            // TODO: Xử lý forfeit nếu cần thiết tại đây hoặc ở logic game chính
        }
    }

    // ... (Các hàm còn lại: startNextRound, playCard, executeRoundRevealAndProceed, ...)
    // Bạn cần đảm bảo hàm startNextRound(matchId) được triển khai để bắt đầu round 1.
// --- HÀM startNextRound ---
    public void startNextRound(String matchId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) {
            System.err.println("⚠️ Cannot start next round for " + matchId + ": Game lock not found.");
            return;
        }

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) {
                System.out.println("🏁 Cannot start next round for " + matchId + ": Game not found or completed.");
                return;
            }

            // 1. Tăng round và Reset trạng thái
            int nextRound = game.getCurrentRound() + 1;
            game.setCurrentRound(nextRound);
            game.setPlayer1PlayedCard(null); // Reset lựa chọn
            game.setPlayer2PlayedCard(null);
            game.setPlayer1AutoPicked(false); // Reset cờ auto-pick
            game.setPlayer2AutoPicked(false);

            System.out.println("⏱️ Starting Round " + nextRound + " for Match " + matchId);

            // 2. Chuẩn bị payload GAME.ROUND_START
            // THAY ĐỔI: Sử dụng GameConstants.ROUND_TIMEOUT_SECONDS * 1000
            long timeoutMillis = GameConstants.ROUND_TIMEOUT_SECONDS * 1000L;
            long deadlineTimestamp = System.currentTimeMillis() + timeoutMillis;

            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId);
            payload.put("roundNumber", nextRound);
            payload.put("deadlineTimestamp", deadlineTimestamp);
            payload.put("durationMs", timeoutMillis); // Gửi duration

            // 3. Gửi thông báo
            notifyPlayer(game.getPlayer1Id(), MessageProtocol.Type.GAME_ROUND_START, payload);
            notifyPlayer(game.getPlayer2Id(), MessageProtocol.Type.GAME_ROUND_START, payload);

            // 4. Hẹn giờ timeout
            scheduler.schedule(() -> {
                handleRoundTimeout(matchId, nextRound);
            }, timeoutMillis, TimeUnit.MILLISECONDS);

            System.out.println("   Scheduled timeout for round " + nextRound + " in " + timeoutMillis + " ms.");

        } finally {
            lock.unlock();
        }
    }


    // --- HÀM handleRoundTimeout ---
    private void handleRoundTimeout(String matchId, int roundNumber) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;

        boolean triggerReveal = false;
        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete() || game.getCurrentRound() != roundNumber) {
                return; // Timeout không hợp lệ
            }
            System.out.println("⏰ Timeout triggered for round " + roundNumber + " in match " + matchId);

            CardDto p1Card = game.getPlayer1PlayedCard();
            CardDto p2Card = game.getPlayer2PlayedCard();
            CardDto pickedCard;

            if (p1Card == null) {
                pickedCard = autoPickCardInternal(game, game.getPlayer1Id());
                if(pickedCard != null) {
                    game.setPlayer1PlayedCard(pickedCard);
                    game.setPlayer1AutoPicked(true);
                    System.out.println("   Auto-picked for player " + game.getPlayer1Id() + ": " + formatCard(pickedCard));
                } else { System.err.println("   ERROR: Failed to auto-pick for player " + game.getPlayer1Id()); }
            }
            if (p2Card == null) {
                pickedCard = autoPickCardInternal(game, game.getPlayer2Id());
                if(pickedCard != null) {
                    game.setPlayer2PlayedCard(pickedCard);
                    game.setPlayer2AutoPicked(true);
                    System.out.println("   Auto-picked for player " + game.getPlayer2Id() + ": " + formatCard(pickedCard));
                } else { System.err.println("   ERROR: Failed to auto-pick for player " + game.getPlayer2Id()); }
            }

            // Trigger reveal nếu cả hai đã có bài
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


    // --- HÀM autoPickCardInternal ---
    // Hàm này được gọi BÊN TRONG lock, nó cần xóa lá bài khỏi tay
// --- HÀM autoPickCardInternal ---
    // Hàm này được gọi BÊN TRONG lock, nó cần xóa lá bài khỏi tay
    private CardDto autoPickCardInternal(GameState game, String playerId) {
        List<CardDto> hand = playerId.equals(game.getPlayer1Id()) ? game.getPlayer1Hand() : game.getPlayer2Hand();
        if (CardUtils.isEmpty(hand)) { return null; }

        // Chọn ngẫu nhiên một lá bài (chưa xóa)
        CardDto pickedCard = CardUtils.pickRandomCard(hand);
        if (pickedCard == null) {
            // Trường hợp hiếm gặp nếu pickRandomCard thất bại dù hand không empty
            System.err.println("CRITICAL ERROR: pickRandomCard returned null despite non-empty hand for player " + playerId);
            return null;
        }

        // THAY ĐỔI: Gọi removeCard và lưu kết quả vào biến CardDto
        CardDto removedCard = CardUtils.removeCard(hand, pickedCard.getCardId());

        // THAY ĐỔI: Kiểm tra xem việc xóa có thành công không (removedCard khác null)
        if (removedCard == null) {
            // Lỗi logic không mong muốn: Tìm thấy lá bài để chọn nhưng không xóa được?
            System.err.println("CRITICAL ERROR: Failed to remove auto-picked card " + pickedCard.getCardId() + " from hand of player " + playerId + ". Hand state might be inconsistent.");
            // Xem xét cách xử lý lỗi này, ví dụ: thử chọn lá khác hoặc ném exception
            return null; // Tạm thời trả về null
        }

        // Nếu xóa thành công, trả về lá bài đã được chọn và xóa
        return pickedCard; // Hoặc trả về removedCard, chúng nên là cùng một object
    }


    // --- HÀM playCard ---
    public CardDto playCard(String matchId, String playerId, int cardId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) throw new IllegalArgumentException("Game not found or already ended: " + matchId);

        CardDto playedCard = null;
        boolean triggerReveal = false;

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) {
                throw new IllegalArgumentException("Game not found or already ended: " + matchId);
            }
            if (game.getCurrentRound() == 0 || game.getCurrentRound() > GameConstants.TOTAL_ROUNDS) {
                throw new IllegalArgumentException("Cannot play card outside of active rounds.");
            }

            List<CardDto> hand;
            boolean isPlayer1 = playerId.equals(game.getPlayer1Id());

            // Kiểm tra xem đã chơi round này chưa
            if (isPlayer1) {
                if (game.getPlayer1PlayedCard() != null) throw new IllegalArgumentException("Already played this round.");
                hand = game.getPlayer1Hand();
            } else {
                if (game.getPlayer2PlayedCard() != null) throw new IllegalArgumentException("Already played this round.");
                hand = game.getPlayer2Hand();
            }

            // THAY ĐỔI: Sử dụng findCard và removeCard
            playedCard = CardUtils.findCard(hand, cardId);
            if (playedCard == null) {
                throw new IllegalArgumentException("Card " + cardId + " not in player's hand.");
            }
            // Nếu tìm thấy thì mới xóa

            CardDto removed = CardUtils.removeCard(hand, cardId);
            if (removed == null) { // Kiểm tra xem có xóa được không
                System.err.println("CRITICAL ERROR: Found card but failed to remove it. CardId: " + cardId + ", Player: " + playerId);
                throw new IllegalStateException("Failed to remove card after finding it.");
            }

            System.out.println("🃏 Player " + playerId + " played card " + formatCard(playedCard) + " in round " + game.getCurrentRound());

            if (isPlayer1) {
                game.setPlayer1PlayedCard(playedCard);
                game.setPlayer1AutoPicked(false);
            } else {
                game.setPlayer2PlayedCard(playedCard);
                game.setPlayer2AutoPicked(false);
            }

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
        ackDto.setCardId(playedCard.getCardId()); // playedCard không thể null ở đây
        notifyPlayer(playerId, MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS, ackDto);

        if (triggerReveal) {
            executeRoundRevealAndProceed(matchId);
        } else {
            GameState game = activeGames.get(matchId);
            if (game != null) {
                String opponentId = playerId.equals(game.getPlayer1Id()) ? game.getPlayer2Id() : game.getPlayer1Id();
                Map<String, String> opponentReadyPayload = Map.of("status", "READY");
                notifyPlayer(opponentId, MessageProtocol.Type.GAME_OPPONENT_READY, opponentReadyPayload);
            }
        }

        return playedCard;
    }


    // --- HÀM executeRoundRevealAndProceed ---
    private void executeRoundRevealAndProceed(String matchId) {
        Lock lock = gameLocks.get(matchId);
        if (lock == null) return;

        RoundRevealDto revealPayloadP1 = null; // Payload cho P1
        RoundRevealDto revealPayloadP2 = null; // Payload cho P2
        boolean gameOver = false;
        GameState gameSnapshotForEnd = null;
        String player1Id = null, player2Id = null;

        lock.lock();
        try {
            GameState game = activeGames.get(matchId);
            if (game == null || game.isComplete()) return;
            if (game.getPlayer1PlayedCard() == null || game.getPlayer2PlayedCard() == null) {
                System.err.println("Attempted reveal for match " + matchId + " too early.");
                return;
            }

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

            // Tạo payload P1
            revealPayloadP1 = RoundRevealDto.builder()
                    .gameId(matchId)
                    .roundNumber(game.getCurrentRound())
                    .playerCard(p1Card)
                    .opponentCard(p2Card)
                    .playerAutoPicked(p1Auto)
                    .opponentAutoPicked(p2Auto)
                    .pointsEarned(p1RoundScore)
                    .playerScore(game.getPlayer1Score())
                    .opponentScore(game.getPlayer2Score())
                    .result(p1RoundScore > p2RoundScore ? "WIN" : (p2RoundScore > p1RoundScore ? "LOSS" : "DRAW"))
                    .build();
            game.addRoundResult(revealPayloadP1); // Chỉ cần lưu 1 bản là đủ

            // Tạo payload P2 từ payload P1
            revealPayloadP2 = createRevealForPlayer2(revealPayloadP1, game.getPlayer1Score(), game.getPlayer2Score(), p2RoundScore); // Truyền điểm mới nhất

            if (game.getCurrentRound() >= GameConstants.TOTAL_ROUNDS) {
                game.setComplete(true);
                gameOver = true;
                gameSnapshotForEnd = game;
                System.out.println("🏁 Game " + matchId + " completed. Final Score: P1=" + game.getPlayer1Score() + ", P2=" + game.getPlayer2Score());
            }

        } finally {
            lock.unlock();
        }

        // --- Gửi thông báo ---
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

    // --- HÀM createRevealForPlayer2 ---
    private RoundRevealDto createRevealForPlayer2(RoundRevealDto p1Reveal, int finalP1Score, int finalP2Score, int p2PointsEarned) {
        if (p1Reveal == null) return null;

        String p2Result = p1Reveal.getResult().equals("WIN") ? "LOSS" : (p1Reveal.getResult().equals("LOSS") ? "WIN" : "DRAW");

        return RoundRevealDto.builder()
                .gameId(p1Reveal.getGameId())
                .roundNumber(p1Reveal.getRoundNumber())
                .playerCard(p1Reveal.getOpponentCard()) // Bài của P2
                .opponentCard(p1Reveal.getPlayerCard())   // Bài của P1
                .playerAutoPicked(p1Reveal.getOpponentAutoPicked()) // AutoPick của P2
                .opponentAutoPicked(p1Reveal.getPlayerAutoPicked()) // AutoPick của P1
                .pointsEarned(p2PointsEarned) // Điểm P2 kiếm được round này
                .playerScore(finalP2Score) // Tổng điểm P2 mới nhất
                .opponentScore(finalP1Score) // Tổng điểm P1 mới nhất
                .result(p2Result)
                .build();
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

    // THÊM: Hàm handleGameEnd còn thiếu
    private void handleGameEnd(GameState completedGame) {
        System.out.println("Handling game end for match " + completedGame.getMatchId());

        // TODO: Xác định người thắng dựa trên completedGame.getPlayer1Score() vs completedGame.getPlayer2Score()
        String winnerId = getGameWinner(completedGame.getMatchId()); // Có thể dùng lại hàm getGameWinner nếu nó an toàn

        // TODO: Gọi Stored Procedure hoặc thực hiện câu lệnh UPDATE để cập nhật user_profiles
        // Ví dụ: updatePlayerStats(completedGame.getPlayer1Id(), completedGame.getPlayer2Id(), winnerId);

        // TODO: Tạo payload chi tiết cho GAME_END (ví dụ: GameEndDto)
        Map<String, Object> gameEndPayload = new HashMap<>();
        gameEndPayload.put("matchId", completedGame.getMatchId());
        gameEndPayload.put("player1Score", completedGame.getPlayer1Score());
        gameEndPayload.put("player2Score", completedGame.getPlayer2Score());
        gameEndPayload.put("winnerId", winnerId); // winnerId có thể null nếu hòa
        // Thêm các thông tin khác nếu cần

        notifyPlayer(completedGame.getPlayer1Id(), MessageProtocol.Type.GAME_END, gameEndPayload);
        notifyPlayer(completedGame.getPlayer2Id(), MessageProtocol.Type.GAME_END, gameEndPayload);
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
