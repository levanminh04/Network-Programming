package network;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/*
 * CoreServerListener chịu trách nhiệm:
 * - Mở cổng TCP (ví dụ 5000) và lắng nghe kết nối từ client.
 * - Mỗi khi có kết nối mới -> tạo ra một ClientConnectionHandler.
 * - Mỗi ClientConnectionHandler chạy trên một luồng riêng (thread).
 */
public class CoreServerListener {
    private int port;
    private boolean running;
    private ServerSocket serverSocket; // giữ tham chiếu để có thể stop an toàn
    public CoreServerListener(int port) {
        this.port = port;
    }
    /*
     * Hàm start() khởi động server TCP.
     */
    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("CoreServerListener running at the gate: " + port);
            while (running) {
                // Chờ client mới kết nối vào
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                // Giao cho ClientConnectionHandler xử lý
                ClientConnectionHandler handler = new ClientConnectionHandler(clientSocket);
                // Tạo thread riêng cho mỗi client
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (IOException e) {
            if (running) { // chỉ in lỗi khi không phải stop chủ động
                e.printStackTrace();
            }
        }
    }
    /*
     * Hàm stop() để dừng server.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // giải phóng accept() đang block
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("CoreServerListener stopped.");
    }
}
