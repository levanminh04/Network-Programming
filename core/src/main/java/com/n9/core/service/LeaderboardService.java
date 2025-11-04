package com.n9.core.service;

import com.n9.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý logic Bảng Xếp Hạng (Leaderboard).
 * 
 * Chức năng:
 * - Lấy top 100 người chơi theo số trận thắng
 * - Hỗ trợ phân trang (pagination)
 * - Tính toán rank cho từng user
 * 
 * @author Solution Architect
 * @version 1.0.0
 * @since 2025-11-04
 */
public class LeaderboardService {

    private final DatabaseManager dbManager;
    private final SessionManager sessionManager;

    public LeaderboardService(DatabaseManager dbManager, SessionManager sessionManager) {
        this.dbManager = dbManager;
        this.sessionManager = sessionManager;
    }

    /**
     * Lấy danh sách top players cho leaderboard.
     * 
     * @param limit Số lượng record tối đa (mặc định 100)
     * @param offset Vị trí bắt đầu (cho pagination, mặc định 0)
     * @return List of Map, mỗi Map chứa thông tin 1 player
     * @throws SQLException Nếu có lỗi database
     */
    public List<Map<String, Object>> getTopPlayers(int limit, int offset) throws SQLException {
        // Validate input
        if (limit <= 0 || limit > 100) {
            limit = 100; // Giới hạn tối đa 100 records
        }
        if (offset < 0) {
            offset = 0;
        }

        List<Map<String, Object>> leaderboard = new ArrayList<>();

        // SQL Query tận dụng index idx_games_won
        String sql = 
            "SELECT " +
            "    u.user_id, " +
            "    u.username, " +
            "    p.games_played, " +
            "    p.games_won, " +
            "    p.games_lost, " +
            "    CASE " +
            "        WHEN p.games_played > 0 THEN ROUND((p.games_won * 100.0 / p.games_played), 2) " +
            "        ELSE 0.00 " +
            "    END AS win_rate, " +
            "    u.last_login " +
            "FROM users u " +
            "INNER JOIN user_profiles p ON u.user_id = p.user_id " +
            "WHERE p.games_played > 0 " + // Chỉ lấy user đã chơi ít nhất 1 trận
            "ORDER BY p.games_won DESC, p.games_played ASC, u.username ASC " + // Sắp xếp: Thắng nhiều → Chơi ít → Tên A-Z
            "LIMIT ? OFFSET ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                int rank = offset + 1; // Rank bắt đầu từ offset + 1

                while (rs.next()) {
                    Map<String, Object> player = new HashMap<>();
                    
                    // Basic Info
                    int userIdInt = rs.getInt("user_id");
                    String userIdStr = String.valueOf(userIdInt);
                    
                    player.put("rank", rank);
                    player.put("userId", userIdInt);
                    player.put("username", rs.getString("username"));
                    
                    // Stats
                    player.put("gamesPlayed", rs.getInt("games_played"));
                    player.put("gamesWon", rs.getInt("games_won"));
                    player.put("gamesLost", rs.getInt("games_lost"));
                    player.put("winRate", rs.getDouble("win_rate")); // Đã tính sẵn trong SQL
                    
                    // 🆕 CHECK ONLINE STATUS (Memory-based)
                    boolean isOnline = sessionManager.isUserOnline(userIdStr);
                    player.put("online", isOnline);
                    
                    // Last Login/Last Seen
                    java.sql.Timestamp lastLogin = rs.getTimestamp("last_login");
                    if (isOnline) {
                        // Nếu đang online, không cần lastSeenTimestamp
                        player.put("lastLogin", lastLogin != null ? lastLogin.toString() : null);
                        player.put("lastSeenTimestamp", null);
                    } else {
                        // Nếu offline, convert lastLogin thành timestamp cho frontend
                        player.put("lastLogin", lastLogin != null ? lastLogin.toString() : null);
                        player.put("lastSeenTimestamp", lastLogin != null ? lastLogin.getTime() : null);
                    }
                    
                    leaderboard.add(player);
                    rank++;
                }
            }
        }

        return leaderboard;
    }

    /**
     * Lấy thông tin rank của 1 user cụ thể.
     * 
     * @param userId ID của user cần tra cứu
     * @return Map chứa rank và stats của user, hoặc null nếu không tìm thấy
     * @throws SQLException Nếu có lỗi database
     */
    public Map<String, Object> getUserRank(int userId) throws SQLException {
        String sql = 
            "SELECT " +
            "    ranked.user_rank, " +
            "    ranked.user_id, " +
            "    ranked.username, " +
            "    ranked.games_played, " +
            "    ranked.games_won, " +
            "    ranked.games_lost, " +
            "    ranked.win_rate, " +
            "    ranked.last_login " +
            "FROM ( " +
            "    SELECT " +
            "        ROW_NUMBER() OVER (ORDER BY p.games_won DESC, p.games_played ASC, u.username ASC) AS user_rank, " +
            "        u.user_id, " +
            "        u.username, " +
            "        p.games_played, " +
            "        p.games_won, " +
            "        p.games_lost, " +
            "        CASE " +
            "            WHEN p.games_played > 0 THEN ROUND((p.games_won * 100.0 / p.games_played), 2) " +
            "            ELSE 0.00 " +
            "        END AS win_rate, " +
            "        u.last_login " +
            "    FROM users u " +
            "    INNER JOIN user_profiles p ON u.user_id = p.user_id " +
            "    WHERE p.games_played > 0 " +
            ") AS ranked " +
            "WHERE ranked.user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> userInfo = new HashMap<>();
                    
                    int userIdInt = rs.getInt("user_id");
                    String userIdStr = String.valueOf(userIdInt);
                    
                    userInfo.put("rank", rs.getInt("user_rank"));
                    userInfo.put("userId", userIdInt);
                    userInfo.put("username", rs.getString("username"));
                    userInfo.put("gamesPlayed", rs.getInt("games_played"));
                    userInfo.put("gamesWon", rs.getInt("games_won"));
                    userInfo.put("gamesLost", rs.getInt("games_lost"));
                    userInfo.put("winRate", rs.getDouble("win_rate"));
                    
                    // 🆕 CHECK ONLINE STATUS
                    boolean isOnline = sessionManager.isUserOnline(userIdStr);
                    userInfo.put("online", isOnline);
                    
                    // Last Login/Last Seen
                    java.sql.Timestamp lastLogin = rs.getTimestamp("last_login");
                    userInfo.put("lastLogin", lastLogin != null ? lastLogin.toString() : null);
                    if (isOnline) {
                        userInfo.put("lastSeenTimestamp", null);
                    } else {
                        userInfo.put("lastSeenTimestamp", lastLogin != null ? lastLogin.getTime() : null);
                    }
                    
                    return userInfo;
                }
            }
        }

        return null; // User không tồn tại hoặc chưa chơi trận nào
    }

    /**
     * Lấy tổng số users trong leaderboard (đã chơi ít nhất 1 trận).
     * Dùng cho pagination ở Frontend.
     * 
     * @return Tổng số users
     * @throws SQLException Nếu có lỗi database
     */
    public int getTotalPlayersCount() throws SQLException {
        String sql = 
            "SELECT COUNT(*) AS total " +
            "FROM user_profiles " +
            "WHERE games_played > 0";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        }

        return 0;
    }
}
