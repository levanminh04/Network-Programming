package com.n9.core.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.MatchmakingService;
import com.n9.core.service.SessionManager;
import com.n9.shared.MessageProtocol;
import com.n9.shared.constants.GameConstants;
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.model.dto.auth.RegisterRequestDto;
import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.protocol.ErrorInfo;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;

import com.n9.shared.util.JsonUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;



public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final GameService gameService;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final ExecutorService pool;
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;

    private DataInputStream in;
    private DataOutputStream out;

    public ClientConnectionHandler(
            Socket socket,
            GameService gameService,
            AuthService authService,
            SessionManager sessionManager,
            MatchmakingService matchmakingService,
            ExecutorService pool,
            ConcurrentHashMap<String, ClientConnectionHandler> activeConnections
    ) {
        this.socket = socket;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.matchmakingService = matchmakingService;
        this.pool = pool;
        this.activeConnections = activeConnections;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();

        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            sendWelcomeMessage();

            while (!socket.isClosed()) {
                int length = in.readInt();

                if (length > GameConstants.MAX_MESSAGE_SIZE) {
                    throw new IOException("Message size exceeds limit: " + length);
                }

                if (length > 0) {
                    byte[] messageBytes = new byte[length];
                    in.readFully(messageBytes, 0, length);
                    final String messageLine = new String(messageBytes, StandardCharsets.UTF_8);


                    Runnable processingTask = () -> {
                        MessageEnvelope request = null;
                        MessageEnvelope response = null;
                        try {
                            request = JsonUtils.fromJson(messageLine, MessageEnvelope.class);
                            if (request == null) {
                                response = new MessageEnvelope(MessageProtocol.Type.SYSTEM_ERROR, "unknown", null);
                                response.setError(new ErrorInfo("INVALID_JSON", "Invalid JSON format."));
                            } else {
                                response = handleMessage(request);
                            }
                        } catch (Exception e) {
                            response = MessageFactory.createErrorResponse(request, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
                        }

                        // Chỉ gửi nếu response không null và có type
                        if (response != null && response.getType() != null) {
                            try {
                                String jsonResponse = JsonUtils.toJson(response);
                                sendMessage(jsonResponse);
                            } catch (JsonProcessingException e) {
                            }
                        }
                        // Nếu response là null (ví dụ từ handlePlayCard), worker thread sẽ không làm gì cả.
                    };
                    pool.submit(processingTask);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup(clientAddress);
        }
    }

    /**
     * Bộ định tuyến chính, được gọi bởi Worker Thread.
     */
    private MessageEnvelope handleMessage(MessageEnvelope envelope) {
        String type = envelope.getType();
        MessageEnvelope response;

        try {
            switch (type) {
                // --- AUTH ---
                case MessageProtocol.Type.AUTH_REGISTER_REQUEST:
                    response = handleRegister(envelope);
                    break;
                case MessageProtocol.Type.AUTH_LOGIN_REQUEST:
                    response = handleLogin(envelope);
                    break;
                case MessageProtocol.Type.AUTH_LOGOUT_REQUEST:
                    response = handleLogout(envelope);
                    break;

                // --- LOBBY ---
                case MessageProtocol.Type.LOBBY_MATCH_REQUEST:
                    response = handleMatchRequest(envelope);
                    break;
                // TODO: Thêm case LOBBY_MATCH_CANCEL

                // --- GAME ---
                case MessageProtocol.Type.GAME_CARD_PLAY_REQUEST:
                    response = handlePlayCard(envelope);
                    break;

                case MessageProtocol.Type.SYSTEM_PING:
                    System.out.println("💓 Received PING from Gateway. Sending PONG.");
                    response = MessageFactory.createResponse(envelope, MessageProtocol.Type.SYSTEM_PONG, null);
                    break;

                default:
                    response = MessageFactory.createErrorResponse(envelope, "UNKNOWN_TYPE", "Unknown message type: " + type);
            }
        } catch (IllegalArgumentException e) {
            response = MessageFactory.createErrorResponse(envelope, "VALIDATION_ERROR", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response = MessageFactory.createErrorResponse(envelope, "INTERNAL_SERVER_ERROR", "An unexpected server error occurred.");
        }

        return response;
    }

    private void sendWelcomeMessage() {
        System.out.println("Sending SYSTEM.WELCOME to Gateway...");
        // THAY ĐỔI: Sử dụng Protocol.Type
        MessageEnvelope welcome = MessageFactory.createNotification(MessageProtocol.Type.SYSTEM_WELCOME, Map.of("message", "Welcome to Core Server v1.2.0"));
        try {
            String json = JsonUtils.toJson(welcome);
            sendMessage(json);
        } catch (JsonProcessingException e) {
        }
    }


    // đăng kí xong mới sessionId
    private MessageEnvelope handleRegister(MessageEnvelope envelope) throws Exception {
        RegisterRequestDto dto = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), RegisterRequestDto.class);
        var responseDto = authService.register(dto.getUsername(), dto.getEmail(), dto.getPassword(), dto.getDisplayName());
//        String sessionId = sessionManager.createSession(responseDto.getUserId(), responseDto.getUsername());
        
        MessageEnvelope response = MessageFactory.createResponse(envelope, MessageProtocol.Type.AUTH_REGISTER_SUCCESS, responseDto);
//        response.setSessionId(sessionId);
//        activeConnections.put(responseDto.getUserId(), this);
        return response;
    }

    private MessageEnvelope handleLogin(MessageEnvelope envelope) throws Exception {
        LoginRequestDto dto = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), LoginRequestDto.class);
        var responseDto = authService.login(dto.getUsername(), dto.getPassword());
        String sessionId = sessionManager.createSession(responseDto.getUserId(), responseDto.getUsername());
        
        MessageEnvelope response = MessageFactory.createResponse(envelope, MessageProtocol.Type.AUTH_LOGIN_SUCCESS, responseDto);
        response.setSessionId(sessionId);
        
        activeConnections.put(responseDto.getUserId(), this);

        return response;
    }

    private MessageEnvelope handleLogout(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context != null) {
            String userId = context.getUserId();
            String matchId = context.getCurrentMatchId();
            

            // Xử lý forfeit nếu đang trong trận
            if (matchId != null) {
                gameService.handleForfeit(matchId, userId);
            }
            
            // Hủy matchmaking nếu đang chờ
            matchmakingService.cancelMatch(userId);
            
            // Xóa khỏi activeConnections và session
            activeConnections.remove(userId);
            sessionManager.removeSession(context.getSessionId());
        }
        return MessageFactory.createResponse(envelope, MessageProtocol.Type.AUTH_LOGOUT_SUCCESS, null);
    }

    private MessageEnvelope handleMatchRequest(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context == null) throw new IllegalArgumentException("Authentication required. Please log in.");
        boolean success = matchmakingService.requestMatch(context.getUserId());
        if (!success) {
            throw new IllegalArgumentException("You are already in the matchmaking queue.");
        }
        return MessageFactory.createResponse(envelope, MessageProtocol.Type.LOBBY_MATCH_REQUEST_ACK, Map.of("status", "SEARCHING"));
    }

    private MessageEnvelope handlePlayCard(MessageEnvelope envelope) throws Exception {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context == null) throw new IllegalArgumentException("Authentication required.");
        PlayCardRequestDto dto = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), PlayCardRequestDto.class);

        try {
            // gameService.playCard() sẽ ném Exception nếu thất bại, và tự gửi response/notification
            gameService.playCard(dto.getGameId(), context.getUserId(), dto.getCardId());
            return null; // Success: worker thread sẽ không gửi gì (GameService đã gửi)
            
        } catch (IllegalArgumentException e) {
            // Card không hợp lệ → Gửi FAILURE với cơ chế retry
            Map<String, Object> failurePayload = new HashMap<>();
            failurePayload.put("gameId", dto.getGameId());
            failurePayload.put("cardId", dto.getCardId());
            failurePayload.put("canRetry", true); // ← Cho phép chọn lại
            failurePayload.put("reason", e.getMessage());
            failurePayload.put("message", "Invalid card selection. Please choose another card.");
            
            return MessageFactory.createResponse(envelope, MessageProtocol.Type.GAME_CARD_PLAY_FAILURE, failurePayload);
        }
    }

    // ... (sendMessage và cleanup giữ nguyên)
    public synchronized void sendMessage(String jsonMessage) {
        try {
            if (out != null && !socket.isClosed()) {
                byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
                int length = jsonBytes.length;
                out.writeInt(length);
                out.write(jsonBytes);
                out.flush();
            }
        } catch (IOException e) {
        }
    }

    private void cleanup(String clientAddress) {

        // Tạo danh sách users thuộc connection này (tránh ConcurrentModificationException)
        Set<String> usersToCleanup = new HashSet<>();
        
        for (Map.Entry<String, ClientConnectionHandler> entry : activeConnections.entrySet()) {
            if (entry.getValue() == this) { // Chỉ lấy users của connection NÀY
                usersToCleanup.add(entry.getKey());
            }
        }
        
        // Dọn dẹp từng user
        for (String userId : usersToCleanup) {

            // Tìm session context từ SessionManager
            SessionManager.SessionContext context = null;
            for (SessionManager.SessionContext ctx : sessionManager.getAllSessions()) {
                if (ctx.getUserId().equals(userId)) {
                    context = ctx;
                    break;
                }
            }
            
            if (context != null) {
                // Xử lý forfeit nếu đang trong game
                if (context.getCurrentMatchId() != null) {
                    gameService.handleForfeit(context.getCurrentMatchId(), userId);
                }
                
                // Hủy matchmaking nếu đang chờ
                matchmakingService.cancelMatch(userId);
                
                // Xóa session
                sessionManager.removeSession(context.getSessionId());
            }
            
            // Xóa khỏi activeConnections
            activeConnections.remove(userId);
        }
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
        }
    }
}

