package com.n9.core.network;

import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.MatchmakingService;
import com.n9.core.service.SessionManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * CoreServerListener - Luồng chuyên dụng để lắng nghe và chấp nhận kết nối.
 * Đóng vai trò là "Người Lễ tân".
 * @version 1.2.0 (Refactored to inject all services)
 */
public class CoreServerListener implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final GameService gameService;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private volatile boolean running = true;

    public CoreServerListener(
            ServerSocket serverSocket,
            ExecutorService pool,
            GameService gameService,
            AuthService authService,
            SessionManager sessionManager,
            ConcurrentHashMap<String, ClientConnectionHandler> activeConnections,
            MatchmakingService matchmakingService
    ) {
        this.serverSocket = serverSocket;
        this.pool = pool;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.activeConnections = activeConnections;
        this.matchmakingService = matchmakingService;
    }

    public void start() {
        new Thread(this, "accept-loop").start();
    }

    @Override
    public void run() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(30000); // 30s read timeout

                // Khởi tạo Handler với đầy đủ dependencies
                ClientConnectionHandler handler = new ClientConnectionHandler(
                        clientSocket,
                        gameService,
                        authService,
                        sessionManager,
                        matchmakingService, // Truyền matchmaking
                        pool,
                        activeConnections
                );

                // Giao cho pool chạy luồng I/O của Handler
                pool.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("❌ Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }
}
