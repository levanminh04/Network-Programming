# KIẾN TRÚC TRIỂN KHAI HỆ THỐNG GAME RÚT BÀI MAY MẮN

## TỔNG QUAN KIẾN TRÚC

Hệ thống Game Rút Bài May Mắn được thiết kế theo mô hình kiến trúc phân tán 4 tầng (4-Tier Distributed Architecture) với sự phân tách rõ ràng giữa các lớp trách nhiệm. Kiến trúc này đảm bảo tính module hóa, khả năng mở rộng và dễ dàng bảo trì trong quá trình phát triển và vận hành hệ thống.

### Mục Tiêu Thiết Kế

Kiến trúc hệ thống hướng đến bốn mục tiêu chính. Thứ nhất là nguyên tắc tách biệt trách nhiệm (Separation of Concerns), trong đó mỗi tầng có nhiệm vụ riêng biệt và không phụ thuộc chặt chẽ vào nhau. Frontend tập trung vào trải nghiệm người dùng và giao diện, Gateway xử lý việc chuyển đổi giao thức và định tuyến message, Core chứa toàn bộ logic nghiệp vụ của game, và Database quản lý dữ liệu bền vững.

Thứ hai là khả năng mở rộng (Scalability), cho phép scale horizontal từng tầng một cách độc lập. Gateway có thể cân bằng tải cho nhiều Core servers, và Database có thể thực hiện replicate hoặc shard khi cần thiết để đáp ứng lưu lượng truy cập tăng cao.

Thứ ba là bảo mật (Security), trong đó Core server không được expose trực tiếp ra internet. Gateway đóng vai trò như một reverse proxy và firewall, tập trung xử lý Authentication và Authorization cho toàn bộ hệ thống.

Thứ tư là hiệu năng cao (High Performance), đạt được thông qua việc sử dụng WebSocket cho giao tiếp realtime, TCP socket với binary framing để đạt throughput cao, cùng với connection pooling và thread pooling để tối ưu hóa việc sử dụng tài nguyên hệ thống

## KIẾN TRÚC 4 TẦNG CHI TIẾT

### Tầng Giao Diện (Client Tier)

Tầng giao diện được xây dựng dưới dạng ứng dụng web Single Page Application (SPA) sử dụng React.js phiên bản 18.2. Ứng dụng chạy trên trình duyệt web và giao tiếp với server thông qua giao thức WebSocket tại địa chỉ ws://localhost:8080/ws trong môi trường development (port 5173 với Vite dev server).

Các component chính bao gồm AuthView chịu trách nhiệm xử lý đăng ký và đăng nhập, LobbyView quản lý giao diện tìm kiếm trận đấu và bảng xếp hạng, và GameView hiển thị giao diện chơi game với các lá bài. State management được thực hiện thông qua AppContext sử dụng useReducer hook của React để quản lý global state. Custom hook useWebSocket đảm nhiệm việc quản lý kết nối WebSocket với các tính năng tự động reconnect và exponential backoff khi mất kết nối.

Technology stack của tầng này bao gồm React 18.2 cho UI framework, WebSocket API cho realtime communication, Tailwind CSS cho styling, và Vite làm build tool. Giao thức WebSocket sử dụng format JSON với encoding UTF-8, đảm bảo khả năng tự động kết nối lại khi gặp sự cố.

### Tầng Trung Gian (Presentation Tier)

Gateway Server được xây dựng trên nền tảng Spring Boot 3.2, chạy trên port 8080 và đóng vai trò là cầu nối giữa client và core server. Component GatewayWebSocketHandler xử lý các kết nối WebSocket từ client thông qua ba phương thức chính: afterConnectionEstablished() để xử lý kết nối mới, handleTextMessage() để định tuyến message đến Core server, và afterConnectionClosed() để dọn dẹp tài nguyên khi client ngắt kết nối.

CoreTcpClient là component quan trọng thiết lập kết nối TCP đến Core server thông qua phương thức connect(), duy trì một background thread liên tục đọc dữ liệu từ Core qua startListening(), và triển khai cơ chế heartbeat với PING/PONG message mỗi 5 giây để phát hiện kết nối bị đứt. Message Translator đảm nhiệm việc chuyển đổi giao thức giữa WebSocket và TCP, ánh xạ correlationId cho các cặp Request/Response, và routing sessionId cho các notification từ server.

