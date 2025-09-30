# Component Design & Architecture - Frontend Strategy

## 🎨 **COMPONENT ARCHITECTURE FRAMEWORK**

Comprehensive design system và component architecture cho **Game Rút Bài May Mắn** frontend với atomic design principles, reusable patterns, và scalable component hierarchy.

---

## 🏗️ **ATOMIC DESIGN METHODOLOGY**

### **Component Hierarchy Structure**

```
🔹 Atoms (Basic Building Blocks)
    ├── Button
    ├── Input
    ├── Icon
    ├── Avatar
    ├── Badge
    └── Spinner

🔸 Molecules (Simple Components Groups)
    ├── SearchBox (Input + Button)
    ├── UserProfile (Avatar + Badge + Text)
    ├── CardComponent (Card + Actions)
    ├── ScoreDisplay (Icon + Number + Label)
    └── PlayerStatus (Avatar + Status + Name)

🔷 Organisms (Complex UI Sections)
    ├── Header (Logo + Navigation + UserProfile)
    ├── GameBoard (PlayerHands + CenterArea + Controls)
    ├── PlayerHand (Multiple Cards + Actions)
    ├── Leaderboard (Multiple UserProfiles + Scores)
    └── GameLobby (PlayerList + GameSettings + Actions)

📄 Templates (Page Layouts)
    ├── MainLayout
    ├── GameLayout
    ├── AuthLayout
    └── ModalLayout

📃 Pages (Specific Instances)
    ├── HomePage
    ├── GamePage
    ├── LoginPage
    └── LeaderboardPage
```

---

## ⚛️ **ATOMS - FOUNDATIONAL COMPONENTS**

### **Button Component với Variants**

```jsx
// src/components/ui/Button/Button.jsx
import React from 'react';
import { cn } from '../../../utils/helpers';
import './Button.css';

const buttonVariants = {
  variant: {
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    success: 'btn-success',
    danger: 'btn-danger',
    warning: 'btn-warning',
    ghost: 'btn-ghost'
  },
  size: {
    sm: 'btn-sm',
    md: 'btn-md',
    lg: 'btn-lg',
    xl: 'btn-xl'
  },
  shape: {
    default: '',
    rounded: 'btn-rounded',
    circle: 'btn-circle'
  }
};

const Button = React.forwardRef(({
  children,
  variant = 'primary',
  size = 'md',
  shape = 'default',
  disabled = false,
  loading = false,
  icon,
  iconPosition = 'left',
  fullWidth = false,
  onClick,
  className,
  ...props
}, ref) => {
  const classes = cn(
    'btn',
    buttonVariants.variant[variant],
    buttonVariants.size[size],
    buttonVariants.shape[shape],
    {
      'btn-disabled': disabled,
      'btn-loading': loading,
      'btn-full-width': fullWidth,
      'btn-icon-right': iconPosition === 'right'
    },
    className
  );

  const handleClick = (e) => {
    if (!disabled && !loading && onClick) {
      onClick(e);
    }
  };

  return (
    <button
      ref={ref}
      className={classes}
      onClick={handleClick}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <span className="btn-spinner" />
      ) : (
        <>
          {icon && iconPosition === 'left' && (
            <span className="btn-icon btn-icon-left">{icon}</span>
          )}
          {children && <span className="btn-text">{children}</span>}
          {icon && iconPosition === 'right' && (
            <span className="btn-icon btn-icon-right">{icon}</span>
          )}
        </>
      )}
    </button>
  );
});

Button.displayName = 'Button';

export default Button;
```

### **Card Component với Animation Support**

