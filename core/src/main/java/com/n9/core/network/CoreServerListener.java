package com.n9.core.network;

import com.n9.core.service.GameService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class CoreServerListener implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final GameService gameService;
    private volatile boolean running = true;

    public CoreServerListener(ServerSocket ss, ExecutorService p, GameService gs) {
        this.serverSocket = ss; this.pool = p; this.gameService = gs;
    }

    public void start() {
        new Thread(this, "accept-loop").start();   // start() được gọi ở đây, ngay lập tức nó sẽ gọi this.run(), this ở  đây chính là CoreServerListener
    }

    @Override public void run() {
        // Luồng "accept-loop" được tạo ra với một mục đích duy nhất: chạy vòng lặp while vô tận để liên tục chờ đợi và chấp nhận các kết nối mới.
        while (running && !serverSocket.isClosed()) {    // vòng while này sẽ chạy vô tận trong thread "accept-loop"
            try {
                Socket s = serverSocket.accept();
                s.setSoTimeout(30000); // 30s
                pool.submit(new ClientConnectionHandler(s, gameService));
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        }
        catch (IOException ignored) {

        }
    }
}

//  ServerSocket serverSocket = new ServerSocket(9090);
//  thì nó chỉ ngồi “nghe” — nghĩa là nó đợi client nào đó kết nối đến port 9090.
//  Nhưng bản thân ServerSocket không tự biết làm gì sau khi client kết nối.
//  Nếu không có một “người đứng giữa” để:
//  liên tục chờ kết nối mới, và
//  chuyển từng kết nối cho luồng riêng để xử lý,
//  thì server sẽ chỉ nhận được 1 client, sau đó bị kẹt mãi (blocking).

//  Server đang mở cổng và lắng nghe; accept() sẽ chờ cho đến khi có một client “gọi điện” (kết nối TCP tới cổng đó). Khi có cuộc gọi đến và bắt tay TCP xong, accept() trả về một Socket mới đại diện cho đường dây riêng giữa server ↔ client đó, để bạn dùng đọc/ghi dữ liệu. ServerSocket thì vẫn tiếp tục lắng nghe cho các kết nối sau.

//  Tại sao phải có CoreServerListener chạy vòng lặp while (running)?
//
//  Nếu chỉ gọi .accept() một lần duy nhất, thì:
//
//  Server sẽ chỉ chấp nhận 1 client đầu tiên,
//
//  Sau đó không bao giờ nhận thêm client khác,
//
//  Vì chương trình đang bận xử lý client đó và không quay lại .accept() nữa.




