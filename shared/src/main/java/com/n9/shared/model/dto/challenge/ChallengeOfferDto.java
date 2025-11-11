package com.n9.shared.model.dto.challenge;

/**
 * DTO cho lời mời thách đấu gửi đến target user (PUSH notification).
 * 
 * Payload example:
 * {
 *   "challengeId": "ch-abc123",
 *   "senderUserId": "102",
 *   "senderUsername": "bob",
 *   "expiresAt": 1730983456789,
 *   "timeoutSeconds": 15
 * }
 */
public class ChallengeOfferDto {
    
    private String challengeId;
    private String senderUserId;
    private String senderUsername;
    private long expiresAt;        // Timestamp (milliseconds)
    private int timeoutSeconds;    // Thời gian timeout (giây)
    
    // Constructors
    public ChallengeOfferDto() {}
    
    public ChallengeOfferDto(String challengeId, String senderUserId, String senderUsername, 
                             long expiresAt, int timeoutSeconds) {
        this.challengeId = challengeId;
        this.senderUserId = senderUserId;
        this.senderUsername = senderUsername;
        this.expiresAt = expiresAt;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    // Getters & Setters
    public String getChallengeId() {
        return challengeId;
    }
    
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }
    
    public String getSenderUserId() {
        return senderUserId;
    }
    
    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
