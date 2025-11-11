package com.n9.shared.model;

/**
 * ChallengeSession - Mô hình dữ liệu cho một lời thách đấu.
 * 
 * Lifecycle:
 * 1. PENDING - Đang chờ target trả lời
 * 2. ACCEPTED - Target chấp nhận → Tạo match
 * 3. DECLINED - Target từ chối
 * 4. TIMEOUT - Hết thời gian (15s)
 * 5. CANCELLED - Sender hủy hoặc disconnect
 * 
 * @author N9 Team
 * @version 1.0.0
 * @since 2025-11-07
 */
public class ChallengeSession {
    
    private final String challengeId;
    private final String senderId;
    private final String targetId;
    private final long createdAt;
    private final long expiresAt;
    private ChallengeStatus status;
    
    /**
     * Trạng thái của challenge.
     */
    public enum ChallengeStatus {
        /** Đang chờ target trả lời */
        PENDING,
        
        /** Target đã chấp nhận */
        ACCEPTED,
        
        /** Target từ chối */
        DECLINED,
        
        /** Hết thời gian (auto-declined) */
        TIMEOUT,
        
        /** Sender hủy hoặc disconnect */
        CANCELLED
    }
    
    /**
     * Constructor.
     * 
     * @param challengeId ID duy nhất của challenge
     * @param senderId ID của người gửi challenge
     * @param targetId ID của người nhận challenge
     * @param timeoutSeconds Thời gian timeout (giây)
     */
    public ChallengeSession(String challengeId, String senderId, String targetId, int timeoutSeconds) {
        this.challengeId = challengeId;
        this.senderId = senderId;
        this.targetId = targetId;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + (timeoutSeconds * 1000L);
        this.status = ChallengeStatus.PENDING;
    }
    
    // ============================
    // GETTERS
    // ============================
    
    public String getChallengeId() {
        return challengeId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public ChallengeStatus getStatus() {
        return status;
    }
    
    // ============================
    // SETTERS
    // ============================
    
    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }
    
    // ============================
    // UTILITIES
    // ============================
    
    /**
     * Kiểm tra challenge đã hết hạn chưa.
     * 
     * @return true nếu đã hết hạn
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * Lấy số giây còn lại trước khi timeout.
     * 
     * @return Số giây còn lại (>= 0)
     */
    public long getRemainingSeconds() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    @Override
    public String toString() {
        return String.format("ChallengeSession{id=%s, sender=%s, target=%s, status=%s, remaining=%ds}",
                challengeId, senderId, targetId, status, getRemainingSeconds());
    }
}
