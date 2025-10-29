package com.n9.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.IdUtils;
import com.n9.shared.util.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MatchmakingService - Quản lý hàng đợi tìm trận và ghép cặp người chơi.
 *
 * @version 1.0.0 (MVP - Simple FIFO Queue)
 */
public class MatchmakingService {

    private final GameService gameService;
    private final SessionManager sessionManager;
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private final ScheduledExecutorService scheduler;

    private final Queue<String> matchmakingQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> usersInQueue = ConcurrentHashMap.newKeySet();

    public MatchmakingService(
            GameService gameService,
            SessionManager sessionManager,
            ConcurrentHashMap<String, ClientConnectionHandler> activeConnections,
            ScheduledExecutorService scheduler
    ) {
        this.gameService = gameService;
        this.sessionManager = sessionManager;
        this.activeConnections = activeConnections;
        this.scheduler = scheduler;
    }

    /**
     * Bắt đầu vòng lặp kiểm tra hàng đợi matchmaking.
     */
    public void startMatchmakingLoop() {
        scheduler.scheduleAtFixedRate(this::tryMatchmaking, 1, 1, TimeUnit.SECONDS);
        System.out.println("✅ Matchmaking loop started.");
    }

    /**
     * Thêm người dùng vào hàng đợi tìm trận.
     */
    public boolean requestMatch(String userId) {
        if (usersInQueue.contains(userId)) {
            System.out.println("⚠️ Player " + userId + " is already in the matchmaking queue.");
            return false;
        }
        if (usersInQueue.add(userId)) {
            matchmakingQueue.offer(userId);
            System.out.println("➕ Player " + userId + " added to matchmaking queue. Queue size: " + usersInQueue.size());
            return true;
        }
        return false;
    }

    /**
     * Xóa người dùng khỏi hàng đợi tìm trận.
     */
    public void cancelMatch(String userId) {
        if (usersInQueue.remove(userId)) {
            matchmakingQueue.remove(userId);
            System.out.println("➖ Player " + userId + " removed from matchmaking queue. Queue size: " + usersInQueue.size());
        }
    }

    /**
     * Logic kiểm tra hàng đợi và ghép cặp (chạy định kỳ).
     */
    private void tryMatchmaking() {
        if (usersInQueue.size() >= 2) {
            String player1Id = matchmakingQueue.poll();
            String player2Id = matchmakingQueue.poll();

            if (player1Id != null && player2Id != null) {
                usersInQueue.remove(player1Id);
                usersInQueue.remove(player2Id);

                System.out.println("🎉 Found a match! Pairing " + player1Id + " and " + player2Id);

                String matchId = IdUtils.generateMatchId(); // Giả sử IdUtils có hàm này

                // Cập nhật session (quan trọng)
                sessionManager.setMatchId(getSessionIdForUser(player1Id), matchId);
                sessionManager.setMatchId(getSessionIdForUser(player2Id), matchId);

                // Khởi tạo game
                GameService.GameState newGame = gameService.initializeGame(matchId, player1Id, player2Id);

                if (newGame != null) {
                    String player1Username = getUsernameForId(player1Id);
                    String player2Username = getUsernameForId(player2Id);
                    notifyPlayerMatchFound(player1Id, matchId, player2Id, player2Username);
                    notifyPlayerMatchFound(player2Id, matchId, player1Id, player1Username);
                } else {
                    System.err.println("❌ Failed to initialize game for match " + matchId);
                    // TODO: Đưa 2 người chơi trở lại hàng đợi hoặc báo lỗi
                }

            } else {
                if (player1Id != null) matchmakingQueue.offer(player1Id);
                if (player2Id != null) matchmakingQueue.offer(player2Id);
            }
        }
    }

    /**
     * Gửi thông báo GAME_MATCH_FOUND.
     */
    private void notifyPlayerMatchFound(String targetUserId, String matchId, String opponentId, String opponentUsername) {
        ClientConnectionHandler handler = activeConnections.get(targetUserId);
        if (handler != null) {
            Map<String, Object> opponentInfo = new HashMap<>();
            opponentInfo.put("userId", opponentId);
            opponentInfo.put("username", opponentUsername != null ? opponentUsername : "Opponent");
            // TODO: Lấy score của đối thủ nếu cần

            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId);
            payload.put("opponent", opponentInfo);

            try {
                MessageEnvelope envelope = MessageFactory.createNotification(MessageProtocol.Type.GAME_MATCH_FOUND, payload);
                handler.sendMessage(JsonUtils.toJson(envelope));
                System.out.println("   Sent GAME.MATCH_FOUND to " + targetUserId);
            } catch (JsonProcessingException e) {
                System.err.println("❌ Error serializing GAME.MATCH_FOUND for user " + targetUserId + ": " + e.getMessage());
            }
        } else {
            System.err.println("⚠️ Cannot send GAME.MATCH_FOUND to player " + targetUserId + ": Handler not found.");
            // TODO: Xử lý hủy trận đấu và đưa người chơi còn lại về queue
        }
    }

    // --- Hàm Helper ---

    /** Lấy sessionId từ userId. */
    private String getSessionIdForUser(String userId) {
        for (SessionManager.SessionContext ctx : sessionManager.getAllSessions()) {
            if (ctx.getUserId().equals(userId)) return ctx.getSessionId();
        }
        System.err.println("⚠️ Could not find sessionId for userId: " + userId);
        return null;
    }

    /** Lấy username từ userId. */
    private String getUsernameForId(String userId) {
        for (SessionManager.SessionContext ctx : sessionManager.getAllSessions()) {
            if (ctx.getUserId().equals(userId)) return ctx.getUsername();
        }
        System.err.println("⚠️ Could not find username for userId: " + userId);
        return "Unknown";
    }
}
