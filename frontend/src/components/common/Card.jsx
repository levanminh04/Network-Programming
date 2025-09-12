import React from "react";
import { SUIT_COLORS } from "../../utils/cards";

const Card = ({ card, isSelected = false, onClick, style = {} }) => {
  const cardStyle = {
    width: "60px",
    height: "84px",
    background: "white",
    border: "2px solid #ddd",
    borderRadius: "8px",
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "4px",
    cursor: onClick ? "pointer" : "default",
    boxShadow: isSelected 
      ? "0 4px 12px rgba(0,0,0,0.3)" 
      : "0 2px 6px rgba(0,0,0,0.1)",
    transform: isSelected ? "translateY(-4px)" : "translateY(0)",
    transition: "all 0.2s ease",
    position: "relative",
    ...style
  };

  const valueStyle = {
    fontSize: "16px",
    fontWeight: "bold",
    color: SUIT_COLORS[card.suit],
    lineHeight: 1
  };

  const suitStyle = {
    fontSize: "20px",
    color: SUIT_COLORS[card.suit],
    lineHeight: 1
  };

  const centerSuitStyle = {
    fontSize: "24px",
    color: SUIT_COLORS[card.suit],
    lineHeight: 1
  };

  return (
    <div style={cardStyle} onClick={onClick}>
      {/* Góc trên trái */}
      <div style={{ alignSelf: "flex-start" }}>
        <div style={valueStyle}>{card.displayValue}</div>
        <div style={suitStyle}>{card.suit}</div>
      </div>
      
      {/* Giữa lá bài */}
      <div style={centerSuitStyle}>{card.suit}</div>
      
      {/* Góc dưới phải (xoay 180 độ) */}
      <div style={{ 
        alignSelf: "flex-end", 
        transform: "rotate(180deg)",
        transformOrigin: "center"
      }}>
        <div style={valueStyle}>{card.displayValue}</div>
        <div style={suitStyle}>{card.suit}</div>
      </div>
    </div>
  );
};

export default Card;
