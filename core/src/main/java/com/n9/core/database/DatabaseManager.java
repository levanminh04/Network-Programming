package com.n9.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;



public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    
    // Private constructor để implement Singleton
    private DatabaseManager() {
        initializePool();
    }
    
    /**
     * Lấy instance duy nhất của DatabaseManager (Singleton)
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Khởi tạo HikariCP Connection Pool
     */
    private void initializePool() {
        try {
            HikariConfig config = new HikariConfig();
            
            // ============================================
            // DATABASE CONNECTION SETTINGS
            // ============================================
            config.setJdbcUrl("jdbc:mysql://localhost:3306/cardgame_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword("sieudenden");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // ============================================
            // POOL SIZE CONFIGURATION (MVP - 10 connections)
            // ============================================
            config.setMaximumPoolSize(10);        // Max 10 connections đồng thời
            config.setMinimumIdle(2);             // Luôn có 2 connections sẵn sàng
            
            // ============================================
            // TIMEOUT SETTINGS
            // ============================================
            config.setConnectionTimeout(30000);   // 30s timeout khi lấy connection
            config.setIdleTimeout(600000);        // 10 phút idle → đóng connection
            config.setMaxLifetime(1800000);       // 30 phút max lifetime mỗi connection
            
            // ============================================
            // PERFORMANCE & RELIABILITY
            // ============================================
            config.setAutoCommit(true);           // Auto-commit mỗi query (MVP đơn giản)
            config.setConnectionTestQuery("SELECT 1"); // Health check query
            
            // ============================================
            // LEAK DETECTION (Development)
            // ============================================
            config.setLeakDetectionThreshold(60000); // Cảnh báo nếu connection không trả về sau 60s
            
            // ============================================
            // POOL NAME (Debugging)
            // ============================================
            config.setPoolName("CardGame-MySQL-Pool");
            
            // Tạo DataSource
            dataSource = new HikariDataSource(config);
            
            System.out.println("✅ HikariCP Connection Pool initialized successfully!");
            System.out.println("   Pool Name: " + config.getPoolName());
            System.out.println("   JDBC URL: " + config.getJdbcUrl());
            System.out.println("   Max Pool Size: " + config.getMaximumPoolSize());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize HikariCP pool!");
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Lấy connection từ pool
     * 
     * QUAN TRỌNG: Phải close() connection sau khi dùng xong!
     * Dùng try-with-resources để tự động close:
     * 
     * <pre>
     * try (Connection conn = dbManager.getConnection()) {
     *     // Your database code here
     * } // Connection tự động trả về pool
     * </pre>
     * 
     * @return Connection từ pool
     * @throws SQLException nếu không lấy được connection
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not available");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Kiểm tra health của database connection
     * 
     * @return true nếu connection OK, false nếu lỗi
     */
    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5); // Timeout 5 giây
        } catch (SQLException e) {
            System.err.println("❌ Database health check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown pool (gọi khi server tắt)
     * 
     * Đóng tất cả connections trong pool.
     * Nên gọi trong shutdown hook của com.n9.core.CoreServer.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("❌ HikariCP Connection Pool closed");
        }
    }
    
    /**
     * Lấy thông tin pool (để debug/monitoring)
     */
    public void printPoolStats() {
        if (dataSource != null) {
            System.out.println("=== HikariCP Pool Statistics ===");
            System.out.println("Active Connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
            System.out.println("Idle Connections: " + dataSource.getHikariPoolMXBean().getIdleConnections());
            System.out.println("Total Connections: " + dataSource.getHikariPoolMXBean().getTotalConnections());
            System.out.println("Threads Awaiting Connection: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
    }
}
