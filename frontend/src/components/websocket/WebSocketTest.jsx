import { useEffect, useRef, useState, useCallback } from "react";

// 🧠 Tạo gói MessageEnvelope theo đúng chuẩn backend
const createEnvelope = (type, payload = {}, sessionId = null) => ({
  type,
  correlationId: Date.now().toString(), // ID duy nhất để backend mapping request-response
  sessionId,
  payload,
  error: null,
});

// 🧩 Hook WebSocket dùng chung cho toàn app
export const useWebSocketService = (url = "ws://localhost:9090/ws") => {
  const [isConnected, setIsConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const socketRef = useRef(null);
  const sessionIdRef = useRef(
    sessionStorage.getItem("sessionId") || null // 🧠 lấy sessionId nếu đã lưu
  );

  // 🔁 Hàm kết nối
  const connect = useCallback(() => {
    const socket = new WebSocket(url);
    socketRef.current = socket;

    socket.onopen = () => {
      console.log("✅ Connected to Gateway:", url);
      setIsConnected(true);
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log("📩 Received:", data);
        setLastMessage(data);

        // 🧠 Nếu backend trả sessionId (sau login/register success) thì lưu lại
        if (data.sessionId) {
          sessionIdRef.current = data.sessionId;
          sessionStorage.setItem("sessionId", data.sessionId);
          console.log("💾 Saved sessionId:", data.sessionId);
        }
      } catch (err) {
        console.error("⚠️ Invalid JSON from server:", event.data);
      }
    };

    socket.onclose = () => {
      console.warn("❌ WebSocket closed. Reconnecting in 3s...");
      setIsConnected(false);
      setTimeout(connect, 3000); // tự reconnect
    };

    socket.onerror = (err) => console.error("⚠️ WebSocket error:", err);
  }, [url]);

  // Kết nối ngay khi mount
  useEffect(() => {
    connect();
    return () => socketRef.current?.close();
  }, [connect]);

  // ✉️ Gửi message có sessionId (tự động thêm)
  const sendMessage = useCallback((type, payload = {}) => {
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      const sessionId =
        sessionIdRef.current || sessionStorage.getItem("sessionId") || null;
      const envelope = createEnvelope(type, payload, sessionId);
      socket.send(JSON.stringify(envelope));
      console.log("🚀 Sent:", envelope);
    } else {
      console.warn("❌ Cannot send message — WebSocket not open");
    }
  }, []);

  // 🧼 Hàm xóa session khi logout
  const clearSession = useCallback(() => {
    sessionIdRef.current = null;
    sessionStorage.removeItem("sessionId");
    console.log("🧹 Cleared sessionId");
  }, []);

  return {
    isConnected,
    lastMessage,
    sendMessage,
    clearSession,
  };
};
