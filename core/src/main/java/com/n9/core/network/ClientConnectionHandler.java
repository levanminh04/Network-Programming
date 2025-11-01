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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * ClientConnectionHandler - Xử lý kết nối TCP từ Gateway.
 * Triển khai mô hình I/O Thread + Worker Pool và Length-Prefixed Framing.
 *
 * @version 1.4.1 (Fixed Heartbeat and Buffering)
 */
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
    private String currentSessionId = null;

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
        System.out.println("✅ I/O Thread started for connection from: " + clientAddress);

        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            sendWelcomeMessage();

            while (!socket.isClosed()) {
                int length = in.readInt();

                // THAY ĐỔI: Bỏ comment kiểm tra kích thước
                if (length > GameConstants.MAX_MESSAGE_SIZE) {
                    throw new IOException("Message size exceeds limit: " + length);
                }

                if (length > 0) {
                    byte[] messageBytes = new byte[length];
                    in.readFully(messageBytes, 0, length);
                    final String messageLine = new String(messageBytes, StandardCharsets.UTF_8);

                    System.out.println("📨 I/O Thread received message of length: " + length);

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
                            System.err.println("❌ Worker thread caught error: " + e.getMessage());
                            response = MessageFactory.createErrorResponse(request, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
                        }

                        // THAY ĐỔI: Chỉ gửi nếu response không null và có type
                        if (response != null && response.getType() != null) {
                            try {
                                String jsonResponse = JsonUtils.toJson(response);
                                sendMessage(jsonResponse);
                            } catch (JsonProcessingException e) {
                                System.err.println("❌ Worker thread failed to serialize response: " + e.getMessage());
                            }
                        }
                        // Nếu response là null (ví dụ từ handlePlayCard), worker thread sẽ không làm gì cả.
                    };
                    pool.submit(processingTask);
                }
            }
        } catch (EOFException e) {
            System.out.println("🔌 Gateway closed the connection gracefully: " + clientAddress);
        } catch (IOException e) {
            System.err.println("❌ Connection lost with " + clientAddress + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in I/O loop for " + clientAddress);
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
                // --- AUTH DOMAIN ---
                case MessageProtocol.Type.AUTH_REGISTER_REQUEST:
                    response = handleRegister(envelope);
                    break;
                case MessageProtocol.Type.AUTH_LOGIN_REQUEST:
                    response = handleLogin(envelope);
                    break;
                case MessageProtocol.Type.AUTH_LOGOUT_REQUEST:
                    response = handleLogout(envelope);
                    break;

                // --- LOBBY DOMAIN ---
                case MessageProtocol.Type.LOBBY_MATCH_REQUEST:
                    response = handleMatchRequest(envelope);
                    break;
                // TODO: Thêm case LOBBY_MATCH_CANCEL

                // --- GAME DOMAIN ---
                case MessageProtocol.Type.GAME_CARD_PLAY_REQUEST:
                    response = handlePlayCard(envelope);
                    break;

                // --- THAY ĐỔI: THÊM CASE CHO PING ---
                case MessageProtocol.Type.SYSTEM_PING:
                    System.out.println("💓 Received PING from Gateway. Sending PONG.");
                    response = MessageFactory.createResponse(envelope, MessageProtocol.Type.SYSTEM_PONG, null);
                    break;

                default:
                    response = MessageFactory.createErrorResponse(envelope, "UNKNOWN_TYPE", "Unknown message type: " + type);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Business logic error ["+ type +"]: " + e.getMessage());
            response = MessageFactory.createErrorResponse(envelope, "VALIDATION_ERROR", e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Critical error in handler ["+ type +"]: " + e.getMessage());
            e.printStackTrace();
            response = MessageFactory.createErrorResponse(envelope, "INTERNAL_SERVER_ERROR", "An unexpected server error occurred.");
        }

        // Cập nhật 'activeConnections' map sau khi Auth thành công
        // THAY ĐỔI: Kiểm tra type của PHẢN HỒI (response.getType())
        if (response != null && response.getError() == null &&
                (response.getType().equals(MessageProtocol.Type.AUTH_LOGIN_SUCCESS) || response.getType().equals(MessageProtocol.Type.AUTH_REGISTER_SUCCESS)))
        {
            SessionManager.SessionContext context = sessionManager.getSession(response.getSessionId());
            if (context != null) {
                String userId = context.getUserId();
                this.currentSessionId = response.getSessionId();
                activeConnections.put(userId, this);
                System.out.println("🔗 User " + userId + " associated with connection: " + socket.getRemoteSocketAddress());
            } else {
                System.err.println("⚠️ Login/Register success but session context not found for sid: " + response.getSessionId());
            }
        }
        else if (response != null && response.getSessionId() != null && response.getError() == null) {
            // Cập nhật sessionId cho các request khác (ví dụ: client gửi PING với session cũ,
            // nhưng response từ LOGIN/REGISTER (cùng 1 client) đã có session mới)
            // Logic này có thể cần xem xét lại, nhưng việc lưu currentSessionId là tốt
            this.currentSessionId = response.getSessionId();
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
            System.err.println("❌ Failed to serialize WELCOME message: " + e.getMessage());
        }
    }

    // ============================================================================
    // AUTH HANDLERS (GỌI SERVICE)
    // ============================================================================
    // (Tất cả các hàm handler: handleRegister, handleLogin, handleLogout,
    // handleMatchRequest, handlePlayCard đều giữ nguyên)
    // ...

    // đăng kí xong mới sessionId
    private MessageEnvelope handleRegister(MessageEnvelope envelope) throws Exception {
        RegisterRequestDto dto = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), RegisterRequestDto.class);
        var responseDto = authService.register(dto.getUsername(), dto.getEmail(), dto.getPassword(), dto.getDisplayName());
        String sessionId = sessionManager.createSession(responseDto.getUserId(), responseDto.getUsername());
        MessageEnvelope response = MessageFactory.createResponse(envelope, MessageProtocol.Type.AUTH_REGISTER_SUCCESS, responseDto);
        response.setSessionId(sessionId);
        return response;
    }

    private MessageEnvelope handleLogin(MessageEnvelope envelope) throws Exception {
        LoginRequestDto dto = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), LoginRequestDto.class);
        var responseDto = authService.login(dto.getUsername(), dto.getPassword());
        String sessionId = sessionManager.createSession(responseDto.getUserId(), responseDto.getUsername());
        MessageEnvelope response = MessageFactory.createResponse(envelope, MessageProtocol.Type.AUTH_LOGIN_SUCCESS, responseDto);
        response.setSessionId(sessionId);
        return response;
    }

    private MessageEnvelope handleLogout(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context != null) {
            sessionManager.removeSession(context.getSessionId());
            activeConnections.remove(context.getUserId());
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
        // THAY ĐỔI: Sử dụng Type đã chuẩn hóa
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
            System.err.println("⚠️ Invalid card selection [" + dto.getCardId() + "]: " + e.getMessage());
            
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
            System.err.println("❌ Failed to send message to " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }

    private void cleanup(String clientAddress) {
        if (this.currentSessionId != null) {
            SessionManager.SessionContext context = sessionManager.getSession(this.currentSessionId);
            if (context != null) {
                if (context.getCurrentMatchId() != null) {
                    gameService.handleForfeit(context.getCurrentMatchId(), context.getUserId());
                }
                matchmakingService.cancelMatch(context.getUserId());
                activeConnections.remove(context.getUserId());
                System.out.println("🔗 Removed connection mapping for user: " + context.getUserId());
                sessionManager.removeSession(this.currentSessionId);
            }
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🧹 Connection cleaned up for: " + clientAddress);
        } catch (IOException e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
        }
    }
}

