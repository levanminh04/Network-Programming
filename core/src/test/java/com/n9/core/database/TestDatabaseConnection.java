package com.n9.core.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test class để verify database connection
 * 
 * Run this để kiểm tra:
 * 1. HikariCP pool hoạt động
 * 2. MySQL connection OK
 * 3. Database cardgame_db tồn tại
 * 
 * @author MVP Team
 */
public class TestDatabaseConnection {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Database Connection ===\n");
        
        try {
            // 1. Khởi tạo DatabaseManager
            System.out.println("1. Initializing DatabaseManager...");
            DatabaseManager dbManager = DatabaseManager.getInstance();
            System.out.println("   ✅ DatabaseManager initialized\n");
            
            // 2. Health check
            System.out.println("2. Running health check...");
            boolean healthy = dbManager.isHealthy();
            if (healthy) {
                System.out.println("   ✅ Database is healthy\n");
            } else {
                System.err.println("   ❌ Database health check failed!\n");
                System.exit(1);
            }
            
            // 3. Test query - Đếm số users
            System.out.println("3. Testing query: SELECT COUNT(*) FROM users");
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM users")) {
                
                if (rs.next()) {
                    int userCount = rs.getInt("total");
                    System.out.println("   ✅ Query successful!");
                    System.out.println("   Total users in database: " + userCount + "\n");
                }
            }
            
            // 4. Test query - Đếm số cards
            System.out.println("4. Testing query: SELECT COUNT(*) FROM cards");
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM cards")) {
                
                if (rs.next()) {
                    int cardCount = rs.getInt("total");
                    System.out.println("   ✅ Query successful!");
                    System.out.println("   Total cards in database: " + cardCount + "\n");
                    
                    if (cardCount != 36) {
                        System.err.println("   ⚠️  WARNING: Expected 36 cards, found " + cardCount);
                        System.err.println("   Run DB_SCRIPT.sql to populate cards table!");
                    }
                }
            }
            
            // 5. Pool statistics
            System.out.println("5. Pool Statistics:");
            dbManager.printPoolStats();
            System.out.println();
            
            // 6. Test multiple connections
            System.out.println("6. Testing multiple concurrent connections...");
            Connection conn1 = dbManager.getConnection();
            Connection conn2 = dbManager.getConnection();
            Connection conn3 = dbManager.getConnection();
            System.out.println("   ✅ Got 3 connections from pool");
            
            dbManager.printPoolStats();
            
            conn1.close();
            conn2.close();
            conn3.close();
            System.out.println("   ✅ Returned 3 connections to pool\n");
            
            // 7. Shutdown
            System.out.println("7. Shutting down pool...");
            dbManager.shutdown();
            System.out.println("   ✅ Pool shutdown successful\n");
            
            System.out.println("=== ALL TESTS PASSED ✅ ===");
            
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
