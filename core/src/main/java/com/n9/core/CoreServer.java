package com.n9.core;

import com.n9.core.database.DatabaseManager;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.core.network.CoreServerListener;
import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.MatchmakingService;
import com.n9.core.service.SessionManager;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class CoreServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Core Server...");
        System.out.println("Initializing database connection pool...");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ConcurrentHashMap<String, ClientConnectionHandler> activeConnections = new ConcurrentHashMap<>();
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
        var sessionManager = new SessionManager(dbManager);

        // THAY ĐỔI: Truyền dbManager VÀ sessionManager vào GameService
        var gameService = new GameService(dbManager, activeConnections, scheduler, sessionManager);

        var authService = new AuthService(dbManager);
        var matchmakingService = new MatchmakingService(gameService, sessionManager, activeConnections, scheduler);

        // --- TRUYỀN TẤT CẢ SERVICE VÀO LISTENER ---
        var listener = new CoreServerListener(
                serverSocket,
                executor,
                gameService,
                authService,
                sessionManager,
                activeConnections,
                matchmakingService
        );
        listener.start();
        matchmakingService.startMatchmakingLoop();

        // --- SHUTDOWN HOOK (Giữ nguyên) ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Core Server ===");
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
            scheduler.shutdownNow(); // Tắt cả scheduler
            dbManager.shutdown();
            System.out.println("=== Server shutdown complete ===");
        }));

        System.out.println("✅ Core server started on port: " + port);
        System.out.println("   Server is ready to accept connections!");
    }
}