Kết nối giữa Gateway và Core server sử dụng TCP socket trên port 9090 với format Length-Prefixed JSON, trong đó 4 bytes đầu chứa độ dài message (integer) và N bytes tiếp theo chứa JSON payload. Cơ chế buffering với BufferedStream được sử dụng để tối ưu hiệu năng, kết hợp với heartbeat PING/PONG mỗi 5 giây để đảm bảo kết nối luôn ổn định.

### Tầng Xử Lý Logic (Business Logic Tier)

Core Server là nơi tập trung toàn bộ business logic của game, được viết bằng Java 17 và chạy trên port 9090. ClientConnectionHandler sử dụng CachedThreadPool để quản lý thread pool, với I/O Thread chuyên đọc/ghi socket và Worker Thread xử lý business logic.

AuthService cung cấp các chức năng register() để tạo tài khoản mới với BCrypt hashing, login() thực hiện xác thực và tạo sessionId duy nhất, và logout() để cleanup session khi người dùng đăng xuất. SessionManager sử dụng ConcurrentHashMap để lưu trữ SessionContext theo sessionId, theo dõi active sessions và online users, đồng thời tự động cleanup các session hết hạn.

GameService là trái tim của game logic, quản lý trạng thái game thông qua ConcurrentHashMap ánh xạ matchId với GameState. Lock Striping với ReentrantLock được triển khai để đảm bảo thread-safety khi nhiều người chơi đồng thời thao tác. Các phương thức chính bao gồm initializeGame() để tạo và shuffle bộ bài, playCard() xử lý việc người chơi chọn bài với lock mechanism, handleRoundTimeout() tự động chọn bài khi hết giờ, và finalizeGame() tính toán người thắng cuộc và lưu vào database.

MatchmakingService triển khai hàng đợi FIFO để ghép cặp người chơi, sử dụng Queue để lưu trữ userId và Set để track users đang trong queue. ScheduledExecutor thực thi tryMatchmaking() mỗi giây để tìm kiếm cặp phù hợp. ChallengeService quản lý các thách đấu 1v1 thông qua ConcurrentHashMap lưu trữ ChallengeSession, với các chức năng createChallenge() gửi lời mời, handleResponse() xử lý Accept/Reject, và timeout tự động hủy sau 15 giây.

LeaderboardService cung cấp getTopPlayers() lấy top 20 người chơi theo điểm, getUserRank() tính toán thứ hạng của người dùng cụ thể, và getOnlineStatus() join với bảng active_sessions để hiển thị trạng thái online.

Kết nối giữa Core server và Database sử dụng JDBC Connection Pool với HikariCP, cấu hình tối đa 10 connections, connection timeout 30 giây, idle timeout 600 giây, và max lifetime 1800 giây để đảm bảo hiệu năng tối ưu.

### Tầng Cơ Sở Dữ Liệu (Data Tier)

MySQL Database phiên bản 8.0 chạy trên port 3306 là tầng lưu trữ dữ liệu bền vững của hệ thống. Cơ sở dữ liệu bao gồm sáu bảng chính với thiết kế tối ưu cho hiệu năng.

Bảng users lưu trữ thông tin authentication với các cột user_id, username (UNIQUE), email (UNIQUE), và password đã được hash. Indexes bao gồm PRIMARY KEY trên user_id và UNIQUE constraints trên username và email để đảm bảo tính duy nhất. Bảng user_profiles chứa thông tin thống kê game với các cột user_id, score, games_played, games_won, và win_rate. Index phức hợp trên (score DESC, games_won) được tạo để tối ưu hóa query leaderboard.

Bảng active_sessions theo dõi người dùng online với session_id làm PRIMARY KEY, user_id, và last_activity_timestamp. Index trên user_id hỗ trợ tra cứu nhanh trạng thái online của người dùng cụ thể. Bảng games ghi lại lịch sử các trận đấu với match_id, player1_id, player2_id, winner_id, và status. Index phức hợp trên (player1_id, player2_id) giúp truy vấn nhanh match history của người chơi.

