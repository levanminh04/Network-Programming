/**
 * MessageProtocol: Định nghĩa tên các loại message (type) & khóa (keys) dùng chung Frontend ↔ Gateway ↔ Core.
 *
 * Quy ước gói tin (envelope) JSON:
 * {
 *   "type": "DOMAIN.ACTION",   // bắt buộc - tên thông điệp, lấy từ MessageProtocol.Type.*
 *   "ts":   1736412000000,     // (khuyến nghị) epoch millis - do FE đặt/ Core điền
 *   "cid":  "c-1",             // (khuyến nghị) correlation id - FE sinh để map request/response
 *   "uid":  "u-1",             // (tùy) user id - đi kèm sau khi login
 *   "matchId": "M1",           // (tùy) id trận - xuất hiện từ lúc MATCH_FOUND/START trở đi
 *   "payload": { ... }         // (tùy) dữ liệu kèm theo - schema phụ thuộc vào từng type
 * }
 *
 * Lưu ý:
 * - FE luôn đọc "type" trước để route xử lý.
 * - Mọi message đều đi qua WebSocket (FE <-> Gateway), Gateway chỉ forward sang TCP Core và ngược lại.
 * - Các ví dụ payload dưới đây là gợi ý schema để FE dễ triển khai; team có thể tinh chỉnh nhẹ nhưng nên nhất quán.
 */
public final class MessageProtocol {
    private MessageProtocol() {}

    public static final class Type {

        // ============================
        // AUTH (Đăng nhập/Đăng xuất)
        // ============================

        /**
         * FE -> Core (qua Gateway)
         * Khi người dùng submit form login.
         *
         * payload: { "username": "alice", "password": "secret" }
         * ví dụ:
         * {
         *   "type": "AUTH.LOGIN_REQUEST",
         *   "cid": "c-1",
         *   "ts": 1736412000000,
         *   "payload": {"username":"alice","password":"secret"}
         * }
         */
        public static final String AUTH_LOGIN_REQUEST   = "AUTH.LOGIN_REQUEST";

        /**
         * Core -> FE (qua Gateway)
         * Phản hồi đăng nhập thành công. FE chuyển sang lobby.
         *
         * payload (gợi ý):
         * { "uid":"u-1","username":"alice","score":120,"token":"..."} // token nếu có
         */
        public static final String AUTH_LOGIN_OK        = "AUTH.LOGIN_OK";

        /**
         * Core -> FE (qua Gateway)
         * Phản hồi đăng nhập thất bại.
         *
         * payload (gợi ý): { "error":"INVALID_CREDENTIALS" }
         */
        public static final String AUTH_LOGIN_FAIL      = "AUTH.LOGIN_FAIL";

        /**
         * FE -> Core (qua Gateway)
         * Người dùng yêu cầu đăng xuất chủ động (đóng phiên, dọn lobby state).
         *
         * payload: {} (không bắt buộc)
         */
        public static final String AUTH_LOGOUT_REQUEST  = "AUTH.LOGOUT_REQUEST";


        // ============================
        // LOBBY (Sảnh chờ)
        // ============================

        /**
         * Core -> FE (qua Gateway)
         * Ảnh chụp đầy đủ lobby tại thời điểm hiện tại (gửi ngay sau login OK
         * hoặc khi FE yêu cầu refresh).
         *
         * payload (gợi ý):
         * {
         *   "players": [
         *     {"uid":"u-1","username":"alice","score":120,"status":"IDLE|BUSY"},
         *     {"uid":"u-2","username":"bob","score":95,"status":"IDLE"}
         *   ]
         * }
         */
        public static final String LOBBY_SNAPSHOT       = "LOBBY.SNAPSHOT";

        /**
         * Core -> FE (broadcast qua Gateway)
         * Cập nhật delta lobby khi có người vào/ra/đổi trạng thái.
         *
         * payload (gợi ý):
         * { "joined":[...], "left":[...], "statusChanged":[{"uid":"u-2","status":"BUSY"}] }
         */
        public static final String LOBBY_UPDATE         = "LOBBY.UPDATE";


        // ============================
        // MATCH (Tìm trận/Trạng thái trận)
        // ============================

