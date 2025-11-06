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

    // sổ ghi nhớ: correlationId -> WebSocketSession của client nào đã gửi
    private final ConcurrentHashMap<String, WebSocketSession> pendingRequests = new ConcurrentHashMap<>();

    // Danh bạ ghi nhớ: sessionId -> WebSocketSession (để gửi thông báo đẩy)
    private final ConcurrentHashMap<String, WebSocketSession> activeClientSessions = new ConcurrentHashMap<>();

    // WebSocket session ID -> sessionId (để cleanup khi disconnect)
    private final ConcurrentHashMap<String, String> sessionWsMap = new ConcurrentHashMap<>();

    public GatewayWebSocketHandler(CoreTcpClient coreTcpClient) {
        this.coreTcpClient = coreTcpClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Frontend connected: " + session.getId());
    }


    // handleTextMessage là phương thức được Spring Framework tự động gọi bất cứ khi nào
    // máy chủ nhận được một tin nhắn dạng văn bản (text) từ một client (ví dụ: trình duyệt web) thông qua kết nối WebSocket.
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

            // Nếu client gửi sessionId, cập nhật danh bạ
            if (envelope.getSessionId() != null) {
                activeClientSessions.put(envelope.getSessionId(), session);
                // Lưu reverse mapping để cleanup sau này
                sessionWsMap.put(session.getId(), envelope.getSessionId());
            }

            // Chuyển tiếp tin nhắn y hệt đến Core Server
            coreTcpClient.sendMessageToCore(jsonPayload);
        } catch (Exception e) {

        }
    }

    /**
     * Được CoreTcpClient gọi khi có tin nhắn từ Core.
     * Tìm đúng client để gửi tin nhắn trả về.
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
                System.out.println("No client session found for message. It might be a broadcast or the client disconnected.");
            }
        } catch (Exception e) {

        }
    }



//    Phát hiện khi WebSocket đóng (user đóng tab, mất mạng, crash browser)
//    Gửi thông báo AUTH.LOGOUT_REQUEST đến Core để dọn dẹp
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        // 1. Tìm sessionId của kết nối vừa đóng
        String sessionId = sessionWsMap.remove(session.getId());
        
        if (sessionId != null) {
            // 2. Xóa khỏi map "danh bạ"
            activeClientSessions.remove(sessionId);


            // Tạo và gửi một tin nhắn AUTH.LOGOUT đến Core
            // Core sẽ nhận tin này và kích hoạt logic cleanup (bao gồm cả forfeit)
            try {
                MessageEnvelope logoutEnvelope = new MessageEnvelope();
                logoutEnvelope.setType(MessageProtocol.Type.AUTH_LOGOUT_REQUEST);
                logoutEnvelope.setSessionId(sessionId);

                String logoutJson = JsonUtils.toJson(logoutEnvelope);
                coreTcpClient.sendMessageToCore(logoutJson);

            } catch (Exception e) {

            }
        }
        
        // Dọn dẹp pendingRequests (như cũ)
        pendingRequests.values().removeIf(s -> Objects.equals(s.getId(), session.getId()));
    }
}