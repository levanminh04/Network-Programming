package com.n9.core;

import com.n9.core.database.DatabaseManager;
import com.n9.core.network.CoreServerListener;
import com.n9.core.service.AuthService; // Thêm import
import com.n9.core.service.GameService;
import com.n9.core.service.SessionManager; // Thêm import
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public final class CoreServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Core Server...");
        System.out.println("Initializing database connection pool...");

        DatabaseManager dbManager = DatabaseManager.getInstance();

        if (!dbManager.isHealthy()) {
            System.exit(1);
        }

        System.out.println("✅ Database connected successfully");
        dbManager.printPoolStats();

        int port = 9090;
        var serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);

        var executor = Executors.newCachedThreadPool();

        // --- KHỞI TẠO CÁC SERVICE ---
        var gameService = new GameService();
        var authService = new AuthService(dbManager); // <-- KHỞI TẠO
        var sessionManager = new SessionManager(dbManager); // <-- KHỞI TẠO

        // --- TRUYỀN TẤT CẢ SERVICE VÀO LISTENER ---
        var listener = new CoreServerListener(
                serverSocket,
                executor,
                gameService,
                authService,    // <-- TRUYỀN
                sessionManager  // <-- TRUYỀN
        );
        listener.start();

        // --- SHUTDOWN HOOK (Giữ nguyên) ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Core Server ===");
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
            dbManager.shutdown();
            System.out.println("=== Server shutdown complete ===");
        }));

        System.out.println("✅ Core server started on port: " + port);
        System.out.println("   Server is ready to accept connections!");
    }
}