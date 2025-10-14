-- ============================================================================
-- Card Game "Rút Bài May Mắn" - MVP Database Schema (Pure Java Compatible)
-- Version: 1.1.0 (Adapted for Pure Java MVP - Network Programming Course)
-- Database: MySQL 8.0+
-- Character Set: utf8mb4_unicode_ci
-- Storage Engine: InnoDB
-- ============================================================================
-- THAY ĐỔI TỪ V1.0.0:
--   - User ID: VARCHAR(50) UUID → INT AUTO_INCREMENT (đơn giản hóa cho Java thuần)
--   - Giữ nguyên TẤT CẢ tính năng MVP (auto-pick, email, display_name, game_mode)
--   - Giữ nguyên cột HOÃN (dễ mở rộng, không ảnh hưởng performance MVP)
--   - Code Java thuần chỉ cần dùng getInt()/setInt() thay vì getString()/setString()
-- ============================================================================
-- Scope: Supports core features (A-D):
--   (A) User Registration & Login
--   (B) Quick Match Matchmaking
--   (C) 3-Round Gameplay with 10s timeout per round (✅ CÓ auto-pick flag)
--   (D) Game Completion with Win/Loss tracking
-- ============================================================================

-- Tạo cơ sở dữ liệu
CREATE DATABASE IF NOT EXISTS cardgame_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE cardgame_db;

