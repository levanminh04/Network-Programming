package com.n9.core;

import com.n9.core.database.DatabaseManager;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.core.network.CoreServerListener;
import com.n9.core.service.AuthService; // Thêm import
import com.n9.core.service.GameService;
import com.n9.core.service.SessionManager; // Thêm import
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class CoreServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Core Server...");
        System.out.println("Initializing database connection pool...");
// Thêm một ScheduledExecutorService để xử lý các tác vụ hẹn giờ (như timeout)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // 1 luồng đủ cho việc hẹn giờ

// Thêm một Map để quản lý các ClientConnectionHandler đang hoạt động
// Key: userId, Value: ClientConnectionHandler instance
// QUAN TRỌNG: Map này cần được cập nhật khi user đăng nhập/mất kết nối
        ConcurrentHashMap<String, ClientConnectionHandler> activeConnections = new ConcurrentHashMap<>();
        DatabaseManager dbManager = DatabaseManager.getInstance();

        if (!dbManager.isHealthy()) {
            System.exit(1);
        }

        System.out.println(" Database connected successfully");
        dbManager.printPoolStats();

        int port = 9090;
        var serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);

        var executor = Executors.newCachedThreadPool();

        // --- KHỞI TẠO CÁC SERVICE ---
        var gameService = new GameService(activeConnections, scheduler);
        var authService = new AuthService(dbManager);
        var sessionManager = new SessionManager(dbManager);

        // --- TRUYỀN TẤT CẢ SERVICE VÀO LISTENER ---
        var listener = new CoreServerListener(
                serverSocket,
                executor,
                gameService,
                authService,
                sessionManager,
                activeConnections
        );
        listener.start();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Core Server ===");
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
            dbManager.shutdown();
            System.out.println("=== Server shutdown complete ===");
        }));

        System.out.println(" Core server started on port: " + port);
        System.out.println("   Server is ready to accept connections!");
    }
}