
public final class MessageProtocol {
    private MessageProtocol() {}

    public static final class Type {
        // AUTH
        public static final String AUTH_LOGIN_REQUEST   = "AUTH.LOGIN_REQUEST";
        public static final String AUTH_LOGIN_OK        = "AUTH.LOGIN_OK";
        public static final String AUTH_LOGIN_FAIL      = "AUTH.LOGIN_FAIL";
        public static final String AUTH_LOGOUT_REQUEST  = "AUTH.LOGOUT_REQUEST";

        // LOBBY
        public static final String LOBBY_SNAPSHOT       = "LOBBY.SNAPSHOT";
        public static final String LOBBY_UPDATE         = "LOBBY.UPDATE";

        // MATCH
        public static final String MATCH_REQUEST        = "MATCH.REQUEST";
        public static final String MATCH_CANCEL         = "MATCH.CANCEL";
        public static final String MATCH_FOUND          = "MATCH.FOUND";
        public static final String MATCH_START          = "MATCH.START";
        public static final String MATCH_RESULT         = "MATCH.RESULT";
        public static final String MATCH_OPPONENT_LEFT  = "MATCH.OPPONENT_LEFT";

        // GAME (3 rounds)
        public static final String GAME_ROUND_START     = "GAME.ROUND_START";
        public static final String GAME_PLAY_CARD       = "GAME.PLAY_CARD";
        public static final String GAME_PICK_ACK        = "GAME.PICK_ACK";
        public static final String GAME_PICK_NACK       = "GAME.PICK_NACK";
        public static final String GAME_OPPONENT_STATUS = "GAME.OPPONENT_STATUS";
        public static final String GAME_ROUND_REVEAL    = "GAME.ROUND_REVEAL";

        // LEADERBOARD
        public static final String LEADERBOARD_REQUEST  = "LEADERBOARD.REQUEST";
        public static final String LEADERBOARD_RESPONSE = "LEADERBOARD.RESPONSE";

        // SYSTEM
        public static final String SYS_PING             = "SYS.PING";
        public static final String SYS_PONG             = "SYS.PONG";
        public static final String SYS_ERROR            = "SYS.ERROR";

        private Type() {}
    }

    public static final class Keys {
        public static final String TYPE     = "type";
        public static final String TS       = "ts";
        public static final String CID      = "cid";
        public static final String MATCH_ID = "matchId";
        public static final String UID      = "uid";
        public static final String PAYLOAD  = "payload";
        private Keys() {}
    }
}
