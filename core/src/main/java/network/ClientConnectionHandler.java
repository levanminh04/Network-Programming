package network;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
/**
 * ClientConnectionHandler:
 * - Xử lý riêng cho 1 client (1 socket).
 * - Đọc message từ client, parse message.
 * - Gọi logic game (giả lập ở đây).
 * - Gửi phản hồi lại cho client hoặc broadcast (chưa implement).
 */
public class ClientConnectionHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    public ClientConnectionHandler(Socket socket) {
        this.clientSocket = socket;
    }
    @Override
    public void run() {
        try {
            // Chuẩn bị luồng vào/ra
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);  
            out.println("You are connected to Core Server.");
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Receive from client: " + message);
                // Ở đây giả lập bằng cách echo lại kết quả
                String response = handleMessage(message);
                // Gửi phản hồi về cho client
                out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client connection closed: " + clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Xử lý message từ clien.
     * Sau này có thể gọi RoomManager/GameRoom để xử lý thực sự.
     */
    private String handleMessage(String msg) {
        if (msg.equalsIgnoreCase("ping")) {
            return "pong";
        } else if (msg.startsWith("login")) {
            // đăng nhập
            return "Log in successfully";
        } else {
            // Echo message
            return "Server processing: " + msg;
        }
    }
}
