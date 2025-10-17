// Giả sử file này được đặt trong thư mục test của module `core`
// package com.n9.core; 

import com.n9.shared.model.dto.auth.LoginRequestDto;
import com.n9.shared.model.dto.auth.RegisterRequestDto;
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.model.dto.match.MatchStartDto;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.MessageProtocol;
import com.n9.shared.util.JsonUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Test Client để kiểm thử Core Server sau khi đã refactor.
 * Mô phỏng luồng: Register -> Login -> Game Start -> Play Card
 *
 * @version 2.1.0 (Refactored for MVP Protocol)
 */
public class TestCoreClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9090;

    public static void main(String[] args) {
        System.out.println("🧪 Test Core Client Starting (MVP Protocol)...");
        System.out.println();

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            System.out.println("✅ Connected to Core Server at " + HOST + ":" + PORT);
            System.out.println();

            // --- BẮT ĐẦU LUỒNG TEST LOGIC ---

            // Test 1: Đăng ký một tài khoản mới
            // Chúng ta không cần sessionId cho lần đầu đăng ký.
            String sessionIdAfterRegister = testRegister(in, out);
            if (sessionIdAfterRegister == null) {
                System.err.println("❌ Registration failed. Aborting further tests.");
                return;
            }

            // Test 2: Đăng nhập với tài khoản vừa tạo
            String sessionIdAfterLogin = testLogin(in, out);
            if (sessionIdAfterLogin == null) {
                System.err.println("❌ Login failed. Aborting further tests.");
                return;
            }
            System.out.println("🔑 Using Session ID for subsequent tests: " + sessionIdAfterLogin);
            System.out.println();

            // Test 3: Bắt đầu một trận game (giả lập)
            // Yêu cầu này cần có sessionId hợp lệ
            testGameStart(in, out, sessionIdAfterLogin);

            // Test 4: Chơi một lá bài
            // Yêu cầu này cũng cần sessionId hợp lệ
            testPlayCard(in, out, sessionIdAfterLogin, "match-mvp-123", 25);

            System.out.println("\n✅ All MVP tests completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ A critical error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test AUTH.REGISTER_REQUEST
     * @return sessionId nếu đăng ký thành công, ngược lại trả về null.
     */
    private static String testRegister(BufferedReader in, BufferedWriter out) throws Exception {
        System.out.println("=== Test 1: AUTH.REGISTER_REQUEST ===");

        RegisterRequestDto registerDto = new RegisterRequestDto();
        // Sử dụng một username ngẫu nhiên để mỗi lần chạy test đều thành công
        String username = "testuser" + System.currentTimeMillis() % 1000;
        registerDto.setUsername(username);
        registerDto.setEmail(username + "@test.com");
        registerDto.setPassword("password123");
        registerDto.setDisplayName("Test User");

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.AUTH_REGISTER_REQUEST, registerDto);

        // Gửi và nhận phản hồi
        MessageEnvelope response = sendAndReceive(in, out, request);

        if (response != null && MessageProtocol.Type.AUTH_REGISTER_SUCCESS.equals(response.getType())) {
            return response.getSessionId();
        }
        return null;
    }

    /**
     * Test AUTH.LOGIN_REQUEST
     * @return sessionId nếu đăng nhập thành công, ngược lại trả về null.
     */
    private static String testLogin(BufferedReader in, BufferedWriter out) throws Exception {
        System.out.println("=== Test 2: AUTH.LOGIN_REQUEST ===");

        LoginRequestDto loginDto = new LoginRequestDto();
        loginDto.setUsername("alice"); // Giả sử user "alice" đã tồn tại
        loginDto.setPassword("password123");

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.AUTH_LOGIN_REQUEST, loginDto);

        MessageEnvelope response = sendAndReceive(in, out, request);

        if (response != null && MessageProtocol.Type.AUTH_LOGIN_SUCCESS.equals(response.getType())) {
            return response.getSessionId();
        }
        return null;
    }

    /**
     * Test GAME.START message
     */
    private static void testGameStart(BufferedReader in, BufferedWriter out, String sessionId) throws Exception {
        System.out.println("=== Test 3: GAME.START ===");

        MatchStartDto startDto = new MatchStartDto();
        startDto.setMatchId("match-mvp-123");

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.GAME_START, startDto);
        request.setSessionId(sessionId); // Gắn sessionId vào request

        sendAndReceive(in, out, request);
    }

    /**
     * Test GAME.CARD_PLAY_REQUEST message
     */
    private static void testPlayCard(BufferedReader in, BufferedWriter out,
                                     String sessionId, String matchId, int cardId) throws Exception {
        System.out.println("=== Test 4: GAME.CARD_PLAY_REQUEST ===");

        PlayCardRequestDto playDto = new PlayCardRequestDto();
        playDto.setGameId(matchId);
        playDto.setRoundNumber(1);
        playDto.setCardId(cardId);

        MessageEnvelope request = MessageFactory.createRequest(MessageProtocol.Type.GAME_CARD_PLAY_REQUEST, playDto);
        request.setSessionId(sessionId); // Gắn sessionId vào request

        sendAndReceive(in, out, request);
    }

    /**
     * Hàm tiện ích để gửi một request và nhận lại response.
     */
    private static MessageEnvelope sendAndReceive(BufferedReader in, BufferedWriter out, MessageEnvelope request) throws Exception {
        String jsonRequest = JsonUtils.toJson(request);
        System.out.println("📤 Sending: " + jsonRequest);

        out.write(jsonRequest);
        out.newLine();
        out.flush();

        String jsonResponse = in.readLine();
        System.out.println("📥 Received: " + jsonResponse);

        MessageEnvelope responseEnvelope = JsonUtils.fromJson(jsonResponse, MessageEnvelope.class);
        if (responseEnvelope != null) {
            System.out.println("   Type: " + responseEnvelope.getType());
            System.out.println("   CorrelationId: " + responseEnvelope.getCorrelationId());
            if (responseEnvelope.getError() != null) {
                System.err.println("   Error: " + responseEnvelope.getError().getCode() + " - " + responseEnvelope.getError().getMessage());
            }
        }
        System.out.println();
        Thread.sleep(500); // Chờ một chút giữa các lần test
        return responseEnvelope;
    }
}