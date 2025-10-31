package com.n9.core.database.test;

import com.n9.shared.MessageProtocol;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
// import com.n9.shared.protocol.Protocol; // Đổi tên file nếu bạn đã đổi
import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.model.dto.auth.RegisterRequestDto;
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.model.dto.match.MatchFoundDto; // Giả sử bạn có DTO này
import com.n9.shared.util.JsonUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Client Test End-to-End (Phiên bản nâng cấp)
 * Tự động tạo user ngẫu nhiên để có thể chạy nhiều instance và test matchmaking.
 */
public class GatewayEndToEndTestClient {

    private static final String GATEWAY_URI = "ws://localhost:8080/ws";

    // THAY ĐỔI: Tạo thông tin user ngẫu nhiên cho mỗi lần chạy
    private static final String TEST_USERNAME = "tester_" + (int)(Math.random() * 100000);
    private static final String TEST_EMAIL = TEST_USERNAME + "@test.com";
    private static final String TEST_PASSWORD = "password123";

    private static String currentSessionId = null;
    private static String currentMatchId = null;
    private static String currentUserId = null;
    private static int currentRound = 1; // Track current round number

    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Starting E2E Test Client for user: " + TEST_USERNAME);
        System.out.println("Connecting to Gateway at: " + GATEWAY_URI);

        WebSocketClient client = new WebSocketClient(new URI(GATEWAY_URI)) {

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("✅ [E2E Test] Connected to Gateway.");
                testRegister(this);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("📥 [E2E Test] Received from Gateway: " + message);

                try {
                    MessageEnvelope response = JsonUtils.fromJson(message, MessageEnvelope.class);
                    if (response.getSessionId() != null) {
                        currentSessionId = response.getSessionId();
                    }

                    switch (response.getType()) {

                        case MessageProtocol.Type.AUTH_REGISTER_SUCCESS:
                            System.out.println("   -> Registration successful. Now logging in...");
                            currentUserId = JsonUtils.convertPayload(response.getPayload(), com.n9.shared.model.dto.auth.RegisterResponseDto.class).getUserId();
                            testLogin(this); // Đăng nhập với thông tin đã tạo
                            break;

                        case MessageProtocol.Type.AUTH_LOGIN_SUCCESS:
                            System.out.println("   -> Login successful. Requesting match...");
                            testRequestMatch(this);
                            break;

                        case MessageProtocol.Type.LOBBY_MATCH_REQUEST_ACK: // Sử dụng hằng số đã thêm
                            System.out.println("   -> Matchmaking requested. Waiting for match...");
                            // Chờ thông báo đẩy GAME.MATCH_FOUND
                            break;

                        case MessageProtocol.Type.GAME_MATCH_FOUND:
                            System.out.println("   -> Match found!");
                            // Parse matchId từ payload
                            try {
                                currentMatchId = JsonUtils.getObjectMapper().convertValue(response.getPayload(), java.util.Map.class).get("matchId").toString();
                                System.out.println("   -> Match ID is: " + currentMatchId);
                            } catch (Exception e) {
                                System.err.println("   -> Failed to parse matchId from GAME.MATCH_FOUND payload");
                            }
                            // Chờ GAME.START
                            break;

                        case MessageProtocol.Type.GAME_START:
                            System.out.println("   -> Game started. Waiting for first round...");
                            // QUAN TRỌNG: Parse matchId từ GAME.START payload
                            try {
                                currentMatchId = JsonUtils.getObjectMapper().convertValue(response.getPayload(), java.util.Map.class).get("matchId").toString();
                                System.out.println("   -> Match ID from GAME.START: " + currentMatchId);
                            } catch (Exception e) {
                                System.err.println("   -> Failed to parse matchId from GAME.START payload");
                                e.printStackTrace();
                            }
                            // Chờ GAME.ROUND_START
                            break;

                        case MessageProtocol.Type.GAME_ROUND_START:
                            System.out.println("   -> Round started. Playing a random card (e.g., ID 1)...");
                            // Parse round number từ payload
                            try {
                                currentRound = (Integer) JsonUtils.getObjectMapper().convertValue(response.getPayload(), java.util.Map.class).get("roundNumber");
                                System.out.println("   -> Current round: " + currentRound);
                            } catch (Exception e) {
                                System.err.println("   -> Failed to parse roundNumber, using default: " + currentRound);
                            }
                            // TODO: Cần lấy danh sách availableCards từ payload và chọn một lá hợp lệ
                            testPlayCard(this, 1 + (int)(Math.random() * 5)); // Chơi 1 lá bài ngẫu nhiên (từ 1-5)
                            break;

                        case MessageProtocol.Type.GAME_CARD_PLAY_SUCCESS:
                            System.out.println("   -> Card played. Waiting for opponent/reveal...");
                            break;

                        case MessageProtocol.Type.GAME_CARD_PLAY_FAILURE:
                            System.err.println("   -> Card play FAILED! Server rejected card.");
                            // Parse failure details
                            try {
                                java.util.Map<String, Object> failurePayload = JsonUtils.getObjectMapper().convertValue(response.getPayload(), java.util.Map.class);
                                String reason = (String) failurePayload.get("reason");
                                Boolean canRetry = (Boolean) failurePayload.get("canRetry");
                                System.err.println("      Reason: " + reason);
                                System.err.println("      Can retry: " + canRetry);
                                
                                if (canRetry != null && canRetry) {
                                    // Retry với card khác (random lại)
                                    int newCardId = 1 + (int)(Math.random() * 36); // Random từ 1-36
                                    System.out.println("   -> Retrying with new card ID: " + newCardId);
                                    testPlayCard(this, newCardId);
                                } else {
                                    System.err.println("   -> Cannot retry. Waiting for timeout...");
                                }
                            } catch (Exception e) {
                                System.err.println("   -> Failed to parse failure payload: " + e.getMessage());
                            }
                            break;

                        case MessageProtocol.Type.GAME_OPPONENT_READY:
                            System.out.println("   -> Opponent has played. Waiting for reveal...");
                            break;

                        case MessageProtocol.Type.GAME_ROUND_REVEAL:
                            System.out.println("   -> Round revealed!");
                            // Logic game sẽ tự động gửi GAME.ROUND_START (nếu còn) hoặc GAME.END
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

        client.connectBlocking();
    }

    // --- Các hàm tạo request ---

    private static void testRegister(WebSocketClient client) {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setUsername(TEST_USERNAME);
        dto.setEmail(TEST_EMAIL);
        dto.setPassword(TEST_PASSWORD);
        dto.setDisplayName(TEST_USERNAME); // Dùng username làm display name

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.AUTH_REGISTER_REQUEST, dto);
        send(client, request);
    }

    private static void testLogin(WebSocketClient client) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.AUTH_LOGIN_REQUEST, dto);
        send(client, request);
    }

    private static void testRequestMatch(WebSocketClient client) {
        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.LOBBY_MATCH_REQUEST, null);
        request.setSessionId(currentSessionId); // Rất quan trọng!
        send(client, request);
    }

    private static void testPlayCard(WebSocketClient client, int cardId) {
        PlayCardRequestDto dto = new PlayCardRequestDto();
        dto.setGameId(currentMatchId);
        dto.setCardId(cardId);
        dto.setRoundNumber(currentRound); // Use current round number

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