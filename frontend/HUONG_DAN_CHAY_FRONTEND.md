# HƯỚNG DẪN CHẠY FRONTEND

## 📋 Tổng quan

File `AppSingleFile.jsx` là một ứng dụng React hoàn chỉnh, tích hợp đầy đủ các chức năng:
- ✅ Xác thực (Đăng nhập/Đăng ký)
- ✅ Tìm trận
- ✅ Chơi game real-time
- ✅ WebSocket protocol tuân thủ backend

## 🚀 Cách 1: Chạy với AppSingleFile.jsx (KHUYẾN NGHỊ)

### Bước 1: Cập nhật main.jsx

Mở file `src/main.jsx` và thay đổi import:

```jsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
// THAY ĐỔI: Import AppSingleFile thay vì App
import App from './AppSingleFile.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

### Bước 2: Chạy frontend

```bash
cd d:\Project\Network-Programming\frontend
npm run dev
```

### Bước 3: Mở trình duyệt

Truy cập: http://localhost:5173

## 🎮 Cách sử dụng

### 1. Đăng ký tài khoản mới
- Nhấn tab "Đăng ký"
- Nhập username (3-50 ký tự)
- Nhập email (định dạng hợp lệ)
- Nhập password (tối thiểu 6 ký tự)
- Nhập tên hiển thị (tùy chọn)
- Nhấn "Đăng ký"

### 2. Đăng nhập
- Nhấn tab "Đăng nhập"
- Nhập username và password
- Nhấn "Đăng nhập"

### 3. Tìm trận
- Sau khi đăng nhập, bạn sẽ vào sảnh chờ
- Nhấn nút "🎯 Tìm trận"
- Chờ hệ thống tìm đối thủ

### 4. Chơi game
- Khi tìm thấy đối thủ, game sẽ tự động bắt đầu
- Mỗi round có 10 giây để chọn bài
- Nhấn vào lá bài để chọn
- Xem kết quả sau khi cả 2 người chơi đã chọn
- Chơi 3 rounds để quyết định người thắng

## 🔧 Cách 2: Tích hợp vào cấu trúc hiện có

Nếu bạn muốn giữ cấu trúc routing hiện tại, bạn có thể:

1. Tách các component từ `AppSingleFile.jsx`:
   - `AuthView` → `src/components/auth/AuthView.jsx`
   - `LobbyView` → `src/components/lobby/LobbyView.jsx`
   - `GameView` → `src/components/game/GameView.jsx`

2. Sử dụng Context và WebSocket hook trong `App.jsx` hiện tại

## 📡 Yêu cầu Backend

Đảm bảo các service đang chạy:

```bash
# Terminal 1: Chạy Core Server (Port 9090)
cd d:\Project\Network-Programming\core
mvn exec:java -Dexec.mainClass="com.n9.core.CoreServer"

# Terminal 2: Chạy Gateway (Port 8080)
cd d:\Project\Network-Programming\gateway
mvn spring-boot:run
```

## 🐛 Troubleshooting

### Lỗi: "Cannot connect to WebSocket"
- Kiểm tra Gateway đang chạy trên port 8080
- Kiểm tra URL WebSocket: `ws://localhost:8080/ws`

### Lỗi: "Authentication failed"
- Kiểm tra Core Server đang chạy
- Kiểm tra database đã được setup

### Lỗi: "Cannot find module"
- Chạy `npm install` để cài đặt dependencies

## 📝 Protocol Messages

### Auth Messages
- `AUTH.LOGIN_REQUEST` - Đăng nhập
- `AUTH.LOGIN_SUCCESS` - Đăng nhập thành công
- `AUTH.LOGIN_FAILURE` - Đăng nhập thất bại
- `AUTH.REGISTER_REQUEST` - Đăng ký
- `AUTH.REGISTER_SUCCESS` - Đăng ký thành công
- `AUTH.REGISTER_FAILURE` - Đăng ký thất bại

### Lobby Messages
- `LOBBY.MATCH_REQUEST` - Tìm trận
- `LOBBY.MATCH_REQUEST_ACK` - Xác nhận tìm trận
- `GAME.MATCH_FOUND` - Đã tìm thấy trận

### Game Messages
- `GAME.START` - Bắt đầu game
- `GAME.ROUND_START` - Bắt đầu round
- `GAME.CARD_PLAY_REQUEST` - Chơi bài
- `GAME.CARD_PLAY_SUCCESS` - Chơi bài thành công
- `GAME.OPPONENT_READY` - Đối thủ đã sẵn sàng
- `GAME.ROUND_REVEAL` - Công bố kết quả round
- `GAME.END` - Kết thúc game

## 🎨 Tính năng UI

- ✅ Responsive design (hoạt động tốt trên mobile, tablet, desktop)
- ✅ Tailwind CSS styling
- ✅ Real-time countdown timer
- ✅ Card animations
- ✅ Connection status indicator
- ✅ Error/Success message handling
- ✅ Loading states
- ✅ Auto-reconnect WebSocket

## 📦 Dependencies

Các package được sử dụng (đã có trong package.json):
- react: ^19.1.1
- react-dom: ^19.1.1
- react-router-dom: ^7.8.2 (không bắt buộc cho AppSingleFile)
- vite: ^7.1.2
- tailwindcss: ^4.1.13

## 🔐 Security Notes

⚠️ **LƯU Ý**: Đây là MVP cho mục đích học tập:
- Password được gửi dưới dạng plaintext
- Không có HTTPS
- Không có rate limiting
- Không phù hợp cho production

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Console của trình duyệt (F12) để xem logs
2. Terminal của Gateway và Core để xem server logs
3. Network tab để xem WebSocket messages

---

**Tác giả**: AI Assistant (GitHub Copilot)
**Ngày tạo**: November 1, 2025
**Phiên bản**: 1.0.0
