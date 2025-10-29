package com.n9.shared.model.dto.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

// THÊM import này
import java.util.List;

import java.util.Objects;

/**
 * Play Card Acknowledgement DTO
 * Confirmation that card selection was accepted. Also provides updated available cards.
 * Sent immediately after valid card selection.
 *
 * Message Type: GAME_CARD_PLAY_SUCCESS (Đã đổi tên type theo Protocol mới)
 *
 * @version 1.1.0 (Added availableCards for Shared Deck)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayCardAckDto {

    /** Game ID */
    @JsonProperty("gameId")
    private String gameId;

    /** Round number (1-3) */
    @JsonProperty("roundNumber")
    private Integer roundNumber; // Giữ lại nếu cần thiết, dù không bắt buộc trong ACK

    /** Selected card ID */
    @JsonProperty("cardId")
    private Integer cardId;

    /** Whether waiting for opponent to select */
    @JsonProperty("waitingForOpponent")
    private Boolean waitingForOpponent;

    // --- THÊM TRƯỜNG MỚI ---
    /** Updated list of available cards after the player's move */
    @JsonProperty("availableCards")
    private List<CardDto> availableCards;
    // ------------------------

    /** Acknowledgement timestamp (epoch milliseconds) */
    @JsonProperty("timestamp")
    private Long timestamp;

    /** Message for player */
    @JsonProperty("message")
    private String message;

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public PlayCardAckDto() {
        this.timestamp = System.currentTimeMillis();
        this.waitingForOpponent = true; // Giá trị mặc định hợp lý
        this.message = "Card selection confirmed."; // Tin nhắn mặc định
    }

    // Constructor có thể được đơn giản hóa hoặc giữ nguyên tùy nhu cầu
    public PlayCardAckDto(String gameId, Integer cardId) {
        this();
        this.gameId = gameId;
        this.cardId = cardId;
    }

    // ============================================================================
    // GETTERS & SETTERS (Thêm getter/setter cho availableCards)
    // ============================================================================

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }

    public Integer getCardId() { return cardId; }
    public void setCardId(Integer cardId) { this.cardId = cardId; }

    public Boolean getWaitingForOpponent() { return waitingForOpponent; }
    public void setWaitingForOpponent(Boolean waitingForOpponent) { this.waitingForOpponent = waitingForOpponent; }

    // --- GETTER & SETTER CHO TRƯỜNG MỚI ---
    public List<CardDto> getAvailableCards() { return availableCards; }
    public void setAvailableCards(List<CardDto> availableCards) { this.availableCards = availableCards; }
    // ------------------------------------

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // ============================================================================
    // OBJECT METHODS (Giữ nguyên hoặc cập nhật nếu cần)
    // ============================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayCardAckDto that = (PlayCardAckDto) o;
        // Có thể thêm availableCards vào equals nếu cần so sánh chính xác
        return Objects.equals(gameId, that.gameId) &&
                Objects.equals(roundNumber, that.roundNumber) && // Nên thêm roundNumber
                Objects.equals(cardId, that.cardId); // Nên thêm cardId
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, roundNumber, cardId); // Nên thêm roundNumber, cardId
    }

    @Override
    public String toString() {
        return "PlayCardAckDto{" +
                "gameId='" + gameId + '\'' +
                ", roundNumber=" + roundNumber +
                ", cardId=" + cardId +
                ", waitingForOpponent=" + waitingForOpponent +
                ", availableCardsCount=" + (availableCards != null ? availableCards.size() : "null") + // Thêm thông tin này
                '}';
    }
}