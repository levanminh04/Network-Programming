import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { gameAPI } from "../../services/game";
import { authAPI } from "../../services/auth";

const Game = () => {
  const navigate = useNavigate();
  const [onlinePlayers, setOnlinePlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [matching, setMatching] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    const user = authAPI.getCurrentUser();
    if (!user) {
      navigate("/login");
      return;
    }
    setCurrentUser(user);
    loadOnlinePlayers();
  }, [navigate]);

  const loadOnlinePlayers = async () => {
    try {
      const result = await gameAPI.getOnlinePlayers();
      if (result.success) {
        setOnlinePlayers(result.players);
      }
    } catch (error) {
      console.error("Lỗi tải danh sách người chơi:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleFindMatch = async () => {
    if (!currentUser) return;
    
    setMatching(true);
    try {
      const result = await gameAPI.findRandomMatch(currentUser.id);
      if (result.success) {
        // Chuyển vào game room
        navigate(`/game-room/${result.match.id}`);
      } else {
        alert(result.message);
      }
    } catch (error) {
      alert("Lỗi tìm trận: " + error.message);
    } finally {
      setMatching(false);
    }
  };

  const handleDirectMatch = async (opponentId) => {
    if (!currentUser) return;
    
    try {
      const result = await gameAPI.createDirectMatch(currentUser.id, opponentId);
      if (result.success) {
        // Chuyển vào game room
        navigate(`/game-room/${result.match.id}`);
      }
    } catch (error) {
      alert("Lỗi tạo trận: " + error.message);
    }
  };

  if (loading) {
    return (
      <div style={{
        position: "fixed",
        inset: 0,
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
      }}>
        <div style={{ 
          color: "white", 
          fontSize: "24px",
          textAlign: "center"
        }}>
          <div style={{
            width: "60px",
            height: "60px",
            border: "4px solid rgba(255, 255, 255, 0.3)",
            borderTop: "4px solid white",
            borderRadius: "50%",
            animation: "spin 1s linear infinite",
            margin: "0 auto 20px"
          }} />
          Đang tải game lobby...
        </div>
      </div>
    );
  }

  return (
    <div style={{
      position: "fixed",
      inset: 0,
      minHeight: "100vh",
      background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
      padding: "20px"
    }}>
      <div style={{
        maxWidth: "1200px",
        margin: "0 auto",
        display: "grid",
        gridTemplateColumns: "1fr 300px",
        gap: "20px",
        height: "calc(100vh - 40px)"
      }}>
        {/* Khu vực chính */}
        <div style={{
          background: "rgba(255, 255, 255, 0.95)",
          borderRadius: "20px",
          padding: "30px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center"
        }}>
          <h1 style={{ 
            color: "#333", 
            marginBottom: "30px",
            fontSize: "32px",
            textAlign: "center"
          }}>
            Game Lobby
          </h1>
          
          <div style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "20px"
          }}>
            <button
              onClick={handleFindMatch}
              disabled={matching}
              style={{
                padding: "20px 50px",
                fontSize: "20px",
                background: matching ? "linear-gradient(135deg, #ff6b6b, #ee5a52)" : "linear-gradient(135deg, #ff4757, #ff3742)",
                color: "white",
                border: "none",
                borderRadius: "15px",
                cursor: matching ? "not-allowed" : "pointer",
                transition: "all 0.3s",
                boxShadow: matching 
                  ? "0 8px 25px rgba(255, 71, 87, 0.4), inset 0 0 20px rgba(255, 255, 255, 0.1)" 
                  : "0 4px 15px rgba(0,0,0,0.2)",
                transform: matching ? "scale(1.02)" : "scale(1)",
                position: "relative",
                overflow: "hidden"
              }}
            >
              {matching ? (
                <div style={{ 
                  display: "flex", 
                  alignItems: "center", 
                  gap: "15px",
                  position: "relative",
                  zIndex: 2
                }}>
                  <div style={{
                    width: "25px",
                    height: "25px",
                    border: "3px solid rgba(255, 255, 255, 0.3)",
                    borderTop: "3px solid white",
                    borderRadius: "50%",
                    animation: "spin 1s linear infinite"
                  }} />
                  <div>
                    <div style={{ fontSize: "18px", fontWeight: "bold" }}>Đang tìm đối thủ...</div>
                    <div style={{ fontSize: "14px", opacity: 0.8 }}>Vui lòng chờ trong giây lát</div>
                  </div>
                </div>
              ) : (
                "Tìm trận ngẫu nhiên"
              )}
              
              {/* Hiệu ứng ánh sáng khi matching */}
              {matching && (
                <div style={{
                  position: "absolute",
                  top: 0,
                  left: "-100%",
                  width: "100%",
                  height: "100%",
                  background: "linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent)",
                  animation: "shimmer 2s infinite"
                }} />
              )}
            </button>

            <div style={{ textAlign: "center", color: "#666" }}>
              <p>Hoặc chọn trực tiếp người chơi bên cạnh</p>
            </div>
          </div>
        </div>

        {/* Danh sách người chơi online */}
        <div style={{
          background: "rgba(255, 255, 255, 0.95)",
          borderRadius: "20px",
          padding: "20px",
          display: "flex",
          flexDirection: "column"
        }}>
          <h3 style={{ 
            color: "#333", 
            marginBottom: "20px",
            textAlign: "center",
            borderBottom: "2px solid #eee",
            paddingBottom: "10px"
          }}>
            Người chơi online ({onlinePlayers.length})
          </h3>
          
          <div 
            className="player-list"
            style={{
              flex: 1,
              overflowY: "auto",
              display: "flex",
              flexDirection: "column",
              gap: "10px",
              paddingRight: "4px",
              maxHeight: "400px"
            }}>
            {onlinePlayers.map((player) => (
              <div
                key={player.id}
                onClick={() => player.id !== currentUser?.id && handleDirectMatch(player.id)}
                style={{
                  padding: "15px",
                  background: player.id === currentUser?.id ? "#f0f0f0" : "#fff",
                  border: "2px solid #eee",
                  borderRadius: "12px",
                  cursor: player.id === currentUser?.id ? "default" : "pointer",
                  transition: "all 0.2s",
                  opacity: player.id === currentUser?.id ? 0.6 : 1
                }}
                onMouseEnter={(e) => {
                  if (player.id !== currentUser?.id) {
                    e.currentTarget.style.borderColor = "#667eea";
                    e.currentTarget.style.transform = "translateY(-2px)";
                  }
                }}
                onMouseLeave={(e) => {
                  if (player.id !== currentUser?.id) {
                    e.currentTarget.style.borderColor = "#eee";
                    e.currentTarget.style.transform = "translateY(0)";
                  }
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <div>
                    <div style={{ 
                      fontWeight: "bold", 
                      color: "#333",
                      fontSize: "16px"
                    }}>
                      {player.username}
                      {player.id === currentUser?.id && " (Bạn)"}
                    </div>
                    <div style={{ 
                      fontSize: "12px", 
                      color: "#666",
                      marginTop: "4px"
                    }}>
                      Rating: {player.rating} | Games: {player.gamesPlayed}
                    </div>
                  </div>
                  <div style={{
                    width: "12px",
                    height: "12px",
                    borderRadius: "50%",
                    background: "#2ed573"
                  }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Nút quay lại */}
      <div style={{
        position: "fixed",
        top: "20px",
        left: "20px"
      }}>
        <button
          onClick={() => navigate("/")}
          style={{
            padding: "10px 20px",
            background: "rgba(255, 255, 255, 0.9)",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontSize: "14px",
            color: "#333"
          }}
        >
          ← Quay lại
        </button>
      </div>

      {/* CSS cho animation loading và custom scroll */}
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        
        @keyframes shimmer {
          0% { left: -100%; }
          100% { left: 100%; }
        }
        
        /* Custom scrollbar cho danh sách người chơi */
        .player-list::-webkit-scrollbar {
          width: 12px;
        }
        
        .player-list::-webkit-scrollbar-track {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 6px;
          margin: 4px 0;
        }
        
        .player-list::-webkit-scrollbar-thumb {
          background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
          border-radius: 6px;
          border: 2px solid rgba(255, 255, 255, 0.1);
          transition: all 0.3s ease;
        }
        
        .player-list::-webkit-scrollbar-thumb:hover {
          background: linear-gradient(180deg, #5a6fd8 0%, #6a4190 100%);
          border: 2px solid rgba(255, 255, 255, 0.2);
        }
        
        .player-list::-webkit-scrollbar-thumb:active {
          background: linear-gradient(180deg, #4e5bc6 0%, #5e377e 100%);
        }
        
        /* Firefox scrollbar */
        .player-list {
          scrollbar-width: thin;
          scrollbar-color: #667eea rgba(255, 255, 255, 0.1);
        }
      `}</style>
    </div>
  );
};

export default Game;