```jsx
// src/components/ui/Card/Card.jsx
import React, { useState, useRef } from 'react';
import { cn } from '../../../utils/helpers';
import './Card.css';

const Card = ({
  suit,
  rank,
  faceDown = false,
  selected = false,
  disabled = false,
  playable = false,
  size = 'md',
  onClick,
  onHover,
  className,
  style,
  animationType = 'none', // 'flip', 'slide', 'bounce', 'glow'
  ...props
}) => {
  const [isAnimating, setIsAnimating] = useState(false);
  const cardRef = useRef(null);

  const getSuitSymbol = (suit) => {
    const symbols = {
      hearts: '♥',
      diamonds: '♦',
      clubs: '♣',
      spades: '♠'
    };
    return symbols[suit?.toLowerCase()] || '';
  };

  const getSuitColor = (suit) => {
    return ['hearts', 'diamonds'].includes(suit?.toLowerCase()) ? 'red' : 'black';
  };

  const handleClick = (e) => {
    if (disabled || !onClick) return;
    
    setIsAnimating(true);
    onClick(e, { suit, rank });
    
    // Reset animation after completion
    setTimeout(() => setIsAnimating(false), 300);
  };

  const handleMouseEnter = (e) => {
    if (onHover) {
      onHover(true, { suit, rank });
    }
  };

  const handleMouseLeave = (e) => {
    if (onHover) {
      onHover(false, { suit, rank });
    }
  };

  const classes = cn(
    'card',
    `card-${size}`,
    `card-${getSuitColor(suit)}`,
    {
      'card-face-down': faceDown,
      'card-selected': selected,
      'card-disabled': disabled,
      'card-playable': playable,
      'card-animating': isAnimating,
      [`card-animation-${animationType}`]: animationType !== 'none'
    },
    className
  );

  return (
    <div
      ref={cardRef}
      className={classes}
      onClick={handleClick}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      style={style}
      role="button"
      tabIndex={disabled ? -1 : 0}
      aria-label={faceDown ? 'Hidden card' : `${rank} of ${suit}`}
      {...props}
    >
      {faceDown ? (
        <div className="card-back">
          <div className="card-back-pattern" />
        </div>
      ) : (
        <div className="card-front">
          <div className="card-corner card-corner-top">
            <span className="card-rank">{rank}</span>
            <span className="card-suit">{getSuitSymbol(suit)}</span>
          </div>
          
          <div className="card-center">
            <span className="card-center-suit">{getSuitSymbol(suit)}</span>
          </div>
          
          <div className="card-corner card-corner-bottom">
            <span className="card-rank">{rank}</span>
            <span className="card-suit">{getSuitSymbol(suit)}</span>
          </div>
        </div>
      )}
      
      {playable && (
        <div className="card-glow-effect" />
      )}
      
      {selected && (
        <div className="card-selection-indicator" />
      )}
    </div>
  );
};

export default Card;
```

### **Input Component với Validation**

```jsx
// src/components/ui/Input/Input.jsx
import React, { useState, useRef, useEffect } from 'react';
import { cn } from '../../../utils/helpers';
import './Input.css';

const Input = React.forwardRef(({
  type = 'text',
  label,
  placeholder,
  value,
  defaultValue,
  onChange,
  onBlur,
  onFocus,
  error,
  helperText,
  disabled = false,
  required = false,
  readOnly = false,
  size = 'md',
  variant = 'default',
  icon,
  iconPosition = 'left',
  clearable = false,
  maxLength,
  pattern,
  autoComplete,
  autoFocus = false,
  className,
  inputClassName,
  ...props
}, ref) => {
  const [focused, setFocused] = useState(false);
  const [hasValue, setHasValue] = useState(Boolean(value || defaultValue));
  const inputRef = useRef(null);

  useEffect(() => {
    setHasValue(Boolean(value));
  }, [value]);

  const handleFocus = (e) => {
    setFocused(true);
    if (onFocus) onFocus(e);
  };

  const handleBlur = (e) => {
    setFocused(false);
    if (onBlur) onBlur(e);
  };

  const handleChange = (e) => {
    const newValue = e.target.value;
    setHasValue(Boolean(newValue));
    if (onChange) onChange(e);
  };

  const handleClear = () => {
    const input = inputRef.current;
    if (input) {
      input.value = '';
      setHasValue(false);
      
      // Trigger onChange event
      const event = new Event('input', { bubbles: true });
      input.dispatchEvent(event);
      
      input.focus();
    }
  };

  const inputClasses = cn(
    'input',
    `input-${size}`,
    `input-${variant}`,
    {
      'input-focused': focused,
      'input-error': error,
      'input-disabled': disabled,
      'input-has-icon': icon,
      'input-has-icon-right': iconPosition === 'right',
      'input-clearable': clearable && hasValue && !disabled && !readOnly
    },
    inputClassName
  );

  const containerClasses = cn(
    'input-container',
    {
      'input-container-focused': focused,
      'input-container-error': error,
      'input-container-disabled': disabled
    },
    className
  );

  return (
    <div className={containerClasses}>
      {label && (
        <label className="input-label">
          {label}
          {required && <span className="input-required">*</span>}
        </label>
      )}
      
      <div className="input-wrapper">
        {icon && iconPosition === 'left' && (
          <span className="input-icon input-icon-left">{icon}</span>
        )}
        
        <input
          ref={ref || inputRef}
          type={type}
          className={inputClasses}
          placeholder={placeholder}
          value={value}
          defaultValue={defaultValue}
          onChange={handleChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          disabled={disabled}
          readOnly={readOnly}
          required={required}
          maxLength={maxLength}
          pattern={pattern}
          autoComplete={autoComplete}
          autoFocus={autoFocus}
          {...props}
        />
        
        {icon && iconPosition === 'right' && (
          <span className="input-icon input-icon-right">{icon}</span>
        )}
        
        {clearable && hasValue && !disabled && !readOnly && (
          <button
            type="button"
            className="input-clear"
            onClick={handleClear}
            aria-label="Clear input"
          >
            ×
          </button>
        )}
      </div>
      
      {(error || helperText) && (
        <div className={cn('input-help', { 'input-help-error': error })}>
          {error || helperText}
        </div>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
```

