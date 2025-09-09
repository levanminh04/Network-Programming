# Giao Thức Thông Điệp (Message Protocol) 

> **Phạm vi:** Giao thức dạng chuỗi (string `type`) dùng chung cho Frontend ↔ Gateway ↔ Core.  
> **Module:** `shared/`, `gateway/`, `core/`, `frontend/` (React).  

---

## Nguyên tắc

- **`type` dạng chuỗi có namespace**: `AUTH.*`, `LOBBY.*`, `MATCH.*`, `GAME.*`, `LEADERBOARD.*`, `SYS.*`.
- **Single Source of Truth**: Mọi hằng số `type` và khóa JSON nằm ở `shared/`, `gateway` và `core` import. FE mirror sang TS.
- **Tiến hóa “add-only”**: Chỉ thêm `type` mới; không đổi tên/xóa `type` cũ để giữ tương thích.
- **Gateway chỉ là cầu nối**: Forward JSON nguyên vẹn (không xử lý nghiệp vụ). Core chịu trách nhiệm logic + DB.
- **Đủ nhưng gọn**: Bao phủ login → lobby → matchmaking → 3 vòng → kết quả → leaderboard, thêm timeout/disconnect + heartbeat.

---

## Envelope (khung) thông điệp

Mọi message **phải** theo format:

