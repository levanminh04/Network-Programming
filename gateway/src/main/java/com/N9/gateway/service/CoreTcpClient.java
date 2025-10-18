package com.N9.gateway.service;

import com.N9.gateway.websocket.GatewayWebSocketHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class CoreTcpClient implements InitializingBean, DisposableBean {

    private static final String CORE_HOST = "localhost";
    private static final int CORE_PORT = 9090;

    private Socket socket;
    // THAY ĐỔI: Sử dụng DataOutputStream và DataInputStream
    private DataOutputStream out;
    private DataInputStream in;

    private final GatewayWebSocketHandler webSocketHandler;

    public CoreTcpClient(@Lazy GatewayWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        startListening();
    }

    private void connect() throws IOException {
        System.out.println("🔌 Connecting to Core Server at " + CORE_HOST + ":" + CORE_PORT + "...");
        this.socket = new Socket(CORE_HOST, CORE_PORT);
        // THAY ĐỔI: Khởi tạo các stream mới
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
        System.out.println("✅ Connected to Core Server.");
    }

    /**
     * THAY ĐỔI: Triển khai logic Length-Prefixed Framing khi gửi.
     */
    public synchronized void sendMessageToCore(String jsonMessage) {
        try {
            if (out != null && !socket.isClosed()) {
                byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
                int length = jsonBytes.length;

                // 1. Gửi 4 byte độ dài của tin nhắn trước
                out.writeInt(length);

                // 2. Gửi nội dung tin nhắn
                out.write(jsonBytes);

                out.flush(); // Đẩy dữ liệu đi ngay
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to send message to Core: " + e.getMessage());
        }
    }

    /**
     * THAY ĐỔI: Triển khai logic đọc Length-Prefixed Framing khi nhận.
     */
    private void startListening() {
        new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    // 1. Đọc 4 byte đầu tiên để biết độ dài gói tin
                    int length = in.readInt();

                    if (length > 0) {
                        // 2. Đọc chính xác `length` byte tiếp theo
                        byte[] messageBytes = new byte[length];
                        in.readFully(messageBytes, 0, length);
                        String lineFromCore = new String(messageBytes, StandardCharsets.UTF_8);

                        System.out.println("Core -> Gateway: " + lineFromCore);
                        webSocketHandler.forwardMessageToClient(lineFromCore);

                    }
                }
            } catch (EOFException e) {
                // Đây là trường hợp bình thường khi Core đóng kết nối
                System.err.println("💔 Connection to Core closed gracefully.");
            } catch (IOException e) {
                System.err.println("💔 Connection to Core lost: " + e.getMessage());
            } finally {
                System.out.println("🛑 Listener thread for Core stopped.");
            }
        }, "core-tcp-listener").start();
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("🔌 Closing connection to Core Server...");
        if (socket != null && !socket.isClosed()) socket.close();
        if (in != null) in.close();
        if (out != null) out.close();
    }
}