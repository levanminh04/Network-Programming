package com.n9.core.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.SessionManager;
import com.n9.shared.protocol.ErrorInfo;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.MessageProtocol;
import com.n9.shared.util.JsonUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

/**
 * ClientConnectionHandler - Xử lý kết nối TCP từ Gateway.
 * Triển khai mô hình I/O Thread + Worker Pool và Length-Prefixed Framing.
 *
 * @version 1.3.0 (Implemented Length-Prefixed Framing)
 */
public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private final GameService gameService;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final ExecutorService pool;

    // THAY ĐỔI: Chuyển sang Data streams để xử lý byte và các kiểu nguyên thủy
    private DataInputStream in;
    private DataOutputStream out;

    private String currentSessionId = null;

    /**
     * Constructor được cập nhật để nhận cả ExecutorService.
     */
    public ClientConnectionHandler(
            Socket socket,
            GameService gameService,
            AuthService authService,
            SessionManager sessionManager,
            ExecutorService pool
    ) {
        this.socket = socket;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.pool = pool;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        System.out.println("✅ I/O Thread started for connection from: " + clientAddress);

        try {
            // THAY ĐỔI: Khởi tạo DataInputStream và DataOutputStream
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // THAY ĐỔI: Vòng lặp vô tận đọc theo cơ chế length-prefixed
            while (!socket.isClosed()) {
                // 1. Đọc 4 byte đầu tiên để biết độ dài tin nhắn
                int length = in.readInt();

                if (length > 0) {
                    // 2. Đọc chính xác `length` byte tiếp theo để có chuỗi JSON hoàn chỉnh
                    byte[] messageBytes = new byte[length];
                    in.readFully(messageBytes, 0, length);
                    final String messageLine = new String(messageBytes, StandardCharsets.UTF_8);

                    System.out.println("📨 I/O Thread received message of length: " + length);

                    // 3. Tạo một "Nhiệm vụ" để xử lý logic trong một luồng worker
                    Runnable processingTask = () -> {
                        MessageEnvelope request = null;
                        MessageEnvelope response;
                        try {
                            request = JsonUtils.fromJson(messageLine, MessageEnvelope.class);
                            if (request == null) {
                                response = new MessageEnvelope(MessageProtocol.Type.SYSTEM_ERROR, "unknown", null);
                                response.setError(new ErrorInfo("INVALID_JSON", "Invalid JSON format."));
                            } else {
                                // Worker thread thực sự gọi handleMessage
                                response = handleMessage(request);
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Worker thread caught error: " + e.getMessage());
                            response = MessageFactory.createErrorResponse(request, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.");
                        }

                        // Worker thread tự gửi response về Gateway
                        try {
                            String jsonResponse = JsonUtils.toJson(response);
                            sendMessage(jsonResponse); // sendMessage bây giờ cũng dùng DataOutputStream
                            System.out.println("📤 Worker thread sent: " + jsonResponse.substring(0, Math.min(100, jsonResponse.length())));
                        } catch (JsonProcessingException e) {
                            System.err.println("❌ Worker thread failed to serialize response: " + e.getMessage());
                        }
                    };

                    // 4. Luồng I/O giao việc và ngay lập tức quay lại chờ tin nhắn tiếp theo
                    pool.submit(processingTask);
                }
            }
        } catch (EOFException e) {
            // Đây là trường hợp bình thường khi Gateway đóng kết nối, không phải lỗi
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

    // handleMessage và các hàm handler khác giữ nguyên, vì chúng được gọi bởi worker thread
    // ...
    private MessageEnvelope handleMessage(MessageEnvelope envelope) {
        String type = envelope.getType();
        MessageEnvelope response;

        switch (type) {
            // ... (Tất cả các case của bạn)
            case MessageProtocol.Type.AUTH_REGISTER_REQUEST:
                response = handleRegister(envelope);
                break;
            case MessageProtocol.Type.AUTH_LOGIN_REQUEST:
                response = handleLogin(envelope);
                break;
            // ...
            default:
                response = MessageFactory.createErrorResponse(envelope, "UNKNOWN_TYPE", "Unknown message type: " + type);
        }

        if (response != null && response.getSessionId() != null && response.getError() == null) {
            this.currentSessionId = response.getSessionId();
        }

        return response;
    }
    private MessageEnvelope handleRegister(MessageEnvelope envelope) { return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Not implemented."); }
    private MessageEnvelope handleLogin(MessageEnvelope envelope) { return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Not implemented."); }
    private MessageEnvelope handleLogout(MessageEnvelope envelope) { return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Not implemented."); }
    private MessageEnvelope handleMatchRequest(MessageEnvelope envelope) { return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Not implemented."); }
    private MessageEnvelope handleGameStart(MessageEnvelope envelope) { return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Not implemented."); }
    private MessageEnvelope handlePlayCard(MessageEnvelope envelope) { return MessageFactory.createErrorResponse(envelope, "NOT_IMPLEMENTED", "Not implemented."); }


    /**
     * THAY ĐỔI: Gửi một tin nhắn theo định dạng length-prefixed.
     * Phương thức này là thread-safe nhờ từ khóa `synchronized`.
     */
    public synchronized void sendMessage(String jsonMessage) {
        try {
            if (out != null && !socket.isClosed()) {
                byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
                int length = jsonBytes.length;

                // 1. Gửi 4 byte độ dài của tin nhắn trước
                out.writeInt(length);
                // 2. Gửi nội dung tin nhắn (dưới dạng byte)
                out.write(jsonBytes);

                out.flush();
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to send message to " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }

    private void cleanup(String clientAddress) {
        if (this.currentSessionId != null) {
            sessionManager.removeSession(this.currentSessionId);
        }
        try {
            // THAY ĐỔI: Đóng các stream mới
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🧹 Connection cleaned up for: " + clientAddress);
        } catch (IOException e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
        }
    }
}