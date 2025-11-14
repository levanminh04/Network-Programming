package com.n9.core.service;

import com.n9.core.network.ClientConnectionHandler;
import com.n9.shared.MessageProtocol;
import com.n9.shared.model.ChallengeSession;
import com.n9.shared.model.dto.challenge.ChallengeOfferDto;
import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageFactory;
import com.n9.shared.util.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ChallengeService - Quản lý hệ thống thách đấu trực tiếp.
 * 
 * Chức năng:
 * - Tạo challenge request từ sender → target
 * - Xử lý response (accept/decline)
 * - Quản lý timeout (15 giây)
 * - Cleanup khi user disconnect
 *
 */
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
    

    /**
     * Tạo challenge từ sender → target.
     * 
     * @param senderId ID của người gửi challenge
     * @param targetId ID của người nhận challenge
     * @return ChallengeSession nếu thành công
     * @throws IllegalArgumentException nếu validation fail
     */
    public ChallengeSession createChallenge(String senderId, String targetId) 
            throws IllegalArgumentException {
        

        validateChallengeRequest(senderId, targetId);
        
        String challengeId = "ch-" + UUID.randomUUID().toString();
        ChallengeSession challenge = new ChallengeSession(
            challengeId, senderId, targetId, CHALLENGE_TIMEOUT_SECONDS
        );
        
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
     * 
     * @param challengeId ID của challenge
     * @param accept true = chấp nhận, false = từ chối
     * @throws IllegalArgumentException nếu challenge không hợp lệ
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
                // [ACCEPT]
                challenge.setStatus(ChallengeSession.ChallengeStatus.ACCEPTED);
                createDirectMatch(challenge);
            } else {
                // [DECLINE]
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
     * 
     * @param challengeId ID của challenge
     * @param reason Lý do hủy
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
     * 
     * @param userId ID của user
     * @return true nếu đang trong challenge
     */
    public boolean isUserInChallenge(String userId) {
        SessionManager.SessionContext ctx = sessionManager.getSessionByUserId(userId);
        return ctx != null && ctx.getChallengeId() != null;
    }
    
    /**
     * Cleanup khi user disconnect (gọi từ ClientConnectionHandler).
     * 
     * @param userId ID của user bị disconnect
     */
    public void handleUserDisconnect(String userId) {
        System.out.println("🔌 User disconnected, checking challenges: " + userId);
        
        // Tìm tất cả challenges có userId
        activeChallenges.values().forEach(challenge -> {
            if (userId.equals(challenge.getSenderId()) || userId.equals(challenge.getTargetId())) {
                String reason = userId.equals(challenge.getSenderId()) 
                    ? "SENDER_DISCONNECTED" 
                    : "TARGET_DISCONNECTED";
                cancelChallenge(challenge.getChallengeId(), reason);
            }
        });
    }
    
    /**
     * Alias method for handleUserDisconnect() - called from handleLogout().
     * 
     * @param userId ID của user
     */
    public void cleanupUserChallenges(String userId) {
        handleUserDisconnect(userId);
    }
    

    /**
     * Validate điều kiện thách đấu.
     */
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
        if (matchmakingService.isUserInQueue(senderId)) {
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
    
    /**
     * Gửi challenge offer đến target user.
     */
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
            } catch (Exception e) {
            }
        }
    }
    
    /**
     * Tạo direct match từ challenge.
     */
    private void createDirectMatch(ChallengeSession challenge) {
        String senderId = challenge.getSenderId();
        String targetId = challenge.getTargetId();
        
        try {
            // Gọi MatchmakingService để tạo match
            matchmakingService.createDirectMatch(senderId, targetId);
            

        } catch (Exception e) {
            e.printStackTrace();
            notifyChallengeCancelled(challenge, "MATCH_CREATION_FAILED");
        }
    }
    
    /**
     * Xử lý timeout (15 giây).
     */
    private void handleChallengeTimeout(String challengeId) {

        Lock lock = challengeLocks.get(challengeId);
        if (lock == null) return;
        
        lock.lock();
        try {
            ChallengeSession challenge = activeChallenges.get(challengeId);
            if (challenge != null && challenge.getStatus() == ChallengeSession.ChallengeStatus.PENDING) {
                challenge.setStatus(ChallengeSession.ChallengeStatus.TIMEOUT);
                notifyChallengeCancelled(challenge, "TIMEOUT");
            }
        } finally {
            lock.unlock();
            cleanupChallenge(challengeId);
        }
    }
    
    /**
     * Thông báo challenge đã bị hủy.
     */
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
        
        // Notify target (nếu reason không phải DECLINED - tránh notification trùng)
        if (!"DECLINED".equals(reason)) {
            notifyUser(challenge.getTargetId(), envelope);
        }
        
    }
    
    /**
     * Gửi notification đến 1 user.
     */
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
            }
        }
    }
    
    /**
     * Hủy timeout task.
     */
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
            SessionManager.SessionContext senderCtx = sessionManager.getSessionByUserId(challenge.getSenderId());
            SessionManager.SessionContext targetCtx = sessionManager.getSessionByUserId(challenge.getTargetId());
            
            if (senderCtx != null && challengeId.equals(senderCtx.getChallengeId())) {
                senderCtx.setChallengeId(null);
            }
            if (targetCtx != null && challengeId.equals(targetCtx.getChallengeId())) {
                targetCtx.setChallengeId(null);
            }
            
        }
    }
}
