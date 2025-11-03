# 📊 Phân Tích Tính Khả Thi - Tính Năng 1: LEADERBOARD (Bảng Xếp Hạng)

> **Tài liệu**: Feasibility Analysis - Leaderboard MVP  
> **Tác giả**: Solution Architect & Senior Technical Advisor  
> **Ngày**: November 3, 2025  
> **Phiên bản**: 1.0.0  
> **Liên kết**: Tài liệu nền tảng cho Feature 2 (Presence) và Feature 3 (Challenge)

---

## 📋 MỤC LỤC

1. [Tổng Quan Tính Năng](#1-tổng-quan-tính-năng)
2. [Đánh Giá Khả Thi & Tận Dụng Tài Sản Hiện Có](#2-đánh-giá-khả-thi--tận-dụng-tài-sản-hiện-có)
3. [Impact Analysis - Các Thành Phần Bị Ảnh Hưởng](#3-impact-analysis---các-thành-phần-bị-ảnh-hưởng)
4. [Protocol & Luồng Dữ Liệu E2E](#4-protocol--luồng-dữ-liệu-e2e)
5. [Implementation Plan Chi Tiết](#5-implementation-plan-chi-tiết)
6. [Estimation & Risk Assessment](#6-estimation--risk-assessment)
7. [Testing Strategy](#7-testing-strategy)
8. [Kết Luận & Khuyến Nghị](#8-kết-luận--khuyến-nghị)

---

## 1. TỔNG QUAN TÍNH NĂNG

### 🎯 Mô Tả

Người chơi đang ở **Lobby** có thể mở giao diện "Bảng Xếp Hạng" để xem:
- Danh sách người chơi được sắp xếp theo **số trận thắng** (`games_won`)
- Thông tin cơ bản: Username, Display Name, Games Played, Games Won
- (Tùy chọn) Win Rate (Tỷ lệ thắng = games_won / games_played)

### 🎨 UI/UX Flow

```
[LOBBY VIEW]
    |
    | User nhấn nút "🏆 Bảng Xếp Hạng"
    |
    ▼
[LOADING...]
    |
    | Frontend gửi LOBBY.GET_LEADERBOARD_REQUEST
    |
    ▼
[LEADERBOARD MODAL/VIEW]
    ┌─────────────────────────────────────────┐
    │  🏆 TOP PLAYERS                         │
    ├────┬──────────┬─────────┬──────┬───────┤
    │ #  │ USERNAME │ PLAYED  │ WON  │ RATE  │
    ├────┼──────────┼─────────┼──────┼───────┤
    │ 1  │ alice    │   50    │  42  │ 84%   │
    │ 2  │ bob      │   35    │  28  │ 80%   │
    │ 3  │ charlie  │   20    │  15  │ 75%   │
    │... │   ...    │   ...   │ ...  │ ...   │
    └────┴──────────┴─────────┴──────┴───────┘
    
    [Đóng] [Làm mới]
```

### 📐 Scope MVP

**TRONG PHẠM VI (MVP Phase 1)**:
- ✅ Hiển thị Top N người chơi (ví dụ: Top 50)
- ✅ Sắp xếp theo `games_won` DESC
- ✅ Hiển thị: Rank, Username, Display Name, Games Played, Games Won
- ✅ Nút "Refresh" để làm mới dữ liệu

**NGOÀI PHẠM VI (Defer to Phase 2)**:
- ❌ Pagination (phân trang)
- ❌ Filter theo rank_tier (BRONZE, SILVER...)
- ❌ Search/Filter theo username
- ❌ Real-time auto-refresh (sẽ làm ở Feature 2)
- ❌ Hiển thị avatar/icon

---

## 2. ĐÁNH GIÁ KHẢ THI & TẬN DỤNG TÀI SẢN HIỆN CÓ

### ✅ TÀI SẢN SẴN CÓ TRONG DATABASE

#### **Bảng `user_profiles` - 100% Ready**

```sql
-- File: DB_SCRIPT.sql (Line ~70-110)
CREATE TABLE user_profiles (
    user_id INT PRIMARY KEY,
    display_name VARCHAR(100),
    
    -- ⭐ CÁC CỘT QUAN TRỌNG CHO LEADERBOARD
    games_played INT DEFAULT 0,   -- ✅ ĐÃ CÓ
    games_won INT DEFAULT 0,      -- ✅ ĐÃ CÓ - Chỉ số chính
    games_lost INT DEFAULT 0,     -- ✅ ĐÃ CÓ
    
    -- Các cột HOÃN (không dùng cho MVP)
    current_rating DECIMAL(10,2) DEFAULT 1000.00,
    rank_tier ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') DEFAULT 'BRONZE',
    -- ...
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_games_won (games_won DESC)  -- ⭐ ĐÃ CÓ INDEX - Performance tốt!
);
```

**🎯 Đánh Giá**:
- ✅ **100% Ready**: Không cần thêm/sửa cột nào!
- ✅ **Index tối ưu**: `idx_games_won` sẵn có → Query nhanh
- ✅ **Dữ liệu chính xác**: Stored procedure `update_user_stats_after_game()` đảm bảo consistency

#### **Bảng `users` - Cần JOIN để lấy username**

```sql
-- File: DB_SCRIPT.sql (Line ~20-50)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,  -- ⭐ CẦN CHO LEADERBOARD
    email VARCHAR(100) UNIQUE NOT NULL,
    -- ...
);
```

**🎯 Đánh Giá**:
- ✅ **Sẵn sàng**: Chỉ cần JOIN đơn giản
- ✅ **Performance**: `username` có UNIQUE index

---

### 📊 QUERY SQL CHO LEADERBOARD

#### **Query Cơ Bản (Top 50)**

```sql
-- Query đơn giản, hiệu năng cao
SELECT 
    u.user_id,
    u.username,
    p.display_name,
    p.games_played,
    p.games_won,
    p.games_lost,
    -- Tính win rate (tùy chọn - có thể tính ở Backend/Frontend)
    CASE 
        WHEN p.games_played > 0 
        THEN ROUND((p.games_won * 100.0 / p.games_played), 2)
        ELSE 0.00
    END AS win_rate
FROM users u
INNER JOIN user_profiles p ON u.user_id = p.user_id
WHERE u.status = 'ACTIVE'              -- Chỉ lấy user đang hoạt động
ORDER BY p.games_won DESC, p.games_played DESC  -- Ưu tiên games_won
LIMIT 50;
```

**🔍 Phân Tích Performance**:

| Yếu Tố | Đánh Giá | Ghi Chú |
|--------|----------|---------|
| **Index Usage** | ⭐⭐⭐⭐⭐ | Sử dụng `idx_games_won` (DESC) |
| **Join Complexity** | ⭐⭐⭐⭐⭐ | INNER JOIN 1:1 (PK-FK) - Rất nhanh |
| **Data Size** | ⭐⭐⭐⭐⭐ | LIMIT 50 → Luôn trả về ít dữ liệu |
| **Computation** | ⭐⭐⭐⭐ | Win rate tính trong SQL (tùy chọn) |

**⏱️ Ước Lượng Thời Gian Thực Thi**:
- Database < 1,000 users: **< 10ms**
- Database < 100,000 users: **< 50ms** (nhờ index)

---

### 🎯 TẬN DỤNG BACKEND SERVICES HIỆN CÓ

#### **Service Pattern Hiện Tại**

```java
// File: AuthService.java (Line ~90-120)
public LoginSuccessDto login(String username, String password) throws SQLException {
    String sql = """
        SELECT u.user_id, u.username, u.email, u.password_hash, u.status, 
               up.display_name, up.total_score, up.games_played, up.games_won
        FROM users u
        JOIN user_profiles up ON u.user_id = up.user_id
        WHERE u.username = ?
        """;
    // ... Thực thi query và map ResultSet sang DTO
}
```

**✅ Pattern Có Thể Tái Sử Dụng**:
1. ✅ JOIN `users` + `user_profiles` (giống AuthService)
2. ✅ Map `ResultSet` → DTO
3. ✅ Exception handling pattern

---

### 🆕 THÀNH PHẦN MỚI CẦN TẠO

#### **1. DTO Class (Shared Module)**

```java
// File: shared/src/main/java/com/n9/shared/model/dto/lobby/LeaderboardEntryDto.java
package com.n9.shared.model.dto.lobby;

public class LeaderboardEntryDto {
    private String userId;
    private String username;
    private String displayName;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private double winRate;  // Tính ở Backend hoặc Frontend
    
    // Constructors, Getters, Setters
    public LeaderboardEntryDto() {}
    
    // Builder pattern (tùy chọn)
    public static class Builder {
        // ...
    }
}
```

```java
// File: shared/src/main/java/com/n9/shared/model/dto/lobby/LeaderboardResponseDto.java
package com.n9.shared.model.dto.lobby;

import java.util.List;

public class LeaderboardResponseDto {
    private List<LeaderboardEntryDto> entries;
    private int totalPlayers;      // Tổng số người chơi
    private long timestamp;        // Thời điểm lấy dữ liệu
    private int limit;             // Số lượng trả về (50)
    
    // Constructors, Getters, Setters
}
```

#### **2. Service Class (Core Module)**

```java
// File: core/src/main/java/com/n9/core/service/LeaderboardService.java
package com.n9.core.service;

import com.n9.core.database.DatabaseManager;
import com.n9.shared.model.dto.lobby.LeaderboardEntryDto;
import com.n9.shared.model.dto.lobby.LeaderboardResponseDto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardService {
    
    private final DatabaseManager dbManager;
    private static final int DEFAULT_LIMIT = 50;
    
    public LeaderboardService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Lấy Top N người chơi theo games_won.
     * 
     * @param limit Số lượng người chơi (mặc định 50)
     * @return LeaderboardResponseDto chứa danh sách xếp hạng
     * @throws SQLException Nếu có lỗi database
     */
    public LeaderboardResponseDto getTopPlayers(int limit) throws SQLException {
        if (limit <= 0 || limit > 100) {
            limit = DEFAULT_LIMIT;  // Giới hạn tối đa 100
        }
        
        String sql = """
            SELECT 
                u.user_id,
                u.username,
                p.display_name,
                p.games_played,
                p.games_won,
                p.games_lost
            FROM users u
            INNER JOIN user_profiles p ON u.user_id = p.user_id
            WHERE u.status = 'ACTIVE'
            ORDER BY p.games_won DESC, p.games_played DESC
            LIMIT ?
            """;
        
        List<LeaderboardEntryDto> entries = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LeaderboardEntryDto entry = new LeaderboardEntryDto();
                    entry.setUserId(String.valueOf(rs.getInt("user_id")));
                    entry.setUsername(rs.getString("username"));
                    entry.setDisplayName(rs.getString("display_name"));
                    entry.setGamesPlayed(rs.getInt("games_played"));
                    entry.setGamesWon(rs.getInt("games_won"));
                    entry.setGamesLost(rs.getInt("games_lost"));
                    
                    // Tính win rate ở Backend
                    int played = entry.getGamesPlayed();
                    double winRate = played > 0 
                        ? (entry.getGamesWon() * 100.0 / played) 
                        : 0.0;
                    entry.setWinRate(Math.round(winRate * 100.0) / 100.0); // 2 chữ số thập phân
                    
                    entries.add(entry);
                }
            }
        }
        
        // Lấy tổng số người chơi (cho hiển thị "Top 50/1234")
        int totalPlayers = getTotalActivePlayers();
        
        LeaderboardResponseDto response = new LeaderboardResponseDto();
        response.setEntries(entries);
        response.setTotalPlayers(totalPlayers);
        response.setTimestamp(System.currentTimeMillis());
        response.setLimit(limit);
        
        return response;
    }
    
    /**
     * Đếm tổng số người chơi đang hoạt động.
     */
    private int getTotalActivePlayers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
```

---

## 3. IMPACT ANALYSIS - CÁC THÀNH PHẦN BỊ ẢNH HƯỞNG

### 📦 Sơ Đồ Tổng Quan

```
┌─────────────────────────────────────────────────────────────────────┐
│                        IMPACT MAP                                   │
└─────────────────────────────────────────────────────────────────────┘

[SHARED MODULE]
  ├─ MessageProtocol.java                   [✏️ THÊM 2 CONSTANTS]
  ├─ model/dto/lobby/
  │   ├─ LeaderboardEntryDto.java          [🆕 TẠO MỚI]
  │   └─ LeaderboardResponseDto.java       [🆕 TẠO MỚI]

[CORE MODULE]
  ├─ service/
  │   └─ LeaderboardService.java           [🆕 TẠO MỚI]
  ├─ CoreServer.java                       [✏️ SỬA - Inject service]
  └─ network/
      └─ ClientConnectionHandler.java      [✏️ SỬA - Thêm case handler]

[GATEWAY MODULE]
  └─ (KHÔNG ẢNH HƯỞNG - Chỉ forward message)

[FRONTEND MODULE]
  ├─ services/
  │   └─ lobby.js                          [🆕 TẠO MỚI]
  ├─ components/lobby/
  │   └─ LeaderboardModal.jsx              [🆕 TẠO MỚI]
  └─ App.jsx hoặc LobbyView                [✏️ SỬA - Thêm button + modal]

[DATABASE]
  └─ (KHÔNG ẢNH HƯỞNG - Schema đã sẵn sàng)
```

---

### 📝 CHI TIẾT TỪNG FILE BỊ ẢNH HƯỞNG

#### **File 1: `MessageProtocol.java` (Shared)**

**📍 Vị trí**: `shared/src/main/java/com/n9/shared/MessageProtocol.java`

**✏️ Thay Đổi**: Thêm 2 hằng số mới

```java
// TRONG class Type, section LOBBY DOMAIN (Line ~40-50)

// ============================
// LOBBY DOMAIN
// ============================
public static final String LOBBY_MATCH_REQUEST   = "LOBBY.MATCH_REQUEST";
public static final String LOBBY_MATCH_REQUEST_ACK = "LOBBY.MATCH_REQUEST_ACK";
public static final String LOBBY_MATCH_CANCEL    = "LOBBY.MATCH_CANCEL";

// 🆕 THÊM 2 DÒNG NÀY:
/** Client yêu cầu lấy bảng xếp hạng. */
public static final String LOBBY_GET_LEADERBOARD_REQUEST  = "LOBBY.GET_LEADERBOARD_REQUEST";
/** Server trả về danh sách xếp hạng. */
public static final String LOBBY_GET_LEADERBOARD_RESPONSE = "LOBBY.GET_LEADERBOARD_RESPONSE";
```

**📊 Impact Level**: ⭐ (Rất thấp - Chỉ thêm constants)

---

#### **File 2: `CoreServer.java` (Core)**

**📍 Vị trí**: `core/src/main/java/com/n9/core/CoreServer.java`

**✏️ Thay Đổi**: Khởi tạo `LeaderboardService` và inject vào `ClientConnectionHandler`

```java
// Trong class CoreServer, method main() hoặc constructor

// Existing services
DatabaseManager dbManager = new DatabaseManager();
SessionManager sessionManager = new SessionManager(dbManager);
AuthService authService = new AuthService(dbManager);

// 🆕 THÊM:
LeaderboardService leaderboardService = new LeaderboardService(dbManager);

GameService gameService = new GameService(dbManager, activeConnections, scheduler, sessionManager);
MatchmakingService matchmakingService = new MatchmakingService(gameService, sessionManager, activeConnections, scheduler);

// Khi tạo ClientConnectionHandler, truyền thêm leaderboardService
ClientConnectionHandler handler = new ClientConnectionHandler(
    clientSocket,
    gameService,
    authService,
    sessionManager,
    matchmakingService,
    pool,
    activeConnections,
    leaderboardService  // 🆕 THÊM THAM SỐ
);
```

**📊 Impact Level**: ⭐⭐ (Thấp - Chỉ thêm 1 dòng khởi tạo)

---

#### **File 3: `ClientConnectionHandler.java` (Core)**

**📍 Vị trí**: `core/src/main/java/com/n9/core/network/ClientConnectionHandler.java`

**✏️ Thay Đổi 1**: Thêm field `leaderboardService`

```java
// Trong class ClientConnectionHandler (Line ~35)

private final GameService gameService;
private final AuthService authService;
private final SessionManager sessionManager;
private final MatchmakingService matchmakingService;
private final LeaderboardService leaderboardService;  // 🆕 THÊM

// Constructor (cập nhật)
public ClientConnectionHandler(
    Socket socket,
    GameService gameService,
    AuthService authService,
    SessionManager sessionManager,
    MatchmakingService matchmakingService,
    ExecutorService pool,
    ConcurrentHashMap<String, ClientConnectionHandler> activeConnections,
    LeaderboardService leaderboardService  // 🆕 THÊM
) {
    this.socket = socket;
    this.gameService = gameService;
    this.authService = authService;
    this.sessionManager = sessionManager;
    this.matchmakingService = matchmakingService;
    this.pool = pool;
    this.activeConnections = activeConnections;
    this.leaderboardService = leaderboardService;  // 🆕 THÊM
}
```

**✏️ Thay Đổi 2**: Thêm case handler trong `handleMessage()`

```java
// Trong method handleMessage() (Line ~130-160)

switch (type) {
    // --- AUTH ---
    case MessageProtocol.Type.AUTH_REGISTER_REQUEST:
        response = handleRegister(envelope);
        break;
    case MessageProtocol.Type.AUTH_LOGIN_REQUEST:
        response = handleLogin(envelope);
        break;
    case MessageProtocol.Type.AUTH_LOGOUT_REQUEST:
        response = handleLogout(envelope);
        break;

    // --- LOBBY ---
    case MessageProtocol.Type.LOBBY_MATCH_REQUEST:
        response = handleMatchRequest(envelope);
        break;
    
    // 🆕 THÊM CASE MỚI:
    case MessageProtocol.Type.LOBBY_GET_LEADERBOARD_REQUEST:
        response = handleGetLeaderboard(envelope);
        break;

    // --- GAME ---
    case MessageProtocol.Type.GAME_CARD_PLAY_REQUEST:
        response = handlePlayCard(envelope);
        break;
    
    // ... (các case khác)
}
```

**✏️ Thay Đổi 3**: Thêm method `handleGetLeaderboard()`

```java
// Thêm method mới trong class ClientConnectionHandler (Line ~250)

/**
 * Xử lý yêu cầu lấy bảng xếp hạng.
 * KHÔNG YÊU CẦU đăng nhập (public leaderboard).
 */
private MessageEnvelope handleGetLeaderboard(MessageEnvelope envelope) {
    try {
        // Payload có thể chứa limit (tùy chọn)
        Integer limit = 50;  // Mặc định
        
        if (envelope.getPayload() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) envelope.getPayload();
            Object limitObj = payload.get("limit");
            if (limitObj instanceof Integer) {
                limit = (Integer) limitObj;
            }
        }
        
        // Gọi service
        LeaderboardResponseDto leaderboardData = leaderboardService.getTopPlayers(limit);
        
        // Trả về response
        return MessageFactory.createResponse(
            envelope, 
            MessageProtocol.Type.LOBBY_GET_LEADERBOARD_RESPONSE, 
            leaderboardData
        );
        
    } catch (SQLException e) {
        e.printStackTrace();
        return MessageFactory.createErrorResponse(
            envelope, 
            "DATABASE_ERROR", 
            "Failed to retrieve leaderboard data."
        );
    } catch (Exception e) {
        e.printStackTrace();
        return MessageFactory.createErrorResponse(
            envelope, 
            "INTERNAL_SERVER_ERROR", 
            "An unexpected error occurred."
        );
    }
}
```

**📊 Impact Level**: ⭐⭐⭐ (Trung bình - Thêm 1 field, 1 case, 1 method)

---

#### **File 4: Frontend - `lobby.js` (Service Layer)**

**📍 Vị trí**: `frontend/src/services/lobby.js` (🆕 TẠO MỚI)

```javascript
// File: frontend/src/services/lobby.js

/**
 * Service layer cho các tính năng Lobby (Leaderboard, etc.)
 */

const MessageType = {
  LOBBY_GET_LEADERBOARD_REQUEST: 'LOBBY.GET_LEADERBOARD_REQUEST',
  LOBBY_GET_LEADERBOARD_RESPONSE: 'LOBBY.GET_LEADERBOARD_RESPONSE'
};

/**
 * Gửi yêu cầu lấy bảng xếp hạng
 * @param {WebSocket} ws - WebSocket connection
 * @param {number} limit - Số lượng người chơi (mặc định 50)
 * @returns {string} correlationId - Để match response
 */
export const requestLeaderboard = (ws, limit = 50) => {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.error('WebSocket not connected');
    return null;
  }
  
  const correlationId = `c-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  
  const request = {
    type: MessageType.LOBBY_GET_LEADERBOARD_REQUEST,
    correlationId: correlationId,
    payload: { limit }
  };
  
  ws.send(JSON.stringify(request));
  console.log('📊 Requested leaderboard (limit:', limit, ')');
  
  return correlationId;
};

/**
 * Parse leaderboard response
 * @param {Object} envelope - Message envelope từ server
 * @returns {Object} Parsed leaderboard data
 */
export const parseLeaderboardResponse = (envelope) => {
  if (envelope.type !== MessageType.LOBBY_GET_LEADERBOARD_RESPONSE) {
    return null;
  }
  
  const payload = envelope.payload;
  
  return {
    entries: payload.entries || [],
    totalPlayers: payload.totalPlayers || 0,
    timestamp: payload.timestamp || Date.now(),
    limit: payload.limit || 50
  };
};
```

**📊 Impact Level**: ⭐⭐ (Thấp - File mới, logic đơn giản)

---

#### **File 5: Frontend - `LeaderboardModal.jsx` (Component)**

**📍 Vị trí**: `frontend/src/components/lobby/LeaderboardModal.jsx` (🆕 TẠO MỚI)

```jsx
// File: frontend/src/components/lobby/LeaderboardModal.jsx

import React, { useState, useEffect } from 'react';
import { requestLeaderboard, parseLeaderboardResponse } from '../../services/lobby';

const LeaderboardModal = ({ isOpen, onClose, ws }) => {
  const [leaderboardData, setLeaderboardData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Fetch leaderboard khi modal mở
  useEffect(() => {
    if (isOpen && ws) {
      fetchLeaderboard();
    }
  }, [isOpen, ws]);

  const fetchLeaderboard = () => {
    setLoading(true);
    setError(null);
    
    const correlationId = requestLeaderboard(ws, 50);
    
    // Lắng nghe response (cần setup listener trong parent component)
    // Hoặc sử dụng global event listener
  };

  // Handler cho response (được gọi từ parent component)
  const handleLeaderboardResponse = (envelope) => {
    const data = parseLeaderboardResponse(envelope);
    if (data) {
      setLeaderboardData(data);
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-4xl max-h-[80vh] overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-yellow-500 to-orange-500 px-6 py-4 flex justify-between items-center">
          <h2 className="text-2xl font-bold text-white flex items-center">
            🏆 Bảng Xếp Hạng
          </h2>
          <button
            onClick={onClose}
            className="text-white hover:text-gray-200 text-2xl font-bold"
          >
            ×
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          {loading && (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
              <p className="mt-4 text-gray-600">Đang tải dữ liệu...</p>
            </div>
          )}

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {!loading && !error && leaderboardData && (
            <>
              {/* Stats Summary */}
              <div className="mb-4 text-sm text-gray-600">
                Hiển thị Top {leaderboardData.entries.length} / {leaderboardData.totalPlayers} người chơi
              </div>

              {/* Table */}
              <div className="overflow-auto max-h-[50vh]">
                <table className="w-full border-collapse">
                  <thead className="bg-gray-100 sticky top-0">
                    <tr>
                      <th className="border px-4 py-2 text-left">#</th>
                      <th className="border px-4 py-2 text-left">Username</th>
                      <th className="border px-4 py-2 text-left">Display Name</th>
                      <th className="border px-4 py-2 text-center">Played</th>
                      <th className="border px-4 py-2 text-center">Won</th>
                      <th className="border px-4 py-2 text-center">Win Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leaderboardData.entries.map((entry, index) => (
                      <tr 
                        key={entry.userId} 
                        className={`hover:bg-gray-50 ${index < 3 ? 'bg-yellow-50' : ''}`}
                      >
                        <td className="border px-4 py-2 font-bold text-gray-700">
                          {index + 1}
                          {index === 0 && ' 🥇'}
                          {index === 1 && ' 🥈'}
                          {index === 2 && ' 🥉'}
                        </td>
                        <td className="border px-4 py-2">{entry.username}</td>
                        <td className="border px-4 py-2 text-gray-600">{entry.displayName}</td>
                        <td className="border px-4 py-2 text-center">{entry.gamesPlayed}</td>
                        <td className="border px-4 py-2 text-center font-semibold text-green-600">
                          {entry.gamesWon}
                        </td>
                        <td className="border px-4 py-2 text-center">
                          {entry.winRate.toFixed(1)}%
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="bg-gray-100 px-6 py-4 flex justify-between">
          <button
            onClick={fetchLeaderboard}
            disabled={loading}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:bg-gray-400"
          >
            🔄 Làm mới
          </button>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};

export default LeaderboardModal;
```

**📊 Impact Level**: ⭐⭐⭐ (Trung bình - Component mới nhưng logic đơn giản)

---

#### **File 6: Frontend - `LobbyView` (Integration)**

**📍 Vị trí**: Trong `AppSingleFile.jsx` hoặc component `LobbyView`

**✏️ Thay Đổi**: Thêm button và modal

```jsx
// Trong LobbyView component

import LeaderboardModal from '../components/lobby/LeaderboardModal';

const LobbyView = () => {
  const { state, dispatch, sendMessage } = useApp();
  const [showLeaderboard, setShowLeaderboard] = useState(false);  // 🆕 THÊM

  // ... existing handlers ...

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-500 to-blue-600">
      {/* ... existing content ... */}
      
      {/* 🆕 THÊM NÚT LEADERBOARD */}
      <button
        onClick={() => setShowLeaderboard(true)}
        className="px-6 py-3 bg-yellow-500 text-white rounded-lg font-bold hover:bg-yellow-600"
      >
        🏆 Bảng Xếp Hạng
      </button>

      {/* 🆕 THÊM MODAL */}
      <LeaderboardModal
        isOpen={showLeaderboard}
        onClose={() => setShowLeaderboard(false)}
        ws={state.ws}
      />
    </div>
  );
};
```

**📊 Impact Level**: ⭐⭐ (Thấp - Chỉ thêm button + import)

---

## 4. PROTOCOL & LUỒNG DỮ LIỆU E2E

### 📡 Message Flow Diagram

```
┌──────────┐                ┌──────────┐                ┌──────────┐
│ FRONTEND │                │ GATEWAY  │                │   CORE   │
│ (React)  │                │ (WS→TCP) │                │  (Java)  │
└──────────┘                └──────────┘                └──────────┘
     │                            │                            │
     │ [1] User clicks "🏆"       │                            │
     │────────────────────────────┼───────────────────────────>│
     │ WS: LOBBY.GET_LEADERBOARD_ │ TCP: Forward               │
     │     REQUEST                │                            │
     │ {                          │                            │
     │   type: "...",             │                            │
     │   correlationId: "c-123",  │                            │
     │   payload: { limit: 50 }   │                            │
     │ }                          │                            │
     │                            │                            │
     │                            │  [2] ClientConnectionHandler│
     │                            │      .handleGetLeaderboard()│
     │                            │      ↓                      │
     │                            │  [3] LeaderboardService    │
     │                            │      .getTopPlayers(50)    │
     │                            │      ↓                      │
     │                            │  [4] SQL Query to DB       │
     │                            │      SELECT ... LIMIT 50   │
     │                            │      ↓                      │
     │                            │  [5] Map ResultSet → DTO   │
     │                            │                            │
     │<───────────────────────────┼────────────────────────────│
     │ WS: LOBBY.GET_LEADERBOARD_ │ TCP: Response              │
     │     RESPONSE               │                            │
     │ {                          │                            │
     │   type: "...",             │                            │
     │   correlationId: "c-123",  │                            │
     │   payload: {               │                            │
     │     entries: [...],        │                            │
     │     totalPlayers: 1234,    │                            │
     │     timestamp: 1699...     │                            │
     │   }                        │                            │
     │ }                          │                            │
     │                            │                            │
     │ [6] Render table           │                            │
     │                            │                            │
```

### 🔄 Sequence Diagram (Chi Tiết)

```
User          Frontend        Gateway         Core            Database
 │                │              │              │                 │
 │ Click 🏆      │              │              │                 │
 │───────────────>│              │              │                 │
 │                │ WS Send      │              │                 │
 │                │─────────────>│ TCP Forward  │                 │
 │                │              │─────────────>│                 │
 │                │              │              │ handleMessage() │
 │                │              │              │─────┐           │
 │                │              │              │     │ Route     │
 │                │              │              │<────┘           │
 │                │              │              │                 │
 │                │              │              │ handleGetLeaderboard()
 │                │              │              │─────┐           │
 │                │              │              │     │ Validate  │
 │                │              │              │<────┘           │
 │                │              │              │                 │
 │                │              │              │ getTopPlayers(50)
 │                │              │              │─────┐           │
 │                │              │              │     │ Service   │
 │                │              │              │<────┘           │
 │                │              │              │                 │
 │                │              │              │ SQL SELECT...   │
 │                │              │              │────────────────>│
 │                │              │              │<────────────────│
 │                │              │              │ ResultSet       │
 │                │              │              │                 │
 │                │              │              │ Map → DTO       │
 │                │              │              │─────┐           │
 │                │              │              │     │ Loop      │
 │                │              │              │<────┘           │
 │                │              │              │                 │
 │                │              │ TCP Response │                 │
 │                │              │<─────────────│                 │
 │                │ WS Response  │              │                 │
 │                │<─────────────│              │                 │
 │                │              │              │                 │
 │                │ Update State │              │                 │
 │                │─────┐        │              │                 │
 │                │     │ Render │              │                 │
 │                │<────┘        │              │                 │
 │                │              │              │                 │
 │ See Table     │              │              │                 │
 │<──────────────│              │              │                 │
```

---

## 5. IMPLEMENTATION PLAN CHI TIẾT

### 📅 Roadmap (3 Phases)

#### **Phase 1: Backend Foundation (Priority: HIGH)**

**Mục tiêu**: Tạo API hoàn chỉnh, test bằng tool (Postman/curl)

| Task | File | Ước Lượng | Dependencies |
|------|------|-----------|--------------|
| 1.1 Tạo DTO classes | `LeaderboardEntryDto.java`, `LeaderboardResponseDto.java` | 30 phút | - |
| 1.2 Tạo `LeaderboardService` | `LeaderboardService.java` | 1 giờ | Task 1.1 |
| 1.3 Cập nhật `MessageProtocol` | `MessageProtocol.java` | 10 phút | - |
| 1.4 Inject service vào `CoreServer` | `CoreServer.java` | 15 phút | Task 1.2 |
| 1.5 Thêm handler trong `ClientConnectionHandler` | `ClientConnectionHandler.java` | 45 phút | Task 1.2, 1.3 |
| 1.6 Unit Test | `LeaderboardServiceTest.java` | 1 giờ | Task 1.2 |
| 1.7 Integration Test | Manual test qua Gateway | 30 phút | Task 1.5 |

**Tổng**: ~4.5 giờ

**Checkpoint**: Backend trả về JSON đúng format khi gửi request từ Postman.

---

#### **Phase 2: Frontend UI (Priority: MEDIUM)**

**Mục tiêu**: Hiển thị leaderboard trong Modal

| Task | File | Ước Lượng | Dependencies |
|------|------|-----------|--------------|
| 2.1 Tạo service layer | `lobby.js` | 30 phút | Phase 1 done |
| 2.2 Tạo LeaderboardModal component | `LeaderboardModal.jsx` | 2 giờ | Task 2.1 |
| 2.3 Integrate vào LobbyView | `LobbyView` component | 30 phút | Task 2.2 |
| 2.4 Styling & Responsive | CSS/Tailwind | 1 giờ | Task 2.2 |
| 2.5 Handle errors/loading states | `LeaderboardModal.jsx` | 30 phút | Task 2.2 |

**Tổng**: ~4.5 giờ

**Checkpoint**: User có thể xem leaderboard, click "Làm mới", đóng modal.

---

#### **Phase 3: Polish & Optimization (Priority: LOW)**

**Mục tiêu**: Cải thiện UX, performance

| Task | Ước Lượng |
|------|-----------|
| 3.1 Add animations (modal fade-in, table row hover) | 1 giờ |
| 3.2 Add "Your Rank" highlight (nếu user trong Top 50) | 1 giờ |
| 3.3 Cache leaderboard data (5 phút TTL) | 1 giờ |
| 3.4 Add skeleton loading | 30 phút |
| 3.5 E2E Testing | 1 giờ |

**Tổng**: ~4.5 giờ

---

### 🎯 TỔNG ESTIMATION

| Phase | Thời Gian | Developer |
|-------|-----------|-----------|
| Phase 1 (Backend) | 4.5 giờ | Backend Dev |
| Phase 2 (Frontend) | 4.5 giờ | Frontend Dev |
| Phase 3 (Polish) | 4.5 giờ | Fullstack Dev |
| **TOTAL** | **13.5 giờ** | **~2 working days** |

**Buffers**: +20% → **~16 giờ** (2.5 ngày)

---

## 6. ESTIMATION & RISK ASSESSMENT

### 📊 Complexity Matrix

| Tiêu Chí | Đánh Giá | Điểm (1-5) | Lý Do |
|----------|----------|------------|-------|
| **Technical Complexity** | Thấp | ⭐⭐ | - Query SQL đơn giản<br>- Không có logic phức tạp<br>- Không cần real-time |
| **Data Complexity** | Thấp | ⭐ | - Chỉ đọc, không ghi<br>- Dữ liệu đã sẵn sàng |
| **Integration Complexity** | Trung bình | ⭐⭐⭐ | - Cần sửa nhiều file<br>- Nhưng pattern đã có |
| **UI/UX Complexity** | Thấp | ⭐⭐ | - Table đơn giản<br>- Modal component cơ bản |

**Tổng Điểm**: **8/20** → **Độ Phức Tạp: THẤP**

---

### ⚠️ RISK ASSESSMENT

#### **Risk 1: Performance Degradation (Likelihood: LOW, Impact: MEDIUM)**

**Mô tả**: Nếu có >100,000 users, query có thể chậm.

**Mitigation**:
- ✅ Index `idx_games_won` đã có sẵn
- ✅ LIMIT 50 → Luôn trả về ít dữ liệu
- ✅ Nếu cần: Add caching (Redis) hoặc materialized view

**Contingency Plan**:
```sql
-- Nếu quá chậm, tạo materialized view (refresh mỗi 5 phút)
CREATE MATERIALIZED VIEW mv_leaderboard AS
SELECT u.user_id, u.username, p.display_name, p.games_played, p.games_won
FROM users u
JOIN user_profiles p ON u.user_id = p.user_id
WHERE u.status = 'ACTIVE'
ORDER BY p.games_won DESC
LIMIT 100;
```

---

#### **Risk 2: Stale Data (Likelihood: HIGH, Impact: LOW)**

**Mô tả**: Leaderboard không tự động cập nhật khi có game kết thúc.

**Mitigation**:
- ✅ User có nút "Làm mới" (manual refresh)
- 🔜 **Feature 2 sẽ giải quyết**: Real-time update qua WebSocket push

**Acceptance Criteria**:
- User nhận thấy data "có thể cũ" → Click "Làm mới" là OK cho MVP.

---

#### **Risk 3: Gateway Bottleneck (Likelihood: LOW, Impact: LOW)**

**Mô tả**: Nếu 1000 users cùng request leaderboard, Gateway có thể bị quá tải.

**Mitigation**:
- ✅ Leaderboard không phải real-time → Không cần đồng bộ ngay
- ✅ Client-side debounce (chỉ cho phép 1 request/5s)
- ✅ Server-side rate limiting (nếu cần)

---

#### **Risk 4: Inconsistent Data (Likelihood: VERY LOW, Impact: HIGH)**

**Mô tả**: `games_won` không khớp với dữ liệu `games` table.

**Mitigation**:
- ✅ Stored procedure `update_user_stats_after_game()` đảm bảo consistency
- ✅ Transaction isolation trong stored procedure
- ✅ Có thể chạy validation script định kỳ

**Validation Script**:
```sql
-- Kiểm tra tính nhất quán
SELECT 
    up.user_id,
    up.games_won AS profile_wins,
    COUNT(CASE WHEN g.winner_id = up.user_id THEN 1 END) AS actual_wins
FROM user_profiles up
LEFT JOIN games g ON g.player1_id = up.user_id OR g.player2_id = up.user_id
WHERE g.status = 'COMPLETED'
GROUP BY up.user_id
HAVING profile_wins != actual_wins;
```

---

## 7. TESTING STRATEGY

### 🧪 Test Plan

#### **Backend Unit Tests**

```java
// File: LeaderboardServiceTest.java

@Test
public void testGetTopPlayers_ReturnsCorrectOrder() throws SQLException {
    // Arrange: Seed database với 10 users
    // User A: 50 wins, User B: 40 wins, ...
    
    // Act
    LeaderboardResponseDto result = service.getTopPlayers(10);
    
    // Assert
    assertEquals(10, result.getEntries().size());
    assertEquals("userA", result.getEntries().get(0).getUsername());
    assertEquals(50, result.getEntries().get(0).getGamesWon());
}

@Test
public void testGetTopPlayers_HandlesEmptyDatabase() throws SQLException {
    // Arrange: Empty database
    
    // Act
    LeaderboardResponseDto result = service.getTopPlayers(50);
    
    // Assert
    assertEquals(0, result.getEntries().size());
    assertEquals(0, result.getTotalPlayers());
}

@Test
public void testGetTopPlayers_RespectsLimit() throws SQLException {
    // Arrange: 100 users in database
    
    // Act
    LeaderboardResponseDto result = service.getTopPlayers(10);
    
    // Assert
    assertEquals(10, result.getEntries().size());
}
```

---

#### **Integration Tests**

**Test Case 1: E2E Flow**
```
1. Start Core Server
2. Start Gateway
3. Connect WebSocket client
4. Send LOBBY.GET_LEADERBOARD_REQUEST
5. Assert: Receive LOBBY.GET_LEADERBOARD_RESPONSE với data hợp lệ
6. Assert: Response time < 500ms
```

**Test Case 2: Error Handling**
```
1. Stop database
2. Send LOBBY.GET_LEADERBOARD_REQUEST
3. Assert: Receive SYSTEM.ERROR với code "DATABASE_ERROR"
```

---

#### **Frontend Tests**

**Component Test**:
```javascript
// LeaderboardModal.test.jsx

test('renders leaderboard table with data', () => {
  const mockData = {
    entries: [
      { userId: '1', username: 'alice', gamesWon: 50, gamesPlayed: 60, winRate: 83.33 }
    ],
    totalPlayers: 100
  };
  
  render(<LeaderboardModal isOpen={true} data={mockData} />);
  
  expect(screen.getByText('alice')).toBeInTheDocument();
  expect(screen.getByText('50')).toBeInTheDocument();
});

test('shows loading state', () => {
  render(<LeaderboardModal isOpen={true} loading={true} />);
  expect(screen.getByText(/Đang tải/i)).toBeInTheDocument();
});
```

---

## 8. KẾT LUẬN & KHUYẾN NGHỊ

### ✅ FEASIBILITY VERDICT

**🎯 Tính Năng Leaderboard là HOÀN TOÀN KHẢ THI cho MVP**

**Lý do**:
1. ✅ **Database sẵn sàng 100%**: Không cần thêm/sửa bảng, có index tối ưu
2. ✅ **Complexity thấp**: Chỉ là query SELECT + render table
3. ✅ **Risk thấp**: Không ảnh hưởng logic game core
4. ✅ **ROI cao**: Tăng engagement, dễ implement

---

### 🎯 KHUYẾN NGHỊ TRIỂN KHAI

#### **Thứ Tự Ưu Tiên (Recommendation)**

```
┌─────────────────────────────────────────────────────────┐
│  LEADERBOARD (Feature 1) - IMPLEMENT FIRST             │
│  ↓                                                       │
│  Vì:                                                     │
│  - Đơn giản nhất (low-hanging fruit)                    │
│  - Không phụ thuộc Feature 2 hoặc 3                     │
│  - Cung cấp nền tảng UI cho Feature 3 (Challenge)      │
└─────────────────────────────────────────────────────────┘
```

#### **Best Practices**

1. **Backend First**: Hoàn thành API + test trước khi làm Frontend
2. **Reusable Components**: LeaderboardModal có thể tái sử dụng cho các tính năng khác
3. **Caching Strategy**: Nếu có >1000 concurrent users, cân nhắc Redis cache (TTL 5 phút)
4. **Monitoring**: Log số lượng request leaderboard để detect abuse

---

### 🔗 LIÊN KẾT VỚI CÁC TÍNH NĂNG KHÁC

#### **Feature 2 (Real-time Presence) - SỬ DỤNG Leaderboard**

Khi Feature 2 được implement:
- Thêm cột "Status" (🟢 Online / ⚪ Offline) vào bảng leaderboard
- Dùng `SessionManager.userSessionMap` để xác định online status
- Không cần sửa Backend logic, chỉ thêm field vào DTO

#### **Feature 3 (Direct Challenge) - XÂY DỰNG TRÊN Leaderboard**

Khi Feature 3 được implement:
- Thêm nút "⚔️ Thách đấu" bên cạnh mỗi người chơi online
- Reuse `LeaderboardModal` làm nền tảng UI
- Tích hợp với `MatchmakingService` (sửa để support direct challenge)

---

### 📚 NEXT STEPS

1. ✅ **Review tài liệu này** với team
2. ✅ **Estimate lại** nếu cần điều chỉnh scope
3. ✅ **Tạo tasks** trong project management tool (Jira/Trello)
4. ✅ **Bắt đầu Phase 1**: Backend Development
5. 🔜 **Đọc Feature 2**: `Feasibility_Feature_2_Presence.md` (sẽ tạo tiếp theo)

---

**📝 End of Document - Feature 1 Analysis**

**Prepared by**: Solution Architect Team  
**Status**: ✅ APPROVED FOR IMPLEMENTATION  
**Next Review**: After Phase 1 Completion
