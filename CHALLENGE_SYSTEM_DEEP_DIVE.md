# 📖 CHALLENGE SYSTEM - TÀI LIỆU KỸ THUẬT CHI TIẾT

> **Tác giả:** N9 Team  
> **Ngày tạo:** 11/11/2025  
> **Phiên bản:** 1.0.0  
> **Mục đích:** Giải thích chi tiết về hệ thống Challenge (Thách đấu trực tiếp)

---

## 📑 MỤC LỤC

1. [Tổng quan về Challenge System](#1-tổng-quan-về-challenge-system)
2. [Tại sao cần ChallengeSession?](#2-tại-sao-cần-challengesession)
3. [Tại sao cần Lock mechanism?](#3-tại-sao-cần-lock-mechanism)
4. [Timeout Management (15 giây)](#4-timeout-management-15-giây)
5. [So sánh với Matchmaking](#5-so-sánh-với-matchmaking)
6. [Race Conditions và Edge Cases](#6-race-conditions-và-edge-cases)
7. [Memory Management](#7-memory-management)
8. [Testing Scenarios](#8-testing-scenarios)
9. [Kết luận](#9-kết-luận)

---

## 1. TỔNG QUAN VỀ CHALLENGE SYSTEM

### 1.1. Challenge là gì?

**Challenge (Thách đấu trực tiếp)** là tính năng cho phép người chơi gửi lời mời chơi **trực tiếp** đến một người cụ thể (không qua matchmaking queue).

**Workflow cơ bản:**
```
Sender click "⚔️ Thách đấu" → Target nhận notification → Target chọn Accept/Decline → Nếu Accept: Tạo match ngay
```

**Đặc điểm:**
- ⏱️ **Thời gian giới hạn:** 15 giây
- 🎯 **1-to-1 relationship:** Mỗi challenge chỉ giữa 2 người
- 🔄 **Stateful:** Cần track trạng thái (PENDING → ACCEPTED/DECLINED/TIMEOUT/CANCELLED)
- 🚫 **Blocking:** Khi đang trong challenge, không thể vào queue hoặc nhận challenge khác

---

## 2. TẠI SAO CẦN CHALLENGESESSION?

### 2.1. "Tại sao không dùng SessionContext thôi?"

**Câu trả lời:** Vì **SessionContext** và **ChallengeSession** phục vụ 2 mục đích khác nhau!

#### **SessionContext** (quản lý user session - dài hạn)
```java
public class SessionContext {
    private String sessionId;        // Session ID (duy nhất, không đổi)
    private String userId;           // User ID
    private String username;         // Username
    private String currentMatchId;   // Match hiện tại (nếu có)
    private String challengeId;      // Challenge hiện tại (nếu có) ← CHỈ LƯU ID!
    private long lastActivity;       // Timestamp hoạt động cuối
}
```

**Mục đích:** 
- Track user đang làm gì (đang chơi game nào? đang trong challenge nào?)
- Lifespan: Từ khi login → logout (có thể vài giờ)
- 1 user = 1 SessionContext

#### **ChallengeSession** (quản lý 1 challenge cụ thể - ngắn hạn)
```java
public class ChallengeSession {
    private String challengeId;           // ID của challenge này
    private String senderId;              // Người gửi
    private String targetId;              // Người nhận
    private ChallengeStatus status;       // PENDING/ACCEPTED/DECLINED/TIMEOUT/CANCELLED
    private long createdAt;               // Thời điểm tạo
    private long expiresAt;               // Thời điểm hết hạn (createdAt + 15s)
}
```

**Mục đích:**
- Track TRẠNG THÁI CỦA 1 CHALLENGE CỤ THỂ
- Lifespan: Từ khi tạo → accept/decline/timeout (tối đa 15 giây)
- 1 challenge = 1 ChallengeSession

---

### 2.2. Tại sao không "gộp chung" vào SessionContext?

Hãy tưởng tượng nếu gộp chung:

```java
// ❌ BAD DESIGN: Gộp challenge info vào SessionContext
public class SessionContext {
    private String userId;
    private String challengeId;              // ID của challenge
    private String challengeTargetId;        // ← Cần thêm field này
    private String challengeSenderId;        // ← Và cả field này
    private ChallengeStatus challengeStatus; // ← Và cả trạng thái
    private long challengeExpiresAt;         // ← Và cả thời gian hết hạn
    // ... phình to ra!
}
```

**Vấn đề:**

1. **Trách nhiệm không rõ ràng:**
   - SessionContext vừa quản lý user session (dài hạn)
   - Vừa quản lý challenge state (ngắn hạn)
   - Vi phạm **Single Responsibility Principle**

2. **Khó mở rộng:**
   - Sau này muốn thêm tính năng "challenge nhiều người"? → SessionContext càng phình to
   - Muốn thêm history của challenges? → Không biết lưu ở đâu

3. **Cleanup phức tạp:**
   - Khi challenge kết thúc, phải clear 5-6 fields trong SessionContext
   - Dễ quên clear → memory leak

4. **Không thể quản lý concurrent challenges:**
   - 1 user có thể nhận 2 challenges cùng lúc từ 2 người khác (trong tương lai)
   - SessionContext chỉ lưu được 1 challengeId → không scale

---

### 2.3. Lợi ích của việc tách riêng ChallengeSession

✅ **Separation of Concerns:**
```
SessionContext → "User đang làm gì?"
ChallengeSession → "Challenge này diễn ra như thế nào?"
```

✅ **Dễ cleanup:**
```java
// Khi challenge kết thúc
activeChallenges.remove(challengeId);  // Xóa ChallengeSession
sessionContext.setChallengeId(null);   // Clear reference trong SessionContext
// DONE! Đơn giản và rõ ràng
```

✅ **Dễ query:**
```java
// Tìm tất cả challenges đang PENDING
activeChallenges.values()
    .stream()
    .filter(c -> c.getStatus() == ChallengeStatus.PENDING)
    .collect(Collectors.toList());
```

✅ **Thread-safe hơn:**
```java
// Mỗi challenge có lock riêng
Lock lock = challengeLocks.get(challengeId);
lock.lock();
try {
    // Modify challenge state
} finally {
    lock.unlock();
}
```

---

### 2.4. Mối quan hệ giữa SessionContext và ChallengeSession

```
┌─────────────────────┐
│  SessionContext     │
│  (User: Alice)      │
│                     │
│  challengeId: "ch-1"│ ─────┐
└─────────────────────┘      │
                             │ References
┌─────────────────────┐      │
│  SessionContext     │      │
│  (User: Bob)        │      │
│                     │      │
│  challengeId: "ch-1"│ ─────┤
└─────────────────────┘      │
                             ↓
                  ┌─────────────────────┐
                  │  ChallengeSession   │
                  │  (ID: "ch-1")       │
                  │                     │
                  │  senderId: "Alice"  │
                  │  targetId: "Bob"    │
                  │  status: PENDING    │
                  │  expiresAt: T+15s   │
                  └─────────────────────┘
```

**Giải thích:**
- Alice và Bob **đều reference** đến cùng 1 ChallengeSession (qua `challengeId`)
- ChallengeSession chứa **full state** của challenge
- SessionContext chỉ lưu **pointer** (`challengeId`)

---

## 3. TẠI SAO CẦN LOCK MECHANISM?

### 3.1. "Mỗi challenge chỉ 15 giây, có cần lock không?"

**Câu trả lời:** CÓ! Vì có **race conditions** rất dễ xảy ra.

---

### 3.2. Race Condition #1: Double Accept

**Kịch bản:**
1. Alice gửi challenge cho Bob (T=0s)
2. Bob click "Accept" (T=5s) → Thread A bắt đầu xử lý
3. Timeout trigger (T=15s) → Thread B bắt đầu xử lý (vì Bob chưa kịp response)

**Không có Lock:**
```java
// Thread A (Bob accepts)
ChallengeSession challenge = activeChallenges.get(id);
if (challenge.getStatus() == PENDING) {  // ✅ Check pass
    // ... processing (takes 100ms)
    challenge.setStatus(ACCEPTED);       // ← CÒN ĐANG XỬ LÝ
    createDirectMatch(...);
}

// Thread B (Timeout) - CÙng LÚC
ChallengeSession challenge = activeChallenges.get(id);
if (challenge.getStatus() == PENDING) {  // ✅ Check pass (vì Thread A chưa set)
    challenge.setStatus(TIMEOUT);        // ← CONFLICT!
    notifyChallengeCancelled(...);
}
```

**Kết quả:**
- ⚠️ Challenge vừa ACCEPTED vừa TIMEOUT
- ⚠️ Match được tạo nhưng notification lại báo "hết hạn"
- ⚠️ Database inconsistency

**Có Lock:**
```java
Lock lock = challengeLocks.get(id);

// Thread A
lock.lock();  // ← LOCK ACQUIRED
try {
    if (challenge.getStatus() == PENDING) {
        challenge.setStatus(ACCEPTED);
        createDirectMatch(...);
    }
} finally {
    lock.unlock();  // ← UNLOCK
}

// Thread B (PHẢI CHỜ Thread A unlock)
lock.lock();  // ← CHỜ ĐỢI...
try {
    if (challenge.getStatus() == PENDING) {  // ❌ FALSE (đã ACCEPTED)
        // KHÔNG VÀO ĐÂY
    }
} finally {
    lock.unlock();
}
```

**Kết quả:**
- ✅ Thread B chờ Thread A xong
- ✅ Khi Thread B check, status đã là ACCEPTED → không set TIMEOUT
- ✅ Consistent!

---

### 3.3. Race Condition #2: Double Decline

**Kịch bản:**
1. Alice gửi challenge cho Bob
2. Bob click "Decline" 2 lần nhanh (double-click hoặc network lag)

**Không có Lock:**
```java
// Thread A (First decline)
if (challenge.getStatus() == PENDING) {  // ✅ Pass
    challenge.setStatus(DECLINED);
    notifyChallengeCancelled(...);
    cleanupChallenge(id);  // ← Xóa khỏi activeChallenges
}

// Thread B (Second decline) - CÙng LÚC
if (challenge.getStatus() == PENDING) {  // ✅ Pass (chưa kịp set)
    challenge.setStatus(DECLINED);
    notifyChallengeCancelled(...);       // ← DUPLICATE NOTIFICATION!
    cleanupChallenge(id);                // ← Cleanup 2 lần!
}
```

**Kết quả:**
- ⚠️ Alice nhận 2 notifications "DECLINED"
- ⚠️ Có thể NullPointerException khi cleanup lần 2

**Có Lock:**
```java
Lock lock = challengeLocks.get(id);

// Thread A
lock.lock();
try {
    if (challenge.getStatus() == PENDING) {
        challenge.setStatus(DECLINED);
        notifyChallengeCancelled(...);
        cleanupChallenge(id);
    }
} finally {
    lock.unlock();
}

// Thread B
lock.lock();  // ← CHỜ
try {
    if (challenge.getStatus() == PENDING) {  // ❌ FALSE
        // KHÔNG VÀO
    }
} finally {
    lock.unlock();
}
```

**Kết quả:**
- ✅ Chỉ 1 notification
- ✅ Cleanup 1 lần

---

### 3.4. Tại sao Matchmaking không cần Lock?

**Câu trả lời:** Matchmaking CŨNG CÓ LOCK! Nhưng ở level khác.

```java
// MatchmakingService.java
public synchronized boolean requestMatch(String userId) {  // ← synchronized method = lock toàn bộ service
    if (matchmakingQueue.contains(userId)) {
        return false;
    }
    matchmakingQueue.add(userId);
    return true;
}
```

**So sánh:**

| Feature | Matchmaking | Challenge |
|---------|-------------|-----------|
| **Lock scope** | Toàn bộ service (coarse-grained) | Từng challenge (fine-grained) |
| **Lock type** | `synchronized` method | `ReentrantLock` per challenge |
| **Concurrency** | Thấp (1 thread/lúc cho toàn bộ queue) | Cao (nhiều threads xử lý nhiều challenges) |
| **State** | Stateless (chỉ có queue) | Stateful (PENDING/ACCEPTED/TIMEOUT...) |

**Tại sao Challenge dùng fine-grained lock?**
- Nhiều challenges xảy ra **đồng thời** (Alice → Bob, Carol → Dave, Eve → Frank)
- Nếu dùng 1 lock chung → chỉ 1 challenge được xử lý/lúc → **bottleneck**
- Dùng lock riêng cho mỗi challenge → **parallel processing**

---

## 4. TIMEOUT MANAGEMENT (15 GIÂY)

### 4.1. Vai trò của ScheduledExecutorService

**ScheduledExecutorService** là một thread pool đặc biệt cho phép **schedule tasks chạy sau một khoảng thời gian**.

```java
private final ScheduledExecutorService scheduler;

// Constructor
public ChallengeService(..., ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
}
```

**Khởi tạo (trong CoreServer):**
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
    10,  // 10 threads cho timeout tasks
    r -> {
        Thread t = new Thread(r, "ChallengeTimeout-Thread");
        t.setDaemon(true);  // Daemon thread → không block JVM shutdown
        return t;
    }
);
```

---

### 4.2. ScheduledFuture - "Vé số" của timeout task

**ScheduledFuture** là một **reference** đến task đã được schedule, cho phép ta **cancel** nó.

```java
// Khi tạo challenge
ScheduledFuture<?> timeoutTask = scheduler.schedule(
    () -> handleChallengeTimeout(challengeId),  // ← Runnable: code chạy sau 15s
    CHALLENGE_TIMEOUT_SECONDS,                  // ← Delay: 15 giây
    TimeUnit.SECONDS                            // ← Unit: giây
);

// Lưu lại để có thể cancel sau này
timeoutTasks.put(challengeId, timeoutTask);
```

**Giải thích:**
- `scheduler.schedule(...)` → Đặt hẹn "chạy hàm `handleChallengeTimeout` sau 15 giây"
- Trả về `ScheduledFuture` → "Vé số" để cancel task này
- Lưu vào `timeoutTasks` → Để tìm lại khi cần cancel

---

### 4.3. Timeline chi tiết của Timeout

```
T=0s   │ Alice gửi challenge → Bob
       │ scheduler.schedule(handleTimeout, 15s)
       │ ScheduledFuture được tạo và lưu vào timeoutTasks
       │
       ↓ (Bob đang suy nghĩ...)
       │
T=5s   │ Bob click "Accept"
       │ cancelTimeoutTask(challengeId) được gọi
       │ ScheduledFuture.cancel(false) → Task bị hủy
       │ Task KHÔNG chạy nữa
       │
T=15s  │ (Không có gì xảy ra - task đã bị cancel)
```

**Nếu Bob KHÔNG response:**
```
T=0s   │ Alice gửi challenge → Bob
       │ scheduler.schedule(handleTimeout, 15s)
       │
       ↓ (Bob AFK - không làm gì)
       │
T=15s  │ ⏰ TIMEOUT TASK TRIGGER
       │ handleChallengeTimeout(challengeId) chạy
       │ Set status = TIMEOUT
       │ Gửi CHALLENGE_CANCELLED cho cả 2
       │ Cleanup challenge
```

---

### 4.4. cancelTimeoutTask() - Cơ chế hủy task

```java
private void cancelTimeoutTask(String challengeId) {
    ScheduledFuture<?> task = timeoutTasks.remove(challengeId);  // ← Lấy ra và xóa khỏi map
    if (task != null && !task.isDone()) {                        // ← Check task còn pending không
        task.cancel(false);                                      // ← Cancel (không interrupt)
        System.out.println("   ⏹️ Cancelled timeout task: " + challengeId);
    }
}
```

**Giải thích:**
1. `timeoutTasks.remove(challengeId)` → Lấy ScheduledFuture từ map (và xóa khỏi map)
2. `!task.isDone()` → Check task chưa chạy xong (nếu đã chạy thì không cần cancel)
3. `task.cancel(false)` → Cancel task
   - `false` = **mayInterruptIfRunning = false** → Không interrupt thread nếu đang chạy
   - Chỉ prevent task chạy **trong tương lai**

**Khi nào cancel được gọi?**
```java
// Case 1: Target accepts
public void handleChallengeResponse(String challengeId, boolean accept) {
    lock.lock();
    try {
        cancelTimeoutTask(challengeId);  // ← Cancel vì đã có response
        if (accept) {
            challenge.setStatus(ACCEPTED);
            createDirectMatch(...);
        } else {
            challenge.setStatus(DECLINED);
            notifyChallengeCancelled(...);
        }
    } finally {
        lock.unlock();
    }
}

// Case 2: Sender cancels
public void cancelChallenge(String challengeId, String reason) {
    lock.lock();
    try {
        cancelTimeoutTask(challengeId);  // ← Cancel vì user cancel
        notifyChallengeCancelled(..., reason);
    } finally {
        lock.unlock();
    }
}

// Case 3: User disconnects
public void handleUserDisconnect(String userId) {
    activeChallenges.values().forEach(challenge -> {
        if (userId.equals(challenge.getSenderId()) || userId.equals(challenge.getTargetId())) {
            cancelChallenge(challenge.getChallengeId(), "DISCONNECTED");  // ← Gọi cancelChallenge → cancel task
        }
    });
}
```

---

### 4.5. "Liệu có bị chồng lấn không?"

**Câu hỏi:** Nếu Bob accept đúng lúc T=15s (timeout cũng trigger), có bị conflict không?

**Câu trả lời:** KHÔNG, nhờ vào **Lock mechanism**!

```java
// T=14.99s: Bob click Accept
Thread A (Accept):
    lock.lock();  // ← LOCK ACQUIRED
    cancelTimeoutTask(challengeId);  // ← Cancel timeout task (task chưa chạy)
    challenge.setStatus(ACCEPTED);
    createDirectMatch(...);
    lock.unlock();  // ← UNLOCK

// T=15.00s: Timeout trigger (0.01s sau Bob accept)
Thread B (Timeout):
    lock.lock();  // ← PHẢI CHỜ Thread A unlock
    // KHI VÀO ĐÂY, challenge.getStatus() đã là ACCEPTED
    if (challenge.getStatus() == PENDING) {  // ❌ FALSE
        // KHÔNG VÀO
    }
    lock.unlock();
```

**Kết quả:**
- ✅ Accept được xử lý trước (vì có lock)
- ✅ Timeout check thấy status không còn PENDING → không làm gì
- ✅ Không có conflict!

**Trường hợp ngược lại:**
```java
// T=14.99s: Timeout trigger
Thread B (Timeout):
    lock.lock();  // ← LOCK ACQUIRED
    challenge.setStatus(TIMEOUT);
    notifyChallengeCancelled(...);
    cleanupChallenge(challengeId);
    lock.unlock();

// T=15.00s: Bob click Accept (muộn 0.01s)
Thread A (Accept):
    lock.lock();  // ← CHỜ Thread B
    // Khi vào, challenge đã bị cleanup
    if (challenge == null || challenge.getStatus() != PENDING) {  // ✅ TRUE
        throw new IllegalArgumentException("Challenge expired");
    }
```

**Kết quả:**
- ✅ Timeout được xử lý trước
- ✅ Accept bị reject với error "Challenge expired"
- ✅ Bob nhận thông báo "Thách đấu đã hết hạn"

---

### 4.6. Memory Management của Timeout Tasks

**Vấn đề:** Nếu không cleanup ScheduledFuture, có memory leak không?

**Câu trả lời:** CÓ (nếu không cleanup)!

```java
// ❌ BAD: Không cleanup
ScheduledFuture<?> task = scheduler.schedule(..., 15s);
// Task chạy xong sau 15s NHƯNG reference trong timeoutTasks vẫn còn
// → Memory leak (small, nhưng tích lũy nếu có 1000 challenges)

// ✅ GOOD: Cleanup
private void cleanupChallenge(String challengeId) {
    activeChallenges.remove(challengeId);
    challengeLocks.remove(challengeId);
    timeoutTasks.remove(challengeId);  // ← XÓA ScheduledFuture reference
}
```

**Khi nào cleanup?**
- ✅ Khi challenge kết thúc (accept/decline/timeout/cancel)
- ✅ Luôn gọi trong `finally` block

---

## 5. SO SÁNH VỚI MATCHMAKING

### 5.1. Tại sao Matchmaking không có session riêng?

| Aspect | Matchmaking | Challenge |
|--------|-------------|-----------|
| **State** | Stateless (chỉ có queue) | Stateful (PENDING → ACCEPTED/DECLINED/TIMEOUT) |
| **Lifetime** | Không xác định (có thể vài giây đến vài phút) | Cố định 15 giây |
| **Timeout** | Không có (chờ mãi cho đến khi có match) | Có (15 giây → cancel) |
| **Participants** | N users trong queue | 2 users (sender + target) |
| **Match creation** | Async (match khi có 2 users) | Sync (match ngay khi accept) |

**Matchmaking chỉ cần:**
```java
private final Set<String> matchmakingQueue = ConcurrentHashMap.newKeySet();
```

**Challenge cần:**
```java
private final ConcurrentHashMap<String, ChallengeSession> activeChallenges;
private final ConcurrentHashMap<String, Lock> challengeLocks;
private final ConcurrentHashMap<String, ScheduledFuture<?>> timeoutTasks;
```

**Lý do:**
- Matchmaking: "Ai đang chờ?" → Chỉ cần Set
- Challenge: "Challenge này đang ở trạng thái gì? Ai gửi? Ai nhận? Hết hạn khi nào?" → Cần object phức tạp

---

### 5.2. Tại sao không dùng SessionContext cho cả Challenge và Matchmaking?

**Câu trả lời:** Vì **SessionContext** là về USER, không phải về CHALLENGE hay MATCHMAKING.

```
SessionContext = "Alice đang làm gì?"
    → đang trong queue
    → đang trong challenge "ch-123"
    → đang chơi game "match-456"

ChallengeSession = "Challenge ch-123 diễn ra như thế nào?"
    → Alice gửi cho Bob
    → Status: PENDING
    → Hết hạn lúc 15:30:45
```

**Tương tự với Matchmaking:**
```
SessionContext = "Alice đang trong queue"
    → matchmakingQueue.contains("alice") = true

Matchmaking Queue = "Ai đang chờ?"
    → Set<String> {"alice", "charlie", "eve"}
```

---

## 6. RACE CONDITIONS VÀ EDGE CASES

### 6.1. Case 1: Sender Cancel + Target Accept (cùng lúc)

**Timeline:**
```
T=5s   │ Alice click "Cancel" → Thread A
       │ Bob click "Accept" → Thread B
       │ (Network delay 100ms)
       │
T=5.1s │ Thread A acquire lock → Set CANCELLED
       │ Thread B đang chờ lock...
       │
T=5.2s │ Thread A unlock
       │ Thread B acquire lock → Check status = CANCELLED → Throw error
```

**Kết quả:**
- ✅ Alice: Challenge cancelled thành công
- ✅ Bob: Nhận error "Challenge no longer valid"

---

### 6.2. Case 2: Double Timeout (2 timeout tasks cùng chạy)

**Có thể xảy ra không?**

**Câu trả lời:** KHÔNG, vì mỗi challenge chỉ có **1 timeout task duy nhất**.

```java
// Khi tạo challenge
ScheduledFuture<?> task1 = scheduler.schedule(..., 15s);
timeoutTasks.put(challengeId, task1);  // ← Lưu vào map

// Nếu tạo lại (không thể xảy ra vì challengeId unique)
ScheduledFuture<?> task2 = scheduler.schedule(..., 15s);
timeoutTasks.put(challengeId, task2);  // ← OVERWRITE task1 (task1 bị mất reference)
```

**Nhưng trong thực tế:**
- `challengeId` là UUID → không trùng
- Mỗi challenge chỉ được tạo 1 lần
- Khi cleanup, task bị remove khỏi map

---

### 6.3. Case 3: User disconnect trong lúc timeout đang chạy

**Timeline:**
```
T=0s   │ Alice gửi challenge → Bob
       │ Timeout task scheduled (15s)
       │
T=10s  │ Alice disconnect
       │ handleUserDisconnect("alice") called
       │ → cancelChallenge(challengeId, "SENDER_DISCONNECTED")
       │ → cancelTimeoutTask(challengeId)  ← Cancel task
       │ → cleanupChallenge(challengeId)
       │
T=15s  │ (Timeout task đã bị cancel → không chạy)
```

**Kết quả:**
- ✅ Timeout không chạy
- ✅ Bob nhận notification "SENDER_DISCONNECTED"

---

## 7. MEMORY MANAGEMENT

### 7.1. Vòng đời của ChallengeSession

```
CREATE:
    activeChallenges.put(id, session)
    challengeLocks.put(id, lock)
    timeoutTasks.put(id, future)
    senderCtx.setChallengeId(id)
    targetCtx.setChallengeId(id)

CLEANUP (sau 15s hoặc khi kết thúc):
    activeChallenges.remove(id)        // ← Remove ChallengeSession
    challengeLocks.remove(id)          // ← Remove Lock
    timeoutTasks.remove(id)            // ← Remove ScheduledFuture
    senderCtx.setChallengeId(null)     // ← Clear reference
    targetCtx.setChallengeId(null)     // ← Clear reference
```

**Tổng memory per challenge:**
```
ChallengeSession: ~200 bytes
    - challengeId: String (36 bytes)
    - senderId: String (10 bytes)
    - targetId: String (10 bytes)
    - status: enum (4 bytes)
    - timestamps: 2 long (16 bytes)

Lock: ~100 bytes (ReentrantLock overhead)
ScheduledFuture: ~50 bytes (wrapper object)

TOTAL: ~350 bytes per challenge
```

**Với 1000 challenges đồng thời:**
- Memory: ~350 KB (rất nhỏ)
- Cleanup sau tối đa 15 giây → không tích lũy

---

### 7.2. Garbage Collection

```java
// Khi cleanup
activeChallenges.remove(challengeId);
// → ChallengeSession không còn reference
// → GC sẽ thu hồi memory

challengeLocks.remove(challengeId);
// → ReentrantLock không còn reference
// → GC sẽ thu hồi

timeoutTasks.remove(challengeId);
// → ScheduledFuture không còn reference
// → GC sẽ thu hồi
```

**Kết luận:** Không có memory leak nếu cleanup đúng cách!

---

## 8. TESTING SCENARIOS

### 8.1. Test Case 1: Happy Path (Accept)

```java
@Test
public void testChallengeAccept() {
    // 1. Alice gửi challenge → Bob
    ChallengeSession challenge = challengeService.createChallenge("alice", "bob");
    assertEquals(ChallengeStatus.PENDING, challenge.getStatus());
    
    // 2. Verify timeout task scheduled
    assertTrue(timeoutTasks.containsKey(challenge.getChallengeId()));
    
    // 3. Bob accepts
    challengeService.handleChallengeResponse(challenge.getChallengeId(), true);
    
    // 4. Verify cleanup
    assertFalse(activeChallenges.containsKey(challenge.getChallengeId()));
    assertFalse(timeoutTasks.containsKey(challenge.getChallengeId()));
    
    // 5. Verify match created
    SessionContext aliceCtx = sessionManager.getSessionByUserId("alice");
    SessionContext bobCtx = sessionManager.getSessionByUserId("bob");
    assertNotNull(aliceCtx.getCurrentMatchId());
    assertEquals(aliceCtx.getCurrentMatchId(), bobCtx.getCurrentMatchId());
}
```

---

### 8.2. Test Case 2: Timeout

```java
@Test
public void testChallengeTimeout() throws InterruptedException {
    // 1. Alice gửi challenge → Bob
    ChallengeSession challenge = challengeService.createChallenge("alice", "bob");
    
    // 2. Không làm gì (Bob AFK)
    Thread.sleep(16000);  // Chờ 16 giây (vượt timeout)
    
    // 3. Verify timeout processed
    assertFalse(activeChallenges.containsKey(challenge.getChallengeId()));
    
    // 4. Verify notifications sent
    // (Check mock notification service)
}
```

---

### 8.3. Test Case 3: Concurrent Accept

```java
@Test
public void testConcurrentAccept() throws InterruptedException {
    ChallengeSession challenge = challengeService.createChallenge("alice", "bob");
    
    // Simulate 2 threads accepting simultaneously
    CountDownLatch latch = new CountDownLatch(2);
    AtomicInteger successCount = new AtomicInteger(0);
    
    Thread t1 = new Thread(() -> {
        try {
            challengeService.handleChallengeResponse(challenge.getChallengeId(), true);
            successCount.incrementAndGet();
        } catch (IllegalArgumentException e) {
            // Expected: second thread should fail
        } finally {
            latch.countDown();
        }
    });
    
    Thread t2 = new Thread(() -> {
        try {
            challengeService.handleChallengeResponse(challenge.getChallengeId(), true);
            successCount.incrementAndGet();
        } catch (IllegalArgumentException e) {
            // Expected
        } finally {
            latch.countDown();
        }
    });
    
    t1.start();
    t2.start();
    latch.await();
    
    // Only 1 thread should succeed
    assertEquals(1, successCount.get());
}
```

---

## 9. KẾT LUẬN

### 9.1. Tóm tắt các câu hỏi

| Câu hỏi | Trả lời ngắn gọn |
|---------|------------------|
| **Tại sao cần ChallengeSession riêng?** | Để tách biệt user session (dài hạn) và challenge state (ngắn hạn), dễ quản lý và cleanup |
| **Tại sao chỉ 15 giây mà phải quản lý phức tạp?** | Vì có nhiều race conditions (accept + timeout, double accept, disconnect...) cần xử lý |
| **Tại sao các chức năng khác không có session riêng?** | Matchmaking chỉ cần queue (stateless), Challenge cần track state (stateful) |
| **Tại sao cần Lock?** | Để tránh race conditions khi nhiều threads xử lý cùng 1 challenge |
| **Vai trò của Timeout Task?** | Auto cancel challenge sau 15 giây nếu không có response |
| **Có bị chồng lấn không?** | Không, nhờ Lock mechanism đảm bảo thread-safety |

---

### 9.2. Best Practices

✅ **Luôn cleanup trong finally block:**
```java
lock.lock();
try {
    // Process challenge
} finally {
    lock.unlock();
    cleanupChallenge(challengeId);
}
```

✅ **Cancel timeout task khi không cần:**
```java
cancelTimeoutTask(challengeId);  // Trước khi cleanup
```

✅ **Check status trước khi modify:**
```java
if (challenge.getStatus() != PENDING) {
    throw new IllegalArgumentException("Challenge no longer valid");
}
```

✅ **Use daemon threads cho scheduler:**
```java
Thread t = new Thread(r, "ChallengeTimeout-Thread");
t.setDaemon(true);  // Không block JVM shutdown
```

---

### 9.3. Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                      CHALLENGE SYSTEM                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  │
│  │SessionManager│   │ChallengeService│   │MatchmakingSvc│  │
│  │              │   │                │   │              │  │
│  │ Track users  │◄──┤ Manage states ├──►│ Create match │  │
│  │ (dài hạn)    │   │ (ngắn hạn)     │   │              │  │
│  └──────────────┘   └────────┬───────┘   └──────────────┘  │
│                              │                              │
│                              ▼                              │
│               ┌──────────────────────────┐                  │
│               │  ChallengeSession        │                  │
│               │  + Lock                  │                  │
│               │  + ScheduledFuture       │                  │
│               └──────────────────────────┘                  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │          ScheduledExecutorService (10 threads)      │  │
│  │  - Schedule timeout tasks                           │  │
│  │  - Auto cancel after 15 seconds                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 9.4. Điểm mấu chốt

🎯 **ChallengeSession tồn tại vì:**
- Challenge là một **entity riêng biệt** với lifecycle và state riêng
- Cần track **relationship giữa 2 users** (sender + target)
- Cần quản lý **timeout** và **cleanup**

🔒 **Lock tồn tại vì:**
- Nhiều threads có thể xử lý cùng 1 challenge (accept + timeout + cancel)
- Cần đảm bảo **atomic state transitions**

⏰ **Timeout mechanism tồn tại vì:**
- Challenge không thể chờ mãi → cần **deadline**
- Auto cleanup để tránh **memory leak**
- Đảm bảo UX tốt (user biết challenge hết hạn)

---

## 📚 TÀI LIỆU THAM KHẢO

- Java Concurrency in Practice (Brian Goetz)
- Effective Java (Joshua Bloch) - Item 66: Synchronize access to shared mutable data
- ScheduledExecutorService JavaDoc: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html
- ReentrantLock JavaDoc: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html

---

**Tài liệu này được viết với mục đích giáo dục. Mọi thắc mắc vui lòng liên hệ N9 Team.**

**© 2025 N9 Team - All Rights Reserved**