        /**
         * FE -> Core (qua Gateway)
         * Người chơi bấm "Tìm trận".
         *
         * payload: {} (có thể thêm mode/rule nếu mở rộng)
         */
        public static final String MATCH_REQUEST        = "MATCH.REQUEST";

        /**
         * FE -> Core (qua Gateway)
         * Hủy yêu cầu tìm trận nếu đang chờ.
         *
         * payload: {}
         */
        public static final String MATCH_CANCEL         = "MATCH.CANCEL";

        /**
         * Core -> FE (qua Gateway)
         * Thông báo đã tìm được đối thủ (có thể dùng trước khi start round 1).
         * Nếu team muốn đơn giản, có thể bỏ MATCH_FOUND và dùng thẳng MATCH_START.
         *
         * payload (gợi ý):
         * { "matchId":"M1","opponent":{"uid":"u-2","username":"bob","score":95"} }
         */
        public static final String MATCH_FOUND          = "MATCH.FOUND";

        /**
         * Core -> FE (qua Gateway)
         * Xác nhận vào bàn và sắp bắt đầu game (vòng 1).
         * FE chuyển màn sang "GameBoard".
         *
         * payload (gợi ý): { "matchId":"M1","round":1, "countdownMs": 1500 }
         */
        public static final String MATCH_START          = "MATCH.START";

        /**
         * Core -> FE (qua Gateway) — gửi cho cả hai người chơi
         * Kết quả chung cuộc sau 3 vòng hoặc do đối thủ bỏ cuộc/timeout luật.
         *
         * payload (gợi ý):
         * {
         *   "matchId":"M1",
         *   "totalA":26,"totalB":24,
         *   "result":"A_WINS|B_WINS|DRAW",
         *   "award":{"A":1.0,"B":0.0}
         * }
         */
        public static final String MATCH_RESULT         = "MATCH.RESULT";

        /**
         * Core -> FE (qua Gateway) — gửi cho người còn lại trong trận
         * Thông báo đối thủ rời trận (mất kết nối/thoát).
         * FE nên hiển thị thông báo và chờ MATCH.RESULT (thắng do forfeit).
         *
         * payload (gợi ý): { "who":"OPPONENT|SELF_UID" }
         */
        public static final String MATCH_OPPONENT_LEFT  = "MATCH.OPPONENT_LEFT";


        // ============================
        // GAME (3 rounds) — Gameplay
        // ============================

        /**
         * Core -> FE (qua Gateway) — gửi cho cả hai bên
         * Bắt đầu một round mới. FE hiển thị đếm ngược theo deadline.
         *
         * payload (gợi ý):
         * { "matchId":"M1","round":1,"deadlineEpochMs":1736412005000,"durationMs":10000 }
         */
        public static final String GAME_ROUND_START     = "GAME.ROUND_START";

        /**
         * FE -> Core (qua Gateway)
         * Người chơi chọn một lá bài trong round hiện tại.
         *
         * payload (gợi ý):
         * { "matchId":"M1","round":1,"index":17,"requestId":"client-gen-id" }
         * - index: vị trí/lá trong bộ còn lại của người chơi (theo quy ước server).
         * - requestId: để FE map ACK/NACK nếu gửi nhanh/đồng thời.
         */
        public static final String GAME_PLAY_CARD       = "GAME.PLAY_CARD";

        /**
         * Core -> FE (qua Gateway) — chỉ gửi cho người vừa chọn hợp lệ
         * Xác nhận server đã ghi nhận lựa chọn và tiết lộ lá bài của chính người đó.
         * (Không tiết lộ bài đối thủ; bài đối thủ chỉ lộ ở ROUND_REVEAL).
         *
         * payload (gợi ý):
         * {
         *   "matchId":"M1","round":1,"index":17,"requestId":"client-gen-id",
         *   "card":{"rank":"Q","suit":"H","val":10}
         * }
         */
        public static final String GAME_PICK_ACK        = "GAME.PICK_ACK";

        /**
         * Core -> FE (qua Gateway) — chỉ gửi cho người bị từ chối
         * Lựa chọn không hợp lệ (vd: trùng lá đã bị đối thủ giành; index sai; hết hạn).
         * FE nên hiển thị thông báo và cho phép chọn lại (nếu còn thời gian).
         *
         * payload (gợi ý):
         * { "matchId":"M1","round":1,"requestId":"client-gen-id","error":"CARD_TAKEN|TIMEOUT|BAD_INDEX" }
         */
        public static final String GAME_PICK_NACK       = "GAME.PICK_NACK";

