/**
 * CoreServer:
 * - Entry point để khởi động CoreServerListener.
 * - Đóng vai trò "trọng tài" điều khiển toàn bộ logic game qua TCP.
 * - CoreServerListener sẽ lắng nghe kết nối từ client
 *   và tạo ClientConnectionHandler cho mỗi kết nối mới.
 * - File này chỉ dùng để chạy server.
 */
public class CoreServer {
    public static void main(String[] args) {
        int port = 5000; // cổng TCP mặc định
        CoreServerListener server = new CoreServerListener(port);
        // Chạy server
        server.start();
    }
}
