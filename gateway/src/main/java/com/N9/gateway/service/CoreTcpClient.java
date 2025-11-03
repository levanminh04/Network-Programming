package com.N9.gateway.service;

import com.N9.gateway.websocket.GatewayWebSocketHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.protocol.MessageEnvelope; // THÊM
import com.n9.shared.protocol.MessageFactory; // THÊM
import com.n9.shared.util.JsonUtils; // THÊM
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
// THÊM CÁC IMPORT NÀY
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CoreTcpClient implements InitializingBean, DisposableBean {

    private static final String CORE_HOST = "localhost";
    private static final int CORE_PORT = 9090;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private final GatewayWebSocketHandler webSocketHandler;

    private ScheduledExecutorService heartbeatScheduler;


//    Khi nào nên dùng @Lazy:
//    Bean nặng, khởi tạo tốn thời gian hoặc tài nguyên (ví dụ: kết nối remote API, đọc file lớn, v.v.).
//    Bean ít khi được dùng, không cần khởi tạo sớm.
//    Tránh vòng lặp phụ thuộc (circular dependency) trong injection.
    public CoreTcpClient(@Lazy GatewayWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        startListening();
        startHeartbeat();
    }

    // kết nối tới core
    private void connect() throws IOException {
        this.socket = new Socket(CORE_HOST, CORE_PORT);
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }


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
            // TODO: Triển khai logic reconnect nếu cần
        }
    }


    private void startListening() {
        new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    int length = in.readInt(); // Chờ Core nói (welcome, response, notification)
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
                stopHeartbeat(); // Dừng gửi ping nếu kết nối mất
            }
        }, "core-tcp-listener").start();
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                // Chỉ gửi ping nếu kết nối đang mở
                if (socket != null && socket.isConnected() && !socket.isClosed()) {
                    System.out.println("💓 Sending PING to Core Server...");
                    // THAY ĐỔI: Đảm bảo dùng đúng class Protocol
                    MessageEnvelope ping = MessageFactory.createRequest(MessageProtocol.Type.SYSTEM_PING, null);
                    sendMessageToCore(JsonUtils.toJson(ping));
                }
            } catch (Exception e) {
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("💓 Heartbeat service started. Sending PING every " + HEARTBEAT_INTERVAL_SECONDS + " seconds.");
    }

    private void stopHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            System.out.println("💓 Heartbeat service stopped.");
        }
    }


    @Override
    public void destroy() throws Exception {
        stopHeartbeat(); // Dừng heartbeat khi tắt
        System.out.println("🔌 Closing connection to Core Server...");
        if (socket != null && !socket.isClosed()) socket.close();
        if (in != null) in.close();
        if (out != null) out.close();
    }
}

