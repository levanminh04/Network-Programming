package com.N9.gateway.service;

import com.N9.gateway.websocket.GatewayWebSocketHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// THÊM CÁC IMPORT NÀY
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
// --------------------
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class CoreTcpClient implements InitializingBean, DisposableBean {

    private static final String CORE_HOST = "localhost";
    private static final int CORE_PORT = 9090;

    private Socket socket;
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

        // THAY ĐỔI: Thêm Buffered streams để tăng hiệu năng và đồng bộ với Core
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        System.out.println("✅ Connected to Core Server.");
    }

    /**
     * Triển khai logic Length-Prefixed Framing khi gửi.
     */
    public synchronized void sendMessageToCore(String jsonMessage) {
        try {
            if (out != null && !socket.isClosed()) {
                byte[] jsonBytes = jsonMessage.getBytes(StandardCharsets.UTF_8);
                int length = jsonBytes.length;

                out.writeInt(length);
                out.write(jsonBytes);
                out.flush(); // Đẩy dữ liệu đi ngay
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to send message to Core: " + e.getMessage());
        }
    }

    /**
     * Triển khai logic đọc Length-Prefixed Framing khi nhận.
     */
    private void startListening() {
        new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    int length = in.readInt(); // Chờ Core nói trước (chờ WELCOME)

                    if (length > 0) {
                        byte[] messageBytes = new byte[length];
                        in.readFully(messageBytes, 0, length);
                        String lineFromCore = new String(messageBytes, StandardCharsets.UTF_8);

                        System.out.println("Core -> Gateway: " + lineFromCore);
                        webSocketHandler.forwardMessageToClient(lineFromCore);
                    }
                }
            } catch (EOFException e) {
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
