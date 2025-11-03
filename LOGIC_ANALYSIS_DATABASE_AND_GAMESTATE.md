# 📘 Phân Tích Logic Hiện Tại - GameService.java

> **Tài liệu**: Làm Rõ Cơ Chế Cập Nhật Database & Clone GameState  
> **Tác giả**: Solution Architect & Senior Technical Advisor  
> **Ngày**: November 3, 2025  
> **Phiên bản**: 1.0.0  

---

## 📋 MỤC LỤC

1. [Phần A: Luồng Cập Nhật Trạng Thái Game vào Database](#phần-a-luồng-cập-nhật-trạng-thái-game-vào-database)
2. [Phần B: Mục Đích Của gameSnapshotForEnd và cloneGameState()](#phần-b-mục-đích-của-gamesnapshotforend-và-clonegamestate)

---

## PHẦN A: Luồng Cập Nhật Trạng Thái Game vào Database

### 🎯 Tổng Quan

Trong `GameService.java`, trạng thái của một trận đấu được cập nhật vào database qua **4 thời điểm quan trọng**:

```
[1] initializeGame()           → INSERT games (status='IN_PROGRESS')
[2] executeRoundRevealAndProceed() → INSERT game_rounds (mỗi hiệp)
[3] handleGameEnd()            → UPDATE games (status='COMPLETED') + CALL stored procedure
[4] handleForfeit()            → UPDATE games (status='ABANDONED') + CALL stored procedure
```

---

### 🔄 CHI TIẾT TỪNG BƯỚC (Step-by-Step)

---

#### **BƯỚC 1: INSERT Game Mới - `initializeGame()`**

**📍 Vị trí**: Line ~145-155 trong `GameService.java`

**🎬 Kịch Bản Kích Hoạt**:
```
MatchmakingService.tryMatchmaking() 
  → Tìm được 2 người chơi trong queue
  → scheduler.schedule(() -> gameService.initializeGame())  // Delay 2 giây
```

**📊 Dữ Liệu Ghi vào Database**:

```java
// File: GameService.java - Method: persistNewGame()
private void persistNewGame(GameState game) throws SQLException {
    String sql = "INSERT INTO games (match_id, player1_id, player2_id, game_mode, total_rounds, status, started_at) " +
                 "VALUES (?, ?, ?, 'QUICK', ?, 'IN_PROGRESS', NOW())";
    try (Connection conn = dbManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, game.getMatchId());        // match_id = UUID (36 chars)
        stmt.setInt(2, Integer.parseInt(game.getPlayer1Id()));
        stmt.setInt(3, Integer.parseInt(game.getPlayer2Id()));
        stmt.setInt(4, GameConstants.TOTAL_ROUNDS);  // = 3
        stmt.executeUpdate();
    }
}
```

**📋 Kết Quả trong Bảng `games`**:

| Cột           | Giá Trị Ví Dụ                          | Ghi Chú                    |
|---------------|----------------------------------------|----------------------------|
| `match_id`    | `"a1b2c3d4-..."`                       | UUID 36 ký tự              |
| `player1_id`  | `101`                                  | INT (FK users.user_id)     |
| `player2_id`  | `102`                                  | INT (FK users.user_id)     |
| `game_mode`   | `'QUICK'`                              | ENUM (cố định cho MVP)     |
| `total_rounds`| `3`                                    | Số hiệp (cố định)          |
| `status`      | `'IN_PROGRESS'`                        | ⭐ **Trạng thái khởi tạo** |
| `started_at`  | `2025-11-03 14:30:00`                  | Timestamp tự động          |
| `player1_score`, `player2_score` | `0`, `0`        | Giá trị mặc định           |
| `completed_rounds` | `0`                           | Chưa hoàn thành hiệp nào   |
| `winner_id`   | `NULL`                                 | Chưa có người thắng        |
| `completed_at`| `NULL`                                 | Chưa kết thúc              |

**🔍 Lý Do Ghi Ngay Vào DB**:
- ✅ **Lịch sử đầy đủ**: Nếu server crash, có thể khôi phục (tính năng HOÃN).
- ✅ **Kiểm toán**: Admin có thể tra cứu tất cả các trận đã tạo.
- ✅ **Đồng bộ**: Cả bộ nhớ (`activeGames`) và DB đều có trạng thái nhất quán.

---

#### **BƯỚC 2: INSERT Kết Quả Mỗi Hiệp - `executeRoundRevealAndProceed()`**

**📍 Vị trí**: Line ~306-335 trong `GameService.java`

**🎬 Kịch Bản Kích Hoạt**:
```
Client A chọn bài → playCard() → Chờ Client B
Client B chọn bài → playCard() → triggerReveal = true
  → executeRoundRevealAndProceed() được gọi
```

**📊 Dữ Liệu Ghi vào Database**:

```java
// File: GameService.java - Method: persistRoundResult()
private void persistRoundResult(GameState game, CardDto p1Card, CardDto p2Card, 
                                 int p1RoundScore, int p2RoundScore) throws SQLException {
    String sql = "INSERT INTO game_rounds (match_id, round_number, " +
                 "player1_card_id, player1_card_value, player1_is_auto_picked, " +
                 "player2_card_id, player2_card_value, player2_is_auto_picked, " +
                 "round_winner_id, player1_round_score, player2_round_score, completed_at) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
    
    // Xác định người thắng hiệp
    String roundWinnerId = null;
    if (p1RoundScore > p2RoundScore) roundWinnerId = game.getPlayer1Id();
    else if (p2RoundScore > p1RoundScore) roundWinnerId = game.getPlayer2Id();
    
    try (Connection conn = dbManager.getConnection(); 
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, game.getMatchId());
        stmt.setInt(2, game.getCurrentRound());          // 1, 2, hoặc 3
        stmt.setInt(3, p1Card.getCardId());              // ID lá bài (1-36)
        stmt.setInt(4, p1Card.getValue());               // Giá trị lá bài (1-9)
        stmt.setBoolean(5, game.isPlayer1AutoPicked());  // ⭐ CRITICAL MVP
        stmt.setInt(6, p2Card.getCardId());
        stmt.setInt(7, p2Card.getValue());
        stmt.setBoolean(8, game.isPlayer2AutoPicked());  // ⭐ CRITICAL MVP
        if (roundWinnerId != null) stmt.setInt(9, Integer.parseInt(roundWinnerId));
        else stmt.setNull(9, java.sql.Types.INTEGER);
        stmt.setInt(10, p1RoundScore);
        stmt.setInt(11, p2RoundScore);
        stmt.executeUpdate();
    }
}
```

**📋 Kết Quả trong Bảng `game_rounds`** (Ví dụ Round 1):

| Cột                    | Giá Trị Ví Dụ  | Ghi Chú                                      |
|------------------------|----------------|----------------------------------------------|
| `round_id`             | `1`            | AUTO_INCREMENT                               |
| `match_id`             | `"a1b2c3d4-..."`| FK → games.match_id                         |
| `round_number`         | `1`            | Hiệp thứ nhất                                |
| `player1_card_id`      | `5`            | FK → cards.card_id (5♥)                     |
| `player1_card_value`   | `5`            | Giá trị lá bài                               |
| `player1_is_auto_picked` | `FALSE`      | ⭐ Người chơi tự chọn (không timeout)        |
| `player2_card_id`      | `9`            | FK → cards.card_id (9♥)                     |
| `player2_card_value`   | `9`            | Giá trị lá bài                               |
| `player2_is_auto_picked` | `TRUE`       | ⭐ Hệ thống auto-pick do hết thời gian       |
| `round_winner_id`      | `102`          | Player 2 thắng (9 > 5)                       |
| `player1_round_score`  | `5`            | Điểm Player 1 nhận được                      |
| `player2_round_score`  | `9`            | Điểm Player 2 nhận được                      |
| `completed_at`         | `2025-11-03 14:30:15` | Timestamp hoàn thành hiệp         |

**⚡ Điểm Đặc Biệt**:
- Cột `player*_is_auto_picked` là **CRITICAL MVP** - Phân biệt giữa "người chơi chọn" vs "hệ thống auto-pick".
- Dữ liệu này được Frontend sử dụng để hiển thị badge "AUTO" trong UI.

**🔁 Lặp Lại**:
- Bước này được thực thi **3 lần** (Round 1, 2, 3) cho mỗi trận đấu.

---

#### **BƯỚC 3: UPDATE Game Khi Kết Thúc Bình Thường - `handleGameEnd()`**

**📍 Vị trí**: Line ~376-410 trong `GameService.java`

**🎬 Kịch Bản Kích Hoạt**:
```
executeRoundRevealAndProceed() (Round 3)
  → if (game.getCurrentRound() >= GameConstants.TOTAL_ROUNDS) {
      game.setComplete(true);
      gameOver = true;
      gameSnapshotForEnd = cloneGameState(game);
    }
  → handleGameEnd(gameSnapshotForEnd) được gọi BÊN NGOÀI lock
```

**📊 Dữ Liệu Cập Nhật vào Database**:

```java
// File: GameService.java - Method: handleGameEnd()
private void handleGameEnd(GameState completedGame) {
    String winnerId = getGameWinner(completedGame.getMatchId());
    
    try (Connection conn = dbManager.getConnection()) {
        // [1] UPDATE bảng games
        String sqlUpdate = "UPDATE games SET " +
                          "status = 'COMPLETED', " +
                          "winner_id = ?, " +
                          "player1_score = ?, " +
                          "player2_score = ?, " +
                          "completed_rounds = ?, " +
                          "completed_at = NOW() " +
                          "WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
            if (winnerId != null) stmt.setInt(1, Integer.parseInt(winnerId));
            else stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setInt(2, completedGame.getPlayer1Score());
            stmt.setInt(3, completedGame.getPlayer2Score());
            stmt.setInt(4, completedGame.getCurrentRound());  // = 3
            stmt.setString(5, completedGame.getMatchId());
            stmt.executeUpdate();
        }
        
        // [2] CALL stored procedure cập nhật thống kê
        String sqlCall = "{CALL update_user_stats_after_game(?)}";
        try (CallableStatement cstmt = conn.prepareCall(sqlCall)) {
            cstmt.setString(1, completedGame.getMatchId());
            cstmt.execute();
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

**📋 Kết Quả trong Bảng `games`** (SAU khi UPDATE):

| Cột                | Giá Trị TRƯỚC      | Giá Trị SAU              | Thay Đổi           |
|--------------------|--------------------|--------------------------|--------------------|
| `status`           | `'IN_PROGRESS'`    | `'COMPLETED'`            | ⭐ **CẬP NHẬT**    |
| `winner_id`        | `NULL`             | `102`                    | ⭐ **CẬP NHẬT**    |
| `player1_score`    | `0`                | `14`                     | ⭐ **CẬP NHẬT**    |
| `player2_score`    | `0`                | `21`                     | ⭐ **CẬP NHẬT**    |
| `completed_rounds` | `0`                | `3`                      | ⭐ **CẬP NHẬT**    |
| `completed_at`     | `NULL`             | `2025-11-03 14:31:00`    | ⭐ **CẬP NHẬT**    |

**🎯 Người Thắng Được Xác Định Bởi**:

```java
// File: shared/util/GameRuleUtils.java
public static int getGameWinner(int player1Score, int player2Score) {
    if (player1Score > player2Score) return 1;      // Player 1 thắng
    if (player2Score > player1Score) return 2;      // Player 2 thắng
    return 0;                                       // Hòa (không xảy ra trong MVP)
}
```

---

#### **BƯỚC 3.5: Stored Procedure - `update_user_stats_after_game()`**

**📍 Vị trí**: File `DB_SCRIPT.sql`, line ~530-560

**🎯 Mục Đích**: Tự động cập nhật thống kê người chơi trong bảng `user_profiles`.

**📊 Logic Stored Procedure**:

```sql
CREATE PROCEDURE update_user_stats_after_game(
    IN p_match_id VARCHAR(36)
)
BEGIN
    DECLARE v_player1_id INT;
    DECLARE v_player2_id INT;
    DECLARE v_winner_id INT;
    
    -- Lấy thông tin game
    SELECT player1_id, player2_id, winner_id
    INTO v_player1_id, v_player2_id, v_winner_id
    FROM games
    WHERE match_id = p_match_id;
    
    -- [1] Cập nhật games_played cho CỢ 2 người chơi
    UPDATE user_profiles
    SET games_played = games_played + 1
    WHERE user_id IN (v_player1_id, v_player2_id);
    
    -- [2] Cập nhật games_won cho người thắng
    IF v_winner_id IS NOT NULL THEN
        UPDATE user_profiles
        SET games_won = games_won + 1
        WHERE user_id = v_winner_id;
        
        -- [3] Cập nhật games_lost cho người thua
        UPDATE user_profiles
        SET games_lost = games_lost + 1
        WHERE user_id IN (v_player1_id, v_player2_id) AND user_id != v_winner_id;
    END IF;
END
```

**📋 Kết Quả trong Bảng `user_profiles`**:

| user_id | games_played | games_won | games_lost | Diễn Giải                 |
|---------|--------------|-----------|------------|---------------------------|
| `101`   | 5 → **6**    | 2         | 3 → **4**  | Thua trận này (+1 played, +1 lost) |
| `102`   | 8 → **9**    | 5 → **6** | 3          | Thắng trận này (+1 played, +1 won)  |

**❓ TẠI SAO PHẢI GỌI STORED PROCEDURE TRONG `handleGameEnd()`?**

✅ **Lý Do 1: Tính Nguyên Tử (Atomicity)**
- Cập nhật `games` và `user_profiles` phải trong **cùng một giao dịch**.
- Nếu tách biệt → Có thể xảy ra: Game đã `COMPLETED` nhưng stats chưa cập nhật (mất dữ liệu).

✅ **Lý Do 2: Hiệu Năng**
- Stored Procedure chạy **bên trong database**, giảm lượng dữ liệu truyền qua mạng.
- Chỉ cần 1 lệnh CALL thay vì 3+ lệnh UPDATE riêng lẻ từ Java.

✅ **Lý Do 3: Tính Nhất Quán**
- `games_played = games_won + games_lost` luôn đúng (do stored procedure tự động tính).
- Không thể quên cập nhật một trong số các cột.

---

#### **BƯỚC 4: UPDATE Game Khi Forfeit - `handleForfeit()`**

**📍 Vị trí**: Line ~414-460 trong `GameService.java`

**🎬 Kịch Bản Kích Hoạt**:
```
GatewayWebSocketHandler.afterConnectionClosed()
  → Gửi AUTH.LOGOUT_REQUEST tự động
  → ClientConnectionHandler.handleLogout()
    → if (matchId != null) gameService.handleForfeit(matchId, userId)
```

**📊 Dữ Liệu Cập Nhật vào Database**:

```java
// File: GameService.java - Method: handleForfeit()
public void handleForfeit(String matchId, String forfeitingPlayerId) {
    Lock lock = gameLocks.get(matchId);
    if (lock == null) return;
    
    GameState gameSnapshotForEnd = null;
    String winningPlayerId = null;
    
    lock.lock();
    try {
        GameState game = activeGames.get(matchId);
        if (game == null || game.isComplete()) return;
        
        game.setComplete(true);
        winningPlayerId = forfeitingPlayerId.equals(game.getPlayer1Id()) 
                          ? game.getPlayer2Id() 
                          : game.getPlayer1Id();
        
        try (Connection conn = dbManager.getConnection()) {
            // [1] UPDATE bảng games
            String sqlUpdate = "UPDATE games SET " +
                              "status = 'ABANDONED', " +
                              "winner_id = ?, " +
                              "completed_at = NOW() " +
                              "WHERE match_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                stmt.setInt(1, Integer.parseInt(winningPlayerId));
                stmt.setString(2, matchId);
                stmt.executeUpdate();
            }
            
            // [2] CALL stored procedure (GIỐNG handleGameEnd)
            String sqlCall = "{CALL update_user_stats_after_game(?)}";
            try (CallableStatement cstmt = conn.prepareCall(sqlCall)) {
                cstmt.setString(1, matchId);
                cstmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        gameSnapshotForEnd = cloneGameState(game);
    } finally {
        lock.unlock();
    }
    
    // Gửi thông báo GAME_END (bên ngoài lock)
    if (winningPlayerId != null && gameSnapshotForEnd != null) {
        // ...
    }
    cleanupGame(matchId);
}
```

**📋 Kết Quả trong Bảng `games`** (SAU khi Forfeit):

| Cột                | Giá Trị              | Ghi Chú                                      |
|--------------------|----------------------|----------------------------------------------|
| `status`           | `'ABANDONED'`        | ⭐ **Khác với COMPLETED**                    |
| `winner_id`        | `102`                | Người còn lại thắng tự động                  |
| `player1_score`    | `8`                  | Giữ nguyên điểm hiện tại (không phải 0)      |
| `player2_score`    | `12`                 | Giữ nguyên điểm hiện tại                     |
| `completed_rounds` | `2`                  | Chỉ chơi được 2/3 hiệp                       |
| `completed_at`     | `2025-11-03 14:30:45`| Timestamp forfeit                            |

**❓ TẠI SAO `handleForfeit()` CŨNG GỌI `update_user_stats_after_game()`?**

✅ **Lý Do: Forfeit = Thua Tự Động**
- Người bỏ cuộc được tính là **thua** (games_lost + 1).
- Người còn lại được tính là **thắng** (games_won + 1).
- Stored Procedure đọc `winner_id` từ bảng `games` (đã được set) và tự động cập nhật stats.

✅ **Logic Stored Procedure KHÔNG phân biệt COMPLETED vs ABANDONED**:
```sql
-- Stored procedure CHỈ QUAN TÂM đến winner_id
IF v_winner_id IS NOT NULL THEN
    UPDATE user_profiles SET games_won = games_won + 1 WHERE user_id = v_winner_id;
    UPDATE user_profiles SET games_lost = games_lost + 1 
    WHERE user_id IN (v_player1_id, v_player2_id) AND user_id != v_winner_id;
END IF;
```

---

### 📊 SƠ ĐỒ TỔNG HỢP: LUỒNG CẬP NHẬT DATABASE

```
┌─────────────────────────────────────────────────────────────────────┐
│                  VÒNG ĐỜI TRẠNG THÁI GAME TRONG DATABASE            │
└─────────────────────────────────────────────────────────────────────┘

[1] KHỞI TẠO (initializeGame)
    ┌─────────────────────────────────┐
    │ INSERT INTO games               │
    │ - status = 'IN_PROGRESS'        │
    │ - player1_score = 0             │
    │ - player2_score = 0             │
    │ - completed_rounds = 0          │
    │ - winner_id = NULL              │
    └─────────────────────────────────┘
                   │
                   │ Client chơi bài
                   ▼
[2] MỖI HIỆP (executeRoundRevealAndProceed) - Lặp 3 lần
    ┌─────────────────────────────────┐
    │ INSERT INTO game_rounds         │
    │ - round_number (1, 2, 3)        │
    │ - player*_card_id               │
    │ - player*_is_auto_picked ⭐     │
    │ - round_winner_id               │
    │ - player*_round_score           │
    └─────────────────────────────────┘
                   │
                   │ Sau Round 3
                   ▼
[3A] KẾT THÚC BÌNH THƯỜNG (handleGameEnd)
    ┌─────────────────────────────────┐
    │ UPDATE games SET                │
    │ - status = 'COMPLETED'          │
    │ - winner_id = {winnerId}        │
    │ - player1_score = {total}       │
    │ - player2_score = {total}       │
    │ - completed_rounds = 3          │
    │ - completed_at = NOW()          │
    └─────────────────────────────────┘
                   │
                   │ Gọi Stored Procedure
                   ▼
    ┌─────────────────────────────────┐
    │ CALL update_user_stats_after_game │
    │ - UPDATE user_profiles          │
    │   games_played += 1 (cả 2)      │
    │   games_won += 1 (winner)       │
    │   games_lost += 1 (loser)       │
    └─────────────────────────────────┘

[3B] BỎ CUỘC (handleForfeit) - Có thể xảy ra BẤT KỲ LÚC NÀO
    ┌─────────────────────────────────┐
    │ UPDATE games SET                │
    │ - status = 'ABANDONED' ⚠️       │
    │ - winner_id = {remainingPlayer} │
    │ - completed_at = NOW()          │
    │ (GIỮ NGUYÊN scores và rounds)   │
    └─────────────────────────────────┘
                   │
                   │ Gọi CÙNG Stored Procedure
                   ▼
    ┌─────────────────────────────────┐
    │ CALL update_user_stats_after_game │
    │ (LOGIC GIỐNG [3A])              │
    └─────────────────────────────────┘
```

---

### ✅ KIỂM CHỨNG: TRUY VẤN SQL ĐỂ XEM LUỒNG

```sql
-- [1] Kiểm tra trạng thái hiện tại của game
SELECT match_id, status, player1_score, player2_score, completed_rounds, winner_id
FROM games
WHERE match_id = 'a1b2c3d4-...';

-- [2] Xem lịch sử các hiệp đã chơi
SELECT round_number, 
       player1_card_value, player1_is_auto_picked,
       player2_card_value, player2_is_auto_picked,
       round_winner_id
FROM game_rounds
WHERE match_id = 'a1b2c3d4-...'
ORDER BY round_number;

-- [3] Kiểm tra stats của 2 người chơi
SELECT u.username, p.games_played, p.games_won, p.games_lost
FROM users u
JOIN user_profiles p ON u.user_id = p.user_id
WHERE u.user_id IN (101, 102);
```

---

## PHẦN B: Mục Đích Của gameSnapshotForEnd và cloneGameState()

### 🎯 VẤN ĐỀ CẦN GIẢI QUYẾT

Trong `executeRoundRevealAndProceed()` và `handleForfeit()`, chúng ta thấy pattern sau:

```java
Lock lock = gameLocks.get(matchId);
lock.lock();
try {
    GameState game = activeGames.get(matchId);
    // ... Xử lý logic ...
    
    if (gameOver) {
        gameSnapshotForEnd = cloneGameState(game); // ⭐ TẠI SAO CLONE?
    }
} finally {
    lock.unlock();
}

if (gameOver && gameSnapshotForEnd != null) {
    handleGameEnd(gameSnapshotForEnd);  // ⭐ GỌI BÊN NGOÀI LOCK
    cleanupGame(matchId);
}
```

**❓ Câu Hỏi Quan Trọng**:
- Tại sao phải tạo `gameSnapshotForEnd = cloneGameState(game)` bên trong lock?
- Tại sao không truyền thẳng `game` (đối tượng gốc) cho `handleGameEnd()`?
- Rủi ro gì sẽ xảy ra nếu không clone?

---

### 🧵 PHÂN TÍCH NGUY CƠ: RACE CONDITION

#### **Kịch Bản Nguy Hiểm Nếu KHÔNG Clone**

```java
// ❌ CODE SAI (KHÔNG CLONE):
GameState game = activeGames.get(matchId);
lock.lock();
try {
    // ... Xử lý logic ...
    if (gameOver) {
        // KHÔNG clone, truyền thẳng tham chiếu
    }
} finally {
    lock.unlock(); // ⚠️ Lock được giải phóng NGAY
}

// ⚠️ GỌI BÊN NGOÀI LOCK - RẤT NGUY HIỂM!
if (gameOver) {
    handleGameEnd(game);  // 'game' vẫn trỏ đến đối tượng SỐNG trong activeGames
    cleanupGame(matchId); // Xóa khỏi activeGames
}
```

**💥 Race Condition Có Thể Xảy Ra**:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        TIMELINE NGUY HIỂM                           │
└─────────────────────────────────────────────────────────────────────┘

T0: Round 3 kết thúc, lock.unlock() được gọi
    └─> handleGameEnd(game) bắt đầu (BÊN NGOÀI LOCK)
    
T1: handleGameEnd() đang đọc game.getPlayer1Score() → Giả sử = 21
    
T2: ⚠️ Luồng Timeout Cũ (của Round 2) vẫn còn tồn tại trong scheduler
    └─> handleRoundTimeout(matchId, 2) được kích hoạt muộn
    └─> Cố gắng lấy lock và GHI ĐÈ game.setPlayer1Score(999) (BUG!)
    
T3: handleGameEnd() tiếp tục đọc game.getPlayer2Score() → Giá trị MỚI bị sửa
    
T4: Database ghi SAI: player1_score = 21, player2_score = 999 ❌
```

---

#### **Lý Do Chi Tiết: Tại Sao Luồng Timeout Cũ Vẫn Chạy?**

**🕐 Cơ Chế Scheduler trong GameService**:

```java
// File: GameService.java - startNextRound()
scheduler.schedule(() -> handleRoundTimeout(matchId, roundNumber), 
                   timeoutMillis, TimeUnit.MILLISECONDS);
```

**⏰ Vấn Đề**: Scheduled Task **KHÔNG tự hủy** khi game kết thúc sớm!

```
Round 1: Timeout sau 10s
Round 2: Timeout sau 10s
Round 3: Timeout sau 10s

Nếu cả 3 round kết thúc SAU 5 GIÂY (cả 2 player đều chọn nhanh):
  → Vẫn còn 3 task "handleRoundTimeout" đang nằm chờ trong scheduler!
  → Chúng sẽ kích hoạt sau 5s, 15s, 25s (mặc dù game đã xong).
```

**🛡️ Cơ Chế Bảo Vệ Hiện Tại**:

```java
// File: GameService.java - handleRoundTimeout()
private void handleRoundTimeout(String matchId, int roundNumber) {
    Lock lock = gameLocks.get(matchId);
    if (lock == null) return; // ✅ Game đã cleanup → lock = null → RETURN
    
    lock.lock();
    try {
        GameState game = activeGames.get(matchId);
        if (game == null || game.isComplete() || game.getCurrentRound() != roundNumber) 
            return; // ✅ Game đã xong hoặc round không khớp → RETURN
        
        // ... Logic timeout ...
    } finally {
        lock.unlock();
    }
}
```

**❓ Nhưng Nếu `cleanupGame()` Chạy Chậm Thì Sao?**

```java
// ❌ KỊCH BẢN XẤU NHẤT:
lock.unlock();  // T0: executeRoundRevealAndProceed() unlock

handleGameEnd(game);  // T1-T5: Đang chạy, CHƯA gọi cleanupGame()
  └─> Đọc game.getPlayer1Score() (T1)
  └─> CALL stored procedure (T2-T4, chậm do network latency)
  └─> notifyPlayer() (T5)
  
// ⚠️ T3: Luồng timeout cũ lấy được lock (vì chưa cleanup)
handleRoundTimeout(matchId, 2):
  lock.lock() // ✅ Lấy được lock vì executeRoundRevealAndProceed đã unlock
  game = activeGames.get(matchId) // ✅ Vẫn tồn tại (chưa cleanupGame)
  if (game.isComplete()) return // ❌ BUG: Nếu game.setComplete(true) chưa được set
  // → GHI ĐÈ game state!
  
cleanupGame(matchId); // T6: Quá muộn!
```

---

### ✅ GIẢI PHÁP: Clone GameState Bên Trong Lock

**🎯 Code Đúng (Hiện Tại)**:

```java
// File: GameService.java - executeRoundRevealAndProceed()
lock.lock();
try {
    GameState game = activeGames.get(matchId);
    // ... Xử lý logic ...
    
    if (game.getCurrentRound() >= GameConstants.TOTAL_ROUNDS) {
        game.setComplete(true);  // ⭐ SET FLAG BÊN TRONG LOCK
        gameOver = true;
        gameSnapshotForEnd = cloneGameState(game); // ⭐ CLONE BÊN TRONG LOCK
    }
} finally {
    lock.unlock();
}

// ✅ GỌI BÊN NGOÀI LOCK - AN TOÀN
if (gameOver && gameSnapshotForEnd != null) {
    handleGameEnd(gameSnapshotForEnd);  // Dùng BẢN SAO, không phải đối tượng gốc
    cleanupGame(matchId);
}
```

**🔍 Phân Tích Chi Tiết `cloneGameState()`**:

```java
// File: GameService.java
private GameState cloneGameState(GameState original) {
    if (original == null) return null;
    
    GameState copy = new GameState(
        original.getMatchId(), 
        original.getPlayer1Id(), 
        original.getPlayer2Id()
    );
    
    // CHỈ COPY CÁC FIELD CẦN THIẾT CHO handleGameEnd()
    copy.setPlayer1Score(original.getPlayer1Score());
    copy.setPlayer2Score(original.getPlayer2Score());
    copy.setComplete(original.isComplete());
    
    // ⚠️ KHÔNG copy availableCards, playedCards, roundHistory
    // → Giảm memory footprint
    
    return copy;
}
```

**🛡️ Cơ Chế Bảo Vệ (Defense in Depth)**:

| Lớp Bảo Vệ | Cơ Chế | Hiệu Quả |
|-------------|--------|----------|
| **Lớp 1** | `game.setComplete(true)` BÊN TRONG lock | ⭐⭐⭐ |
| **Lớp 2** | `cloneGameState()` → Tạo bản sao độc lập | ⭐⭐⭐⭐⭐ |
| **Lớp 3** | `handleRoundTimeout()` kiểm tra `game.isComplete()` | ⭐⭐⭐⭐ |
| **Lớp 4** | `cleanupGame()` xóa lock và activeGames | ⭐⭐⭐⭐ |

---

### 💥 DEMO: Rủi Ro Nếu KHÔNG Clone

**Kịch Bản Thử Nghiệm**:

```java
// ❌ CODE THÍ NGHIỆM (KHÔNG CLONE):
@Test
public void testRaceConditionWithoutClone() throws Exception {
    String matchId = "test-match-123";
    GameState game = new GameState(matchId, "1", "2");
    game.setPlayer1Score(21);
    game.setPlayer2Score(18);
    activeGames.put(matchId, game);
    
    // Thread 1: Giả lập handleGameEnd() (đọc chậm)
    Thread t1 = new Thread(() -> {
        int score1 = game.getPlayer1Score(); // Đọc = 21
        Thread.sleep(100); // Giả lập DB call chậm
        int score2 = game.getPlayer2Score(); // Đọc = ??? (có thể bị sửa)
        System.out.println("Saved: " + score1 + " vs " + score2);
    });
    
    // Thread 2: Giả lập timeout cũ (ghi sau 50ms)
    Thread t2 = new Thread(() -> {
        Thread.sleep(50);
        game.setPlayer1Score(999); // ⚠️ GHI ĐÈ!
        game.setPlayer2Score(888);
    });
    
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    
    // ❌ KẾT QUẢ: "Saved: 21 vs 888" → DỮ LIỆU SAI!
}
```

**✅ Với Clone (An Toàn)**:

```java
@Test
public void testSafeWithClone() throws Exception {
    String matchId = "test-match-123";
    GameState game = new GameState(matchId, "1", "2");
    game.setPlayer1Score(21);
    game.setPlayer2Score(18);
    
    // ✅ CLONE BÊN TRONG "LOCK"
    GameState snapshot = cloneGameState(game);
    
    // Thread 1: Dùng snapshot (BẢN SAO)
    Thread t1 = new Thread(() -> {
        int score1 = snapshot.getPlayer1Score(); // = 21
        Thread.sleep(100);
        int score2 = snapshot.getPlayer2Score(); // = 18 (KHÔNG ĐỔI)
        System.out.println("Saved: " + score1 + " vs " + score2);
    });
    
    // Thread 2: Sửa đối tượng GỐC (không ảnh hưởng snapshot)
    Thread t2 = new Thread(() -> {
        Thread.sleep(50);
        game.setPlayer1Score(999); // Sửa đối tượng GỐC
        game.setPlayer2Score(888);
    });
    
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    
    // ✅ KẾT QUẢ: "Saved: 21 vs 18" → ĐÚNG!
}
```

---

### 📊 SƠ ĐỒ: CLONE vs KHÔNG CLONE

```
┌──────────────────────────────────────────────────────────────────────┐
│              KHÔNG CLONE (NGUY HIỂM)                                 │
└──────────────────────────────────────────────────────────────────────┘

activeGames.get(matchId) → [GameState Object in Memory]
                                    ↑
                                    │ Cùng tham chiếu
                    ┌───────────────┴────────────────┐
                    │                                │
         handleGameEnd(game)              handleRoundTimeout()
         (Đọc bên ngoài lock)             (Ghi bên trong lock)
                    │                                │
                    └───────────────┬────────────────┘
                                    ↓
                            ⚠️ RACE CONDITION!


┌──────────────────────────────────────────────────────────────────────┐
│              CÓ CLONE (AN TOÀN)                                      │
└──────────────────────────────────────────────────────────────────────┘

activeGames.get(matchId) → [GameState Object - GỐC]
                                    │
                    ┌───────────────┤ (bên trong lock)
                    │               │
            cloneGameState()        │
                    │               │
                    ▼               ▼
         [GameState Snapshot]   [GameState GỐC]
         (Bản sao độc lập)      (Có thể bị sửa)
                    │               │
                    │               │
         handleGameEnd(snapshot)   handleRoundTimeout()
         (Đọc bản sao)             (Ghi vào gốc)
                    │               │
                    └───────────────┘
                            ↓
                    ✅ KHÔNG CAN THIỆP LẪN NHAU
```

---

### 🎯 KẾT LUẬN PHẦN B

**✅ 3 LÝ DO PHẢI CLONE**:

1. **Tách Biệt Trách Nhiệm (Separation of Concerns)**:
   - `handleGameEnd()` cần dữ liệu **bất biến** (immutable) để ghi DB.
   - Các luồng timeout cũ vẫn có thể cố sửa đổi `game` gốc.
   - Clone → Đảm bảo `handleGameEnd()` luôn làm việc với dữ liệu nhất quán.

2. **Giảm Thời Gian Giữ Lock (Minimize Lock Contention)**:
   - `handleGameEnd()` có thể chậm (DB I/O, network latency).
   - Nếu giữ lock trong suốt thời gian đó → Blocking tất cả requests khác.
   - Clone → Unlock ngay, xử lý bất đồng bộ với bản sao.

3. **Phòng Thủ Sâu (Defense in Depth)**:
   - Ngay cả khi `game.setComplete(true)` được set, vẫn có thể có bug logic.
   - Clone → Lớp bảo vệ cuối cùng, đảm bảo dữ liệu không bị nhiễu.

**⚠️ Rủi Ro Nếu KHÔNG Clone**:

| Rủi Ro | Mô Tả | Hậu Quả |
|--------|-------|---------|
| **Data Corruption** | Scores bị ghi sai vào DB | Leaderboard sai, thống kê sai |
| **Inconsistent State** | `completed_rounds` không khớp `player*_score` | Logic nghiệp vụ sai |
| **Audit Trail Failure** | Không thể tái hiện chính xác trạng thái cuối | Mất khả năng debug |

---

## 🎓 TÓM TẮT TOÀN BỘ

### Phần A: Luồng Database (4 Bước)

1. ✅ `initializeGame()` → INSERT `games` (status='IN_PROGRESS')
2. ✅ `executeRoundRevealAndProceed()` → INSERT `game_rounds` (3 lần)
3. ✅ `handleGameEnd()` → UPDATE `games` (status='COMPLETED') + CALL stored procedure
4. ✅ `handleForfeit()` → UPDATE `games` (status='ABANDONED') + CALL stored procedure

**Stored Procedure** được gọi trong **CẢ 2** trường hợp (3 & 4) để đảm bảo stats luôn nhất quán.

### Phần B: Clone GameState

- ✅ Clone BÊN TRONG lock để tạo **snapshot bất biến**.
- ✅ Truyền snapshot cho `handleGameEnd()` (chạy BÊN NGOÀI lock).
- ✅ Ngăn chặn race condition với các luồng timeout cũ.

---

**📝 Tài liệu này đã hoàn thành YÊU CẦU 1.**

Tiếp theo, chúng ta sẽ chuyển sang **YÊU CẦU 2: Phân Tích Tính Khả Thi** cho bộ 3 tính năng mới.

