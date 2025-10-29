package com.n9.core.service;

import com.n9.core.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - Quản lý các phiên làm việc (session) đang hoạt động của người dùng.
 *
 * Vai trò chính:
 * - Tạo session sau khi người dùng đăng nhập/đăng ký thành công.
 * - Xác thực sessionId trong mỗi request cần được bảo vệ.
 * - Cung cấp "bối cảnh người dùng" (user context) từ sessionId.
 * - Xóa session khi người dùng đăng xuất hoặc mất kết nối.
 *
 * Chiến lược: Sử dụng ConcurrentHashMap trong bộ nhớ để tra cứu nhanh,
 * và đồng bộ xuống database để theo dõi và quản lý.
 *
 * @version 1.1.0 (Refactored for MVP)
 */
public class SessionManager {

    /**
     * Lớp nội (inner class) chứa thông tin của một phiên làm việc.
     * Đây là "bộ nhớ" về một người dùng đang đăng nhập.
     */
    public static class SessionContext {
        private final String sessionId;
        private final String userId;
        private final String username;
        private String currentMatchId; // Sẽ được cập nhật khi người dùng vào trận
        private long lastActivityTimestamp;

        public SessionContext(String sessionId, String userId, String username) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.lastActivityTimestamp = System.currentTimeMillis();
        }

        public void updateActivity() {
            this.lastActivityTimestamp = System.currentTimeMillis();
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getCurrentMatchId() { return currentMatchId; }

        // Setter
        public void setCurrentMatchId(String currentMatchId) { this.currentMatchId = currentMatchId; }
    }

    // "Danh bạ" trong bộ nhớ, tra cứu cực nhanh: sessionId -> SessionContext
    private final ConcurrentHashMap<String, SessionContext> activeSessions;
    private final DatabaseManager dbManager;

    public SessionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    /**
     *  Tạo một session mới sau khi người dùng đăng nhập/đăng ký thành công.
     *
     * @param userId ID của người dùng (dưới dạng String)
     * @param username Tên đăng nhập của người dùng
     * @return Chuỗi sessionId duy nhất được tạo ra.
     */
    public String createSession(String userId, String username) {
        // 1. Tạo sessionId ngẫu nhiên, an toàn
        String sessionId = UUID.randomUUID().toString();

        // 2. Tạo đối tượng context để lưu vào bộ nhớ
        SessionContext context = new SessionContext(sessionId, userId, username);
        activeSessions.put(sessionId, context);

        // 3. (Tùy chọn) Lưu session xuống DB để theo dõi
        // Bỏ qua lỗi nếu DB có vấn đề, vì session trong bộ nhớ vẫn hoạt động
        try {
            persistSessionToDB(sessionId, userId);
        } catch (SQLException e) {
            System.err.println("⚠ WARNING: Failed to persist session to DB: " + e.getMessage());
        }

        System.out.println(" Session created: " + sessionId + " for user: " + username);
        return sessionId;
    }

    /**
     *  Kiểm tra một sessionId có hợp lệ không và lấy thông tin người dùng.
     * Đây là hàm được gọi ở đầu mỗi request cần xác thực.
     *
     * @param sessionId Session ID do client gửi lên.
     * @return SessionContext nếu hợp lệ, hoặc null nếu không tìm thấy/không hợp lệ.
     */
    public SessionContext getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        SessionContext context = activeSessions.get(sessionId);

        // Nếu session tồn tại, cập nhật thời gian hoạt động cuối cùng
        if (context != null) {
            context.updateActivity();
        }

        return context;
    }

    /**
     * 🧹 Xóa một session khi người dùng đăng xuất hoặc mất kết nối.
     *
     * @param sessionId Session ID cần xóa.
     */
    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }

        // Xóa khỏi bộ nhớ
        SessionContext removedContext = activeSessions.remove(sessionId);

        // Nếu xóa thành công, thì xóa cả trong DB
        if (removedContext != null) {
            try {
                deleteSessionFromDB(sessionId);
            } catch (SQLException e) {
                System.err.println("⚠WARNING: Failed to delete session from DB: " + e.getMessage());
            }
            System.out.println(" Session removed for user: " + removedContext.getUsername());
        }
    }

    /**
     *  Cập nhật trạng thái của người dùng khi họ tham gia một trận đấu.
     *
     * @param sessionId Session của người dùng.
     * @param matchId ID của trận đấu họ vừa tham gia.
     */
    public void setMatchId(String sessionId, String matchId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context != null) {
            context.setCurrentMatchId(matchId);
            // (Tùy chọn nâng cao) có thể cập nhật trạng thái này xuống DB
        }
    }

    /**
     * Lấy số lượng session đang hoạt động. Hữu ích cho việc monitoring.
     * @return Số lượng người dùng đang online.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }


    // ============================================================================
    // CÁC HÀM TƯƠNG TÁC VỚI DATABASE (PRIVATE)
    // ============================================================================

    private void persistSessionToDB(String sessionId, String userId) throws SQLException {
        // ĐIỀU CHỈNH: Sử dụng đúng câu lệnh SQL khớp với script database v1.1.0
        // "INSERT ... ON DUPLICATE KEY UPDATE" sẽ tạo mới nếu chưa có,
        // hoặc cập nhật thời gian nếu session đã tồn tại (trường hợp hiếm).
        String sql = """
            INSERT INTO active_sessions (session_id, user_id, status, last_heartbeat, created_at)
            VALUES (?, ?, 'IN_LOBBY', NOW(), NOW())
            ON DUPLICATE KEY UPDATE last_heartbeat = NOW(), status = 'IN_LOBBY'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            stmt.setInt(2, Integer.parseInt(userId)); // Script DB đã đổi user_id thành INT
            stmt.executeUpdate();
        }
    }

    private void deleteSessionFromDB(String sessionId) throws SQLException {
        String sql = "DELETE FROM active_sessions WHERE session_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);
            stmt.executeUpdate();
        }
    }
}