---

## 🧬 **MOLECULES - COMPOSITE COMPONENTS**

### **PlayerStatus Component**

```jsx
// src/components/ui/PlayerStatus/PlayerStatus.jsx
import React from 'react';
import Avatar from '../Avatar/Avatar';
import Badge from '../Badge/Badge';
import { cn } from '../../../utils/helpers';
import './PlayerStatus.css';

const PlayerStatus = ({
  player,
  showStatus = true,
  showScore = true,
  showTurnIndicator = true,
  isCurrentTurn = false,
  isCurrentUser = false,
  size = 'md',
  layout = 'horizontal', // 'horizontal', 'vertical'
  onClick,
  className,
  ...props
}) => {
  const getStatusColor = (status) => {
    const colors = {
      online: 'success',
      playing: 'primary',
      waiting: 'warning',
      offline: 'secondary'
    };
    return colors[status] || 'secondary';
  };

  const getTurnIndicatorIcon = () => {
    return isCurrentTurn ? '🎯' : '';
  };

  const classes = cn(
    'player-status',
    `player-status-${size}`,
    `player-status-${layout}`,
    {
      'player-status-current-turn': isCurrentTurn,
      'player-status-current-user': isCurrentUser,
      'player-status-clickable': onClick
    },
    className
  );

  const handleClick = () => {
    if (onClick) {
      onClick(player);
    }
  };

  return (
    <div className={classes} onClick={handleClick} {...props}>
      <div className="player-status-avatar">
        <Avatar
          src={player.avatar}
          alt={player.username}
          size={size}
          status={showStatus ? player.status : null}
        />
        {isCurrentTurn && showTurnIndicator && (
          <div className="player-turn-indicator">
            {getTurnIndicatorIcon()}
          </div>
        )}
      </div>
      
      <div className="player-status-info">
        <div className="player-status-name">
          {player.username}
          {isCurrentUser && (
            <Badge variant="primary" size="sm" className="current-user-badge">
              You
            </Badge>
          )}
        </div>
        
        {showScore && player.score !== undefined && (
          <div className="player-status-score">
            Score: {player.score}
          </div>
        )}
        
        {showStatus && (
          <Badge
            variant={getStatusColor(player.status)}
            size="sm"
            className="player-status-badge"
          >
            {player.status}
          </Badge>
        )}
      </div>
    </div>
  );
};

export default PlayerStatus;
```

### **CardHand Component**

