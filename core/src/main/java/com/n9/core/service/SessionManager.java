package com.n9.core.service;

import com.n9.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - Quản lý các phiên làm việc (session).
 */
public class SessionManager {

    public static class SessionContext {
        private final String sessionId;
        private final String userId;
        private final String username;
        private String currentMatchId;
        private long lastActivityTimestamp;

        public SessionContext(String sid, String uid, String uname) {
            this.sessionId = sid;
            this.userId = uid;
            this.username = uname;
            this.lastActivityTimestamp = System.currentTimeMillis();
        }

        public void updateActivity() {
            this.lastActivityTimestamp = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getCurrentMatchId() {
            return currentMatchId;
        }

        public void setCurrentMatchId(String mid) {
            this.currentMatchId = mid;
        }
    }

    private final ConcurrentHashMap<String, SessionContext> activeSessions;
    private final ConcurrentHashMap<String, SessionContext> userSessionMap; // Map tra cứu ngược: userId -> SessionContext
    private final DatabaseManager dbManager;

    public SessionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.activeSessions = new ConcurrentHashMap<>();
        this.userSessionMap = new ConcurrentHashMap<>(); // Khởi tạo
    }

    public String createSession(String userId, String username) {
        removeSessionByUserId(userId); // Đảm bảo single-session

        String sessionId = UUID.randomUUID().toString();
        SessionContext context = new SessionContext(sessionId, userId, username);
        activeSessions.put(sessionId, context);
        userSessionMap.put(userId, context); // Thêm vào map tra cứu ngược

        try {
            persistSessionToDB(sessionId, userId);
        } catch (SQLException e) {
            System.err.println("⚠️ WARNING: Failed to persist session to DB: " + e.getMessage());
        }
        System.out.println("✅ Session created: " + sessionId + " for user: " + username);
        return sessionId;
    }

    public SessionContext getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return null;
        SessionContext context = activeSessions.get(sessionId);
        if (context != null) context.updateActivity();
        return context;
    }

    // --- THÊM HÀM MỚI ---

    /**
     * Lấy SessionContext bằng userId.
     */
    public SessionContext getSessionByUserId(String userId) {
        if (userId == null) return null;
        return userSessionMap.get(userId);
    }
    // --------------------

    public void removeSession(String sessionId) {
        if (sessionId == null) return;
        SessionContext removedContext = activeSessions.remove(sessionId);
        if (removedContext != null) {
            userSessionMap.remove(removedContext.getUserId());
            try {
                deleteSessionFromDB(sessionId);
            } catch (SQLException e) {
                System.err.println("⚠️ WARNING: Failed to delete session from DB: " + e.getMessage());
            }
            System.out.println("🧹 Session removed for user: " + removedContext.getUsername());
        }
    }

    private void removeSessionByUserId(String userId) {
        if (userId == null) return;
        SessionContext oldContext = userSessionMap.remove(userId);
        if (oldContext != null) {
            activeSessions.remove(oldContext.getSessionId());
            try {
                deleteSessionFromDB(oldContext.getSessionId());
            } catch (SQLException e) {
                System.err.println("⚠️ WARNING: Failed to delete OLD session from DB: " + e.getMessage());
            }
            System.out.println("🧹 Removed old session for user: " + userId);
        }
    }

    public void setMatchId(String sessionId, String matchId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context != null) {
            context.setCurrentMatchId(matchId);
            // TODO: Cập nhật status='IN_GAME' trong DB
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public Collection<SessionContext> getAllSessions() {
        return activeSessions.values();
    }


    private void persistSessionToDB(String sessionId, String userId) throws SQLException {
        // THAY ĐỔI: Xóa cột 'created_at' khỏi câu lệnh INSERT
        String sql = """
                INSERT INTO active_sessions (session_id, user_id, status, last_heartbeat)
                VALUES (?, ?, 'IN_LOBBY', NOW())
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

