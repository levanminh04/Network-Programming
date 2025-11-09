import { useEffect, useState } from "react";
import { useWebSocketService } from "../websocket/WebSocketTest";

export default function Login() {
  const { isConnected, lastMessage, sendMessage } = useWebSocketService();
  const [form, setForm] = useState({ username: "", password: "" });
  const [status, setStatus] = useState("");

  // 🔁 Lắng nghe message từ backend
  useEffect(() => {
    if (!lastMessage) return;

    switch (lastMessage.type) {
      case "AUTH.LOGIN_SUCCESS":
        setStatus(`✅ Welcome ${lastMessage.payload.displayName || form.username}`);
        localStorage.setItem("token", lastMessage.payload.token);
        break;

      case "AUTH.LOGIN_FAILURE":
        setStatus(`❌ ${lastMessage.payload.error || "Login failed"}`);
        break;

      default:
        break;
    }
  }, [lastMessage]);

  const handleSubmit = (e) => {
    e.preventDefault();
    sendMessage("AUTH.LOGIN_REQUEST", form);
    setStatus("⏳ Logging in...");
  };

  return (
    <div className="max-w-sm mx-auto p-4 rounded-2xl shadow-lg bg-white">
      <h2 className="text-xl font-bold mb-4 text-center">Login</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          className="border p-2 w-full mb-2 rounded"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
        />
        <input
          type="password"
          placeholder="Password"
          className="border p-2 w-full mb-2 rounded"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <button
          type="submit"
          disabled={!isConnected}
          className={`p-2 w-full rounded text-white ${
            isConnected ? "bg-blue-600 hover:bg-blue-700" : "bg-gray-400"
          }`}
        >
          {isConnected ? "Login" : "Connecting..."}
        </button>
      </form>
      {status && <p className="mt-3 text-center text-sm">{status}</p>}
    </div>
  );
}
