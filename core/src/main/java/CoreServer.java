


import com.n9.core.service.GameService;
import network.CoreServerListener;

import java.net.ServerSocket;
import java.util.concurrent.Executors;

public final class CoreServer {
    public static void main(String[] args) throws Exception {
        int port = 9090;
        var serverSocket = new ServerSocket(port);  //   NGHĨA LÀ CLIENT MUỐN TRUY CẬP VÀO GAME CẦN KẾT NỐI ĐẾN CỔNG NÀy
        serverSocket.setReuseAddress(true);

        var executor = Executors.newCachedThreadPool();
        var gameService = new GameService();

        var listener = new CoreServerListener(serverSocket, executor, gameService);
        listener.start(); // “luồng riêng” của CoreServerListener được tạo ra chỉ để chạy vòng lặp nhận kết nối (accept()) liên tục.
        // listener.start(): tạo một thread mới và chạy run() bên trong thread đó
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
        }));
        System.out.println("Core server started on :" + port);
    }
}
