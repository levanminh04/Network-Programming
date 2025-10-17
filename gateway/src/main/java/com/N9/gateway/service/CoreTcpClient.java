package com.N9.gateway.service;

import com.N9.gateway.websocket.GatewayWebSocketHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class CoreTcpClient implements InitializingBean, DisposableBean {

    // Cấu hình địa chỉ Core ở đây hoặc trong application.properties
    private static final String CORE_HOST = "localhost";
    private static final int CORE_PORT = 9090;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    private final GatewayWebSocketHandler webSocketHandler;

    // @Lazy để giải quyết việc phụ thuộc vòng tròn: TcpClient cần WsHandler và ngược lại
    public CoreTcpClient(@Lazy GatewayWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    // Được Spring gọi tự động sau khi Gateway khởi động xong
    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        startListening();
    }

    private void connect() throws IOException {
        System.out.println("🔌 Connecting to Core Server at " + CORE_HOST + ":" + CORE_PORT + "...");
        this.socket = new Socket(CORE_HOST, CORE_PORT);
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        System.out.println("✅ Connected to Core Server.");
    }

    /**
     * Gửi một tin nhắn đến Core Server.
     * Đây là phương thức mà WebSocketHandler sẽ gọi.
     * `synchronized` để đảm bảo các tin nhắn từ nhiều client không bị trộn lẫn.
     */
    public synchronized void sendMessageToCore(String jsonMessage) {
        try {
            if (writer != null && !socket.isClosed()) {
                writer.write(jsonMessage);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to send message to Core: " + e.getMessage());
            // TODO: Triển khai logic reconnect nếu cần
        }
    }

    /**
     * Khởi động một luồng riêng để lắng nghe tin nhắn từ Core.
     * Việc này cực kỳ quan trọng để Gateway không bị "đứng hình" khi chờ tin.
     */
    private void startListening() {
        new Thread(() -> {
            try {
                String lineFromCore;
                while ((lineFromCore = reader.readLine()) != null) {
                    System.out.println("Core -> Gateway: " + lineFromCore);
                    // Khi nhận được tin từ Core, chuyển tiếp nó cho WebSocketHandler xử lý
                    webSocketHandler.forwardMessageToClient(lineFromCore);
                }
            } catch (IOException e) {
                System.err.println("💔 Connection to Core lost: " + e.getMessage());
            } finally {
                System.out.println("🛑 Listener thread for Core stopped.");
                // TODO: Triển khai logic reconnect
            }
        }, "core-tcp-listener").start();
    }

    // Được Spring gọi tự động khi Gateway tắt
    @Override
    public void destroy() throws Exception {
        System.out.println("🔌 Closing connection to Core Server...");
        if (socket != null && !socket.isClosed()) socket.close();
        if (reader != null) reader.close();
        if (writer != null) writer.close();
    }
}