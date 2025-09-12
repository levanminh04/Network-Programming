import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { authAPI } from "../../services/auth";

const MainMenu = () => {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    const user = authAPI.getCurrentUser();
    setCurrentUser(user);
  }, []);

  const handleLogout = () => {
    authAPI.logout();
    setCurrentUser(null);
  };

  const getMenuItems = () => {
    if (currentUser) {
      return [
        { key: "play", label: "Chơi ngay", path: "/game", accent: "#ff4757" },
        { key: "logout", label: "Đăng xuất", action: handleLogout, accent: "#e74c3c" },
        { key: "leaderboard", label: "Bảng xếp hạng", path: "/leaderboard", accent: "#ffa502" }
      ];
    } else {
      return [
        { key: "play", label: "Chơi ngay", path: "/game", accent: "#ff4757" },
        { key: "login", label: "Đăng nhập", path: "/login", accent: "#1e90ff" },
        { key: "register", label: "Đăng ký", path: "/register", accent: "#2ed573" },
        { key: "leaderboard", label: "Bảng xếp hạng", path: "/leaderboard", accent: "#ffa502" }
      ];
    }
  };

  const menuItems = getMenuItems();

  return (
    <div style={{
      position: "fixed",
      inset: 0,
      minHeight: "100vh",
      width: "100%",
      background: `
        linear-gradient(rgba(0,0,0,0.4), rgba(0,0,0,0.6)),
        url('/image/Gemini_Generated_Image_jx90ocjx90ocjx90.png')
      `,
      backgroundSize: "cover",
      backgroundPosition: "center",
      backgroundRepeat: "no-repeat",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "40px 20px",
      overflow: "hidden",
      zIndex: 0
    }}>
      {/* Local animations */}
      <style>{`
        @keyframes beamMove { 0% { transform: translateX(-20%) rotate(12deg); } 50% { transform: translateX(20%) rotate(12deg);} 100% { transform: translateX(-20%) rotate(12deg);} }
        @keyframes float { 0% { transform: translateY(0px); } 50% { transform: translateY(-10px);} 100% { transform: translateY(0px);} }
        @keyframes twinkle { 0% { opacity: 0.15; } 50% { opacity: 0.6; } 100% { opacity: 0.15; } }
        @keyframes gradientShift { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }
        @keyframes gridScroll { 0% { background-position: 0 0; } 100% { background-position: 600px 600px; } }
        @keyframes fireBorder { 0% { box-shadow: 0 0 20px #ff4500, 0 0 40px #ff6500, 0 0 60px #ff8500, inset 0 0 20px rgba(255, 69, 0, 0.1); } 25% { box-shadow: 0 0 25px #ff5500, 0 0 50px #ff7500, 0 0 75px #ff9500, inset 0 0 25px rgba(255, 85, 0, 0.15); } 50% { box-shadow: 0 0 30px #ff6500, 0 0 60px #ff8500, 0 0 90px #ffa500, inset 0 0 30px rgba(255, 101, 0, 0.2); } 75% { box-shadow: 0 0 25px #ff5500, 0 0 50px #ff7500, 0 0 75px #ff9500, inset 0 0 25px rgba(255, 85, 0, 0.15); } 100% { box-shadow: 0 0 20px #ff4500, 0 0 40px #ff6500, 0 0 60px #ff8500, inset 0 0 20px rgba(255, 69, 0, 0.1); } }
        @keyframes fireGlow { 0% { filter: drop-shadow(0 0 10px #ff4500) drop-shadow(0 0 20px #ff6500); } 50% { filter: drop-shadow(0 0 15px #ff5500) drop-shadow(0 0 30px #ff7500); } 100% { filter: drop-shadow(0 0 10px #ff4500) drop-shadow(0 0 20px #ff6500); } }
      `}</style>

      {/* Viền lửa animation xung quanh */}
      <div style={{
        position: "absolute",
        inset: 0,
        border: "4px solid transparent",
        borderRadius: "20px",
        background: "linear-gradient(45deg, #ff4500, #ff6500, #ff8500, #ffa500, #ff8500, #ff6500, #ff4500)",
        backgroundSize: "400% 400%",
        animation: "gradientShift 3s ease-in-out infinite",
        mask: "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
        maskComposite: "xor",
        WebkitMask: "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
        WebkitMaskComposite: "xor",
        pointerEvents: "none",
        zIndex: 1
      }} />
      
      {/* Viền lửa bên trong */}
      <div style={{
        position: "absolute",
        inset: "8px",
        border: "2px solid transparent",
        borderRadius: "16px",
        background: "linear-gradient(45deg, #ff4500, #ff6500, #ff8500, #ffa500, #ff8500, #ff6500, #ff4500)",
        backgroundSize: "400% 400%",
        animation: "gradientShift 3s ease-in-out infinite reverse",
        mask: "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
        maskComposite: "xor",
        WebkitMask: "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
        WebkitMaskComposite: "xor",
        pointerEvents: "none",
        zIndex: 1
      }} />

      {/* Animated soft gradient layer */}
      <div style={{
        position: "absolute",
        inset: 0,
        background: "linear-gradient(120deg, rgba(100,108,255,0.12), rgba(255,71,87,0.10), rgba(46,213,115,0.10))",
        backgroundSize: "400% 400%",
        mixBlendMode: "screen",
        animation: "gradientShift 22s ease-in-out infinite",
        pointerEvents: "none",
        zIndex: 0
      }} />

      {/* Subtle diagonal grid moving */}
      <div style={{
        position: "absolute",
        inset: 0,
        backgroundImage: "repeating-linear-gradient(45deg, rgba(255,255,255,0.03) 0 2px, transparent 2px 10px)",
        animation: "gridScroll 60s linear infinite",
        opacity: 0.35,
        pointerEvents: "none",
        zIndex: 0
      }} />

      {/* Moving light beams */}
      <div style={{
        position: "absolute",
        top: "-20%",
        left: "-10%",
        width: "140%",
        height: 220,
        background: "linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.12), transparent)",
        filter: "blur(24px)",
        animation: "beamMove 10s linear infinite",
        pointerEvents: "none",
        zIndex: 0
      }} />
      <div style={{
        position: "absolute",
        bottom: "-22%",
        right: "-10%",
        width: "140%",
        height: 220,
        background: "linear-gradient(90deg, transparent, rgba(46, 213, 115, 0.10), transparent)",
        filter: "blur(22px)",
        animation: "beamMove 12s linear infinite",
        pointerEvents: "none",
        zIndex: 0
      }} />

      {/* Twinkling particles */}
      <div style={{ position: "absolute", inset: 0, pointerEvents: "none", zIndex: 0 }}>
        {[...Array(36)].map((_, i) => (
          <span key={i} style={{
            position: "absolute",
            left: `${Math.random()*100}%`,
            top: `${Math.random()*100}%`,
            width: 3,
            height: 3,
            background: "#ffffff",
            borderRadius: "50%",
            opacity: 0.25,
            filter: "blur(0.5px)",
            animation: `twinkle ${4 + Math.random()*4}s ease-in-out ${Math.random()*2}s infinite`
          }} />
        ))}
      </div>

      <div style={{
        width: "min(1100px, 92vw)",
        display: "grid",
        gridTemplateColumns: menuItems.length === 3 
          ? "repeat(3, 1fr)" 
          : "repeat(auto-fit, minmax(220px, 1fr))",
        gap: "22px",
        alignItems: "stretch",
        justifyContent: "center",
        position: "relative",
        zIndex: 2
      }}>
        <div style={{
          gridColumn: "1 / -1",
          textAlign: "center",
          marginBottom: "10px"
        }}>
          <h1 style={{
            margin: 0,
            fontSize: "48px",
            letterSpacing: "6px",
            textTransform: "uppercase",
            color: "#e6edf3",
            textShadow: "0 2px 16px rgba(0,0,0,0.6)",
            animation: "fireGlow 2s ease-in-out infinite"
          }}>Rút Bài Online</h1>
          <p style={{
            marginTop: "6px",
            color: "#9fb3c8"
          }}>Trải nghiệm rút bài sống động</p>
        </div>

        {menuItems.map((item) => (
          <div
            key={item.key}
            onClick={() => item.action ? item.action() : navigate(item.path)}
            style={{
              userSelect: "none",
              cursor: "pointer",
              background: "linear-gradient(180deg, rgba(16,22,38,0.95) 0%, rgba(13,18,30,0.95) 100%)",
              border: `1px solid ${item.accent}33`,
              borderRadius: "14px",
              padding: "22px",
              boxShadow: `0 12px 28px -12px ${item.accent}40, inset 0 0 0 1px #ffffff08`,
              transition: "transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease",
              display: "flex",
              flexDirection: "column",
              justifyContent: "center",
              alignItems: "center",
              position: "relative",
              overflow: "hidden",
              transformStyle: "preserve-3d"
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = "translateY(-2px)";
              e.currentTarget.style.boxShadow = `0 18px 40px -16px ${item.accent}66, inset 0 0 0 1px #ffffff10`;
              e.currentTarget.style.borderColor = `${item.accent}66`;
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = "translateY(0px)";
              e.currentTarget.style.boxShadow = `0 12px 28px -12px ${item.accent}40, inset 0 0 0 1px #ffffff08`;
              e.currentTarget.style.borderColor = `${item.accent}33`;
            }}
            onMouseMove={(e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              const x = (e.clientX - rect.left) / rect.width - 0.5;
              const y = (e.clientY - rect.top) / rect.height - 0.5;
              const rotateX = (-y * 6).toFixed(2);
              const rotateY = (x * 6).toFixed(2);
              e.currentTarget.style.transform = `translateY(-2px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
            }}
          >
            <div style={{
              position: "absolute",
              inset: 0,
              background: `radial-gradient(400px 160px at 50% -40px, ${item.accent}22, transparent 60%)`,
              pointerEvents: "none"
            }} />

            <div style={{
              width: 64,
              height: 64,
              borderRadius: 12,
              background: `${item.accent}1a`,
              border: `1px solid ${item.accent}44`,
              display: "grid",
              placeItems: "center",
              marginBottom: 14,
              boxShadow: `inset 0 0 24px ${item.accent}22`
            }}>
              <div style={{
                width: 28,
                height: 36,
                borderRadius: 6,
                background: `linear-gradient(180deg, ${item.accent} 0%, ${item.accent}cc 100%)`,
                boxShadow: `0 8px 24px ${item.accent}55`
              }} />
            </div>

            <div style={{
              textAlign: "center"
            }}>
              <div style={{
                fontSize: 20,
                fontWeight: 700,
                letterSpacing: 2,
                textTransform: "uppercase",
                color: "#e6edf3"
              }}>{item.label}</div>
              <div style={{
                marginTop: 6,
                fontSize: 13,
                color: "#9fb3c8"
              }}>{item.key === "play" ? "Bắt đầu ván bài" : item.key === "leaderboard" ? "Người chơi dẫn đầu" : item.key === "login" ? "Chào mừng trở lại" : item.key === "logout" ? "Thoát khỏi tài khoản" : "Tạo tài khoản mới"}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MainMenu;