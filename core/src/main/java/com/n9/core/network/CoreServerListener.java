package com.n9.core.network;

import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.MatchmakingService; // Thêm import
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
    private final MatchmakingService matchmakingService; // Thêm
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private volatile boolean running = true;

    // Constructor đã được cập nhật
    public CoreServerListener(
            ServerSocket serverSocket,
            ExecutorService pool,
            GameService gameService,
            AuthService authService,
            SessionManager sessionManager,
            ConcurrentHashMap<String, ClientConnectionHandler> activeConnections,
            MatchmakingService matchmakingService // Thêm
    ) {
        this.serverSocket = serverSocket;
        this.pool = pool;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.activeConnections = activeConnections;
        this.matchmakingService = matchmakingService; // Thêm
    }

    public void start() {
        new Thread(this, "accept-loop").start();
    }

    @Override
    public void run() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();

                // THAY ĐỔI: ĐÃ XÓA DÒNG clientSocket.setSoTimeout(30000);
                // Chúng ta sẽ dựa vào Heartbeat để quản lý kết nối.

                ClientConnectionHandler handler = new ClientConnectionHandler(
                        clientSocket,
                        gameService,
                        authService,
                        sessionManager,
                        matchmakingService,
                        pool,
                        activeConnections
                );

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
// Các comment gốc của bạn về ServerSocket và vòng lặp while vẫn hoàn toàn chính xác.

