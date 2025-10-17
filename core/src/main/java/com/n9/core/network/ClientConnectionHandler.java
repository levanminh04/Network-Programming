package com.n9.core.network;

import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.SessionManager;
import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.dto.game.PlayCardAckDto;
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.model.dto.match.MatchStartDto;
import com.n9.shared.protocol.ErrorInfo;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.MessageProtocol; //
import com.n9.shared.util.JsonUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * ClientConnectionHandler - Xử lý một kết nối TCP từ client.
 * Đóng vai trò là bộ định tuyến chính (Main Router), nhận tất cả các tin nhắn,
 * xác thực session, và ủy quyền cho các service tương ứng xử lý.
 *
 * @version 1.1.0 (Refactored for MVP)
 */
public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final GameService gameService;
    private final AuthService authService;
    private final SessionManager sessionManager;

    private BufferedReader reader;
    private BufferedWriter writer;

    /**
     * Constructor đã được cập nhật để nhận tất cả các service cần thiết.
     */
    public ClientConnectionHandler(
            Socket socket,
            GameService gameService,
            AuthService authService,
            SessionManager sessionManager
    ) {
        this.socket = socket;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        System.out.println("✅ New connection from: " + clientAddress);

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("📨 Received: " + line.substring(0, Math.min(100, line.length())));

                MessageEnvelope request = null;
                MessageEnvelope response;
                try {
                    request = JsonUtils.fromJson(line, MessageEnvelope.class);
                    if (request == null) {
                        response = new MessageEnvelope(MessageProtocol.Type.SYSTEM_ERROR, "unknown", null);
                        response.setError(new ErrorInfo("INVALID_JSON", "Invalid JSON format received."));
                    } else {
                        response = handleMessage(request);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Unhandled error processing message: " + e.getMessage());
                    e.printStackTrace();
                    response = MessageFactory.createErrorResponse(request, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
                }

                String jsonResponse = JsonUtils.toJson(response);
                sendMessage(jsonResponse);
                System.out.println("📤 Sent: " + jsonResponse.substring(0, Math.min(100, jsonResponse.length())));
            }
        } catch (SocketTimeoutException e) {
            System.err.println("⏱️ Timeout from " + clientAddress);
        } catch (IOException e) {
            System.err.println("❌ Connection error from " + clientAddress + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in run loop: " + clientAddress);
            e.printStackTrace();
        } finally {
            cleanup(clientAddress);
        }
    }

    /**
     * Bộ định tuyến chính, nhận MessageEnvelope và gọi handler tương ứng.
     */
    private MessageEnvelope handleMessage(MessageEnvelope envelope) {
        String type = envelope.getType();

        switch (type) {
            // ========== AUTH DOMAIN (Dành cho Lập trình viên A) ==========
            case MessageProtocol.Type.AUTH_REGISTER_REQUEST:
                return handleRegister(envelope);
            case MessageProtocol.Type.AUTH_LOGIN_REQUEST:
                return handleLogin(envelope);
            case MessageProtocol.Type.AUTH_LOGOUT_REQUEST:
                return handleLogout(envelope);

            // ========== LOBBY DOMAIN (Dành cho Lập trình viên B) ==========
            case MessageProtocol.Type.LOBBY_MATCH_REQUEST:
                return handleMatchRequest(envelope);

            // ========== GAME DOMAIN (Ví dụ đã refactor) ==========
            case MessageProtocol.Type.GAME_START:
                return handleGameStart(envelope);
            case MessageProtocol.Type.GAME_CARD_PLAY_REQUEST:
                return handlePlayCard(envelope);

            default:
                return MessageFactory.createErrorResponse(envelope, "UNKNOWN_TYPE", "Unknown message type: " + type);
        }
    }

    // ============================================================================
    // AUTH HANDLERS (PLACEHOLDERS CHO LẬP TRÌNH VIÊN A)
    // ============================================================================

    private MessageEnvelope handleRegister(MessageEnvelope envelope) {
        // TODO: Lập trình viên A triển khai logic này.
        return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Register functionality is not yet implemented.");
    }

    private MessageEnvelope handleLogin(MessageEnvelope envelope) {
        // TODO: Lập trình viên A triển khai logic này.
        return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Login functionality is not yet implemented.");
    }

    private MessageEnvelope handleLogout(MessageEnvelope envelope) {
        // TODO: Lập trình viên A triển khai logic này.
        return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Logout functionality is not yet implemented.");
    }

    // ============================================================================
    // LOBBY HANDLERS (PLACEHOLDERS CHO LẬP TRÌNH VIÊN B)
    // ============================================================================

    private MessageEnvelope handleMatchRequest(MessageEnvelope envelope) {
        // TODO: Lập trình viên B triển khai logic này.
        return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Matchmaking functionality is not yet implemented.");
    }

    // ============================================================================
    // GAME HANDLERS (VÍ DỤ ĐÃ REFACTOR)
    // ============================================================================

    private MessageEnvelope handleGameStart(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context == null) {
            return MessageFactory.createErrorResponse(envelope, "AUTH_REQUIRED", "Invalid or missing session.");
        }
        String playerId = context.getUserId();

        try {
            MatchStartDto startDto = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), MatchStartDto.class);
            String matchId = startDto.getMatchId();

            if (matchId == null) {
                return MessageFactory.createErrorResponse(envelope, "VALIDATION_ERROR", "Missing matchId in payload.");
            }

            Object responsePayload = new Object(); // Thay thế bằng DTO response thực tế

            return MessageFactory.createResponse(envelope, MessageProtocol.Type.GAME_START, responsePayload);

        } catch (Exception e) {
            System.err.println("❌ Error in handleGameStart: " + e.getMessage());
            return MessageFactory.createErrorResponse(envelope, "INTERNAL_SERVER_ERROR", "Failed to start game.");
        }
    }

    private MessageEnvelope handlePlayCard(MessageEnvelope envelope) {
        SessionManager.SessionContext context = sessionManager.getSession(envelope.getSessionId());
        if (context == null) {
            return MessageFactory.createErrorResponse(envelope, "AUTH_REQUIRED", "Invalid or missing session.");
        }
        String playerId = context.getUserId();

        try {
            PlayCardRequestDto request = JsonUtils.getObjectMapper().convertValue(envelope.getPayload(), PlayCardRequestDto.class);
            String matchId = request.getGameId();

            if (matchId == null) {
                return MessageFactory.createErrorResponse(envelope, "VALIDATION_ERROR", "Missing gameId in payload.");
            }

            CardDto playedCard = gameService.playCard(matchId, playerId, request.getCardId());
            System.out.println("🃏 Player " + playerId + " played a card in match " + matchId);

            PlayCardAckDto ackDto = new PlayCardAckDto();
            ackDto.setGameId(matchId);
            ackDto.setCardId(playedCard.getCardId());

            return MessageFactory.createResponse(envelope, MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS, ackDto);

        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Invalid card play: " + e.getMessage());
            return MessageFactory.createErrorResponse(envelope, "INVALID_PLAY", e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error in handlePlayCard: " + e.getMessage());
            return MessageFactory.createErrorResponse(envelope, "INTERNAL_SERVER_ERROR", "Failed to play card.");
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public synchronized void sendMessage(String jsonMessage) {
        try {
            if (writer != null && !socket.isClosed()) {
                writer.write(jsonMessage);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to send message to " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }

    private void cleanup(String clientAddress) {
        // THAY ĐỔI: Xóa session khi client ngắt kết nối
        String sessionId = "UNKNOWN"; // Cần một cách để lấy sessionId của handler này
        // sessionManager.removeSession(sessionId);

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🧹 Connection closed: " + clientAddress);
        } catch (IOException e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
        }
    }
}