Bảng game_rounds lưu trữ chi tiết từng round trong game với round_id, match_id, round_number, player1_card, và các thông tin khác. FOREIGN KEY constraint trên match_id đảm bảo tính toàn vẹn dữ liệu. Bảng cards là reference data chứa 52 lá bài với card_id, rank, suit, và display_name.

Database sử dụng Storage Engine InnoDB để hỗ trợ ACID transactions và Foreign Keys. Character set utf8mb4 với collation utf8mb4_unicode_ci được áp dụng để hỗ trợ emoji và các ký tự đặc biệt

## LUỒNG DỮ LIỆU (DATA FLOW)

### Luồng Xử Lý Request (Client → Server → Database)

Luồng xử lý request trong hệ thống bắt đầu từ hành động của người dùng như click button hoặc chọn bài. Frontend React sẽ dispatch action và cập nhật local state trước khi gửi message JSON qua WebSocket Client. Message này có cấu trúc bao gồm type (ví dụ GAME.CARD_PLAY_REQUEST), correlationId để tracking request/response, sessionId để định danh người dùng, và payload chứa dữ liệu như cardId.

Khi Gateway nhận được message tại GatewayWebSocketHandler.handleTextMessage(), nó sẽ parse JSON, lưu trữ ánh xạ correlationId với webSocketSession vào pendingRequests, sau đó forward message đến Core server qua TCP. Core server với ClientConnectionHandler.run() sử dụng I/O Thread để đọc length-prefixed message, sau đó submit task xử lý vào Worker Pool thông qua pool.submit().

Tại GameService.playCard(), hệ thống sẽ acquire lock cho matchId cụ thể, validate card, cập nhật GameState, kiểm tra xem cả hai người chơi đã chơi bài chưa, rồi release lock. Sau đó Core thực hiện database queries như INSERT vào bảng game_rounds và UPDATE điểm số trong user_profiles.

Core gửi response trở lại Gateway với type CARD_PLAY_SUCCESS và cùng correlationId ban đầu. Gateway's Listener Thread nhận response, lookup webSocketSession từ pendingRequests dựa trên correlationId, và forward về client qua WebSocket. Cuối cùng, Frontend's useWebSocket.onmessage() parse JSON, dispatch Redux action CARD_PLAY_SUCCESS, và React re-render với state mới.

### Luồng Notification (Server Push)

Luồng notification được kích hoạt khi Core server's GameService phát hiện các sự kiện quan trọng như cả hai người chơi đã chơi bài. Core sẽ gửi notification đến cả hai người chơi với type GAME.ROUND_REVEAL, bao gồm sessionId của từng người chơi và payload chứa thông tin như playerCard, opponentCard, và result.

Gateway's Listener Thread nhận notification, lookup webSocketSession từ activeClientSessions dựa trên sessionId, và forward đến client tương ứng qua WebSocket. Frontend's useWebSocket.onmessage() sẽ dispatch ROUND_REVEAL action, hiển thị modal kết quả và cập nhật điểm số trên giao diện

## BẢO MẬT & XÁC THỰC

Hệ thống triển khai nhiều lớp bảo mật để đảm bảo tính an toàn cho dữ liệu người dùng và tính toàn vẹn của game. Về mật khẩu, hệ thống sử dụng thuật toán BCrypt với cost factor 10, tương đương 2^10 hay 1024 rounds hashing. Salt được tự động sinh ra cho mỗi mật khẩu, đảm bảo cùng một mật khẩu cũng tạo ra hash khác nhau.

Quản lý session được thực hiện thông qua SessionId dạng UUID v4 với độ dài 128-bit, được lưu trữ cả trong bảng active_sessions của database và in-memory cache để truy xuất nhanh. Session có thời hạn 24 giờ và được tự động cleanup khi hết hạn. Mọi request đều phải validate sessionId để đảm bảo tính hợp lệ trước khi xử lý.

Để phòng chống SQL Injection, hệ thống sử dụng PreparedStatement cho tất cả database queries, kết hợp với input validation và sanitization. Chỉ sử dụng parameterized queries, không bao giờ concatenate string trực tiếp vào SQL statement.

