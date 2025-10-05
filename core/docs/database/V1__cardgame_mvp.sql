
-- Create database
CREATE DATABASE IF NOT EXISTS cardgame_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE cardgame_db;
-- ============================================================================
-- Trò chơi thẻ "Rút Bài May Mắn" - Lược đồ CSDL cho MVP
-- Phiên bản: 1.0.0 (MVP - Sản phẩm khả dụng tối thiểu)
-- Cơ sở dữ liệu: MySQL 8.0+
-- Bộ mã ký tự: utf8mb4_unicode_ci
-- Bộ máy lưu trữ: InnoDB
-- ============================================================================
-- Phạm vi: Hỗ trợ các tính năng lõi (A-D):
--   (A) Đăng ký & Đăng nhập người dùng
--   (B) Ghép trận Nhanh (Quick Match)
--   (C) Chơi 3 hiệp với thời gian chờ 10s mỗi hiệp
--   (D) Kết thúc ván với theo dõi Thắng/Thua
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
-- ============================================================================
CREATE TABLE users (
    -- Định danh chính (ĐANG DÙNG - MVP)
    id VARCHAR(50) PRIMARY KEY COMMENT 'Định danh người dùng duy nhất (định dạng UUID)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT 'Tên đăng nhập duy nhất (3-20 ký tự)',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT 'Email người dùng để khôi phục tài khoản',
    
    -- Xác thực (ĐANG DÙNG - MVP)
    password VARCHAR(255) NOT NULL COMMENT 'ĐANG DÙNG: Mật khẩu dạng plain cho MVP. HOÃN: Sẽ băm bằng BCrypt/Argon2 trong môi trường thật',
    
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
-- ============================================================================
CREATE TABLE user_profiles (
    -- Khóa chính (ĐANG DÙNG - MVP)
    user_id VARCHAR(50) PRIMARY KEY COMMENT 'Khóa ngoại tới bảng users',
    
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
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
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
-- ============================================================================
CREATE TABLE cards (
    -- Định danh chính (ĐANG DÙNG - MVP)
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ĐANG DÙNG: Định danh lá bài duy nhất (1-36)',
    
    -- Thuộc tính lá bài (ĐANG DÙNG - MVP)
    suit ENUM('HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES') NOT NULL COMMENT 'ĐANG DÙNG: Chất của lá bài',
    `rank` VARCHAR(3) NOT NULL COMMENT 'ĐANG DÙNG: Hạng của lá (A, 2, 3, 4, 5, 6, 7, 8, 9)',
    card_value INT NOT NULL COMMENT 'ĐANG DÙNG: Giá trị số để so sánh (A=1, 2=2, ..., 9=9)',
    
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
-- ============================================================================
CREATE TABLE games (
    -- Định danh chính (ĐANG DÙNG - MVP)
    id VARCHAR(50) PRIMARY KEY COMMENT 'ĐANG DÙNG: Định danh ván duy nhất (định dạng UUID)',
    
    -- Người chơi (ĐANG DÙNG - MVP)
    player1_id VARCHAR(50) NOT NULL COMMENT 'ĐANG DÙNG: ID người chơi 1',
    player2_id VARCHAR(50) NOT NULL COMMENT 'ĐANG DÙNG: ID người chơi 2',
    
    -- Cấu hình ván (ĐANG DÙNG - MVP)
    game_mode ENUM('QUICK', 'RANKED', 'CUSTOM', 'TOURNAMENT') NOT NULL DEFAULT 'QUICK' 
        COMMENT 'ĐANG DÙNG: QUICK cho ghép trận MVP. RANKED/CUSTOM/TOURNAMENT là tính năng HOÃN',
    total_rounds INT DEFAULT 3 COMMENT 'ĐANG DÙNG: Cố định 3 hiệp cho MVP',
    
    -- Trạng thái ván (ĐANG DÙNG - MVP)
    status ENUM('WAITING_TO_START', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED', 'CANCELLED') 
        DEFAULT 'WAITING_TO_START' 
        COMMENT 'ĐANG DÙNG: WAITING_TO_START (đang ghép), IN_PROGRESS (đang chơi), COMPLETED (kết thúc), ABANDONED (thoát giữa chừng)',
    
    -- Kết quả (ĐANG DÙNG - MVP)
    winner_id VARCHAR(50) NULL COMMENT 'ĐANG DÙNG: ID người thắng (NULL nếu bỏ dở/hủy)',
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
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL,
    
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
-- ============================================================================
CREATE TABLE game_rounds (
    -- Định danh chính (ĐANG DÙNG - MVP)
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ĐANG DÙNG: ID hiệp tự tăng',
    game_id VARCHAR(50) NOT NULL COMMENT 'ĐANG DÙNG: Khóa ngoại tới bảng games',
    round_number INT NOT NULL COMMENT 'ĐANG DÙNG: Số thứ tự hiệp (1-3)',
    
    -- Nước đi người chơi 1 (ĐANG DÙNG - MVP)
    player1_card_id INT COMMENT 'ĐANG DÙNG: ID lá bài người chơi 1 chọn (FK cards.id)',
    player1_card_value INT COMMENT 'ĐANG DÙNG: Giá trị lá bài để so sánh nhanh',
    player1_is_auto_picked BOOLEAN DEFAULT FALSE COMMENT 'ĐANG DÙNG: TRUE nếu lá được auto-pick do hết thời gian',
    
    -- Nước đi người chơi 2 (ĐANG DÙNG - MVP)
    player2_card_id INT COMMENT 'ĐANG DÙNG: ID lá bài người chơi 2 chọn (FK cards.id)',
    player2_card_value INT COMMENT 'ĐANG DÙNG: Giá trị lá bài để so sánh nhanh',
    player2_is_auto_picked BOOLEAN DEFAULT FALSE COMMENT 'ĐANG DÙNG: TRUE nếu lá được auto-pick do hết thời gian',
    
    -- Kết quả hiệp (ĐANG DÙNG - MVP)
    round_winner_id VARCHAR(50) COMMENT 'ĐANG DÙNG: Người thắng hiệp (NULL nếu hòa)',
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
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (round_winner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (player1_card_id) REFERENCES cards(id) ON DELETE SET NULL,
    FOREIGN KEY (player2_card_id) REFERENCES cards(id) ON DELETE SET NULL,
    
    -- Ràng buộc duy nhất (ngăn trùng hiệp)
    UNIQUE KEY uk_game_round (game_id, round_number) COMMENT 'Mỗi ván có đúng 3 hiệp duy nhất',
    
    -- Chỉ mục hiệu năng
    INDEX idx_game_rounds (game_id, round_number) COMMENT 'ĐANG DÙNG: Lấy danh sách hiệp theo ván',
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
-- ============================================================================
CREATE TABLE active_sessions (
    -- Định danh chính (ĐANG DÙNG - MVP)
    session_id VARCHAR(100) PRIMARY KEY COMMENT 'ĐANG DÙNG: Định danh phiên duy nhất',
    user_id VARCHAR(50) NOT NULL COMMENT 'ĐANG DÙNG: ID người dùng của phiên',
    
    -- Trạng thái phiên (ĐANG DÙNG - MVP)
    game_id VARCHAR(50) COMMENT 'ĐANG DÙNG: ID ván hiện tại (NULL nếu đang ở sảnh)',
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
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE SET NULL,
    
    -- Chỉ mục hiệu năng
    INDEX idx_user_session (user_id) COMMENT 'ĐANG DÙNG: Tìm phiên theo người dùng',
    INDEX idx_game_session (game_id) COMMENT 'ĐANG DÙNG: Tìm các phiên trong một ván',
    INDEX idx_status (status) COMMENT 'ĐANG DÙNG: Lọc theo trạng thái phiên',
    INDEX idx_last_heartbeat (last_heartbeat) COMMENT 'ĐANG DÙNG: Phát hiện phiên lỗi thời (heartbeat > 30s)',
    INDEX idx_server_instance (server_instance) COMMENT 'HOÃN: Truy vấn cân bằng tải'
) ENGINE=InnoDB 
  CHARACTER SET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='ĐANG DÙNG (MVP): Quản lý phiên cho trạng thái online và theo dõi kết nối';

-- ============================================================================
-- CÁC BẢNG HOÃN (Chỉ cấu trúc - Không dùng trong MVP)
-- Mục đích: Giữ lược đồ cho tính năng tương lai mà không phá migration
-- ============================================================================

-- HOÃN: Leaderboards (dùng user_profiles.games_won cho MVP)
-- CREATE TABLE leaderboards (...) COMMENT='HOÃN: Leaderboard nâng cao theo chu kỳ (ngày/tuần/tháng)';

-- HOÃN: Audit Logs (bảo mật và tuân thủ)
-- CREATE TABLE audit_logs (...) COMMENT='HOÃN: Nhật ký kiểm toán bảo mật cho môi trường thật';

-- HOÃN: User Statistics (phân tích chi tiết)
-- CREATE TABLE user_statistics (...) COMMENT='HOÃN: Theo dõi thống kê theo ngày/tuần/tháng';

-- HOÃN: System Configuration (cờ tính năng)
-- CREATE TABLE system_config (...) COMMENT='HOÃN: Quản trị cấu hình runtime';

-- ============================================================================
-- DỮ LIỆU KHỞI TẠO: 36 Lá (A-9 của 4 chất)
-- ============================================================================
INSERT INTO cards (suit, `rank`, card_value) VALUES
-- Hearts (♥)
('HEARTS', 'A', 1),
('HEARTS', '2', 2),
('HEARTS', '3', 3),
('HEARTS', '4', 4),
('HEARTS', '5', 5),
('HEARTS', '6', 6),
('HEARTS', '7', 7),
('HEARTS', '8', 8),
('HEARTS', '9', 9),

-- Diamonds (♦)
('DIAMONDS', 'A', 1),
('DIAMONDS', '2', 2),
('DIAMONDS', '3', 3),
('DIAMONDS', '4', 4),
('DIAMONDS', '5', 5),
('DIAMONDS', '6', 6),
('DIAMONDS', '7', 7),
('DIAMONDS', '8', 8),
('DIAMONDS', '9', 9),

-- Clubs (♣)
('CLUBS', 'A', 1),
('CLUBS', '2', 2),
('CLUBS', '3', 3),
('CLUBS', '4', 4),
('CLUBS', '5', 5),
('CLUBS', '6', 6),
('CLUBS', '7', 7),
('CLUBS', '8', 8),
('CLUBS', '9', 9),

-- Spades (♠)
('SPADES', 'A', 1),
('SPADES', '2', 2),
('SPADES', '3', 3),
('SPADES', '4', 4),
('SPADES', '5', 5),
('SPADES', '6', 6),
('SPADES', '7', 7),
('SPADES', '8', 8),
('SPADES', '9', 9);

-- ============================================================================
-- RÀNG BUỘC & QUY TẮC NGHIỆP VỤ
-- ============================================================================
-- Trigger: Ngăn việc chọn trùng lá trong cùng một hiệp
-- HOÃN: Thực hiện ở tầng ứng dụng cho MVP (dễ debug hơn)

-- Trigger: Tự động cập nhật thống kê user_profiles khi kết thúc ván
-- HOÃN: Thực hiện ở tầng ứng dụng cho MVP

-- Ranh giới giao dịch:
-- 1. Tạo ván (INSERT games + UPDATE active_sessions) - Tầng ứng dụng
-- 2. Kết thúc hiệp (INSERT game_rounds + UPDATE games.player*_score) - Tầng ứng dụng
-- 3. Kết thúc ván (UPDATE games.status/winner + UPDATE user_profiles) - Tầng ứng dụng

-- ============================================================================
-- TỐI ƯU HIỆU NĂNG
-- ============================================================================
-- InnoDB buffer pool (chạy riêng với quyền DBA)
-- SET GLOBAL innodb_buffer_pool_size = 134217728; -- 128MB cho MVP (điều chỉnh theo máy chủ)

-- Query cache (chạy riêng với quyền DBA)
-- SET GLOBAL query_cache_size = 33554432; -- 32MB cho MVP

-- ============================================================================
-- GHI CHÚ MIGRATION
-- ============================================================================
-- Phiên bản: V1 (MVP)
-- Áp dụng: Tạo lược đồ ban đầu
-- Phiên bản kế: V2 sẽ kích hoạt dần các trường HOÃN
--   - Thêm băm mật khẩu (BCrypt)
--   - Bật xác minh email
--   - Thêm hệ số xếp hạng ELO
--   - Bật nhật ký kiểm toán
-- ============================================================================

-- Kiểm tra dữ liệu seed
SELECT 
    suit, 
    COUNT(*) as card_count,
    MIN(`rank`) as min_rank,
    MAX(`rank`) as max_rank
FROM cards 
GROUP BY suit 
ORDER BY suit;

-- Kết quả mong đợi: 4 chất, mỗi chất 9 lá (A-9)
