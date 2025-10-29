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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class CoreServer {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Core Server...");
        System.out.println("Initializing database connection pool...");

        // Khởi tạo các thành phần cơ sở
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.isHealthy()) {
            System.err.println("❌ Database connection failed! Exiting.");
            System.exit(1);
        }
        System.out.println("✅ Database connected successfully");
        dbManager.printPoolStats();

        ExecutorService executor = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // 1 luồng cho hẹn giờ là đủ
        ConcurrentHashMap<String, ClientConnectionHandler> activeConnections = new ConcurrentHashMap<>();

        int port = 9090;
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);

        // --- KHỞI TẠO CÁC SERVICE ---
        // Các service này là Singleton và được chia sẻ
        var sessionManager = new SessionManager(dbManager);
        var authService = new AuthService(dbManager);
        var gameService = new GameService(dbManager, activeConnections, scheduler);
        var matchmakingService = new MatchmakingService(gameService, sessionManager, activeConnections, scheduler);

        // --- TRUYỀN TẤT CẢ SERVICE VÀO LISTENER ---
        var listener = new CoreServerListener(
                serverSocket,
                executor,
                gameService,
                authService,
                sessionManager,
                activeConnections,
                matchmakingService // Truyền MatchmakingService vào
        );
        listener.start();

        // Bắt đầu vòng lặp matchmaking
        matchmakingService.startMatchmakingLoop();

        // --- SHUTDOWN HOOK ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Core Server ===");
            try { serverSocket.close(); } catch (Exception ignored) {}
            scheduler.shutdownNow(); // Tắt cả scheduler
            executor.shutdownNow();
            dbManager.shutdown();
            System.out.println("=== Server shutdown complete ===");
        }));

        System.out.println("✅ Core server started on port: " + port);
        System.out.println("   Server is ready to accept connections!");
    }
}
