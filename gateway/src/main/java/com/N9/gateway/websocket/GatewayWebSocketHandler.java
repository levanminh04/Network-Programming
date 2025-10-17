package com.N9.gateway.websocket;

import com.N9.gateway.service.CoreTcpClient;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.MessageProtocol;
import com.n9.shared.util.JsonUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GatewayWebSocketHandler extends TextWebSocketHandler {

    private final CoreTcpClient coreTcpClient;

    // "Cuốn sổ" ghi nhớ: correlationId -> WebSocketSession của client nào đã gửi
    private final ConcurrentHashMap<String, WebSocketSession> pendingRequests = new ConcurrentHashMap<>();

    // "Danh bạ" ghi nhớ: sessionId -> WebSocketSession (để gửi thông báo đẩy)
    private final ConcurrentHashMap<String, WebSocketSession> activeClientSessions = new ConcurrentHashMap<>();

    public GatewayWebSocketHandler(CoreTcpClient coreTcpClient) {
        this.coreTcpClient = coreTcpClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Frontend connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String jsonPayload = message.getPayload();
        System.out.println("Frontend -> Gateway: " + jsonPayload);

        try {
            MessageEnvelope envelope = JsonUtils.fromJson(jsonPayload, MessageEnvelope.class);
            if (envelope == null) return;

            // Lưu lại "ai đã hỏi gì" để biết đường trả lời
            if (envelope.getCorrelationId() != null) {
                pendingRequests.put(envelope.getCorrelationId(), session);
            }

            // Nếu client gửi sessionId, cập nhật "danh bạ" của chúng ta
            if (envelope.getSessionId() != null) {
                activeClientSessions.put(envelope.getSessionId(), session);
            }

            // Chuyển tiếp tin nhắn y hệt đến Core Server
            coreTcpClient.sendMessageToCore(jsonPayload);
        } catch (Exception e) {
            System.err.println("❌ Error processing message from Frontend: " + e.getMessage());
        }
    }

    /**
     * Được CoreTcpClient gọi khi có tin nhắn từ Core.
     * Nhiệm vụ: Tìm đúng client để gửi tin nhắn trả về.
     */
    public void forwardMessageToClient(String jsonMessageFromCore) {
        try {
            MessageEnvelope envelope = JsonUtils.fromJson(jsonMessageFromCore, MessageEnvelope.class);
            if (envelope == null) return;

            WebSocketSession clientSession = null;

            // Ưu tiên 1: Nếu là tin nhắn response, tìm client qua correlationId
            if (envelope.getCorrelationId() != null) {
                clientSession = pendingRequests.get(envelope.getCorrelationId());
            }

            // Ưu tiên 2: Nếu là tin nhắn đẩy (notification), tìm client qua sessionId
            // Đây là cách xử lý `GAME_MATCH_FOUND`
            if (clientSession == null && envelope.getSessionId() != null) {
                clientSession = activeClientSessions.get(envelope.getSessionId());
            }

            // Gửi tin nhắn nếu tìm thấy client và kết nối còn mở
            if (clientSession != null && clientSession.isOpen()) {
                clientSession.sendMessage(new TextMessage(jsonMessageFromCore));

                // Nếu là response, xóa yêu cầu khỏi danh sách chờ để giải phóng bộ nhớ
                if (envelope.getCorrelationId() != null) {
                    pendingRequests.remove(envelope.getCorrelationId());
                }
            } else {
                // TODO: Xử lý trường hợp broadcast (gửi cho tất cả mọi người)
                System.out.println("No client session found for message. It might be a broadcast or the client disconnected.");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to forward message to client: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("Frontend disconnected: " + session.getId() + " with status: " + status);

        // Dọn dẹp: Xóa session này khỏi tất cả các map quản lý
        pendingRequests.values().removeIf(s -> Objects.equals(s.getId(), session.getId()));
        activeClientSessions.values().removeIf(s -> Objects.equals(s.getId(), session.getId()));
    }
}