Input validation được áp dụng nghiêm ngặt với các quy tắc cụ thể: Username phải có từ 3 đến 50 ký tự, chỉ chấp nhận alphanumeric và underscore; Email phải tuân thủ RFC 5322 format; Password tối thiểu 6 ký tự không giới hạn tối đa; Card ID phải nằm trong danh sách availableCards của người chơi.

Authorization được kiểm tra kỹ lưỡng để đảm bảo người dùng chỉ có thể tham gia vào game của chính họ, không thể truy cập session của người dùng khác. Các admin operations sẽ yêu cầu special role trong tương lai khi mở rộng hệ thống

## HIỆU NĂNG & TỐI ƯU HÓA

### Mô Hình Threading

Core Server triển khai kiến trúc đa luồng phức tạp để tối đa hóa hiệu năng. Main Thread chịu trách nhiệm khởi động TCP listener thông qua CoreServer.main() và chấp nhận connections mới qua ServerSocket.accept(). Mỗi connection được xử lý bởi một I/O Thread riêng trong ClientConnectionHandler.run(), đảm nhiệm việc đọc dữ liệu từ socket với in.readInt() và in.readFully(), cũng như ghi dữ liệu ra socket với out.writeInt() và out.write().

Worker Pool được cấu hình dạng CachedThreadPool, xử lý business logic như playCard, login, và các database queries thông qua JDBC operations. Pool này tự động scale, tạo threads mới khi cần thiết để đáp ứng workload. Scheduler Threads được sử dụng cho các tác vụ định kỳ như MatchmakingService gọi tryMatchmaking() mỗi giây, GameService xử lý round timeout sau 15 giây, và SessionManager cleanup các expired sessions.

Gateway Server cũng sử dụng kiến trúc multi-threading với WebSocket Threads quản lý mỗi connection thông qua Spring WebSocket Handler và xử lý message bất đồng bộ. TCP Listener Thread được khởi tạo bởi CoreTcpClient.startListening() để liên tục đọc dữ liệu từ Core server. Heartbeat Thread chạy CoreTcpClient.startHeartbeat() để gửi PING/PONG mỗi 5 giây, đảm bảo kết nối luôn hoạt động.

### Kiểm Soát Concurrency

Hệ thống sử dụng kỹ thuật Lock Striping trong GameService thông qua Map ánh xạ matchId với ReentrantLock. Mỗi game có lock riêng, ngăn chặn race condition khi cả hai người chơi đồng thời chơi bài. ConcurrentHashMap được sử dụng rộng rãi cho thread-safe storage của activeGames, activeSessions, và pendingRequests (ánh xạ correlationId với client).

Các atomic operations được triển khai với matchmakingQueue sử dụng ConcurrentLinkedQueue và usersInQueue sử dụng ConcurrentHashMap.newKeySet(). Critical sections như playCard() được bảo vệ bởi lock pattern: lock.lock() trước khi update game state và check if both played, sau đó đảm bảo lock.unlock() trong finally block để tránh deadlock.

### Tối Ưu Hóa Database

Connection pooling được triển khai với HikariCP, cấu hình pool size là 10 connections theo công thức (core_count × 2) + effective_spindle_count. Connection timeout được đặt ở 30 giây và idle timeout là 600 giây để cân bằng giữa hiệu năng và resource usage.

Indexes được thiết kế tối ưu cho các query phổ biến. Index phức hợp trên user_profiles(score DESC, games_won DESC) tăng tốc độ query leaderboard, index trên active_sessions(user_id) giúp tra cứu nhanh online status, và index trên games(player1_id, player2_id) hỗ trợ retrieval match history hiệu quả.

Query optimization được thực hiện bằng cách sử dụng JOIN thay vì multiple SELECTs riêng lẻ, áp dụng LIMIT cho pagination như lấy top 20 trong leaderboard, và luôn specify columns cụ thể thay vì SELECT *. Transaction management được quản lý với auto-commit cho simple queries và explicit transaction cho game finalization, bao gồm BEGIN TRANSACTION, UPDATE user_profiles, INSERT INTO games, rồi COMMIT để đảm bảo tính atomic

## TRIỂN KHAI (DEPLOYMENT)

### Môi Trường Development