```jsx
// src/components/game/CardHand/CardHand.jsx
import React, { useState, useCallback } from 'react';
import Card from '../../ui/Card/Card';
import { cn } from '../../../utils/helpers';
import './CardHand.css';

const CardHand = ({
  cards = [],
  onCardSelect,
  onCardPlay,
  selectedCardIndex = null,
  playableCards = [],
  layout = 'fan', // 'fan', 'stack', 'grid'
  maxCards = 13,
  size = 'md',
  interactive = true,
  sortCards = true,
  className,
  ...props
}) => {
  const [hoveredCardIndex, setHoveredCardIndex] = useState(null);

  // Sort cards by suit and rank if enabled
  const sortedCards = sortCards ? [...cards].sort((a, b) => {
    const suitOrder = { spades: 0, hearts: 1, diamonds: 2, clubs: 3 };
    const rankOrder = { 
      A: 1, '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7,
      '8': 8, '9': 9, '10': 10, J: 11, Q: 12, K: 13
    };
    
    if (suitOrder[a.suit] !== suitOrder[b.suit]) {
      return suitOrder[a.suit] - suitOrder[b.suit];
    }
    return rankOrder[a.rank] - rankOrder[b.rank];
  }) : cards;

  const handleCardClick = useCallback((index, card) => {
    if (!interactive) return;
    
    if (selectedCardIndex === index) {
      // Double click to play card
      if (onCardPlay && playableCards.includes(index)) {
        onCardPlay(index, card);
      }
    } else {
      // Single click to select card
      if (onCardSelect) {
        onCardSelect(index, card);
      }
    }
  }, [interactive, selectedCardIndex, onCardSelect, onCardPlay, playableCards]);

  const handleCardHover = useCallback((index, isHovering) => {
    if (interactive) {
      setHoveredCardIndex(isHovering ? index : null);
    }
  }, [interactive]);

  const getCardStyle = (index, totalCards) => {
    if (layout === 'fan') {
      const angleStep = Math.min(60 / Math.max(totalCards - 1, 1), 10);
      const startAngle = -angleStep * (totalCards - 1) / 2;
      const angle = startAngle + angleStep * index;
      
      const radius = 20;
      const x = Math.sin(angle * Math.PI / 180) * radius;
      const y = Math.cos(angle * Math.PI / 180) * radius;
      
      return {
        transform: `translate(${x}px, ${y}px) rotate(${angle}deg)`,
        zIndex: index,
        marginRight: index < totalCards - 1 ? '-60px' : '0'
      };
    } else if (layout === 'stack') {
      return {
        marginRight: index < totalCards - 1 ? '-80px' : '0',
        zIndex: index
      };
    }
    return {};
  };

  const classes = cn(
    'card-hand',
    `card-hand-${layout}`,
    `card-hand-${size}`,
    {
      'card-hand-interactive': interactive,
      'card-hand-sortable': sortCards
    },
    className
  );

  return (
    <div className={classes} {...props}>
      <div className="card-hand-container">
        {sortedCards.slice(0, maxCards).map((card, index) => (
          <div
            key={`${card.suit}-${card.rank}-${index}`}
            className={cn('card-hand-slot', {
              'card-hand-slot-hovered': hoveredCardIndex === index,
              'card-hand-slot-selected': selectedCardIndex === index
            })}
            style={getCardStyle(index, sortedCards.length)}
          >
            <Card
              suit={card.suit}
              rank={card.rank}
              faceDown={card.faceDown}
              selected={selectedCardIndex === index}
              playable={playableCards.includes(index)}
              size={size}
              onClick={() => handleCardClick(index, card)}
              onHover={(isHovering) => handleCardHover(index, isHovering)}
              animationType={selectedCardIndex === index ? 'glow' : 'none'}
            />
          </div>
        ))}
      </div>
      
      {sortedCards.length > maxCards && (
        <div className="card-hand-overflow">
          +{sortedCards.length - maxCards} more
        </div>
      )}
    </div>
  );
};

export default CardHand;
```

---

## 🏛️ **ORGANISMS - COMPLEX COMPONENTS**

### **GameBoard Component**

