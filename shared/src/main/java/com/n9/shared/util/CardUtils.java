package com.n9.shared.util;

import com.n9.shared.model.dto.game.CardDto;
import com.n9.shared.model.enums.CardRank;
import com.n9.shared.model.enums.CardSuit;
import com.n9.shared.constants.GameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;



public final class CardUtils {

    private static final Random random = new Random();

    private CardUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }



    public static List<CardDto> generateDeck() {
        List<CardDto> deck = new ArrayList<>(GameConstants.DECK_SIZE);
        int cardId = 1;
        for (CardSuit suit : CardSuit.values()) {
            for (CardRank rank : CardRank.values()) {
                CardDto card = new CardDto();
                card.setCardId(cardId);
                card.setSuit(suit);
                card.setRank(rank.getCode());
                card.setValue(rank.getValue());
                card.setIndex(cardId - 1);
                card.setDisplayName(rank.getCode() + suit.getSymbol());
                deck.add(card);
                cardId++;
            }
        }
        return deck;
    }

    public static void shuffle(List<CardDto> deck) {
        if (deck == null || deck.size() <= 1) return;
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(deck, i, j);
        }
    }


    public static boolean isValidCardId(int cardId) {
        return GameConstants.isValidCardId(cardId); // Giả sử GameConstants có hàm này
    }

    public static boolean isCardAvailable(List<CardDto> availableCards, int cardId) {
        if (isEmpty(availableCards)) return false;
        return availableCards.stream()
                .anyMatch(card -> card.getCardId() != null && card.getCardId() == cardId);
    }

    public static CardDto findCard(List<CardDto> cardList, int cardId) {
        if (isEmpty(cardList)) return null;
        return cardList.stream()
                .filter(card -> card.getCardId() != null && card.getCardId() == cardId)
                .findFirst()
                .orElse(null);
    }


    public static CardDto removeCard(List<CardDto> cardList, int cardId) {
        if (isEmpty(cardList)) return null;
        for (int i = 0; i < cardList.size(); i++) {
            // Thêm kiểm tra null an toàn cho cardId
            Integer currentCardId = cardList.get(i).getCardId();
            if (currentCardId != null && currentCardId == cardId) {
                return cardList.remove(i);
            }
        }
        return null;
    }


    public static CardDto findAndRemoveCard(List<CardDto> cardList, int cardId) {
        CardDto cardToRemove = findCard(cardList, cardId);
        if (cardToRemove != null) {
            boolean removed = cardList.remove(cardToRemove); // Dùng remove(Object) an toàn hơn remove(index)
            if(removed) {
                return cardToRemove;
            } else {
                // Lỗi không mong muốn nếu tìm thấy nhưng không xóa được
                System.err.println("Inconsistency: Found card " + cardId + " but failed to remove it.");
                return null;
            }
        }
        return null;
    }




    public static String formatCard(CardDto card) {
        if (card == null) return "??";
        // Cần đảm bảo CardRank và CardSuit có các hàm cần thiết
        CardRank rank = CardRank.fromCode(card.getRank()); // Giả sử có hàm fromCode
        CardSuit suit = card.getSuit();
        if (rank == null || suit == null) return "??";
        return rank.getCode() + suit.getSymbol(); // Giả sử có getSymbol
    }



    public static CardDto pickRandomCard(List<CardDto> cardList) {
        if (isEmpty(cardList)) return null;
        int index = random.nextInt(cardList.size());
        return cardList.get(index);
    }


    public static boolean isEmpty(List<CardDto> cardList) {
        return cardList == null || cardList.isEmpty();
    }
}