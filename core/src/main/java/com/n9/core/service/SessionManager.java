package com.n9.core.service;

import com.n9.core.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection; // Thêm import
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - Quản lý các phiên làm việc (session) đang hoạt động của người dùng.
 * @version 1.1.0 (Refactored for MVP)
 */
public class SessionManager {

    /**
     * Lớp nội (inner class) chứa thông tin của một phiên làm việc.
     */
    public static class SessionContext {
        private final String sessionId;
        private final String userId;
        private final String username;
        private String currentMatchId;
        private long lastActivityTimestamp;

        public SessionContext(String sessionId, String userId, String username) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.lastActivityTimestamp = System.currentTimeMillis();
        }
        public void updateActivity() { this.lastActivityTimestamp = System.currentTimeMillis(); }
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getCurrentMatchId() { return currentMatchId; }
        public void setCurrentMatchId(String currentMatchId) { this.currentMatchId = currentMatchId; }
    }

    private final ConcurrentHashMap<String, SessionContext> activeSessions;
    private final DatabaseManager dbManager;

    public SessionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    /**
     * Tạo một session mới.
     */
    public String createSession(String userId, String username) {
        String sessionId = UUID.randomUUID().toString();
        SessionContext context = new SessionContext(sessionId, userId, username);
        activeSessions.put(sessionId, context);
        try {
            persistSessionToDB(sessionId, userId);
        } catch (SQLException e) {
            System.err.println("⚠️ WARNING: Failed to persist session to DB: " + e.getMessage());
        }
        System.out.println("✅ Session created: " + sessionId + " for user: " + username);
        return sessionId;
    }

    /**
     * Kiểm tra một sessionId và lấy context.
     */
    public SessionContext getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        SessionContext context = activeSessions.get(sessionId);
        if (context != null) {
            context.updateActivity();
        }
        return context;
    }

    /**
     * Xóa một session.
     */
    public void removeSession(String sessionId) {
        if (sessionId == null) return;
        SessionContext removedContext = activeSessions.remove(sessionId);
        if (removedContext != null) {
            try {
                deleteSessionFromDB(sessionId);
            } catch (SQLException e) {
                System.err.println("⚠️ WARNING: Failed to delete session from DB: " + e.getMessage());
            }
            System.out.println("🧹 Session removed for user: " + removedContext.getUsername());
        }
    }

    /**
     * Gán matchId cho một session.
     */
    public void setMatchId(String sessionId, String matchId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context != null) {
            context.setCurrentMatchId(matchId);
            // TODO (Nâng cao): Cập nhật trạng thái 'IN_GAME' trong bảng active_sessions
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    // --- THÊM HÀM NÀY VÀO ---
    /**
     * Lấy danh sách tất cả các SessionContext đang hoạt động.
     * Chỉ nên được sử dụng bởi các service nội bộ (như MatchmakingService).
     * @return Một Collection chứa các SessionContext.
     */
    public Collection<SessionContext> getAllSessions() {
        return activeSessions.values();
    }
    // -------------------------

    private void persistSessionToDB(String sessionId, String userId) throws SQLException {
        String sql = """
            INSERT INTO active_sessions (session_id, user_id, status, last_heartbeat, created_at)
            VALUES (?, ?, 'IN_LOBBY', NOW(), NOW())
            ON DUPLICATE KEY UPDATE last_heartbeat = NOW(), status = 'IN_LOBBY'
            """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setInt(2, Integer.parseInt(userId));
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