-- ============================================================================
-- BẢNG: users
-- Mục đích: Xác thực người dùng và quản lý tài khoản (MVP ĐANG DÙNG)
-- Phạm vi: Đăng ký, Đăng nhập, Trạng thái tài khoản
-- THAY ĐỔI: id VARCHAR(50) → user_id INT AUTO_INCREMENT
-- ============================================================================
CREATE TABLE users (
    -- Định danh chính (ĐANG DÙNG - MVP)
    user_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Định danh người dùng duy nhất (tự tăng)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT 'Tên đăng nhập duy nhất (3-20 ký tự)',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT 'Email người dùng để khôi phục tài khoản',
    
    -- Xác thực (ĐANG DÙNG - MVP)
    password_hash VARCHAR(255) NOT NULL COMMENT 'ĐANG DÙNG: Mật khẩu dạng plain cho MVP. HOÃN: Sẽ băm bằng BCrypt/Argon2 trong môi trường thật',
    
    -- Trạng thái tài khoản (ĐANG DÙNG - MVP)
    status ENUM('ACTIVE', 'SUSPENDED', 'BANNED') DEFAULT 'ACTIVE' COMMENT 'ĐANG DÙNG: Trạng thái tài khoản. SUSPENDED/BANNED cho tính năng kiểm duyệt (HOÃN)',
    
    -- Dấu thời gian (ĐANG DÙNG - MVP)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'ĐANG DÙNG: Thời điểm tạo tài khoản',
    last_login TIMESTAMP NULL COMMENT 'ĐANG DÙNG: Lần đăng nhập gần nhất (hiển thị trạng thái online)',
    
    -- Trường HOÃN (dành cho mở rộng sau)
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'HOÃN: Tự động cập nhật khi có thay đổi',
    email_verified BOOLEAN DEFAULT FALSE COMMENT 'HOÃN: Xác minh email cho bảo mật môi trường thật',
    failed_login_attempts INT DEFAULT 0 COMMENT 'HOÃN: Đếm số lần đăng nhập thất bại để khóa tài khoản',
    locked_until TIMESTAMP NULL COMMENT 'HOÃN: Thời điểm hết hạn khóa tài khoản',
    
    -- Chỉ mục hiệu năng
    INDEX idx_username (username) COMMENT 'Tăng tốc tra cứu username khi đăng nhập',
    INDEX idx_email (email) COMMENT 'Tăng tốc tra cứu email khi kiểm tra đăng ký',
    INDEX idx_status (status) COMMENT 'Lọc người dùng đang hoạt động',
    INDEX idx_last_login (last_login) COMMENT 'ĐANG DÙNG: Truy vấn người dùng online (last_login trong 5 phút)'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Tài khoản người dùng cho xác thực và hồ sơ cơ bản';

-- ============================================================================
-- BẢNG: user_profiles
-- Mục đích: Thống kê chơi và dữ liệu bảng xếp hạng (MVP ĐANG DÙNG)
-- Phạm vi: Theo dõi Thắng/Thua, Bảng xếp hạng đơn giản (theo games_won)
-- THAY ĐỔI: user_id VARCHAR(50) → INT (FK users.user_id)
-- ============================================================================
CREATE TABLE user_profiles (
    -- Khóa chính (ĐANG DÙNG - MVP)
    user_id INT PRIMARY KEY COMMENT 'Khóa ngoại tới bảng users',
    
    -- Thông tin hiển thị (ĐANG DÙNG - MVP)
    display_name VARCHAR(100) COMMENT 'ĐANG DÙNG: Tên hiển thị ở sảnh và leaderboard',
    
    -- Thống kê trò chơi (ĐANG DÙNG - MVP)
    games_played INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Tổng số ván đã hoàn thành (phục vụ sắp xếp leaderboard)',
    games_won INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Tổng số trận thắng (chỉ số chính cho leaderboard MVP)',
    games_lost INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Tổng số trận thua (tính tỷ lệ thắng)',
    
    -- Trường HOÃN (dành cho mở rộng sau)
    games_drawn INT DEFAULT 0 COMMENT 'HOÃN: Số trận hòa (không dùng cho thể thức 3 hiệp của MVP)',
    rank_tier ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') DEFAULT 'BRONZE' COMMENT 'HOÃN: Hạng bậc để ghép trận nâng cao',
    current_rating DECIMAL(10,2) DEFAULT 1000.00 COMMENT 'HOÃN: Hệ số xếp hạng kiểu ELO',
    peak_rating DECIMAL(10,2) DEFAULT 1000.00 COMMENT 'HOÃN: Mức xếp hạng cao nhất đạt được',
    total_score DECIMAL(12,2) DEFAULT 0.00 COMMENT 'HOÃN: Tổng điểm tích lũy qua các ván',
    win_streak_current INT DEFAULT 0 COMMENT 'HOÃN: Chuỗi thắng hiện tại',
    win_streak_best INT DEFAULT 0 COMMENT 'HOÃN: Chuỗi thắng tốt nhất',
    total_playtime_minutes INT DEFAULT 0 COMMENT 'HOÃN: Tổng thời gian chơi',
    achievements JSON COMMENT 'HOÃN: Mảng JSON thành tích đã mở khóa',
    preferences JSON COMMENT 'HOÃN: Tùy chọn người dùng (giao diện, thông báo, v.v.)',
    
    -- Ràng buộc khóa ngoại
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Chỉ mục hiệu năng
    INDEX idx_games_won (games_won DESC) COMMENT 'ĐANG DÙNG: Sắp xếp leaderboard theo số trận thắng',
    INDEX idx_games_played (games_played) COMMENT 'ĐANG DÙNG: Lọc người chơi giàu kinh nghiệm',
    INDEX idx_rating (current_rating) COMMENT 'HOÃN: Cho ghép trận dựa trên ELO',
    INDEX idx_rank (rank_tier) COMMENT 'HOÃN: Cho ghép trận theo bậc hạng'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Thống kê người chơi và dữ liệu leaderboard đơn giản';

-- ============================================================================
-- BẢNG: cards
-- Mục đích: Định nghĩa bộ bài (36 lá: A-9 của 4 chất)
-- Phạm vi: Bảng tham chiếu tĩnh phục vụ logic game
-- THAY ĐỔI: id AUTO_INCREMENT → card_id INT (fixed 1-36, không AUTO_INCREMENT)
-- ============================================================================
CREATE TABLE cards (
    -- Định danh chính (ĐANG DÙNG - MVP)
    card_id INT PRIMARY KEY COMMENT 'ĐANG DÙNG: Định danh lá bài duy nhất (1-36, fixed)',
    
    -- Thuộc tính lá bài (ĐANG DÙNG - MVP)
    suit ENUM('HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES') NOT NULL COMMENT 'ĐANG DÙNG: Chất của lá bài',
    `rank` VARCHAR(3) NOT NULL COMMENT 'ĐANG DÙNG: Hạng của lá (A, 2, 3, 4, 5, 6, 7, 8, 9)',
    card_value INT NOT NULL COMMENT 'ĐANG DÙNG: Giá trị số để so sánh (A=1, 2=2, ..., 9=9)',
    display_name VARCHAR(10) COMMENT 'ĐANG DÙNG: Tên hiển thị UI (ví dụ: A♥, 2♦)',
    
    -- Ràng buộc duy nhất
    UNIQUE KEY uk_suit_rank (suit, `rank`) COMMENT 'Ngăn trùng lặp lá bài',
    
    -- Chỉ mục
    INDEX idx_suit (suit) COMMENT 'Lọc theo chất',
    INDEX idx_value (card_value) COMMENT 'Sắp xếp theo giá trị để hiển thị'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Bảng tham chiếu bộ bài (36 lá: A-9 của 4 chất)';

-- ============================================================================
-- BẢNG: games
-- Mục đích: Quản lý phiên chơi và kết quả
-- Phạm vi: Tạo ván, theo dõi tiến trình, ghi nhận kết quả cuối
-- THAY ĐỔI: 
--   - id VARCHAR(50) UUID → match_id VARCHAR(36) (UUID format chuẩn)
--   - player*_id VARCHAR(50) → INT (FK users.user_id)
-- ============================================================================
CREATE TABLE games (
    -- Định danh chính (ĐANG DÙNG - MVP)
    match_id VARCHAR(36) PRIMARY KEY COMMENT 'ĐANG DÙNG: Định danh ván duy nhất (định dạng UUID: 36 ký tự)',
    
    -- Người chơi (ĐANG DÙNG - MVP)
    player1_id INT NOT NULL COMMENT 'ĐANG DÙNG: ID người chơi 1 (FK users.user_id)',
    player2_id INT NOT NULL COMMENT 'ĐANG DÙNG: ID người chơi 2 (FK users.user_id)',
    
    -- Cấu hình ván (ĐANG DÙNG - MVP)
    game_mode ENUM('QUICK', 'RANKED', 'CUSTOM', 'TOURNAMENT') NOT NULL DEFAULT 'QUICK' 
        COMMENT 'ĐANG DÙNG: QUICK cho ghép trận MVP. RANKED/CUSTOM/TOURNAMENT là tính năng HOÃN',
    total_rounds INT DEFAULT 3 COMMENT 'ĐANG DÙNG: Cố định 3 hiệp cho MVP',
    
    -- Trạng thái ván (ĐANG DÙNG - MVP)
    status ENUM('WAITING_TO_START', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED', 'CANCELLED') 
        DEFAULT 'WAITING_TO_START' 
        COMMENT 'ĐANG DÙNG: WAITING_TO_START (đang ghép), IN_PROGRESS (đang chơi), COMPLETED (kết thúc), ABANDONED (thoát giữa chừng)',
    
    -- Kết quả (ĐANG DÙNG - MVP)
    winner_id INT NULL COMMENT 'ĐANG DÙNG: ID người thắng (NULL nếu bỏ dở/hủy)',
    player1_score INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Tổng điểm người chơi 1 (tổng 3 hiệp)',
    player2_score INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Tổng điểm người chơi 2 (tổng 3 hiệp)',
    completed_rounds INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Số hiệp đã hoàn thành (0-3)',
    
    -- Dấu thời gian (ĐANG DÙNG - MVP)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'ĐANG DÙNG: Thời điểm tạo ván (bắt đầu ghép)',
    started_at TIMESTAMP NULL COMMENT 'ĐANG DÙNG: Thời điểm bắt đầu (cả hai sẵn sàng)',
    completed_at TIMESTAMP NULL COMMENT 'ĐANG DÙNG: Thời điểm kết thúc (xong 3 hiệp hoặc bỏ dở)',
    
    -- Trường HOÃN (dành cho mở rộng sau)
    duration_seconds INT NULL COMMENT 'HOÃN: Tổng thời lượng ván cho thống kê',
    game_seed BIGINT COMMENT 'HOÃN: Hạt giống ngẫu nhiên để xáo bài tái lập',
    player1_rating_before DECIMAL(10,2) COMMENT 'HOÃN: Rating trước ván của người chơi 1 (tính ELO)',
    player1_rating_after DECIMAL(10,2) COMMENT 'HOÃN: Rating sau ván của người chơi 1',
    player2_rating_before DECIMAL(10,2) COMMENT 'HOÃN: Rating trước ván của người chơi 2',
    player2_rating_after DECIMAL(10,2) COMMENT 'HOÃN: Rating sau ván của người chơi 2',
    server_instance VARCHAR(50) COMMENT 'HOÃN: Máy chủ xử lý ván này',
    game_data JSON COMMENT 'HOÃN: Siêu dữ liệu bổ sung (chat, replay, v.v.)',
    
    -- Ràng buộc khóa ngoại
    FOREIGN KEY (player1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE SET NULL,
    
    -- Chỉ mục hiệu năng
    INDEX idx_players (player1_id, player2_id) COMMENT 'ĐANG DÙNG: Tìm ván theo cặp người chơi',
    INDEX idx_status (status) COMMENT 'ĐANG DÙNG: Lọc ván đang hoạt động/đã xong',
    INDEX idx_game_mode (game_mode) COMMENT 'ĐANG DÙNG: Lọc theo chế độ chơi',
    INDEX idx_created_at (created_at) COMMENT 'ĐANG DÙNG: Sắp ván mới nhất',
    INDEX idx_completed_at (completed_at) COMMENT 'ĐANG DÙNG: Sắp ván đã hoàn thành',
    INDEX idx_winner (winner_id) COMMENT 'ĐANG DÙNG: Truy vấn số trận thắng cho leaderboard',
    INDEX idx_player1_games (player1_id, created_at) COMMENT 'ĐANG DÙNG: Lịch sử ván của người chơi 1',
    INDEX idx_player2_games (player2_id, created_at) COMMENT 'ĐANG DÙNG: Lịch sử ván của người chơi 2'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Phiên ván chơi với theo dõi kết quả';

-- ============================================================================
-- BẢNG: game_rounds
-- Mục đích: Theo dõi từng hiệp trong một ván (3 hiệp mỗi ván)
-- Phạm vi: Chọn bài, tự chọn khi quá thời gian, xác định người thắng hiệp
-- THAY ĐỔI:
--   - id BIGINT AUTO_INCREMENT → round_id INT AUTO_INCREMENT
--   - game_id VARCHAR(50) → match_id VARCHAR(36)
--   - round_winner_id VARCHAR(50) → INT
-- GIỮ NGUYÊN: player*_is_auto_picked (CRITICAL - Tính năng MVP C)
-- ============================================================================
CREATE TABLE game_rounds (
    -- Định danh chính (ĐANG DÙNG - MVP)
    round_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ĐANG DÙNG: ID hiệp tự tăng',
    match_id VARCHAR(36) NOT NULL COMMENT 'ĐANG DÙNG: Khóa ngoại tới bảng games',
    round_number INT NOT NULL COMMENT 'ĐANG DÙNG: Số thứ tự hiệp (1-3)',
    
    -- Nước đi người chơi 1 (ĐANG DÙNG - MVP)
    player1_card_id INT COMMENT 'ĐANG DÙNG: ID lá bài người chơi 1 chọn (FK cards.card_id)',
    player1_card_value INT COMMENT 'ĐANG DÙNG: Giá trị lá bài để so sánh nhanh',
    player1_is_auto_picked BOOLEAN DEFAULT FALSE COMMENT '✅ CRITICAL MVP (C): TRUE nếu lá được auto-pick do hết thời gian',
    
    -- Nước đi người chơi 2 (ĐANG DÙNG - MVP)
    player2_card_id INT COMMENT 'ĐANG DÙNG: ID lá bài người chơi 2 chọn (FK cards.card_id)',
    player2_card_value INT COMMENT 'ĐANG DÙNG: Giá trị lá bài để so sánh nhanh',
    player2_is_auto_picked BOOLEAN DEFAULT FALSE COMMENT '✅ CRITICAL MVP (C): TRUE nếu lá được auto-pick do hết thời gian',
    
    -- Kết quả hiệp (ĐANG DÙNG - MVP)
    round_winner_id INT COMMENT 'ĐANG DÙNG: Người thắng hiệp (NULL nếu hòa)',
    player1_round_score INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Điểm của người chơi 1 trong hiệp',
    player2_round_score INT DEFAULT 0 COMMENT 'ĐANG DÙNG: Điểm của người chơi 2 trong hiệp',
    
    -- Dấu thời gian (ĐANG DÙNG - MVP)
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'ĐANG DÙNG: Thời điểm bắt đầu hiệp',
    completed_at TIMESTAMP NULL COMMENT 'ĐANG DÙNG: Thời điểm kết thúc hiệp (cả hai lật bài)',
    
    -- Trường HOÃN (dành cho mở rộng sau)
    player1_moved_at TIMESTAMP NULL COMMENT 'HOÃN: Thời điểm người chơi 1 chọn bài (phân tích)',
    player2_moved_at TIMESTAMP NULL COMMENT 'HOÃN: Thời điểm người chơi 2 chọn bài',
    round_duration_ms INT COMMENT 'HOÃN: Thời lượng hiệp (ms)',
    
    -- Ràng buộc khóa ngoại
    FOREIGN KEY (match_id) REFERENCES games(match_id) ON DELETE CASCADE,
    FOREIGN KEY (round_winner_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (player1_card_id) REFERENCES cards(card_id) ON DELETE SET NULL,
    FOREIGN KEY (player2_card_id) REFERENCES cards(card_id) ON DELETE SET NULL,
    
    -- Ràng buộc duy nhất (ngăn trùng hiệp)
    UNIQUE KEY uk_game_round (match_id, round_number) COMMENT 'Mỗi ván có đúng 3 hiệp duy nhất',
    
    -- Chỉ mục hiệu năng
    INDEX idx_game_rounds (match_id, round_number) COMMENT 'ĐANG DÙNG: Lấy danh sách hiệp theo ván',
    INDEX idx_round_winner (round_winner_id) COMMENT 'ĐANG DÙNG: Truy vấn người thắng theo hiệp',
    INDEX idx_completed_at (completed_at) COMMENT 'ĐANG DÙNG: Sắp theo thời điểm hoàn tất'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Theo dõi từng hiệp với xử lý quá thời gian';

-- ============================================================================
-- BẢNG: active_sessions
-- Mục đích: Theo dõi người dùng online và kết nối ván đang hoạt động
-- Phạm vi: Quản lý phiên, heartbeat, trạng thái online
-- THAY ĐỔI:
--   - user_id VARCHAR(50) → INT
--   - game_id VARCHAR(50) → match_id VARCHAR(36)
-- ============================================================================
CREATE TABLE active_sessions (
    -- Định danh chính (ĐANG DÙNG - MVP)
    session_id VARCHAR(100) PRIMARY KEY COMMENT 'ĐANG DÙNG: Định danh phiên duy nhất',
    user_id INT NOT NULL COMMENT 'ĐANG DÙNG: ID người dùng của phiên (FK users.user_id)',
    
    -- Trạng thái phiên (ĐANG DÙNG - MVP)
    match_id VARCHAR(36) COMMENT 'ĐANG DÙNG: ID ván hiện tại (NULL nếu đang ở sảnh)',
    status ENUM('CONNECTED', 'IN_LOBBY', 'IN_GAME', 'DISCONNECTED') DEFAULT 'CONNECTED' 
        COMMENT 'ĐANG DÙNG: Trạng thái phiên phục vụ cập nhật UI',
    
    -- Giám sát kết nối (ĐANG DÙNG - MVP)
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'ĐANG DÙNG: Nhịp heartbeat gần nhất từ client (trạng thái online)',
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'ĐANG DÙNG: Hoạt động gần nhất của người dùng (bất kỳ hành động)',
    
    -- Trường HOÃN (dành cho mở rộng sau)
    connection_id VARCHAR(100) COMMENT 'HOÃN: Định danh kết nối WebSocket/TCP',
    server_instance VARCHAR(50) COMMENT 'HOÃN: Máy chủ xử lý phiên này',
    connection_data JSON COMMENT 'HOÃN: Siêu dữ liệu kết nối bổ sung',
    
    -- Ràng buộc khóa ngoại
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (match_id) REFERENCES games(match_id) ON DELETE SET NULL,
    
    -- Chỉ mục hiệu năng
    INDEX idx_user_session (user_id) COMMENT 'ĐANG DÙNG: Tìm phiên theo người dùng',
    INDEX idx_game_session (match_id) COMMENT 'ĐANG DÙNG: Tìm các phiên trong một ván',
    INDEX idx_status (status) COMMENT 'ĐANG DÙNG: Lọc theo trạng thái phiên',
    INDEX idx_last_heartbeat (last_heartbeat) COMMENT 'ĐANG DÙNG: Phát hiện phiên lỗi thời (heartbeat > 30s)',
    INDEX idx_server_instance (server_instance) COMMENT 'HOÃN: Truy vấn cân bằng tải'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Quản lý phiên cho trạng thái online và theo dõi kết nối';

-- ============================================================================
-- DỮ LIỆU KHỞI TẠO: 36 Lá (A-9 của 4 chất)
-- ============================================================================
INSERT INTO cards (card_id, suit, `rank`, card_value, display_name) VALUES
-- Hearts (♥) - IDs 1-9
(1, 'HEARTS', 'A', 1, 'A♥'),
(2, 'HEARTS', '2', 2, '2♥'),
(3, 'HEARTS', '3', 3, '3♥'),
(4, 'HEARTS', '4', 4, '4♥'),
(5, 'HEARTS', '5', 5, '5♥'),
(6, 'HEARTS', '6', 6, '6♥'),
(7, 'HEARTS', '7', 7, '7♥'),
(8, 'HEARTS', '8', 8, '8♥'),
(9, 'HEARTS', '9', 9, '9♥'),

-- Diamonds (♦) - IDs 10-18
(10, 'DIAMONDS', 'A', 1, 'A♦'),
(11, 'DIAMONDS', '2', 2, '2♦'),
(12, 'DIAMONDS', '3', 3, '3♦'),
(13, 'DIAMONDS', '4', 4, '4♦'),
(14, 'DIAMONDS', '5', 5, '5♦'),
(15, 'DIAMONDS', '6', 6, '6♦'),
(16, 'DIAMONDS', '7', 7, '7♦'),
(17, 'DIAMONDS', '8', 8, '8♦'),
(18, 'DIAMONDS', '9', 9, '9♦'),

-- Clubs (♣) - IDs 19-27
(19, 'CLUBS', 'A', 1, 'A♣'),
(20, 'CLUBS', '2', 2, '2♣'),
(21, 'CLUBS', '3', 3, '3♣'),
(22, 'CLUBS', '4', 4, '4♣'),
(23, 'CLUBS', '5', 5, '5♣'),
(24, 'CLUBS', '6', 6, '6♣'),
(25, 'CLUBS', '7', 7, '7♣'),
(26, 'CLUBS', '8', 8, '8♣'),
(27, 'CLUBS', '9', 9, '9♣'),

-- Spades (♠) - IDs 28-36
(28, 'SPADES', 'A', 1, 'A♠'),
(29, 'SPADES', '2', 2, '2♠'),
(30, 'SPADES', '3', 3, '3♠'),
(31, 'SPADES', '4', 4, '4♠'),
(32, 'SPADES', '5', 5, '5♠'),
(33, 'SPADES', '6', 6, '6♠'),
(34, 'SPADES', '7', 7, '7♠'),
(35, 'SPADES', '8', 8, '8♠'),
(36, 'SPADES', '9', 9, '9♠');

-- ============================================================================
-- TRIGGER: Auto-create user_profile khi tạo user mới
-- Mục đích: Đảm bảo mỗi user có profile ngay khi đăng ký
-- ============================================================================
DELIMITER //

CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO user_profiles (user_id, display_name, games_played, games_won, games_lost)
    VALUES (NEW.user_id, NEW.username, 0, 0, 0);
END//

DELIMITER ;

-- ============================================================================
-- STORED PROCEDURE: Update user stats sau khi kết thúc ván
-- Mục đích: Tự động cập nhật games_played, games_won, games_lost
-- ============================================================================
DELIMITER //

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
    
    -- Cập nhật games_played cho cả 2 người chơi
    UPDATE user_profiles
    SET games_played = games_played + 1
    WHERE user_id IN (v_player1_id, v_player2_id);
    
    -- Cập nhật games_won cho người thắng
    IF v_winner_id IS NOT NULL THEN
        UPDATE user_profiles
        SET games_won = games_won + 1
        WHERE user_id = v_winner_id;
        
        -- Cập nhật games_lost cho người thua
        UPDATE user_profiles
        SET games_lost = games_lost + 1
        WHERE user_id IN (v_player1_id, v_player2_id) AND user_id != v_winner_id;
    END IF;
END//

DELIMITER ;

-- ============================================================================
-- RÀNG BUỘC & QUY TẮC NGHIỆP VỤ
-- ============================================================================
-- 1. Mỗi user tự động có profile (trigger after_user_insert)
-- 2. Stats tự động cập nhật (stored procedure update_user_stats_after_game)
-- 3. Ranh giới giao dịch:
--    - Tạo ván (INSERT games + UPDATE active_sessions) - Tầng ứng dụng
--    - Kết thúc hiệp (INSERT game_rounds + UPDATE games.player*_score) - Tầng ứng dụng
--    - Kết thúc ván (UPDATE games.status/winner + CALL update_user_stats_after_game) - Tầng ứng dụng

-- ============================================================================
-- KIỂM TRA DỮ LIỆU SEED
-- ============================================================================
SELECT 
    suit, 
    COUNT(*) as card_count,
    MIN(`rank`) as min_rank,
    MAX(`rank`) as max_rank,
    MIN(card_id) as min_id,
    MAX(card_id) as max_id
FROM cards 
GROUP BY suit 
ORDER BY suit;

-- Kết quả mong đợi:
-- +-----------+------------+----------+----------+--------+--------+
-- | suit      | card_count | min_rank | max_rank | min_id | max_id |
-- +-----------+------------+----------+----------+--------+--------+
-- | HEARTS    |          9 | 2        | A        |      1 |      9 |
-- | DIAMONDS  |          9 | 2        | A        |     10 |     18 |
-- | CLUBS     |          9 | 2        | A        |     19 |     27 |
-- | SPADES    |          9 | 2        | A        |     28 |     36 |
-- +-----------+------------+----------+----------+--------+--------+

-- ============================================================================
-- GHI CHÚ MIGRATION
-- ============================================================================
-- Phiên bản: V1.1 (Pure Java Compatible)
-- Áp dụng: Tạo lược đồ tương thích với Pure Java MVP
-- Thay đổi từ V1.0:
--   ✅ User ID: VARCHAR(50) UUID → INT AUTO_INCREMENT (đơn giản hóa Java code)
--   ✅ Game ID: VARCHAR(50) UUID → VARCHAR(36) (chuẩn UUID format)
--   ✅ Card ID: AUTO_INCREMENT → Fixed 1-36 (static reference)
--   ✅ GIỮ NGUYÊN: Tất cả tính năng MVP (auto-pick, email, display_name, game_mode)
--   ✅ GIỮ NGUYÊN: Cột HOÃN (dễ mở rộng, không ảnh hưởng performance)
--   ✅ THÊM: Trigger auto-create profile
--   ✅ THÊM: Stored procedure update stats
-- 
-- Phiên bản kế: V2 sẽ kích hoạt dần các trường HOÃN
--   - Băm mật khẩu (BCrypt)
--   - Xác minh email
--   - Hệ số xếp hạng ELO
--   - Nhật ký kiểm toán
-- ============================================================================
