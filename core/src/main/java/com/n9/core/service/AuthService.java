package com.n9.core.service;

import com.n9.core.database.DatabaseManager;
import com.n9.shared.model.dto.auth.LoginSuccessDto;
import com.n9.shared.model.dto.auth.RegisterResponseDto;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * AuthService - Xác thực người dùng và quản lý tài khoản với MySQL.
 *
 * @version 1.1.0 (Using BCrypt for hashing)
 */
public class AuthService {

    private final DatabaseManager dbManager;

    public AuthService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Đăng ký người dùng mới.
     * @throws IllegalArgumentException Nếu username/email đã tồn tại hoặc input không hợp lệ.
     * @throws SQLException           Nếu có lỗi database.
     */
    public RegisterResponseDto register(String username, String email, String password, String displayName)
            throws SQLException, IllegalArgumentException {

        if (username == null || username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters.");
        }
        if (email == null || !email.contains("@") || email.length() > 100) {
            throw new IllegalArgumentException("Invalid email format or length.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        // 2. Kiểm tra username/email tồn tại
        checkUserExists(username, email); // Sẽ ném Exception nếu tồn tại

        // 3. Hash mật khẩu
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        String finalDisplayName = (displayName != null && !displayName.trim().isEmpty()) ? displayName.trim() : username;

        String sql = "INSERT INTO users (username, email, password_hash, created_at) VALUES (?, ?, ?, NOW())";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, email.toLowerCase());
            stmt.setString(3, passwordHash);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int userIdInt = generatedKeys.getInt(1);
                    String userId = String.valueOf(userIdInt);

                    // 7. Cập nhật display_name vào user_profiles (do trigger tạo)
                    updateDisplayName(conn, userIdInt, finalDisplayName);

                    RegisterResponseDto response = new RegisterResponseDto();
                    response.setUserId(userId);
                    response.setUsername(username);
                    response.setEmail(email.toLowerCase());
                    response.setDisplayName(finalDisplayName);
                    response.setTimestamp(System.currentTimeMillis());

                    System.out.println("✅ User registered: " + username + " (ID: " + userId + ")");
                    return response;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Race condition during registration: " + e.getMessage());
            if (e.getMessage().contains("username")) throw new IllegalArgumentException("Username already exists.");
            if (e.getMessage().contains("email")) throw new IllegalArgumentException("Email already registered.");
            throw new IllegalArgumentException("User already exists.");
        }
    }

    /**
     * Đăng nhập người dùng.
     * @throws IllegalArgumentException Nếu sai thông tin đăng nhập, tài khoản không hoạt động.
     * @throws SQLException           Nếu có lỗi database.
     */
    public LoginSuccessDto login(String username, String password) throws SQLException, IllegalArgumentException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }

        String sql = """
            SELECT u.user_id, u.username, u.email, u.password_hash, u.status, up.display_name
            FROM users u
            JOIN user_profiles up ON u.user_id = up.user_id
            WHERE u.username = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    int userIdInt = rs.getInt("user_id");
                    String userId = String.valueOf(userIdInt);
                    String email = rs.getString("email");
                    String displayName = rs.getString("display_name");
                    String status = rs.getString("status");

                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        throw new IllegalArgumentException("Account is currently " + status.toLowerCase() + ".");
                    }

                    if (BCrypt.checkpw(password, storedHash)) {
                        updateLastLoginAsync(userIdInt);

                        LoginSuccessDto response = new LoginSuccessDto();
                        response.setUserId(userId);
                        response.setUsername(username);
                        response.setEmail(email);
                        response.setDisplayName(displayName);
                        response.setTimestamp(System.currentTimeMillis());

                        System.out.println("✅ User logged in: " + username + " (ID: " + userId + ")");
                        return response;
                    } else {
                        throw new IllegalArgumentException("Invalid username or password.");
                    }
                } else {
                    throw new IllegalArgumentException("Invalid username or password.");
                }
            }
        }
    }

    private void checkUserExists(String username, String email) throws SQLException, IllegalArgumentException {
        String sql = "SELECT username, email FROM users WHERE username = ? OR email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if(rs.getString("username").equalsIgnoreCase(username)) {
                        throw new IllegalArgumentException("Username already exists.");
                    }
                    if(rs.getString("email").equalsIgnoreCase(email.toLowerCase())) {
                        throw new IllegalArgumentException("Email already registered.");
                    }
                }
            }
        }
    }

    private void updateDisplayName(Connection conn, int userId, String displayName) throws SQLException {
        String sql = "UPDATE user_profiles SET display_name = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, displayName);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    private void updateLastLoginAsync(int userId) {
        new Thread(() -> {
            String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("⚠️ Failed to update last_login for user " + userId + ": " + e.getMessage());
            }
        }).start();
    }
}
