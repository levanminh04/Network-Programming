// Hệ thống 36 lá bài (1-9, 4 chất: Cơ, Rô, Tép, Bích)

export const SUITS = {
  HEARTS: '♥',    // Cơ - màu đỏ
  DIAMONDS: '♦',  // Rô - màu đỏ  
  CLUBS: '♣',     // Tép - màu đen
  SPADES: '♠'     // Bích - màu đen
};

export const SUIT_COLORS = {
  [SUITS.HEARTS]: '#e74c3c',
  [SUITS.DIAMONDS]: '#e74c3c', 
  [SUITS.CLUBS]: '#2c3e50',
  [SUITS.SPADES]: '#2c3e50'
};

// Tạo bộ bài 36 lá
export const createDeck = () => {
  const deck = [];
  const suits = Object.values(SUITS);
  
  for (let suit of suits) {
    for (let value = 1; value <= 9; value++) {
      deck.push({
        id: `${suit}-${value}`,
        suit,
        value,
        displayValue: value,
        color: SUIT_COLORS[suit]
      });
    }
  }
  
  return deck;
};

// Xáo trộn bộ bài
export const shuffleDeck = (deck) => {
  const shuffled = [...deck];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
};

// Chia bài cho người chơi
export const dealCards = (deck, numPlayers = 2, cardsPerPlayer = 9) => {
  const hands = Array(numPlayers).fill().map(() => []);
  let deckIndex = 0;
  
  for (let round = 0; round < cardsPerPlayer; round++) {
    for (let player = 0; player < numPlayers; player++) {
      if (deckIndex < deck.length) {
        hands[player].push(deck[deckIndex]);
        deckIndex++;
      }
    }
  }
  
  return {
    hands,
    remainingDeck: deck.slice(deckIndex)
  };
};

// Lấy thông tin lá bài
export const getCardInfo = (card) => {
  return {
    ...card,
    suitName: Object.keys(SUITS).find(key => SUITS[key] === card.suit),
    isRed: card.suit === SUITS.HEARTS || card.suit === SUITS.DIAMONDS,
    isBlack: card.suit === SUITS.CLUBS || card.suit === SUITS.SPADES
  };
};
