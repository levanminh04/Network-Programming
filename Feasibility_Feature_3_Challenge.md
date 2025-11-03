# ⚔️ Phân Tích Tính Khả Thi - Tính Năng 3: DIRECT CHALLENGE (Thách Đấu 1v1)

> **Tài liệu**: Feasibility Analysis - Direct Player Challenge System  
> **Tác giả**: Solution Architect & Senior Technical Advisor  
> **Ngày**: November 3, 2025  
> **Phiên bản**: 1.0.0  
> **Phụ thuộc**: 
> - ✅ Feature 1 (Leaderboard) - PHẢI HOÀN THÀNH
> - ✅ Feature 2 (Real-time Presence) - PHẢI HOÀN THÀNH
> 
> **Complexity Level**: ⭐⭐⭐⭐ (HIGH - Tính năng phức tạp nhất)

---

## 📋 MỤC LỤC

1. [Tổng Quan Tính Năng](#1-tổng-quan-tính-năng)
2. [Đánh Giá Khả Thi & Kiến Trúc Tổng Quan](#2-đánh-giá-khả-thi--kiến-trúc-tổng-quan)
3. [Protocol Design - Message Types Mới](#3-protocol-design---message-types-mới)
4. [State Machine - Vòng Đời Challenge](#4-state-machine---vòng-đời-challenge)
5. [Impact Analysis - Các Thành Phần Bị Ảnh Hưởng](#5-impact-analysis---các-thành-phần-bị-ảnh-hưởng)
6. [Luồng Dữ Liệu E2E - 4 Scenarios](#6-luồng-dữ-liệu-e2e---4-scenarios)
7. [Edge Cases & Error Handling](#7-edge-cases--error-handling)
8. [Implementation Plan Chi Tiết](#8-implementation-plan-chi-tiết)
9. [Estimation & Risk Assessment](#9-estimation--risk-assessment)
10. [Testing Strategy](#10-testing-strategy)
11. [Kết Luận & Khuyến Nghị](#11-kết-luận--khuyến-nghị)

---

## 1. TỔNG QUAN TÍNH NĂNG

### 🎯 Mô Tả

Người chơi có thể **thách đấu trực tiếp** một người chơi cụ thể (không qua matchmaking queue):

- Trên **Bảng Xếp Hạng**, bên cạnh người chơi **đang online**, có nút **"⚔️ Thách đấu"**
- Khi click, một **lời mời thách đấu** được gửi đến người chơi đó
- Người nhận có thể **Chấp nhận** hoặc **Từ chối**
- Nếu chấp nhận → Khởi tạo trận đấu **bypass matchmaking queue**

### 🎨 UI/UX Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    LEADERBOARD VIEW                         │
├────┬────┬──────────┬─────────┬──────┬───────┬──────────────┤
│ #  │ 🟢 │ USERNAME │ PLAYED  │ WON  │ RATE  │ ACTION       │
├────┼────┼──────────┼─────────┼──────┼───────┼──────────────┤
│ 1  │ 🟢 │ alice    │   50    │  42  │ 84%   │ [⚔️ Thách đấu]│ ← ONLINE
│ 2  │ ⚪ │ bob      │   35    │  28  │ 80%   │              │ ← OFFLINE (no button)
│ 3  │ 🟢 │ charlie  │   20    │  15  │ 75%   │ [⚔️ Thách đấu]│ ← ONLINE
└────┴────┴──────────┴─────────┴──────┴───────┴──────────────┘

User clicks "⚔️ Thách đấu" bên cạnh alice
    ↓
┌─────────────────────────────────────────────────────────────┐
│  Đang gửi lời mời thách đấu đến alice...                    │
│  [Hủy]                                                       │
└─────────────────────────────────────────────────────────────┘

Trên màn hình của alice (đang ở Lobby):
    ↓
┌─────────────────────────────────────────────────────────────┐
│  ⚔️ THÁCH ĐẤU                                               │
│  ────────────────────────────────────────────────────────    │
│  charlie muốn thách đấu bạn!                                │
│                                                              │
│  [✅ Chấp nhận]  [❌ Từ chối]                                │
│                                                              │
│  Thời gian còn lại: 15s                                     │
└─────────────────────────────────────────────────────────────┘

Nếu alice chấp nhận:
    ↓
[CẢ 2 USERS] → Chuyển sang GAME VIEW
              → Game bắt đầu (giống matchmaking thông thường)
```

### 📐 Scope MVP

**TRONG PHẠM VI (MVP Phase 3)**:
- ✅ Nút "Thách đấu" chỉ hiển thị cho users **đang online**
- ✅ Modal "Incoming Challenge" với Accept/Decline buttons
- ✅ Timeout 15 giây (nếu không trả lời → Auto decline)
- ✅ Ngăn chặn spam: Mỗi user chỉ gửi 1 challenge tại 1 thời điểm
- ✅ Validation: Không thể thách đấu nếu đang **trong queue** hoặc **đang chơi**
- ✅ Notification real-time qua WebSocket push

**NGOÀI PHẠM VI (Defer to Phase 4)**:
- ❌ Challenge history/log
- ❌ "Rematch" button sau game
- ❌ "Challenge declined" notification cho sender (chỉ timeout)
- ❌ ELO-based matching restrictions (chỉ thách đấu người cùng rank)
- ❌ Wager/Bet system
- ❌ Private message kèm challenge

---

## 2. ĐÁNH GIÁ KHẢ THI & KIẾN TRÚC TỔNG QUAN

### 🏗️ ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────┐
│                   CHALLENGE SYSTEM ARCHITECTURE                     │
└─────────────────────────────────────────────────────────────────────┘

[FRONTEND - User A]                      [FRONTEND - User B]
    │                                           │
    │ Click "Challenge alice"                   │ (Đang ở Lobby)
    │                                           │
    ▼                                           │
[SEND REQUEST]                                  │
    │                                           │
    ├─> WS: GAME.CHALLENGE_REQUEST              │
    │   { targetUserId: "101" }                 │
    │                                           │
    ▼                                           │
[GATEWAY]                                       │
    │                                           │
    ├─> TCP Forward                             │
    │                                           │
    ▼                                           │
[CORE - ClientConnectionHandler]                │
    │                                           │
    ├─> handleChallengeRequest()                │
    │   ↓                                       │
    │   [VALIDATION]                            │
    │   - Sender có đang free?                  │
    │   - Target có online?                     │
    │   - Target có đang free?                  │
    │   ↓                                       │
    │   [ChallengeService]                      │
    │   - Tạo ChallengeSession                  │
    │   - Store trong activeChallenges map      │
    │   - Schedule timeout task (15s)           │
    │   ↓                                       │
    │   [NOTIFY TARGET]                         │
    │   ├─> activeConnections.get("101")       │
    │   │   .sendMessage(CHALLENGE_OFFER)       │
    │   │                                       ▼
    │   │                                   [RECEIVE OFFER]
    │   │                                       │
    │   │                                   [SHOW MODAL]
    │   │                                       │
    │   │                           User B clicks "Accept"
    │   │                                       │
    │   │                                       ▼
    │   │                           [SEND RESPONSE]
    │   │                                       │
    │   │   ┌───────────────────────────────────┘
    │   │   │ WS: GAME.CHALLENGE_RESPONSE
    │   │   │ { accept: true }
    │   │   │
    │   │   ▼
    │   [CORE - handleChallengeResponse]
    │       ↓
    │   [ChallengeService]
    │   - Validate challenge còn hợp lệ?
    │   - Cancel timeout task
    │   - Remove từ activeChallenges
    │   ↓
    │   IF accept == true:
    │       ├─> MatchmakingService.createDirectMatch(A, B)
    │       │   ↓
    │       │   GameService.initializeGame(matchId, A, B)
    │       │   ↓
    │       │   [NOTIFY BOTH]
    │       │   - GAME_MATCH_FOUND
    │       │   - GAME_START
    │       │   - GAME_ROUND_START
    │   ELSE:
    │       └─> [NOTIFY SENDER] Challenge declined
    │
    ▼                                           ▼
[User A - GAME VIEW]                    [User B - GAME VIEW]
```

### ✅ TÀI SẢN SẴN CÓ (REUSE)

#### **1. SessionManager (Core)**

```java
// ĐÃ CÓ:
- userSessionMap<userId, SessionContext>  // Check online status
- SessionContext.getCurrentMatchId()      // Check if user is busy

// CẦN THÊM:
- SessionContext.currentChallengeId       // Track challenge state
- SessionContext.setChallengeId(String)
```

#### **2. MatchmakingService (Core)**

```java
// ĐÃ CÓ:
- Queue<String> matchmakingQueue
- Set<String> usersInQueue
- tryMatchmaking()  // Auto-pairing

// CẦN THÊM:
- createDirectMatch(String user1, String user2)  // Bypass queue
```

#### **3. activeConnections (Core)**

```java
// ĐÃ CÓ - PERFECT FOR PUSH NOTIFICATIONS:
ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;

// SỬ DỤNG:
activeConnections.get(targetUserId).sendMessage(challengeOfferJson);
```

---

### 🆕 THÀNH PHẦN MỚI CẦN TẠO

#### **1. ChallengeService (Core) - NEW**

**Trách nhiệm**:
- Quản lý **lifecycle** của challenges (create, accept, decline, timeout)
- Validate điều kiện thách đấu
- Store **activeChallenges** (in-memory map)
- Schedule timeout tasks

**API**:
```java
public class ChallengeService {
    
    // Data structures
    private final ConcurrentHashMap<String, ChallengeSession> activeChallenges;
    private final ConcurrentHashMap<String, Lock> challengeLocks;
    private final ScheduledExecutorService scheduler;
    
    // Dependencies
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    
    /**
     * Tạo challenge request từ sender → target.
     * @return challengeId nếu thành công, null nếu validation fail
     */
    public String createChallenge(String senderId, String targetId) throws IllegalArgumentException;
    
    /**
     * Xử lý response từ target (accept/decline).
     */
    public void handleChallengeResponse(String challengeId, boolean accept);
    
    /**
     * Hủy challenge (từ sender hoặc timeout).
     */
    public void cancelChallenge(String challengeId, String reason);
    
    /**
     * Kiểm tra user có đang trong challenge nào không.
     */
    public boolean isUserInChallenge(String userId);
}
```

#### **2. ChallengeSession (Model) - NEW**

```java
public class ChallengeSession {
    private final String challengeId;
    private final String senderId;
    private final String targetId;
    private final long createdAt;
    private final long expiresAt;
    private ChallengeStatus status;
    
    public enum ChallengeStatus {
        PENDING,    // Đang chờ target trả lời
        ACCEPTED,   // Target đã chấp nhận
        DECLINED,   // Target từ chối
        TIMEOUT,    // Hết thời gian
        CANCELLED   // Sender hủy
    }
}
```

---

## 3. PROTOCOL DESIGN - MESSAGE TYPES MỚI

### 📡 CẬP NHẬT MessageProtocol.java

```java
// File: shared/src/main/java/com/n9/shared/MessageProtocol.java

public static final class Type {
    
    // ... (existing types)
    
    // ============================
    // GAME DOMAIN - CHALLENGE
    // ============================
    
    /**
     * Client gửi yêu cầu thách đấu một người chơi cụ thể.
     * Payload: { "targetUserId": "101" }
     */
    public static final String GAME_CHALLENGE_REQUEST = "GAME.CHALLENGE_REQUEST";
    
    /**
     * Server xác nhận đã nhận request (gửi cho sender).
     * Payload: { "challengeId": "ch-123", "targetUsername": "alice", "status": "SENT" }
     */
    public static final String GAME_CHALLENGE_REQUEST_ACK = "GAME.CHALLENGE_REQUEST_ACK";
    
    /**
     * Server gửi lời mời thách đấu đến target user (PUSH notification).
     * Payload: { 
     *   "challengeId": "ch-123", 
     *   "senderUserId": "102",
     *   "senderUsername": "bob",
     *   "expiresAt": 1699... (timestamp)
     * }
     */
    public static final String GAME_CHALLENGE_OFFER = "GAME.CHALLENGE_OFFER";
    
    /**
     * Target user gửi response (accept/decline).
     * Payload: { "challengeId": "ch-123", "accept": true/false }
     */
    public static final String GAME_CHALLENGE_RESPONSE = "GAME.CHALLENGE_RESPONSE";
    
    /**
     * Server thông báo challenge đã bị hủy (timeout/cancelled).
     * Gửi cho CẢ 2 users.
     * Payload: { "challengeId": "ch-123", "reason": "TIMEOUT" | "CANCELLED" | "DECLINED" }
     */
    public static final String GAME_CHALLENGE_CANCELLED = "GAME.CHALLENGE_CANCELLED";
    
    /**
     * Server thông báo challenge thất bại (validation error).
     * Gửi cho sender.
     * Payload: { "reason": "TARGET_OFFLINE" | "TARGET_BUSY" | "SENDER_BUSY" }
     */
    public static final String GAME_CHALLENGE_FAILURE = "GAME.CHALLENGE_FAILURE";
}
```

**📊 Tổng Số Message Types Mới**: **6 types**

---

## 4. STATE MACHINE - VÒNG ĐỜI CHALLENGE

### 🔄 STATE TRANSITION DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────┐
│                  CHALLENGE STATE MACHINE                            │
└─────────────────────────────────────────────────────────────────────┘

                    [START]
                       │
                       │ User A clicks "Challenge"
                       ▼
                   ┌─────────┐
                   │ PENDING │  ← Đang chờ target trả lời
                   └─────────┘
                       │
         ┌─────────────┼─────────────┬──────────────┐
         │             │             │              │
    Target clicks  Target clicks  15s timeout   Sender clicks
    "Accept"       "Decline"                     "Cancel"
         │             │             │              │
         ▼             ▼             ▼              ▼
    ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
    │ACCEPTED │   │DECLINED │   │ TIMEOUT │   │CANCELLED│
    └─────────┘   └─────────┘   └─────────┘   └─────────┘
         │             │             │              │
         │             │             │              │
         │             └─────────────┴──────────────┘
         │                         │
         │                    [CLEANUP]
         │                    - Remove từ activeChallenges
         │                    - Notify users
         │                    - Clear session challenge IDs
         │
         ▼
    [CREATE MATCH]
    - MatchmakingService.createDirectMatch()
    - GameService.initializeGame()
    - Notify: GAME_MATCH_FOUND
    - Transition cả 2 users sang GAME view
         │
         ▼
      [END]
```

### 📊 STATE TRANSITION TABLE

| Current State | Event | Next State | Actions |
|---------------|-------|------------|---------|
| - | `CHALLENGE_REQUEST` | `PENDING` | 1. Validate sender & target<br>2. Create ChallengeSession<br>3. Store in activeChallenges<br>4. Send CHALLENGE_OFFER to target<br>5. Schedule timeout (15s)<br>6. Send REQUEST_ACK to sender |
| `PENDING` | `CHALLENGE_RESPONSE (accept=true)` | `ACCEPTED` | 1. Cancel timeout task<br>2. Create direct match<br>3. Initialize game<br>4. Send GAME_MATCH_FOUND to both<br>5. Cleanup challenge |
| `PENDING` | `CHALLENGE_RESPONSE (accept=false)` | `DECLINED` | 1. Cancel timeout task<br>2. Send CHALLENGE_CANCELLED to sender<br>3. Cleanup challenge |
| `PENDING` | `TIMEOUT (15s elapsed)` | `TIMEOUT` | 1. Send CHALLENGE_CANCELLED to both<br>2. Cleanup challenge |
| `PENDING` | `SENDER_CANCEL` | `CANCELLED` | 1. Cancel timeout task<br>2. Send CHALLENGE_CANCELLED to target<br>3. Cleanup challenge |
| `PENDING` | `SENDER_DISCONNECT` | `CANCELLED` | 1. Auto-cancel (trong cleanup)<br>2. Notify target |
| `PENDING` | `TARGET_DISCONNECT` | `CANCELLED` | 1. Auto-cancel<br>2. Notify sender |

---

## 5. IMPACT ANALYSIS - CÁC THÀNH PHẦN BỊ ẢNH HƯỞNG

### 📦 Sơ Đồ Tổng Quan

```
┌─────────────────────────────────────────────────────────────────────┐
│                    IMPACT MAP - FEATURE 3                           │
└─────────────────────────────────────────────────────────────────────┘

[SHARED MODULE]
  ├─ MessageProtocol.java                     [✏️ SỬA - Thêm 6 constants]
  └─ model/
      ├─ dto/challenge/
      │   ├─ ChallengeRequestDto.java         [🆕 TẠO MỚI]
      │   ├─ ChallengeOfferDto.java           [🆕 TẠO MỚI]
      │   └─ ChallengeResponseDto.java        [🆕 TẠO MỚI]
      └─ ChallengeSession.java                [🆕 TẠO MỚI]

[CORE MODULE]
  ├─ service/
  │   ├─ ChallengeService.java                [🆕 TẠO MỚI - 400 lines]
  │   ├─ SessionManager.java                  [✏️ SỬA - Thêm challengeId field]
  │   └─ MatchmakingService.java              [✏️ SỬA - Thêm createDirectMatch()]
  ├─ CoreServer.java                          [✏️ SỬA - Inject ChallengeService]
  └─ network/
      └─ ClientConnectionHandler.java         [✏️ SỬA - Thêm 3 handlers + cleanup]

[FRONTEND MODULE]
  ├─ services/
  │   └─ challenge.js                         [🆕 TẠO MỚI]
  ├─ components/
  │   ├─ lobby/
  │   │   ├─ LeaderboardModal.jsx             [✏️ SỬA - Thêm Challenge button]
  │   │   └─ IncomingChallengeModal.jsx       [🆕 TẠO MỚI]
  │   └─ common/
  │       └─ ChallengeStatusToast.jsx         [🆕 TẠO MỚI - Optional]
  └─ LobbyView or AppSingleFile.jsx           [✏️ SỬA - Handle challenge events]

[GATEWAY]
  └─ (KHÔNG ẢNH HƯỞNG - Transparent forwarding)

[DATABASE]
  └─ (KHÔNG ẢNH HƯỞNG - Challenge state chỉ lưu trong memory)
```

**📊 Tổng Số Files**:
- 🆕 **Tạo mới**: 7 files
- ✏️ **Sửa đổi**: 6 files
- **Total**: 13 files (phức tạp nhất trong 3 features)

---

### 📝 CHI TIẾT TỪNG FILE - BACKEND

#### **File 1: `ChallengeSession.java` (Shared Model) - NEW**

**📍 Vị trí**: `shared/src/main/java/com/n9/shared/model/ChallengeSession.java`

```java
package com.n9.shared.model;

public class ChallengeSession {
    
    private final String challengeId;
    private final String senderId;
    private final String targetId;
    private final long createdAt;
    private final long expiresAt;
    private ChallengeStatus status;
    
    public enum ChallengeStatus {
        PENDING,    // Đang chờ target trả lời
        ACCEPTED,   // Target đã chấp nhận
        DECLINED,   // Target từ chối
        TIMEOUT,    // Hết thời gian (auto-declined)
        CANCELLED   // Sender hủy
    }
    
    public ChallengeSession(String challengeId, String senderId, String targetId, int timeoutSeconds) {
        this.challengeId = challengeId;
        this.senderId = senderId;
        this.targetId = targetId;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + (timeoutSeconds * 1000L);
        this.status = ChallengeStatus.PENDING;
    }
    
    // Getters
    public String getChallengeId() { return challengeId; }
    public String getSenderId() { return senderId; }
    public String getTargetId() { return targetId; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public ChallengeStatus getStatus() { return status; }
    
    // Setters
    public void setStatus(ChallengeStatus status) { this.status = status; }
    
    // Utilities
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    public long getRemainingSeconds() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
```

**📊 Impact**: ⭐ (Thấp - Model đơn giản)

---

#### **File 2-4: DTOs (Shared) - NEW**

```java
// File: shared/model/dto/challenge/ChallengeRequestDto.java
package com.n9.shared.model.dto.challenge;

public class ChallengeRequestDto {
    private String targetUserId;
    
    // Getters, Setters, Constructors
}
```

```java
// File: shared/model/dto/challenge/ChallengeOfferDto.java
package com.n9.shared.model.dto.challenge;

public class ChallengeOfferDto {
    private String challengeId;
    private String senderUserId;
    private String senderUsername;
    private long expiresAt;        // Timestamp
    private int timeoutSeconds;    // 15
    
    // Getters, Setters, Constructors
}
```

```java
// File: shared/model/dto/challenge/ChallengeResponseDto.java
package com.n9.shared.model.dto.challenge;

public class ChallengeResponseDto {
    private String challengeId;
    private boolean accept;  // true = accept, false = decline
    
    // Getters, Setters, Constructors
}
```

**📊 Impact**: ⭐ (Thấp - DTOs đơn giản)

---

#### **File 5: `ChallengeService.java` (Core) - NEW (CRITICAL)**

**📍 Vị trí**: `core/src/main/java/com/n9/core/service/ChallengeService.java`

**⚠️ File phức tạp nhất - ~400 lines** - Tôi sẽ outline structure:

```java
package com.n9.core.service;

import com.n9.shared.model.ChallengeSession;
import com.n9.shared.model.dto.challenge.*;
import com.n9.shared.MessageProtocol;
import com.n9.shared.protocol.*;
import com.n9.shared.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ChallengeService {
    
    // ============================
    // CONSTANTS
    // ============================
    private static final int CHALLENGE_TIMEOUT_SECONDS = 15;
    
    // ============================
    // DATA STRUCTURES
    // ============================
    private final ConcurrentHashMap<String, ChallengeSession> activeChallenges;
    private final ConcurrentHashMap<String, Lock> challengeLocks;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> timeoutTasks;
    
    // ============================
    // DEPENDENCIES
    // ============================
    private final SessionManager sessionManager;
    private final MatchmakingService matchmakingService;
    private final ConcurrentHashMap<String, ClientConnectionHandler> activeConnections;
    private final ScheduledExecutorService scheduler;
    
    // ============================
    // CONSTRUCTOR
    // ============================
    public ChallengeService(
        SessionManager sessionManager,
        MatchmakingService matchmakingService,
        ConcurrentHashMap<String, ClientConnectionHandler> activeConnections,
        ScheduledExecutorService scheduler
    ) {
        this.sessionManager = sessionManager;
        this.matchmakingService = matchmakingService;
        this.activeConnections = activeConnections;
        this.scheduler = scheduler;
        this.activeChallenges = new ConcurrentHashMap<>();
        this.challengeLocks = new ConcurrentHashMap<>();
        this.timeoutTasks = new ConcurrentHashMap<>();
    }
    
    // ============================
    // PUBLIC API
    // ============================
    
    /**
     * Tạo challenge từ sender → target.
     * 
     * @throws IllegalArgumentException nếu validation fail
     * @return ChallengeSession nếu thành công
     */
    public ChallengeSession createChallenge(String senderId, String targetId) 
            throws IllegalArgumentException {
        
        // [1] VALIDATION
        validateChallengeRequest(senderId, targetId);
        
        // [2] CREATE CHALLENGE SESSION
        String challengeId = IdUtils.generateChallengeId();  // "ch-123..."
        ChallengeSession challenge = new ChallengeSession(
            challengeId, senderId, targetId, CHALLENGE_TIMEOUT_SECONDS
        );
        
        // [3] STORE
        activeChallenges.put(challengeId, challenge);
        challengeLocks.put(challengeId, new ReentrantLock());
        
        // [4] UPDATE SESSION CONTEXTS
        SessionManager.SessionContext senderCtx = sessionManager.getSessionByUserId(senderId);
        SessionManager.SessionContext targetCtx = sessionManager.getSessionByUserId(targetId);
        if (senderCtx != null) senderCtx.setChallengeId(challengeId);
        if (targetCtx != null) targetCtx.setChallengeId(challengeId);
        
        // [5] SCHEDULE TIMEOUT
        ScheduledFuture<?> timeoutTask = scheduler.schedule(
            () -> handleChallengeTimeout(challengeId),
            CHALLENGE_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        );
        timeoutTasks.put(challengeId, timeoutTask);
        
        // [6] SEND OFFER TO TARGET
        sendChallengeOfferToTarget(challenge);
        
        return challenge;
    }
    
    /**
     * Xử lý response từ target.
     */
    public void handleChallengeResponse(String challengeId, boolean accept) 
            throws IllegalArgumentException {
        
        Lock lock = challengeLocks.get(challengeId);
        if (lock == null) {
            throw new IllegalArgumentException("Challenge not found or expired.");
        }
        
        lock.lock();
        try {
            ChallengeSession challenge = activeChallenges.get(challengeId);
            if (challenge == null || challenge.getStatus() != ChallengeSession.ChallengeStatus.PENDING) {
                throw new IllegalArgumentException("Challenge no longer valid.");
            }
            
            // Cancel timeout
            cancelTimeoutTask(challengeId);
            
            if (accept) {
                // [ACCEPT PATH]
                challenge.setStatus(ChallengeSession.ChallengeStatus.ACCEPTED);
                createDirectMatch(challenge);
            } else {
                // [DECLINE PATH]
                challenge.setStatus(ChallengeSession.ChallengeStatus.DECLINED);
                notifyChallengeCancelled(challenge, "DECLINED");
            }
            
        } finally {
            lock.unlock();
            cleanupChallenge(challengeId);
        }
    }
    
    /**
     * Hủy challenge (từ sender hoặc system).
     */
    public void cancelChallenge(String challengeId, String reason) {
        Lock lock = challengeLocks.get(challengeId);
        if (lock == null) return;
        
        lock.lock();
        try {
            ChallengeSession challenge = activeChallenges.get(challengeId);
            if (challenge != null) {
                challenge.setStatus(ChallengeSession.ChallengeStatus.CANCELLED);
                cancelTimeoutTask(challengeId);
                notifyChallengeCancelled(challenge, reason);
            }
        } finally {
            lock.unlock();
            cleanupChallenge(challengeId);
        }
    }
    
    /**
     * Kiểm tra user có đang trong challenge không.
     */
    public boolean isUserInChallenge(String userId) {
        SessionManager.SessionContext ctx = sessionManager.getSessionByUserId(userId);
        return ctx != null && ctx.getChallengeId() != null;
    }
    
    // ============================
    // PRIVATE HELPERS
    // ============================
    
    private void validateChallengeRequest(String senderId, String targetId) 
            throws IllegalArgumentException {
        
        // [1] Target phải khác sender
        if (senderId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot challenge yourself.");
        }
        
        // [2] Target phải online
        if (!sessionManager.isUserOnline(targetId)) {
            throw new IllegalArgumentException("Target user is offline.");
        }
        
        // [3] Sender không được đang trong queue hoặc game
        SessionManager.SessionContext senderCtx = sessionManager.getSessionByUserId(senderId);
        if (senderCtx == null) {
            throw new IllegalArgumentException("Sender session not found.");
        }
        if (senderCtx.getCurrentMatchId() != null) {
            throw new IllegalArgumentException("You are already in a game.");
        }
        if (senderCtx.getChallengeId() != null) {
            throw new IllegalArgumentException("You already have an active challenge.");
        }
        if (matchmakingService.isUserInQueue(senderId)) {  // CẦN THÊM METHOD NÀY
            throw new IllegalArgumentException("You are in matchmaking queue. Please cancel first.");
        }
        
        // [4] Target không được đang busy
        SessionManager.SessionContext targetCtx = sessionManager.getSessionByUserId(targetId);
        if (targetCtx == null) {
            throw new IllegalArgumentException("Target session not found.");
        }
        if (targetCtx.getCurrentMatchId() != null) {
            throw new IllegalArgumentException("Target user is already in a game.");
        }
        if (targetCtx.getChallengeId() != null) {
            throw new IllegalArgumentException("Target user is already in a challenge.");
        }
        if (matchmakingService.isUserInQueue(targetId)) {
            throw new IllegalArgumentException("Target user is in matchmaking queue.");
        }
    }
    
    private void sendChallengeOfferToTarget(ChallengeSession challenge) {
        String targetId = challenge.getTargetId();
        String senderId = challenge.getSenderId();
        
        // Lấy username của sender
        SessionManager.SessionContext senderCtx = sessionManager.getSessionByUserId(senderId);
        String senderUsername = senderCtx != null ? senderCtx.getUsername() : "Unknown";
        
        // Tạo DTO
        ChallengeOfferDto offer = new ChallengeOfferDto();
        offer.setChallengeId(challenge.getChallengeId());
        offer.setSenderUserId(senderId);
        offer.setSenderUsername(senderUsername);
        offer.setExpiresAt(challenge.getExpiresAt());
        offer.setTimeoutSeconds(CHALLENGE_TIMEOUT_SECONDS);
        
        // Tạo message envelope
        MessageEnvelope envelope = MessageFactory.createNotification(
            MessageProtocol.Type.GAME_CHALLENGE_OFFER,
            offer
        );
        
        // Set sessionId
        SessionManager.SessionContext targetCtx = sessionManager.getSessionByUserId(targetId);
        if (targetCtx != null) {
            envelope.setSessionId(targetCtx.getSessionId());
        }
        
        // Send
        ClientConnectionHandler targetHandler = activeConnections.get(targetId);
        if (targetHandler != null) {
            try {
                targetHandler.sendMessage(JsonUtils.toJson(envelope));
                System.out.println("   Sent CHALLENGE_OFFER to " + targetId);
            } catch (Exception e) {
                System.err.println("   Failed to send CHALLENGE_OFFER: " + e.getMessage());
            }
        }
    }
    
    private void createDirectMatch(ChallengeSession challenge) {
        String senderId = challenge.getSenderId();
        String targetId = challenge.getTargetId();
        
        try {
            // Gọi MatchmakingService (method mới)
            matchmakingService.createDirectMatch(senderId, targetId);
            
            System.out.println("   Direct match created: " + senderId + " vs " + targetId);
            
        } catch (Exception e) {
            System.err.println("   Failed to create direct match: " + e.getMessage());
            // Notify both users về error
            notifyChallengeCancelled(challenge, "MATCH_CREATION_FAILED");
        }
    }
    
    private void handleChallengeTimeout(String challengeId) {
        Lock lock = challengeLocks.get(challengeId);
        if (lock == null) return;
        
        lock.lock();
        try {
            ChallengeSession challenge = activeChallenges.get(challengeId);
            if (challenge != null && challenge.getStatus() == ChallengeSession.ChallengeStatus.PENDING) {
                challenge.setStatus(ChallengeSession.ChallengeStatus.TIMEOUT);
                System.out.println("   Challenge timeout: " + challengeId);
                notifyChallengeCancelled(challenge, "TIMEOUT");
            }
        } finally {
            lock.unlock();
            cleanupChallenge(challengeId);
        }
    }
    
    private void notifyChallengeCancelled(ChallengeSession challenge, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("challengeId", challenge.getChallengeId());
        payload.put("reason", reason);
        
        MessageEnvelope envelope = MessageFactory.createNotification(
            MessageProtocol.Type.GAME_CHALLENGE_CANCELLED,
            payload
        );
        
        // Notify sender
        notifyUser(challenge.getSenderId(), envelope);
        
        // Notify target (if reason is not DECLINED - avoid redundant notification)
        if (!"DECLINED".equals(reason)) {
            notifyUser(challenge.getTargetId(), envelope);
        }
    }
    
    private void notifyUser(String userId, MessageEnvelope envelope) {
        SessionManager.SessionContext ctx = sessionManager.getSessionByUserId(userId);
        if (ctx != null) {
            envelope.setSessionId(ctx.getSessionId());
        }
        
        ClientConnectionHandler handler = activeConnections.get(userId);
        if (handler != null) {
            try {
                handler.sendMessage(JsonUtils.toJson(envelope));
            } catch (Exception e) {
                System.err.println("   Failed to notify user " + userId + ": " + e.getMessage());
            }
        }
    }
    
    private void cancelTimeoutTask(String challengeId) {
        ScheduledFuture<?> task = timeoutTasks.remove(challengeId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }
    
    private void cleanupChallenge(String challengeId) {
        ChallengeSession challenge = activeChallenges.remove(challengeId);
        challengeLocks.remove(challengeId);
        timeoutTasks.remove(challengeId);
        
        if (challenge != null) {
            // Clear challenge IDs from session contexts
            SessionManager.SessionContext senderCtx = sessionManager.getSessionByUserId(challenge.getSenderId());
            SessionManager.SessionContext targetCtx = sessionManager.getSessionByUserId(challenge.getTargetId());
            
            if (senderCtx != null && challengeId.equals(senderCtx.getChallengeId())) {
                senderCtx.setChallengeId(null);
            }
            if (targetCtx != null && challengeId.equals(targetCtx.getChallengeId())) {
                targetCtx.setChallengeId(null);
            }
            
            System.out.println("🧹 Cleaned up challenge: " + challengeId);
        }
    }
    
    /**
     * Cleanup khi user disconnect (gọi từ ClientConnectionHandler).
     */
    public void handleUserDisconnect(String userId) {
        // Tìm tất cả challenges có userId
        activeChallenges.values().forEach(challenge -> {
            if (userId.equals(challenge.getSenderId()) || userId.equals(challenge.getTargetId())) {
                String reason = userId.equals(challenge.getSenderId()) ? "SENDER_DISCONNECTED" : "TARGET_DISCONNECTED";
                cancelChallenge(challenge.getChallengeId(), reason);
            }
        });
    }
}
```

**📊 Impact**: ⭐⭐⭐⭐⭐ (Rất cao - Service phức tạp nhất, ~400 lines)

---

**🔔 LƯU Ý**: Do giới hạn độ dài response, tôi sẽ tiếp tục các files còn lại trong response tiếp theo. 

Bạn có muốn tôi:
1. ✅ **Tiếp tục ngay** với các files còn lại (SessionManager, MatchmakingService, Frontend...)
2. ⏸️ **Dừng lại** để review ChallengeService trước

Tôi khuyến nghị **tiếp tục ngay** để giữ mạch logic liền mạch! 😊