        /**
         * Core -> FE (qua Gateway) — gửi cho đối thủ của người vừa chọn xong
         * Cập nhật UI: “Đối thủ đã chọn xong”.
         *
         * payload (gợi ý): { "who":"OPPONENT","status":"DONE" }
         */
        public static final String GAME_OPPONENT_STATUS = "GAME.OPPONENT_STATUS";

        /**
         * Core -> FE (qua Gateway) — gửi đồng thời cho cả hai bên
         * Lật bài của cả hai và cập nhật điểm cộng dồn sau round.
         *
         * payload (gợi ý):
         * {
         *   "matchId":"M1","round":1,
         *   "aCard":{"rank":"Q","suit":"H","val":10,"index":17},
         *   "bCard":{"rank":"7","suit":"S","val":7,"index":5},
         *   "aSumSoFar":10,"bSumSoFar":7
         * }
         */
        public static final String GAME_ROUND_REVEAL    = "GAME.ROUND_REVEAL";


        // ============================
        // LEADERBOARD (Bảng xếp hạng)
        // ============================

        /**
         * FE -> Core (qua Gateway)
         * Yêu cầu lấy bảng xếp hạng hiện tại.
         *
         * payload (tùy chọn): { "limit":50, "offset":0 }
         */
        public static final String LEADERBOARD_REQUEST  = "LEADERBOARD.REQUEST";

        /**
         * Core -> FE (qua Gateway)
         * Trả bảng xếp hạng.
         *
         * payload (gợi ý):
         * {
         *   "items":[
         *     {"rank":1,"uid":"u-9","username":"eve","score":420},
         *     {"rank":2,"uid":"u-1","username":"alice","score":390}
         *   ],
         *   "total": 1256
         * }
         */
        public static final String LEADERBOARD_RESPONSE = "LEADERBOARD.RESPONSE";


        // ============================
        // SYSTEM (Hệ thống/Heartbeat)
        // ============================

        /**
         * FE -> Core (qua Gateway)
         * Ping kiểm tra kết nối/độ trễ (nếu FE muốn tự đo).
         *
         * payload (gợi ý): { "nonce":"random-uuid" }
         */
        public static final String SYS_PING             = "SYS.PING";

        /**
         * Core -> FE (qua Gateway)
         * Phản hồi PING; FE có thể so ts gửi/nhận để ước lượng latency.
         *
         * payload (gợi ý): { "nonce":"random-uuid","serverTs":1736412000000 }
         */
        public static final String SYS_PONG             = "SYS.PONG";

        /**
         * Core -> FE (qua Gateway) hoặc Gateway -> FE
         * Báo lỗi chung cấp hệ thống (parse JSON fail, size quá lớn, không nhận diện type,...).
         *
         * payload (gợi ý): { "code":"BAD_REQUEST|UNKNOWN_TYPE|RATE_LIMIT", "message":"..." }
         */
        public static final String SYS_ERROR            = "SYS.ERROR";

        private Type() {}
    }

    public static final class Keys {
        // ===== Khóa chuẩn hóa trong envelope JSON =====

        /** "type": tên thông điệp (bắt buộc) — dùng để route ở FE. */
        public static final String TYPE     = "type";

        /** "ts": timestamp epoch millis — FE/Server gắn để log/sắp xếp/đo độ trễ. */
        public static final String TS       = "ts";

        /** "cid": correlation id — FE sinh để ghép cặp request/response (debug/tracking). */
        public static final String CID      = "cid";

        /** "matchId": id trận hiện thời — xuất hiện từ lúc MATCH_FOUND/START và mọi message gameplay. */
        public static final String MATCH_ID = "matchId";

        /** "uid": id người dùng — dùng sau khi login OK; hỗ trợ multi-tab hoặc đồng bộ client. */
        public static final String UID      = "uid";

        /** "payload": dữ liệu đi kèm — schema phụ thuộc từng type. */
        public static final String PAYLOAD  = "payload";

        private Keys() {}
    }
}
