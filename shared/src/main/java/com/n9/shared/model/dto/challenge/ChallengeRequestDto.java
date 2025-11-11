package com.n9.shared.model.dto.challenge;

/**
 * DTO cho request thách đấu từ client.
 * 
 * Payload example:
 * {
 *   "targetUserId": "101"
 * }
 */
public class ChallengeRequestDto {
    
    private String targetUserId;
    
    // Constructors
    public ChallengeRequestDto() {}
    
    public ChallengeRequestDto(String targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    // Getters & Setters
    public String getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }
}
