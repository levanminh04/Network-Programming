package com.n9.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol; // Đổi tên nếu bạn đã đổi
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.IdUtils;
import com.n9.shared.util.JsonUtils;

import java.util.Collection; // THÊM
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MatchmakingService {

    private final GameService gameService;
    private final SessionManager sessionManager;
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private final ScheduledExecutorService scheduler;

    private final Queue<String> matchmakingQueue = new ConcurrentLinkedQueue<>(); // Ai vào hàng đợi trước phải được ghép cặp trước
    private final Set<String> usersInQueue = ConcurrentHashMap.newKeySet();  // dùng để tìm kiếm user cho nhanh, vì Queue tìm theo O(n)

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

    public void startMatchmakingLoop() {
        // scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit)
        // initialDelay: Thời gian chờ trước khi chạy lần đầu tiên.
        // period: Chu kỳ lặp lại.
        scheduler.scheduleAtFixedRate(this::tryMatchmaking, 1, 1, TimeUnit.SECONDS);
    }

    public boolean requestMatch(String userId) {
        if (usersInQueue.contains(userId)) {
            return false;
        }
        if (usersInQueue.add(userId)) {
            matchmakingQueue.offer(userId); // thêm vào cuối hàng đợi
            return true;
        }
        return false;
    }

    public void cancelMatch(String userId) {
        if (usersInQueue.remove(userId)) {
            matchmakingQueue.remove(userId);
        }
    }

    private void tryMatchmaking() {
        if (usersInQueue.size() >= 2) {
            String player1Id = matchmakingQueue.poll(); // lấy và loại bỏ phần tử đầu tiên (đầu hàng).
            String player2Id = matchmakingQueue.poll();

            if (player1Id != null && player2Id != null) {
                usersInQueue.remove(player1Id);
                usersInQueue.remove(player2Id);

                String matchId = IdUtils.generateMatchId();

                String p1SessionId = getSessionIdForUser(player1Id);
                String p2SessionId = getSessionIdForUser(player2Id);

                sessionManager.setMatchId(p1SessionId, matchId);
                sessionManager.setMatchId(p2SessionId, matchId);

                String player1Username = getUsernameForId(player1Id);
                String player2Username = getUsernameForId(player2Id);

                notifyPlayerMatchFound(player1Id, p1SessionId, matchId, player2Id, player2Username);
                notifyPlayerMatchFound(player2Id, p2SessionId, matchId, player1Id, player1Username);

                scheduler.schedule(() -> {
                    GameService.GameState newGame = gameService.initializeGame(matchId, player1Id, player2Id);
                    if (newGame == null) {
                        System.err.println("❌ Failed to initialize game for match " + matchId);
                    }
                }, 2, TimeUnit.SECONDS);
            } else {
                if (player1Id != null) matchmakingQueue.offer(player1Id);
                if (player2Id != null) matchmakingQueue.offer(player2Id);
            }
        }
    }


    // Gửi thông báo GAME_MATCH_FOUND.
    private void notifyPlayerMatchFound(String targetUserId, String targetSessionId, String matchId, String opponentId, String opponentUsername) {
        ClientConnectionHandler handler = activeConnections.get(targetUserId);
        if (handler != null) {
            Map<String, Object> opponentInfo = new HashMap<>();
            opponentInfo.put("userId", opponentId);
            opponentInfo.put("username", opponentUsername != null ? opponentUsername : "Opponent");

            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", matchId);
            payload.put("opponent", opponentInfo);

            try {
                MessageEnvelope envelope = MessageFactory.createNotification(MessageProtocol.Type.GAME_MATCH_FOUND, payload);
                envelope.setSessionId(targetSessionId);
                handler.sendMessage(JsonUtils.toJson(envelope));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
        }
    }


    private String getSessionIdForUser(String userId) {
        if (sessionManager == null) return null;
        for (SessionManager.SessionContext ctx : sessionManager.getAllSessions()) {
            if (ctx.getUserId().equals(userId)) return ctx.getSessionId();
        }
        return null;
    }

    private String getUsernameForId(String userId) {
        if (sessionManager == null) return "Unknown"; // Thêm kiểm tra an toàn
        for (SessionManager.SessionContext ctx : sessionManager.getAllSessions()) {
            if (ctx.getUserId().equals(userId)) return ctx.getUsername();
        }
        return "Unknown";
    }
}

