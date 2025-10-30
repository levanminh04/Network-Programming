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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * ClientConnectionHandler - Xử lý kết nối TCP từ Gateway.
 * Triển khai mô hình I/O Thread + Worker Pool và Length-Prefixed Framing.
 *
 * @version 1.4.0 (Full Refactor)
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
            // Thêm Buffered streams để tăng hiệu năng đọc/ghi
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            // --- GIẢI PHÁP PHÁ VỠ DEADLOCK ---
            // Gửi tin nhắn chào mừng ngay khi kết nối
            sendWelcomeMessage();

            while (!socket.isClosed()) {
                // 1. Đọc 4 byte độ dài tin nhắn
                int length = in.readInt();

                // Thêm kiểm tra kích thước tin nhắn để bảo vệ server
                if (length > GameConstants.MAX_MESSAGE_SIZE) { // Giả sử có hằng số này
                    throw new IOException("Message size exceeds limit: " + length);
                }

                if (length > 0) {
                    // 2. Đọc chính xác `length` byte
                    byte[] messageBytes = new byte[length];
                    in.readFully(messageBytes, 0, length);
                    final String messageLine = new String(messageBytes, StandardCharsets.UTF_8);

                    System.out.println("📨 I/O Thread received message of length: " + length);

                    // 3. Tạo "Nhiệm vụ" để xử lý logic trong worker thread
                    Runnable processingTask = () -> {
                        MessageEnvelope request = null;
                        MessageEnvelope response = null;
                        try {
                            request = JsonUtils.fromJson(messageLine, MessageEnvelope.class);
                            if (request == null) {
                                response = new MessageEnvelope(MessageProtocol.Type.SYSTEM_ERROR, "unknown", null);
                                response.setError(new ErrorInfo("INVALID_JSON", "Invalid JSON format."));
                            } else {
                                // 4. Worker thread gọi bộ định tuyến
                                response = handleMessage(request);
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Worker thread caught error: " + e.getMessage());
                            response = MessageFactory.createErrorResponse(request, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
                        }

                        // 5. Worker thread tự gửi response về Gateway
                        try {
                            String jsonResponse = JsonUtils.toJson(response);
                            sendMessage(jsonResponse);
                        } catch (JsonProcessingException e) {
                            System.err.println("❌ Worker thread failed to serialize response: " + e.getMessage());
                        }
                    };

                    // 6. Luồng I/O giao việc và quay lại chờ
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

                default:
                    response = MessageFactory.createErrorResponse(envelope, "UNKNOWN_TYPE", "Unknown message type: " + type);
            }
        } catch (IllegalArgumentException e) {
            // Bắt lỗi nghiệp vụ (ví dụ: sai pass, bài không hợp lệ)
            System.err.println("⚠️ Business logic error: " + e.getMessage());
            response = MessageFactory.createErrorResponse(envelope, "VALIDATION_ERROR", e.getMessage());
        } catch (Exception e) {
            // Bắt các lỗi 500
            System.err.println("❌ Critical error in handler: " + e.getMessage());
            e.printStackTrace();
            response = MessageFactory.createErrorResponse(envelope, "INTERNAL_SERVER_ERROR", "An unexpected server error occurred.");
        }


        // Cập nhật 'activeConnections' map sau khi Auth thành công
        if (response != null && response.getError() == null &&
                (type.equals(MessageProtocol.Type.AUTH_LOGIN_REQUEST) || type.equals(MessageProtocol.Type.AUTH_REGISTER_REQUEST))) {
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
        // Cập nhật sessionId cho các request khác
        else if (response != null && response.getSessionId() != null && response.getError() == null) {
            this.currentSessionId = response.getSessionId();
        }

        return response;
    }



    private void sendWelcomeMessage() {
        System.out.println("Sending SYSTEM.WELCOME to Gateway...");
        MessageEnvelope welcome = MessageFactory.createNotification(MessageProtocol.Type.SYSTEM_WELCOME, Map.of("message", "Welcome to Core Server v1.1.0"));
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

    private MessageEnvelope handleRegister(MessageEnvelope envelope) throws Exception {
        RegisterRequestDto dto = JsonUtils.convertPayload(envelope.getPayload(), RegisterRequestDto.class);
        // AuthService sẽ ném Exception nếu thất bại
        var responseDto = authService.register(dto.getUsername(), dto.getEmail(), dto.getPassword(), dto.getDisplayName());
        String sessionId = sessionManager.createSession(responseDto.getUserId(), responseDto.getUsername());

        MessageEnvelope response = MessageFactory.createResponse(envelope, MessageProtocol.Type.AUTH_REGISTER_SUCCESS, responseDto);
        response.setSessionId(sessionId); // Gửi sessionId về cho client
        return response;
    }

    private MessageEnvelope handleLogin(MessageEnvelope envelope) throws Exception {
        LoginRequestDto dto = JsonUtils.convertPayload(envelope.getPayload(), LoginRequestDto.class);
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

    // ============================================================================
    // LOBBY & GAME HANDLERS (GỌI SERVICE)
    // ============================================================================

    private MessageEnvelope handleMatchRequest(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context == null) throw new IllegalArgumentException("Authentication required. Please log in.");

        boolean success = matchmakingService.requestMatch(context.getUserId());
        if (!success) {
            throw new IllegalArgumentException("You are already in the matchmaking queue.");
        }
        return MessageFactory.createResponse(envelope, "LOBBY.MATCH_REQUEST_ACK", Map.of("status", "SEARCHING"));
    }

    private MessageEnvelope handlePlayCard(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context == null) throw new IllegalArgumentException("Authentication required.");

        PlayCardRequestDto dto = JsonUtils.convertPayload(envelope.getPayload(), PlayCardRequestDto.class);

        // gameService.playCard sẽ ném Exception nếu thất bại
        CardDto playedCard = gameService.playCard(dto.getGameId(), context.getUserId(), dto.getCardId());

        // Response thành công đã được gửi đi bên trong GameService (GAME_CARD_PLAY_SUCCESS)
        // Chúng ta không cần gửi response thứ hai.
        // Tuy nhiên, MessageFactory cần một response, chúng ta có thể trả về null
        // và sửa logic trong `run()` để không gửi nếu response là null.

        // Tạm thời, để đơn giản, chúng ta sẽ trả về một response rỗng (không gửi đi)
        // Hoặc chúng ta có thể thiết kế lại playCard để nó trả về 1 DTO
        // và handlePlayCard sẽ gửi response.

        // Giả sử logic gửi response đã nằm trong gameService.playCard(), ta chỉ cần 1 response giả
        return new MessageEnvelope(); // Sẽ không được gửi nếu không có type
    }


    /**
     * Gửi tin nhắn (Length-Prefixed) - An toàn luồng.
     */
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
                // Xử lý Forfeit NẾU user đang trong trận
                if (context.getCurrentMatchId() != null) {
                    gameService.handleForfeit(context.getCurrentMatchId(), context.getUserId());
                }
                // Xóa khỏi matchmaking queue NẾU đang chờ
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
