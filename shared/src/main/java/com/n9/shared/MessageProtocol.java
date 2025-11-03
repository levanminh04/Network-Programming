package com.n9.shared;

/**
 * Protocol: Định nghĩa "Hợp đồng Giao tiếp" duy nhất cho toàn bộ hệ thống.
 * Đây là nguồn chân lý cho các loại message (type) và cấu trúc gói tin (envelope).
 *
 * QUY ƯỚC GÓI TIN (ENVELOPE) JSON TINH GỌN:
 * {
 * "type": "DOMAIN.ACTION_MODIFIER", // Bắt buộc - Lấy từ Protocol.Type.*
 * "correlationId": "c-xyz",        // Bắt buộc cho request/response - Để debug
 * "sessionId": "s-abc",            // Bắt buộc sau khi đăng nhập
 * "payload": { ... },              // Dữ liệu cụ thể của message
 * "error": { ... }                 // Chỉ xuất hiện khi có lỗi
 * }
 *
 * @author N9 Team (Refactored for MVP)
 * @version 1.0.0
 */
public final class MessageProtocol {
    private MessageProtocol() {}

    public static final class Type {

        // ============================
        // AUTH DOMAIN
        // ============================

        public static final String AUTH_REGISTER_REQUEST = "AUTH.REGISTER_REQUEST";
        public static final String AUTH_REGISTER_SUCCESS = "AUTH.REGISTER_SUCCESS";
        public static final String AUTH_REGISTER_FAILURE = "AUTH.REGISTER_FAILURE";

        public static final String AUTH_LOGIN_REQUEST    = "AUTH.LOGIN_REQUEST";
        // THAY ĐỔI: Đổi AUTH_LOGIN_OK/FAIL thành _SUCCESS/_FAILURE cho nhất quán
        public static final String AUTH_LOGIN_SUCCESS    = "AUTH.LOGIN_SUCCESS";
        public static final String AUTH_LOGIN_FAILURE    = "AUTH.LOGIN_FAILURE";

        public static final String AUTH_LOGOUT_REQUEST   = "AUTH.LOGOUT_REQUEST";
        public static final String AUTH_LOGOUT_SUCCESS   = "AUTH.LOGOUT_SUCCESS";


        // ============================
        // LOBBY DOMAIN
        // ============================

        // ============================
        // LOBBY DOMAIN
        // ============================
        public static final String LOBBY_MATCH_REQUEST   = "LOBBY.MATCH_REQUEST";
        // THÊM HẰNG SỐ CÒN THIẾU
        /** Phản hồi từ server, xác nhận đã nhận yêu cầu tìm trận. */
        public static final String LOBBY_MATCH_REQUEST_ACK = "LOBBY.MATCH_REQUEST_ACK";
        public static final String LOBBY_MATCH_CANCEL    = "LOBBY.MATCH_CANCEL";


        // ============================
        // GAME DOMAIN
        // ============================

        /** Thông báo đẩy từ server cho 2 client khi tìm thấy trận. */
        public static final String GAME_MATCH_FOUND      = "GAME.MATCH_FOUND";

        /** Bắt đầu trận đấu, gửi thông tin ban đầu. */
        public static final String GAME_START            = "GAME.START";

        /** Bắt đầu một vòng đấu mới. */
        public static final String GAME_ROUND_START      = "GAME.ROUND_START";

        /** Kết thúc một vòng đấu, công bố kết quả round. */
        public static final String GAME_ROUND_REVEAL     = "GAME.ROUND_REVEAL";

        // THAY ĐỔI: Hợp nhất GAME_PLAY_CARD, ACK, NACK thành một cặp request/response
        /** Client gửi yêu cầu đánh một lá bài. */
        public static final String GAME_CARD_PLAY_REQUEST  = "GAME.CARD_PLAY_REQUEST";
        /** Server xác nhận nước đi hợp lệ. */
        public static final String GAME_CARD_PLAY_SUCCESS  = "GAME.CARD_PLAY_SUCCESS";
        /** Server báo nước đi không hợp lệ. */
        public static final String GAME_CARD_PLAY_FAILURE  = "GAME.CARD_PLAY_FAILURE";

        /** Thông báo cho client biết đối thủ đã sẵn sàng/đánh bài xong. */
        public static final String GAME_OPPONENT_READY     = "GAME.OPPONENT_READY";

        /** Công bố kết quả cuối cùng của trận đấu. */
        public static final String GAME_END                = "GAME.END";

        /** Thông báo đối thủ đã thoát trận. */
        public static final String GAME_OPPONENT_LEFT      = "GAME.OPPONENT_LEFT";

        /** Client gửi yêu cầu đầu hàng/thoát trận. */
        public static final String GAME_FORFEIT_REQUEST    = "GAME.FORFEIT_REQUEST";
        /** Server xác nhận đã xử lý forfeit. */
        public static final String GAME_FORFEIT_SUCCESS    = "GAME.FORFEIT_SUCCESS";


        // ============================
        // SYSTEM DOMAIN
        // ============================
        // THÊM: Tin nhắn chào mừng để phá vỡ deadlock
        public static final String SYSTEM_WELCOME        = "SYSTEM.WELCOME";
        public static final String SYSTEM_PING           = "SYSTEM.PING";
        public static final String SYSTEM_PONG           = "SYSTEM.PONG";
        public static final String SYSTEM_ERROR          = "SYSTEM.ERROR";

    }

    // THAY ĐỔI: Keys class được cập nhật để khớp với MessageEnvelope tinh gọn.
    public static final class Keys {
        public static final String TYPE            = "type";
        public static final String CORRELATION_ID  = "correlationId";
        public static final String SESSION_ID      = "sessionId";
        public static final String PAYLOAD         = "payload";
        public static final String ERROR           = "error";

        private Keys() {}
    }
}