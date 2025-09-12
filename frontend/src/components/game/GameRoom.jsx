import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { gameAPI } from "../../services/game";
import { authAPI } from "../../services/auth";
import { createDeck, shuffleDeck, dealCards } from "../../utils/cards";
import Card from "../common/Card";

const GameRoom = () => {
  const { matchId } = useParams();
  const navigate = useNavigate();
  const [match, setMatch] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [opponent, setOpponent] = useState(null);
  const [gameState, setGameState] = useState({
    deck: [],
    playerHand: [],
    opponentHand: [],
    currentPlayer: null,
    gameStarted: false
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const user = authAPI.getCurrentUser();
    if (!user) {
      navigate("/login");
      return;
    }
    setCurrentUser(user);
    loadMatch();
  }, [matchId, navigate]);

  const loadMatch = async () => {
    try {
      const result = await gameAPI.getMatch(parseInt(matchId));
      if (result.success && result.match) {
        setMatch(result.match);
        // Tìm opponent
        const opponentId = result.match.players.find(id => id !== currentUser?.id);
        // Giả lập thông tin opponent
        setOpponent({
          id: opponentId,
          username: `Player_${opponentId}`,
          rating: 1200
        });
        startGame();
      } else {
        alert("Không tìm thấy trận đấu");
        navigate("/game");
      }
    } catch (error) {
      console.error("Lỗi tải trận đấu:", error);
      navigate("/game");
    } finally {
      setLoading(false);
    }
  };

  const startGame = () => {
    // Tạo và xáo trộn bộ bài
    const deck = shuffleDeck(createDeck());
    
    setGameState({
      deck: deck, // Toàn bộ bộ bài
      playerHand: [], // Bắt đầu với 0 lá
      opponentHand: [], // Bắt đầu với 0 lá
      currentPlayer: currentUser?.id,
      gameStarted: true
    });
  };

  const handleDeckClick = () => {
    if (gameState.deck.length === 0) {
      alert("Hết bài!");
      return;
    }

    const newCard = gameState.deck[0];
    const newDeck = gameState.deck.slice(1);
    
    setGameState(prev => ({
      ...prev,
      deck: newDeck,
      playerHand: [...prev.playerHand, newCard]
    }));
  };

  const drawCard = () => {
    if (gameState.deck.length === 0) {
      alert("Hết bài!");
      return;
    }

    const newCard = gameState.deck[0];
    const newDeck = gameState.deck.slice(1);
    
    setGameState(prev => ({
      ...prev,
      deck: newDeck,
      playerHand: [...prev.playerHand, newCard]
    }));
  };

  const handleCardClick = (card) => {
    if (!gameState.gameStarted) return;
    
    // Logic chơi bài (đơn giản)
    console.log("Chọn lá bài:", card);
    // TODO: Implement game logic
  };

  if (loading) {
    return (
      <div style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
      }}>
        <div style={{ color: "white", fontSize: "18px" }}>Đang tải trận đấu...</div>
      </div>
    );
  }

  return (
    <div style={{
      position: "fixed",
      inset: 0,
      minHeight: "100vh",
      background: "linear-gradient(135deg, #2c3e50 0%, #34495e 100%)",
      padding: "20px",
      display: "flex",
      flexDirection: "column"
    }}>
      {/* Header */}
      <div style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        marginBottom: "20px",
        color: "white"
      }}>
        <div>
          <h1 style={{ margin: 0, fontSize: "24px" }}>Trận đấu #{matchId}</h1>
          <p style={{ margin: 0, opacity: 0.8 }}>
            vs {opponent?.username} (Rating: {opponent?.rating})
          </p>
        </div>
        <button
          onClick={() => navigate("/game")}
          style={{
            padding: "10px 20px",
            background: "rgba(255, 255, 255, 0.2)",
            border: "1px solid rgba(255, 255, 255, 0.3)",
            borderRadius: "8px",
            color: "white",
            cursor: "pointer"
          }}
        >
          Thoát trận
        </button>
      </div>

      {/* Game Area */}
      <div style={{
        flex: 1,
        display: "grid",
        gridTemplateRows: "1fr 0.6fr 1.2fr",
        gap: "15px",
        minHeight: 0
      }}>
        {/* Opponent Area */}
        <div style={{
          background: "rgba(255, 255, 255, 0.1)",
          borderRadius: "15px",
          padding: "15px",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden"
        }}>
          <h3 style={{ color: "white", marginBottom: "15px" }}>
            {opponent?.username} ({gameState.opponentHand.length}/3 lá)
          </h3>
          <div style={{
            display: "flex",
            gap: "10px",
            flexWrap: "wrap"
          }}>
            {gameState.opponentHand.map((card, index) => (
              <div
                key={card.id}
                style={{
                  width: "60px",
                  height: "84px",
                  background: "linear-gradient(135deg, #667eea, #764ba2)",
                  border: "2px solid rgba(255, 255, 255, 0.3)",
                  borderRadius: "8px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "white",
                  fontSize: "24px"
                }}
              >
                ?
              </div>
            ))}
            {/* Hiển thị ô trống cho lá bài chưa rút */}
            {Array.from({ length: 3 - gameState.opponentHand.length }).map((_, index) => (
              <div
                key={`empty-${index}`}
                style={{
                  width: "60px",
                  height: "84px",
                  background: "rgba(255, 255, 255, 0.1)",
                  border: "2px dashed rgba(255, 255, 255, 0.3)",
                  borderRadius: "8px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "rgba(255, 255, 255, 0.5)",
                  fontSize: "16px"
                }}
              >
                Trống
              </div>
            ))}
          </div>
        </div>

        {/* Center Area - Game Info & Deck */}
        <div style={{
          background: "rgba(255, 255, 255, 0.1)",
          borderRadius: "15px",
          padding: "15px",
          textAlign: "center",
          color: "white",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden"
        }}>
          <h3>Trạng thái trận đấu</h3>
          <p>Lượt của: {gameState.currentPlayer === currentUser?.id ? "Bạn" : opponent?.username}</p>
          <p>Bài còn lại: {gameState.deck.length} lá</p>
          
          {/* Bộ bài trải ra */}
          <div style={{ flex: 1, display: "flex", flexDirection: "column", marginTop: "10px" }}>
            <h4 style={{ margin: "0 0 8px 0", fontSize: "16px" }}>Bộ bài ({gameState.deck.length} lá)</h4>
            <div style={{
              display: "flex",
              flexWrap: "wrap",
              gap: "4px",
              justifyContent: "center",
              flex: 1,
              overflowY: "auto",
              padding: "6px",
              background: "rgba(0, 0, 0, 0.2)",
              borderRadius: "8px",
              minHeight: 0,
              maxHeight: "120px"
            }}>
              {gameState.deck.map((card, index) => (
                <div
                  key={card.id}
                  onClick={handleDeckClick}
                  style={{
                    width: "32px",
                    height: "44px",
                    background: "linear-gradient(135deg, #667eea, #764ba2)",
                    border: "1px solid rgba(255, 255, 255, 0.3)",
                    borderRadius: "4px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "white",
                    fontSize: "10px",
                    cursor: "pointer",
                    transition: "all 0.2s ease",
                    transform: `rotate(${(index - gameState.deck.length/2) * 0.3}deg) translateY(${Math.sin(index * 0.2) * 1}px)`,
                    boxShadow: "0 1px 4px rgba(0,0,0,0.3)",
                    position: "relative",
                    zIndex: gameState.deck.length - index
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.transform = `rotate(${(index - gameState.deck.length/2) * 0.3}deg) translateY(-6px) scale(1.2)`;
                    e.currentTarget.style.zIndex = 1000;
                    e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.5)";
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = `rotate(${(index - gameState.deck.length/2) * 0.3}deg) translateY(${Math.sin(index * 0.2) * 1}px) scale(1)`;
                    e.currentTarget.style.zIndex = gameState.deck.length - index;
                    e.currentTarget.style.boxShadow = "0 1px 4px rgba(0,0,0,0.3)";
                  }}
                >
                  <div style={{ 
                    transform: "rotate(90deg)",
                    fontSize: "8px",
                    fontWeight: "bold"
                  }}>
                    {card.displayValue}{card.suit}
                  </div>
                </div>
              ))}
            </div>
            <p style={{ fontSize: "11px", margin: "5px 0 0 0", opacity: 0.8 }}>
              Click vào lá bài để rút
            </p>
          </div>
        </div>

        {/* Player Area */}
        <div style={{
          background: "rgba(255, 255, 255, 0.1)",
          borderRadius: "15px",
          padding: "15px",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden"
        }}>
          <h3 style={{ color: "white", marginBottom: "15px" }}>
            Bài của bạn ({gameState.playerHand.length}/3 lá)
          </h3>
          <div style={{
            display: "flex",
            gap: "10px",
            flexWrap: "wrap"
          }}>
            {gameState.playerHand.map((card) => (
              <Card
                key={card.id}
                card={card}
                onClick={() => handleCardClick(card)}
                style={{ cursor: "pointer" }}
              />
            ))}
            {/* Hiển thị ô trống cho lá bài chưa rút */}
            {Array.from({ length: 3 - gameState.playerHand.length }).map((_, index) => (
              <div
                key={`empty-${index}`}
                style={{
                  width: "60px",
                  height: "84px",
                  background: "rgba(255, 255, 255, 0.1)",
                  border: "2px dashed rgba(255, 255, 255, 0.3)",
                  borderRadius: "8px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "rgba(255, 255, 255, 0.5)",
                  fontSize: "16px"
                }}
              >
                Trống
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Game Controls */}
      <div style={{
        marginTop: "20px",
        display: "flex",
        justifyContent: "center",
        gap: "20px"
      }}>
        <button
          style={{
            padding: "12px 24px",
            background: "#e74c3c",
            color: "white",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontSize: "16px"
          }}
        >
          Bỏ lượt
        </button>
        <button
          onClick={drawCard}
          disabled={gameState.playerHand.length >= 3 || gameState.deck.length === 0}
          style={{
            padding: "12px 24px",
            background: gameState.playerHand.length >= 3 || gameState.deck.length === 0 ? "#95a5a6" : "#2ecc71",
            color: "white",
            border: "none",
            borderRadius: "8px",
            cursor: gameState.playerHand.length >= 3 || gameState.deck.length === 0 ? "not-allowed" : "pointer",
            fontSize: "16px"
          }}
        >
          {gameState.playerHand.length >= 3 ? "Đã đủ 3 lá" : "Rút bài tự động"}
        </button>
      </div>
    </div>
  );
};

export default GameRoom;
