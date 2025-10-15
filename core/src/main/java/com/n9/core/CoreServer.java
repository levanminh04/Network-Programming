package com.n9.core;

import com.n9.core.database.DatabaseManager;
import com.n9.core.service.GameService;
import com.n9.core.network.CoreServerListener;

import java.net.ServerSocket;
import java.util.concurrent.Executors;

public final class CoreServer {
    public static void main(String[] args) throws Exception {
        // ============================================
        // 1. KHỞI TẠO DATABASE CONNECTION POOL
        // ============================================
        System.out.println("Starting Core Server...");
        System.out.println("Initializing database connection pool...");
        
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Health check - Fail fast nếu DB không connect được
        if (!dbManager.isHealthy()) {
            System.err.println("❌ Database connection failed!");
            System.err.println("   Please check:");
            System.err.println("   1. MySQL is running on localhost:3306");
            System.err.println("   2. Database 'cardgame_db' exists");
            System.err.println("   3. Username/password is correct");
            System.exit(1);
        }
        
        System.out.println("✅ Database connected successfully");
        dbManager.printPoolStats(); // In thông tin pool
        
        // ============================================
        // 2. KHỞI TẠO TCP SERVER
        // ============================================
        int port = 9090;
        var serverSocket = new ServerSocket(port);  //   NGHĨA LÀ CLIENT MUỐN TRUY CẬP VÀO GAME CẦN KẾT NỐI ĐẾN CỔNG NÀy
        serverSocket.setReuseAddress(true);

        var executor = Executors.newCachedThreadPool();
        var gameService = new GameService();

        var listener = new CoreServerListener(serverSocket, executor, gameService);
        listener.start(); // "luồng riêng" của CoreServerListener được tạo ra chỉ để chạy vòng lặp nhận kết nối (accept()) liên tục.
        // listener.start(): tạo một thread mới và chạy run() bên trong thread đó
        
        // ============================================
        // 3. SHUTDOWN HOOK - Cleanup khi tắt server
        // ============================================
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Core Server ===");
            
            // Đóng ServerSocket (stop accepting new connections)
            try { 
                serverSocket.close(); 
                System.out.println("✅ Server socket closed");
            } catch (Exception ignored) {}
            
            // Shutdown thread pool
            executor.shutdownNow();
            System.out.println("✅ Thread pool shutdown");
            
            // Đóng database connection pool
            dbManager.shutdown();
            System.out.println("✅ Database pool closed");
            
            System.out.println("=== Server shutdown complete ===");
        }));
        
        System.out.println("✅ Core server started on port: " + port);
        System.out.println("   Server is ready to accept connections!");
    }
}