```jsx
// src/components/game/GameBoard/GameBoard.jsx
import React, { useEffect, useState, useCallback } from 'react';
import { useSelector } from 'react-redux';
import CardHand from '../CardHand/CardHand';
import PlayerStatus from '../../ui/PlayerStatus/PlayerStatus';
import Button from '../../ui/Button/Button';
import { useGameState } from '../../../hooks/useGameState';
import { cn } from '../../../utils/helpers';
import './GameBoard.css';

const GameBoard = ({
  gameId,
  onGameAction,
  className,
  ...props
}) => {
  const { 
    currentGame, 
    playerHand, 
    playCard, 
    isMyTurn, 
    canPlayCard 
  } = useGameState();
  
  const [selectedCardIndex, setSelectedCardIndex] = useState(null);
  const [playableCards, setPlayableCards] = useState([]);
  const [isAnimating, setIsAnimating] = useState(false);

  // Calculate playable cards based on game rules
  useEffect(() => {
    if (!isMyTurn || !playerHand || !currentGame) {
      setPlayableCards([]);
      return;
    }

    // Game-specific logic để determine playable cards
    const currentSuit = currentGame.currentSuit;
    const playable = playerHand.map((card, index) => {
      // If player has cards of the current suit, must play that suit
      const hasSuit = playerHand.some(c => c.suit === currentSuit);
      if (hasSuit) {
        return card.suit === currentSuit ? index : null;
      }
      // Otherwise can play any card
      return index;
    }).filter(index => index !== null);

    setPlayableCards(playable);
  }, [isMyTurn, playerHand, currentGame]);

  const handleCardSelect = useCallback((index, card) => {
    if (!canPlayCard || !playableCards.includes(index)) {
      return;
    }
    
    setSelectedCardIndex(index === selectedCardIndex ? null : index);
  }, [canPlayCard, playableCards, selectedCardIndex]);

  const handleCardPlay = useCallback(async (index, card) => {
    if (!canPlayCard || !playableCards.includes(index)) {
      return;
    }

    setIsAnimating(true);
    
    try {
      await playCard(index);
      setSelectedCardIndex(null);
      
      // Trigger success animation
      setTimeout(() => {
        setIsAnimating(false);
      }, 1000);
    } catch (error) {
      console.error('Failed to play card:', error);
      setIsAnimating(false);
    }
  }, [canPlayCard, playableCards, playCard]);

  const handlePlaySelectedCard = useCallback(() => {
    if (selectedCardIndex !== null) {
      handleCardPlay(selectedCardIndex, playerHand[selectedCardIndex]);
    }
  }, [selectedCardIndex, handleCardPlay, playerHand]);

  if (!currentGame) {
    return (
      <div className="game-board-empty">
        <p>No active game</p>
      </div>
    );
  }

  const classes = cn(
    'game-board',
    {
      'game-board-my-turn': isMyTurn,
      'game-board-animating': isAnimating
    },
    className
  );

  return (
    <div className={classes} {...props}>
      {/* Opponent's Area */}
      <div className="game-board-opponent">
        <PlayerStatus
          player={currentGame.opponent}
          isCurrentTurn={!isMyTurn}
          showTurnIndicator
          size="lg"
        />
        
        <CardHand
          cards={currentGame.opponent.hand || []}
          layout="stack"
          size="sm"
          interactive={false}
          sortCards={false}
          className="opponent-hand"
        />
      </div>

      {/* Center Play Area */}
      <div className="game-board-center">
        <div className="play-area">
          {currentGame.lastPlayedCards && currentGame.lastPlayedCards.length > 0 && (
            <div className="last-played-cards">
              {currentGame.lastPlayedCards.map((card, index) => (
                <Card
                  key={`played-${index}`}
                  suit={card.suit}
                  rank={card.rank}
                  size="md"
                  className="played-card"
                />
              ))}
            </div>
          )}
          
          <div className="game-info">
            <div className="round-info">
              Round {currentGame.currentRound} / {currentGame.totalRounds}
            </div>
            {currentGame.currentSuit && (
              <div className="trump-suit">
                Trump: {currentGame.currentSuit}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Player's Area */}
      <div className="game-board-player">
        <CardHand
          cards={playerHand}
          onCardSelect={handleCardSelect}
          onCardPlay={handleCardPlay}
          selectedCardIndex={selectedCardIndex}
          playableCards={playableCards}
          layout="fan"
          size="md"
          interactive={canPlayCard}
          className="player-hand"
        />
        
        <div className="player-controls">
          <PlayerStatus
            player={currentGame.currentUser}
            isCurrentUser
            isCurrentTurn={isMyTurn}
            showTurnIndicator
            size="lg"
          />
          
          {isMyTurn && selectedCardIndex !== null && (
            <Button
              variant="primary"
              size="lg"
              onClick={handlePlaySelectedCard}
              disabled={isAnimating}
              loading={isAnimating}
            >
              Play Card
            </Button>
          )}
        </div>
      </div>

      {/* Turn Indicator */}
      {isMyTurn && (
        <div className="turn-indicator">
          <div className="turn-message">Your Turn!</div>
          {playableCards.length > 0 && (
            <div className="turn-hint">
              Select a card to play
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default GameBoard;
```

