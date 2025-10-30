package com.n9.core.database.test;

import com.n9.shared.MessageProtocol;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
// import com.n9.shared.protocol.Protocol; // THAY ĐỔI: Xóa import không cần thiết này
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.model.dto.auth.RegisterRequestDto;
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.util.JsonUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Client Test End-to-End
 * Kết nối đến GATEWAY (Port 8080) để kiểm thử toàn bộ luồng.
 * Yêu cầu:
 * 1. Chạy Core Server (Port 9090)
 * 2. Chạy Gateway Server (Port 8080)
 * 3. Chạy file này.
 */
public class GatewayEndToEndTestClient {

    private static final String GATEWAY_URI = "ws://localhost:8080/ws";

    private static String currentSessionId = null;
    private static String currentMatchId = null;
    private static String currentUserId = null;

    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Starting End-to-End Test Client...");
        System.out.println("Connecting to Gateway at: " + GATEWAY_URI);

        WebSocketClient client = new WebSocketClient(new URI(GATEWAY_URI)) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("✅ [E2E Test] Connected to Gateway.");
                // Bắt đầu kịch bản test bằng việc ĐĂNG KÝ
                testRegister(this);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("📥 [E2E Test] Received from Gateway: " + message);

                try {
                    MessageEnvelope response = JsonUtils.fromJson(message, MessageEnvelope.class);
                    // Lưu lại sessionId nếu có
                    if (response.getSessionId() != null) {
                        currentSessionId = response.getSessionId();
                    }

                    // --- XỬ LÝ PHẢN HỒI VÀ GỌI BƯỚC TIẾP THEO ---
                    switch (response.getType()) {

                        case MessageProtocol.Type.AUTH_REGISTER_SUCCESS:
                            System.out.println("   -> Registration successful. Now logging in...");
                            // Giả sử DTO của bạn nằm đúng package
                            currentUserId = JsonUtils.convertPayload(response.getPayload(), com.n9.shared.model.dto.auth.RegisterResponseDto.class).getUserId();
                            testLogin(this, "testuser_e2e", "password123");
                            break;

                        case MessageProtocol.Type.AUTH_LOGIN_SUCCESS:
                            System.out.println("   -> Login successful. Requesting match...");
                            testRequestMatch(this);
                            break;

                        case "LOBBY.MATCH_REQUEST_ACK": // Phản hồi từ handleMatchRequest
                            System.out.println("   -> Matchmaking requested. Waiting for match...");
                            // Chờ thông báo đẩy GAME.MATCH_FOUND
                            break;

                        case MessageProtocol.Type.GAME_MATCH_FOUND:
                            System.out.println("   -> Match found!");
                            // TODO: Cần parse payload để lấy matchId thực tế
                            // currentMatchId = JsonUtils.convertPayload(response.getPayload(), ...).getMatchId();
                            currentMatchId = "dummy_match_id"; // Tạm thời
                            // Chờ GAME.START
                            break;

                        case MessageProtocol.Type.GAME_START:
                            System.out.println("   -> Game started. Waiting for first round...");
                            // Chờ GAME.ROUND_START
                            break;

                        case MessageProtocol.Type.GAME_ROUND_START:
                            System.out.println("   -> Round started. Playing a card...");
                            // Tự động chơi một lá bài
                            testPlayCard(this, 1); // Giả sử chơi lá bài 1
                            break;

                        case MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS:
                            System.out.println("   -> Card played. Waiting for reveal...");
                            break;

                        case MessageProtocol.Type.GAME_ROUND_REVEAL:
                            System.out.println("   -> Round revealed!");
                            // Logic game sẽ tự động gửi GAME.ROUND_START tiếp theo
                            // hoặc GAME.END
                            break;

                        case MessageProtocol.Type.GAME_END:
                            System.out.println("   -> GAME OVER! Closing connection.");
                            this.close();
                            break;

                        case MessageProtocol.Type.SYSTEM_ERROR:
                            System.err.println("   -> Received SYSTEM_ERROR: " + (response.getError() != null ? response.getError().getMessage() : "Unknown Error"));
                            this.close();
                            break;
                    }

                } catch (Exception e) {
                    System.err.println("❌ Failed to parse message from Gateway: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("🛑 [E2E Test] Disconnected from Gateway. Code: " + code + " Reason: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("❌ [E2E Test] WebSocket Error: " + ex.getMessage());
            }
        };

        client.connectBlocking(); // Chờ kết nối
    }

    // --- Các hàm tạo request (sử dụng MessageFactory) ---

    private static void testRegister(WebSocketClient client) {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setUsername("testuser_e2e");
        dto.setEmail("e2e@test.com");
        dto.setPassword("password123");
        dto.setDisplayName("E2E Tester");

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.AUTH_REGISTER_REQUEST, dto);
        send(client, request);
    }

    private static void testLogin(WebSocketClient client, String user, String pass) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername(user);
        dto.setPassword(pass);

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.AUTH_LOGIN_REQUEST, dto);
        send(client, request);
    }

    private static void testRequestMatch(WebSocketClient client) {
        // THAY ĐỔI: Sửa lỗi cú pháp ở đây
        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.LOBBY_MATCH_REQUEST, null);
        request.setSessionId(currentSessionId); // Rất quan trọng!
        send(client, request);
    }

    private static void testPlayCard(WebSocketClient client, int cardId) {
        PlayCardRequestDto dto = new PlayCardRequestDto();
        dto.setGameId(currentMatchId); // Cần có matchId thực tế
        dto.setCardId(cardId);
        dto.setRoundNumber(1); // Cần lấy round number thực tế

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.GAME_CARD_PLAY_REQUEST, dto);
        request.setSessionId(currentSessionId); // Rất quan trọng!
        send(client, request);
    }

    // Hàm helper để gửi
    private static void send(WebSocketClient client, MessageEnvelope envelope) {
        try {
            String json = JsonUtils.toJson(envelope);
            System.out.println("📤 [E2E Test] Sending to Gateway: " + json);
            client.send(json);
        } catch (Exception e) {
            System.err.println("❌ Failed to serialize or send message: " + e.getMessage());
        }
    }
}