Môi trường development được thiết lập trên local machine với bốn components chạy song song. Frontend chạy trên port 5173, được khởi động bằng lệnh npm install để cài đặt dependencies, sau đó npm run dev để start Vite dev server với tính năng hot reload, giúp developer thấy ngay các thay đổi mà không cần refresh browser.

Gateway chạy trên port 8080 với Spring Boot embedded Tomcat, được khởi động bằng lệnh mvn spring-boot:run từ thư mục gateway. Core server chạy trên port 9090 như một pure Java application, được compile và execute thông qua mvn compile exec:java từ thư mục core.

Database MySQL 8.0 chạy trên port 3306, có thể được khởi động nhanh chóng bằng Docker với lệnh docker run, truyền environment variables MYSQL_ROOT_PASSWORD và MYSQL_DATABASE. Schema được import bằng lệnh mysql -u root -p < db/schema.sql sau khi container đã chạy.

### Triển Khai Production

Môi trường production sử dụng Docker Compose để orchestrate tất cả services. File docker-compose.yml phiên bản 3.8 định nghĩa bốn services chính: database, core, gateway, và frontend.

Database service sử dụng image mysql:8.0, expose port 3306, nhận password từ environment variable ${DB_PASSWORD} để tăng tính bảo mật. Volume mysql_data được mount vào /var/lib/mysql để persist data ngay cả khi container restart. Service này kết nối với backend network.

Core service được build từ thư mục ./core, expose port 9090, và nhận DB_HOST cùng DB_PORT qua environment variables. Service này phụ thuộc vào database (depends_on) nên chỉ start sau khi database đã ready, và cũng kết nối với backend network.

Gateway service được build từ ./gateway, expose port 8080, nhận CORE_HOST và CORE_PORT để kết nối với Core server. Nó phụ thuộc vào core service và kết nối với cả backend và frontend networks, đóng vai trò cầu nối giữa hai tầng.

Frontend service được build từ ./frontend, expose port 80 (HTTP standard port), phụ thuộc vào gateway, và chỉ kết nối với frontend network. Hai networks frontend và backend được tách biệt để tăng cường bảo mật, đảm bảo frontend không thể truy cập trực tiếp vào core hay database

## GIAO THỨC GIAO TIẾP

### Giao Thức WebSocket (Client ↔ Gateway)

Giao tiếp giữa Client và Gateway sử dụng WebSocket protocol với format JSON. Mỗi message có cấu trúc chuẩn bao gồm trường type theo format DOMAIN.ACTION_MODIFIER để phân loại message, correlationId dạng c-timestamp-random để tracking request/response pairs, sessionId dạng sess-uuid để định danh người dùng, payload chứa dữ liệu cụ thể theo domain, và error object với code và message khi có lỗi xảy ra.

Các loại message được phân chia theo domain: AUTH.* cho authentication bao gồm LOGIN, REGISTER, LOGOUT; LOBBY.* cho matchmaking và leaderboard; GAME.* cho game logic như START, PLAY_CARD, END; và SYSTEM.* cho heartbeat và error handling.

### Giao Thức TCP (Gateway ↔ Core)

Giao tiếp giữa Gateway và Core server sử dụng TCP socket với Length-Prefixed Framing protocol. Mỗi message bắt đầu bằng 4 bytes chứa integer N đại diện cho độ dài của JSON payload, theo sau là N bytes chứa JSON data được encode bằng UTF-8.

Việc implement trong Java được thực hiện đơn giản với thao tác write: convert JSON message thành byte array với UTF-8 encoding, gọi out.writeInt() để ghi 4 bytes length header, sau đó out.write() để ghi N bytes payload, và cuối cùng out.flush() để đảm bảo data được gửi ngay lập tức. Thao tác read thực hiện ngược lại: in.readInt() để đọc 4 bytes length, tạo buffer với size tương ứng, in.readFully() để đọc đủ N bytes vào buffer, và convert buffer thành String với UTF-8 encoding. Cơ chế length-prefixed này giải quyết vấn đề message boundary trong TCP stream, đảm bảo mỗi message được đọc đầy đủ và chính xác

## XỬ LÝ LỖI & RECOVERY

