import { useEffect, useState } from "react";
import { useWebSocketService } from "../websocket/WebSocketTest";

export default function Register() {
  const { isConnected, lastMessage, sendMessage } = useWebSocketService();
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [status, setStatus] = useState("");

  // 🔁 Lắng message từ backend
  useEffect(() => {
    if (!lastMessage) return;

    switch (lastMessage.type) {
      case "AUTH.REGISTER_SUCCESS":
        setStatus("✅ Register success! You can now log in.");
        break;

      case "AUTH.REGISTER_FAILURE":
        setStatus(`❌ ${lastMessage.payload.error || "Register failed"}`);
        break;

      default:
        break;
    }
  }, [lastMessage]);

  const handleSubmit = (e) => {
    e.preventDefault();
    sendMessage("AUTH.REGISTER_REQUEST", form);
    setStatus("⏳ Registering...");
  };

  return (
    <div className="max-w-sm mx-auto p-4 rounded-2xl shadow-lg bg-white">
      <h2 className="text-xl font-bold mb-4 text-center">Register</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          className="border p-2 w-full mb-2 rounded"
          value={form.username}
          onChange={(e) => setForm({ ...form, username: e.target.value })}
        />
        <input
          type="email"
          placeholder="Email"
          className="border p-2 w-full mb-2 rounded"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
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
            isConnected ? "bg-green-600 hover:bg-green-700" : "bg-gray-400"
          }`}
        >
          {isConnected ? "Register" : "Connecting..."}
        </button>
      </form>
      {status && <p className="mt-3 text-center text-sm">{status}</p>}
    </div>
  );
}
