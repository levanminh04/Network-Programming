package com.n9.shared.model.dto.challenge;

/**
 * DTO cho response từ target user (accept/decline).
 * 
 * Payload example:
 * {
 *   "challengeId": "ch-abc123",
 *   "accept": true
 * }
 */
public class ChallengeResponseDto {
    
    private String challengeId;
    private boolean accept;  // true = accept, false = decline
    
    // Constructors
    public ChallengeResponseDto() {}
    
    public ChallengeResponseDto(String challengeId, boolean accept) {
        this.challengeId = challengeId;
        this.accept = accept;
    }
    
    // Getters & Setters
    public String getChallengeId() {
        return challengeId;
    }
    
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }
    
    public boolean isAccept() {
        return accept;
    }
    
    public void setAccept(boolean accept) {
        this.accept = accept;
    }
}