```jsonc
{
  "type": "GAME.ROUND_START", // chuỗi namespaced
  "ts": 1736412345678,        // epoch millis tại nơi gửi
  "cid": "c-92a1",            // correlation id / client id (khuyến nghị)
  "matchId": "m-7f31",        // id trận (nếu có)
  "uid": "u-1",               // id người dùng (nếu có, sau login)
  "payload": { ... }          // dữ liệu phụ thuộc vào type
}


"Pha 1 — Đăng nhập (Auth)
1.1 FE → Gateway → Core

Gửi: AUTH.LOGIN_REQUEST
Khi nào: người dùng submit form login.
Payload:

{
  "type": "AUTH.LOGIN_REQUEST",
  "ts": 1736412000000,
  "cid": "c-1",
  "payload": { "username": "alice", "password": "secret" }
}


Gateway: kiểm tra có type, forward nguyên vẹn.

Core: kiểm tra DB (users), băm mật khẩu, xác thực.

1.2 Core → Gateway → FE

Trả về một trong hai:

AUTH.LOGIN_OK

{
  "type": "AUTH.LOGIN_OK",
  "ts": 1736412000100,
  "cid": "c-1",
  "uid": "u-1",
  "payload": {
    "userId": 1, "username": "alice",
    "totalScore": 12, "wins": 10, "losses": 6, "draws": 4, "gamesPlayed": 20
  }
}


AUTH.LOGIN_FAIL

{
  "type": "AUTH.LOGIN_FAIL",
  "ts": 1736412000100,
  "cid": "c-1",
  "payload": { "reason": "INVALID_CREDENTIALS" }
}


FE xử lý:

Nếu OK → lưu uid, chuyển UI sang Lobby.

Nếu FAIL → hiển thị lỗi.

Pha 2 — Lobby (hiển thị người chơi/ trạng thái)

Ngay sau LOGIN_OK, Core có thể gửi:

2.1 Core → FE

LOBBY.SNAPSHOT (toàn bộ ảnh chụp)

{
  "type": "LOBBY.SNAPSHOT",
  "ts": 1736412000500,
  "uid": "u-1",
  "payload": {
    "players": [
      { "userId":1,"username":"alice","totalScore":12,"status":"IDLE" },
      { "userId":2,"username":"bob","totalScore":9,"status":"IN_MATCH" }
    ]
  }
}


LOBBY.UPDATE (delta khi có ai vào/ra/đổi trạng thái)

{
  "type": "LOBBY.UPDATE",
  "ts": 1736412001000,
  "uid": "u-1",
  "payload": { "userId": 3, "username": "carol", "totalScore": 0, "status": "IDLE" }
}


FE xử lý: render danh sách lobby theo snapshot, về sau cập nhật theo update (không cần refetch toàn bộ).

Pha 3 — Tìm trận (Matchmaking)
3.1 FE → Core (qua Gateway)

Gửi: MATCH.REQUEST khi người chơi nhấn “Tìm trận”.

{ "type":"MATCH.REQUEST", "ts":1736412010000, "cid":"c-2", "uid":"u-1", "payload":{} }


Nếu người chơi đổi ý trước khi ghép xong, FE gửi MATCH.CANCEL.

3.2 Core ghép cặp xong → FE

MATCH.FOUND (mỗi bên nhận đối thủ của mình)

{
  "type": "MATCH.FOUND",
  "ts": 1736412015000,
  "cid": "c-2",
  "uid": "u-1",
  "payload": {
    "matchId": "m-7f31",
    "opponent": { "userId": 2, "username": "bob", "totalScore": 9 }
  }
}


Tiếp theo: MATCH.START báo bắt đầu trận, thiết lập round=1

{
  "type": "MATCH.START",
  "ts": 1736412015200,
  "uid": "u-1",
  "matchId": "m-7f31",
  "payload": { "round": 1 }
}


FE xử lý: chuyển UI vào bàn chơi (table), hiển thị đối thủ, round hiện tại.

Pha 4 — Vòng chơi (3 vòng)

Mỗi vòng có các bước: ROUND_START → (người chơi chọn) → ACK/NACK → REVEAL.

4.1 Bắt đầu vòng

Core → FE: GAME.ROUND_START (cả 2 bên đều nhận)

{
  "type": "GAME.ROUND_START",
  "ts": 1736412020000,
  "uid": "u-1",
  "matchId": "m-7f31",
  "payload": { "round": 1, "deadlineEpochMs": 1736412030000 } // 10 giây
}


FE hiển thị: đồng hồ đếm ngược đến deadlineEpochMs.
Logic timeout Core: nếu hết hạn mà người chơi chưa chọn → auto-pick lá nhỏ nhất còn lại và vẫn tiến hành vòng chơi.

4.2 Người chơi chọn lá

FE → Core: GAME.PLAY_CARD

{
  "type": "GAME.PLAY_CARD",
  "ts": 1736412021000,
  "cid": "c-3",
  "uid": "u-1",
  "matchId": "m-7f31",
  "payload": { "round": 1, "index": 12, "requestId": "r-001" }
}


Tranh chấp chọn trùng:

Nếu cả hai cùng chọn một lá hợp lệ và luật quy định “ai gửi trước được lá đó” → Core cấp cho người đến trước, người đến sau sẽ NACK với reason:"CONFLICT" để chọn lại.

4.3 Core phản hồi chọn lá

ACK (hợp lệ): GAME.PICK_ACK

{
  "type": "GAME.PICK_ACK",
  "ts": 1736412021100,
  "uid": "u-1",
  "matchId": "m-7f31",
  "payload": {
    "round": 1,
    "index": 12,
    "card": { "rank": 10, "suit": "H", "val": 10, "index": 12 }
  }
}


NACK (không hợp lệ/tranh chấp/hết hạn): GAME.PICK_NACK

{
  "type": "GAME.PICK_NACK",
  "ts": 1736412021150,
  "uid": "u-1",
  "matchId": "m-7f31",
  "payload": { "round": 1, "reason": "CONFLICT" } // hoặc TIMEOUT / INVALID
}


FE logic:

Nhận ACK → khoá vị trí lá đã chọn, hiển thị “đã chọn”.

Nhận NACK → nếu còn thời gian, yêu cầu người chơi chọn lại; nếu TIMEOUT thì chờ ROUND_REVEAL.

4.4 Trạng thái đối thủ (giúp UI mượt)

Core → FE: GAME.OPPONENT_STATUS

Gửi cho đối phương khi một bên bắt đầu/hoàn tất chọn.

{ "type":"GAME.OPPONENT_STATUS","ts":1736412021200,"uid":"u-1","matchId":"m-7f31",
  "payload": { "whoUserId": 2, "status": "DONE" } }

4.5 Lật bài & cộng dồn

Core → FE (cả 2 bên): GAME.ROUND_REVEAL

Chỉ gửi sau khi cả 2 đã chọn xong hoặc hết hạn (auto-pick phần còn lại).

{
  "type": "GAME.ROUND_REVEAL",
  "ts": 1736412023000,
  "uid": "u-1",
  "matchId": "m-7f31",
  "payload": {
    "round": 1,
    "aCard": { "rank": 10, "suit": "H", "val": 10 },
    "bCard": { "rank": 7, "suit": "S", "val": 7 },
    "aSumSoFar": 10,
    "bSumSoFar": 7
  }
}


FE hiển thị: lật đồng thời hai lá + tổng điểm tới hiện tại.
Core: ghi draw_log (nếu dùng), cập nhật điểm tổng trong ngữ cảnh trận.

4.x Lặp lại vòng 2, 3

Core phát GAME.ROUND_START (round=2), rồi quy trình PLAY_CARD → ACK/NACK → OPPONENT_STATUS → ROUND_REVEAL lặp lại.

Hết vòng 3 sẽ sang phần kết quả.

Pha 5 — Kết thúc trận & Bảng xếp hạng
5.1 Kết quả chung cuộc

Core → FE: MATCH.RESULT

Cập nhật DB:

Bảng matches: tổng điểm, người thắng, thời gian…

Bảng users: totalScore, wins/losses/draws, gamesPlayed.

{
  "type":"MATCH.RESULT",
  "ts":1736412045000,
  "uid":"u-1",
  "matchId":"m-7f31",
  "payload":{
    "totalA": 24, "totalB": 20,
    "result": "A_WINS",
    "scoreAward": { "A": 1.0, "B": 0.0 }
  }
}


FE: hiển thị màn hình kết quả (thắng/thua/hòa) + điểm cộng.

5.2 Xem bảng xếp hạng

FE → Core: LEADERBOARD.REQUEST
Core → FE: LEADERBOARD.RESPONSE

// Request
{ "type":"LEADERBOARD.REQUEST", "ts":1736412050000, "cid":"c-9", "uid":"u-1", "payload":{ "top": 10 } }

// Response
{
  "type":"LEADERBOARD.RESPONSE",
  "ts":1736412050300,
  "cid":"c-9",
  "uid":"u-1",
  "payload":{
    "entries":[
      {"username":"alice","totalScore":13,"wins":11,"losses":6,"draws":4,"gamesPlayed":21},
      {"username":"bob","totalScore":9,"wins":8,"losses":7,"draws":2,"gamesPlayed":17}
    ],
    "generatedAt": 1736412050300
  }
}

Pha 6 — Heartbeat & Lỗi & Disconnect
6.1 Heartbeat

FE → Core: SYS.PING mỗi ~30s (hoặc dựa WS ping/pong có sẵn).

Core → FE: SYS.PONG để giữ phiên “sống”.

6.2 Lỗi khung

Khi JSON sai, thiếu trường, hoặc người chơi gửi sai ngữ cảnh → Core → FE: SYS.ERROR

{
  "type":"SYS.ERROR",
  "ts":1736412060000,
  "payload": { "code":"BAD_REQUEST", "message":"missing round/index" }
}

6.3 Đối thủ rời trận

Core phát: MATCH.OPPONENT_LEFT cho người còn lại, đồng thời xử thua phía rời trận.

{ "type":"MATCH.OPPONENT_LEFT","ts":1736412035000,"uid":"u-1",
  "matchId":"m-7f31","payload":{ "whoUserId": 2 } }


FE: dừng đếm ngược, hiển thị thắng do đối thủ rời.

Vai trò Gateway trong mọi pha (tóm tắt)

Nhận JSON từ FE → kiểm chuẩn tối thiểu (type có mặt) → prefix độ dài → send TCP tới Core.

Nhận từ Core → định tuyến theo session dựa cid/map kết nối → send WS tới FE.

Không deserialise/hiểu payload → giữ kiến trúc độc lập, dễ thay thế.

Quản lý kết nối: WS handshake, ping/pong, cleanup khi client rời.

State máy mini (gợi ý cho FE & Core)

Client State: NOT_AUTH → AUTHED/LOBBY → MATCHING → IN_GAME(round=1..3) → POST_GAME
Core State (per user): tương tự; per match: WAITING_PLAYERS → START → ROUND k (WAIT_CHOICES) → REVEAL → … → END

Chuyển trạng thái chính nhờ các message:

LOGIN_OK → vào LOBBY

MATCH.REQUEST / MATCH.CANCEL → MATCHING / LOBBY

MATCH.FOUND/MATCH.START → IN_GAME round=1

ROUND_START → chờ PLAY_CARD

đủ 3 lần ROUND_REVEAL → MATCH.RESULT → POST_GAME

Checklist implement nhanh

shared/: tạo Message.java (Type + Keys).

frontend/: tạo MessageType.ts (mirror), helper send(type, payload).

gateway/:

WebSocketController nhận text → validate có type → đẩy sang TcpBridge.

TcpBridge giữ map sessionId ↔ tcpConn, prefix 4-byte length, forward.

core/:

TCP server accept; ClientHandler parse JSON theo type; switch gọi handle*.

Quản lý matchmaking queue; quản lý MatchContext (deck, picks, timers).

Dùng ScheduledExecutor cho deadline 10s/auto-pick.

Ghi DB ở MATCH.RESULT và (tuỳ) draw_log.

test luồng: login → snapshot → match → 3 vòng (test: trùng lá → NACK; hết hạn → auto-pick) → result → leaderboard."