### **GameLobby Component**

```jsx
// src/components/game/GameLobby/GameLobby.jsx
import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import PlayerStatus from '../../ui/PlayerStatus/PlayerStatus';
import Button from '../../ui/Button/Button';
import Input from '../../ui/Input/Input';
import { useWebSocket } from '../../../hooks/useWebSocket';
import { cn } from '../../../utils/helpers';
import './GameLobby.css';

const GameLobby = ({
  onGameStart,
  onLeaveLobby,
  className,
  ...props
}) => {
  const dispatch = useDispatch();
  const { sendMessage, isConnected } = useWebSocket();
  
  const currentUser = useSelector(state => state.auth.user);
  const lobbyState = useSelector(state => state.lobby);
  
  const [gameSettings, setGameSettings] = useState({
    gameMode: 'QUICK',
    maxPlayers: 2,
    timeLimit: 300, // 5 minutes
    difficulty: 'NORMAL'
  });
  
  const [joinGameId, setJoinGameId] = useState('');
  const [isCreatingGame, setIsCreatingGame] = useState(false);
  const [isJoiningGame, setIsJoiningGame] = useState(false);

  useEffect(() => {
    // Listen for lobby updates
    if (isConnected) {
      sendMessage('GET_LOBBY_STATE', {});
    }
  }, [isConnected, sendMessage]);

  const handleCreateGame = async () => {
    if (!isConnected) return;
    
    setIsCreatingGame(true);
    
    try {
      const response = await sendMessage('CREATE_GAME', {
        settings: gameSettings
      });
      
      if (response.success) {
        // Game created successfully, wait for another player
        console.log('Game created:', response.gameId);
      }
    } catch (error) {
      console.error('Failed to create game:', error);
    } finally {
      setIsCreatingGame(false);
    }
  };

  const handleJoinGame = async () => {
    if (!isConnected || !joinGameId.trim()) return;
    
    setIsJoiningGame(true);
    
    try {
      const response = await sendMessage('JOIN_GAME', {
        gameId: joinGameId.trim()
      });
      
      if (response.success) {
        console.log('Joined game:', joinGameId);
      }
    } catch (error) {
      console.error('Failed to join game:', error);
    } finally {
      setIsJoiningGame(false);
    }
  };

  const handleQuickMatch = async () => {
    if (!isConnected) return;
    
    try {
      const response = await sendMessage('QUICK_MATCH', {
        preferences: {
          gameMode: gameSettings.gameMode,
          skillLevel: currentUser.skillLevel
        }
      });
      
      if (response.success) {
        console.log('Quick match started');
      }
    } catch (error) {
      console.error('Quick match failed:', error);
    }
  };

  const handleStartGame = async () => {
    if (!lobbyState.currentGame?.id) return;
    
    try {
      const response = await sendMessage('START_GAME', {
        gameId: lobbyState.currentGame.id
      });
      
      if (response.success && onGameStart) {
        onGameStart(lobbyState.currentGame.id);
      }
    } catch (error) {
      console.error('Failed to start game:', error);
    }
  };

  const handleLeaveGame = async () => {
    if (!lobbyState.currentGame?.id) return;
    
    try {
      await sendMessage('LEAVE_GAME', {
        gameId: lobbyState.currentGame.id
      });
      
      if (onLeaveLobby) {
        onLeaveLobby();
      }
    } catch (error) {
      console.error('Failed to leave game:', error);
    }
  };

  const canStartGame = lobbyState.currentGame && 
                      lobbyState.currentGame.players.length >= 2 &&
                      lobbyState.currentGame.hostId === currentUser.id;

  const classes = cn('game-lobby', className);

  return (
    <div className={classes} {...props}>
      <div className="lobby-header">
        <h2>Game Lobby</h2>
        <div className="connection-status">
          <span className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`} />
          {isConnected ? 'Connected' : 'Disconnected'}
        </div>
      </div>

      {!lobbyState.currentGame ? (
        <div className="lobby-actions">
          {/* Quick Match */}
          <div className="lobby-section">
            <h3>Quick Match</h3>
            <p>Find an opponent quickly với automatic matchmaking</p>
            <Button
              variant="primary"
              size="lg"
              onClick={handleQuickMatch}
              disabled={!isConnected}
              fullWidth
            >
              Find Match
            </Button>
          </div>

          {/* Create Game */}
          <div className="lobby-section">
            <h3>Create Game</h3>
            
            <div className="game-settings">
              <div className="setting-group">
                <label>Game Mode</label>
                <select
                  value={gameSettings.gameMode}
                  onChange={(e) => setGameSettings(prev => ({
                    ...prev,
                    gameMode: e.target.value
                  }))}
                  className="setting-select"
                >
                  <option value="QUICK">Quick Game</option>
                  <option value="RANKED">Ranked Match</option>
                  <option value="TOURNAMENT">Tournament</option>
                </select>
              </div>
              
              <div className="setting-group">
                <label>Time Limit</label>
                <select
                  value={gameSettings.timeLimit}
                  onChange={(e) => setGameSettings(prev => ({
                    ...prev,
                    timeLimit: parseInt(e.target.value)
                  }))}
                  className="setting-select"
                >
                  <option value={180}>3 minutes</option>
                  <option value={300}>5 minutes</option>
                  <option value={600}>10 minutes</option>
                  <option value={0}>No limit</option>
                </select>
              </div>
            </div>
            
            <Button
              variant="success"
              size="lg"
              onClick={handleCreateGame}
              loading={isCreatingGame}
              disabled={!isConnected || isCreatingGame}
              fullWidth
            >
              Create Game
            </Button>
          </div>

          {/* Join Game */}
          <div className="lobby-section">
            <h3>Join Game</h3>
            <div className="join-game-form">
              <Input
                placeholder="Enter Game ID"
                value={joinGameId}
                onChange={(e) => setJoinGameId(e.target.value)}
                disabled={!isConnected}
              />
              <Button
                variant="secondary"
                onClick={handleJoinGame}
                loading={isJoiningGame}
                disabled={!isConnected || !joinGameId.trim() || isJoiningGame}
              >
                Join
              </Button>
            </div>
          </div>
        </div>
      ) : (
        <div className="game-waiting">
          <div className="game-info">
            <h3>Game: {lobbyState.currentGame.id}</h3>
            <p>Mode: {lobbyState.currentGame.gameMode}</p>
            <p>Waiting for players...</p>
          </div>

          <div className="players-list">
            <h4>Players ({lobbyState.currentGame.players.length}/{lobbyState.currentGame.maxPlayers})</h4>
            {lobbyState.currentGame.players.map(player => (
              <PlayerStatus
                key={player.id}
                player={player}
                isCurrentUser={player.id === currentUser.id}
                size="lg"
                layout="horizontal"
              />
            ))}
          </div>

          <div className="waiting-actions">
            {canStartGame && (
              <Button
                variant="primary"
                size="lg"
                onClick={handleStartGame}
                fullWidth
              >
                Start Game
              </Button>
            )}
            
            <Button
              variant="secondary"
              size="lg"
              onClick={handleLeaveGame}
              fullWidth
            >
              Leave Game
            </Button>
          </div>
        </div>
      )}

      {/* Active Games List */}
      {lobbyState.availableGames && lobbyState.availableGames.length > 0 && (
        <div className="available-games">
          <h3>Available Games</h3>
          <div className="games-list">
            {lobbyState.availableGames.map(game => (
              <div key={game.id} className="game-item">
                <div className="game-item-info">
                  <span className="game-mode">{game.gameMode}</span>
                  <span className="player-count">
                    {game.players.length}/{game.maxPlayers}
                  </span>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setJoinGameId(game.id)}
                >
                  Join
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default GameLobby;
```

---

## 📱 **RESPONSIVE DESIGN PATTERNS**

### **Mobile-First Approach**

```css
/* src/styles/responsive.css */

/* Base Mobile Styles (320px+) */
.card-hand {
  --card-size: 60px;
  --card-spacing: -40px;
  --fan-radius: 15px;
}

.game-board {
  grid-template-areas: 
    "opponent"
    "center"
    "player";
  grid-template-rows: auto 1fr auto;
  padding: 1rem;
}

/* Tablet Styles (768px+) */
@media (min-width: 768px) {
  .card-hand {
    --card-size: 80px;
    --card-spacing: -50px;
    --fan-radius: 20px;
  }
  
  .game-board {
    padding: 1.5rem;
  }
  
  .game-lobby {
    grid-template-columns: 1fr 1fr;
    gap: 2rem;
  }
}

/* Desktop Styles (1024px+) */
@media (min-width: 1024px) {
  .card-hand {
    --card-size: 100px;
    --card-spacing: -60px;
    --fan-radius: 25px;
  }
  
  .game-board {
    grid-template-areas: 
      ". opponent ."
      "player center player-info"
      ". player-controls .";
    grid-template-columns: 1fr 2fr 1fr;
    padding: 2rem;
  }
}

/* Large Desktop Styles (1440px+) */
@media (min-width: 1440px) {
  .card-hand {
    --card-size: 120px;
    --card-spacing: -70px;
    --fan-radius: 30px;
  }
  
  .game-board {
    max-width: 1200px;
    margin: 0 auto;
  }
}
```

---

## 🎨 **DESIGN SYSTEM & THEMING**

### **CSS Custom Properties Setup**

```css
/* src/styles/design-system.css */

:root {
  /* Colors */
  --color-primary: #3b82f6;
  --color-primary-dark: #1d4ed8;
  --color-secondary: #6b7280;
  --color-success: #10b981;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
  
  /* Card Suits */
  --color-hearts: #dc2626;
  --color-diamonds: #dc2626;
  --color-clubs: #000000;
  --color-spades: #000000;
  
  /* Spacing */
  --spacing-xs: 0.25rem;
  --spacing-sm: 0.5rem;
  --spacing-md: 1rem;
  --spacing-lg: 1.5rem;
  --spacing-xl: 2rem;
  
  /* Typography */
  --font-family-base: 'Inter', sans-serif;
  --font-family-display: 'Poppins', sans-serif;
  --font-size-xs: 0.75rem;
  --font-size-sm: 0.875rem;
  --font-size-md: 1rem;
  --font-size-lg: 1.125rem;
  --font-size-xl: 1.25rem;
  
  /* Shadows */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  
  /* Transitions */
  --transition-fast: 150ms ease-in-out;
  --transition-normal: 300ms ease-in-out;
  --transition-slow: 500ms ease-in-out;
  
  /* Z-index Scale */
  --z-dropdown: 1000;
  --z-sticky: 1020;
  --z-fixed: 1030;
  --z-modal-backdrop: 1040;
  --z-modal: 1050;
  --z-popover: 1060;
  --z-tooltip: 1070;
}

/* Dark Theme */
[data-theme="dark"] {
  --color-background: #111827;
  --color-surface: #1f2937;
  --color-text: #f9fafb;
  --color-text-secondary: #d1d5db;
  --color-border: #374151;
}

/* Light Theme */
[data-theme="light"] {
  --color-background: #ffffff;
  --color-surface: #f9fafb;
  --color-text: #111827;
  --color-text-secondary: #6b7280;
  --color-border: #e5e7eb;
}
```

---

**Component architecture này provides scalable, maintainable, và reusable foundation cho entire frontend application. Next step: State management strategy với Redux Toolkit! 🏪**