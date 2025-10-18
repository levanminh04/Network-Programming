package com.n9.core.network;

import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.SessionManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * CoreServerListener - Luồng chuyên dụng để lắng nghe và chấp nhận kết nối client.
 * Đóng vai trò là "Người Lễ tân", giao mỗi kết nối mới cho một ClientConnectionHandler.
 *
 * @version 1.1.0 (Refactored to inject all services)
 */
public class CoreServerListener implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final GameService gameService;
    // THAY ĐỔI: Thêm các service cần thiết
    private final AuthService authService;
    private final SessionManager sessionManager;

    private volatile boolean running = true;

    // THAY ĐỔI: Constructor được cập nhật để nhận tất cả các service
    public CoreServerListener(
            ServerSocket serverSocket,
            ExecutorService pool,
            GameService gameService,
            AuthService authService,
            SessionManager sessionManager
    ) {
        this.serverSocket = serverSocket;
        this.pool = pool;
        this.gameService = gameService;
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    public void start() {
        // start() được gọi ở đây, ngay lập tức nó sẽ gọi this.run(), this ở đây chính là CoreServerListener
        new Thread(this, "accept-loop").start();
    }

    @Override
    public void run() {
        // Luồng "accept-loop" được tạo ra với một mục đích duy nhất: chạy vòng lặp while vô tận để liên tục chờ đợi và chấp nhận các kết nối mới.
        // vòng while này sẽ chạy vô tận trong thread "accept-loop"
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(30000); // 30s timeout

                // THAY ĐỔI: Khởi tạo ClientConnectionHandler với đầy đủ các service
                ClientConnectionHandler handler = new ClientConnectionHandler(
                        clientSocket,
                        gameService,
                        authService,
                        sessionManager,
                        pool
                );

                pool.submit(handler); // chỉ được gọi đúng một lần

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
        } catch (IOException ignored) {
            // Ignored because we are shutting down
        }
    }
}
// ServerSocket serverSocket = new ServerSocket(9090);
// thì nó chỉ ngồi “nghe” — nghĩa là nó đợi client nào đó kết nối đến port 9090.
// Nhưng bản thân ServerSocket không tự biết làm gì sau khi client kết nối.
// Nếu không có một “người đứng giữa” để:
// liên tục chờ kết nối mới, và
// chuyển từng kết nối cho luồng riêng để xử lý,
// thì server sẽ chỉ nhận được 1 client, sau đó bị kẹt mãi (blocking).

// Server đang mở cổng và lắng nghe; accept() sẽ chờ cho đến khi có một client “gọi điện” (kết nối TCP tới cổng đó). Khi có cuộc gọi đến và bắt tay TCP xong, accept() trả về một Socket mới đại diện cho đường dây riêng giữa server ↔ client đó, để bạn dùng đọc/ghi dữ liệu. ServerSocket thì vẫn tiếp tục lắng nghe cho các kết nối sau.

// Tại sao phải có CoreServerListener chạy vòng lặp while (running)?
//
// Nếu chỉ gọi .accept() một lần duy nhất, thì:
//
// Server sẽ chỉ chấp nhận 1 client đầu tiên,
//
// Sau đó không bao giờ nhận thêm client khác,
//
// Vì chương trình đang bận xử lý client đó và không quay lại .accept() nữa.