Hệ thống được thiết kế để xử lý gracefully nhiều kịch bản lỗi khác nhau. Khi client disconnect bình thường, WebSocket onClose event trigger Gateway cleanup, Gateway gửi AUTH.LOGOUT_REQUEST đến Core, Core gọi SessionManager.removeSession() để cleanup session. Nếu người dùng đang trong game, đối thủ sẽ thắng do forfeit.

Trường hợp client crash đột ngột, Gateway phát hiện qua WebSocket onClose hoặc không nhận được response cho PING message. Sau 30 giây inactivity, hệ thống tự động logout và gửi notification GAME.OPPONENT_LEFT cho đối thủ nếu đang trong game.

Khi Gateway crash, Core server phát hiện IOException khi đọc socket, đóng ClientConnectionHandler tương ứng, và cleanup tất cả sessions từ Gateway bị crash. Tất cả users kết nối qua Gateway đó sẽ bị disconnect và cần reconnect qua Gateway khác hoặc chờ Gateway restart.

Nếu Core crash, Gateway phát hiện IOException khi đọc TCP socket và tự động thử reconnect với exponential backoff strategy. Clients sẽ thấy message "Connection lost" trên giao diện. Game state đang chơi dở sẽ bị mất do chưa được persist vào database.

Khi mất kết nối database, HikariCP connection pool tự động retry với backoff mechanism. Nếu retry thất bại sau nhiều lần thử, hệ thống sẽ return SYSTEM.ERROR cho client và log error để investigation.

Deadlock prevention được triển khai thông qua nhiều cơ chế: SYSTEM.WELCOME được gửi ngay lập tức khi client kết nối để establish connection state, heartbeat PING/PONG liên tục duy trì connection alive, và timeout được đặt cho tất cả blocking operations để tránh thread bị block vô thời hạn

## MONITORING & LOGGING

Hệ thống triển khai chiến lược observability và logging toàn diện. Trong môi trường development, console logs được sử dụng để track connection events như "Client X connected", message routing với log "Received: GAME.PLAY_CARD", và error traces chi tiết như "Failed to parse JSON: ..." để hỗ trợ debugging.

Trong môi trường production, các metrics quan trọng cần được track bao gồm số lượng active connections cho cả WebSocket và TCP, số games đang diễn ra thông qua activeGames.size(), độ dài matchmaking queue qua queue.size(), thời gian database query từ HikariCP metrics, và message throughput tính bằng messages per second.

Health checks được implement ở nhiều mức: endpoint /health cho Gateway status, database ping để kiểm tra connection pool health, và Core TCP ping thông qua heartbeat status. Các health checks này giúp phát hiện sớm các vấn đề và trigger alerts khi cần thiết.

## KẾT LUẬN

Kiến trúc 4 tầng phân tán được triển khai cho hệ thống Game Rút Bài May Mắn mang lại nhiều lợi ích quan trọng. Tách biệt trách nhiệm rõ ràng giữa các tầng giúp mỗi component có nhiệm vụ riêng biệt và không phụ thuộc chặt chẽ vào nhau. Điều này dẫn đến khả năng bảo trì và mở rộng tốt hơn, cho phép thay đổi hoặc scale từng tầng một cách độc lập mà không ảnh hưởng đến toàn bộ hệ thống.

Hiệu năng cao được đạt được thông qua việc kết hợp WebSocket cho realtime communication, TCP socket với length-prefixed framing cho throughput cao, và connection pooling với thread pooling để tối ưu hóa resource usage. Bảo mật được đảm bảo khi Core server không expose trực tiếp ra internet, với Gateway đóng vai trò reverse proxy và authentication/authorization được tập trung xử lý.

Khả năng xử lý lỗi tốt với graceful degradation và auto-recovery mechanism giúp hệ thống duy trì hoạt động ngay cả khi gặp sự cố. WebSocket push notifications mang lại realtime experience mượt mà cho người chơi.

Hệ thống đã sẵn sàng cho các bước mở rộng tiếp theo như horizontal scaling bằng cách thêm nhiều Core servers, triển khai load balancing ở Gateway tier để phân phối traffic, database replication hoặc sharding để tăng capacity, và tích hợp monitoring cùng analytics tools để có cái nhìn sâu hơn về performance và user behavior
