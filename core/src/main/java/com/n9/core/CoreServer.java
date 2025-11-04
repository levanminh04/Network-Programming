package com.n9.core;

import com.n9.core.database.DatabaseManager;
import com.n9.core.network.ClientConnectionHandler;
import com.n9.core.network.CoreServerListener;
import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.LeaderboardService;
import com.n9.core.service.MatchmakingService;
import com.n9.core.service.SessionManager;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class CoreServer {
    public static void main(String[] args) throws Exception {

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ConcurrentHashMap<String, ClientConnectionHandler> activeConnections = new ConcurrentHashMap<>();
        DatabaseManager dbManager = DatabaseManager.getInstance();

        if (!dbManager.isHealthy()) {
            System.exit(1);
        }
        dbManager.printPoolStats();

        int port = 9090;
        var serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        var executor = Executors.newCachedThreadPool();

        var sessionManager = new SessionManager(dbManager);

        var gameService = new GameService(dbManager, activeConnections, scheduler, sessionManager);

        var authService = new AuthService(dbManager);
        var leaderboardService = new LeaderboardService(dbManager, sessionManager);
        var matchmakingService = new MatchmakingService(gameService, sessionManager, activeConnections, scheduler);

        var listener = new CoreServerListener(
                serverSocket,
                executor,
                gameService,
                authService,
                sessionManager,
                activeConnections,
                matchmakingService,
                leaderboardService
        );
        listener.start();
        matchmakingService.startMatchmakingLoop();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Core Server ===");
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
            scheduler.shutdownNow(); // Tắt cả scheduler
            dbManager.shutdown();
            System.out.println("=== Server shutdown complete ===");
        }));

        System.out.println("=== Core server started on port: " + port + " ===");
        System.out.println("   Server is ready to accept connections!");
    }
}

