# State Management Strategy - Redux Toolkit Architecture

## 🏪 **CENTRALIZED STATE MANAGEMENT**

Comprehensive state management architecture cho **Game Rút Bài May Mắn** frontend sử dụng Redux Toolkit với efficient data flow, real-time synchronization, và optimized performance patterns.

---

## 🏗️ **REDUX STORE ARCHITECTURE**

### **Store Structure Overview**

```
Redux Store
├── auth              # 🔐 Authentication & User Management
│   ├── user          # Current user data
│   ├── token         # JWT tokens
│   ├── permissions   # User permissions
│   └── loginState    # Login/logout status
├── game              # 🎮 Game State Management
│   ├── currentGame   # Active game data
│   ├── playerHand    # Player's cards
│   ├── gameHistory   # Previous games
│   └── gameSettings  # Game configuration
├── lobby             # 🚪 Lobby & Matchmaking
│   ├── availableGames # List of joinable games
│   ├── playerQueue   # Matchmaking queue
│   ├── roomState     # Current room status
│   └── invitations   # Game invitations
├── websocket         # 🔌 Real-time Communication
│   ├── connectionState # WebSocket connection status
│   ├── messageQueue  # Pending messages
│   ├── subscriptions # Event subscriptions
│   └── reconnectInfo # Reconnection data
├── ui                # 🎨 User Interface State
│   ├── modals        # Modal visibility state
│   ├── notifications # Toast notifications
│   ├── loading       # Loading indicators
│   └── theme         # Theme preferences
└── leaderboard       # 🏆 Rankings & Statistics
    ├── globalRankings # Global player rankings
    ├── playerStats   # Individual statistics
    ├── achievements  # Player achievements
    └── tournaments   # Tournament data
```

---

## ⚙️ **STORE CONFIGURATION**

### **Root Store Setup**

```javascript
// src/store/index.js
import { configureStore } from '@reduxjs/toolkit';
import { persistStore, persistReducer } from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import { combineReducers } from '@reduxjs/toolkit';

// Import slices
import authSlice from './slices/authSlice';
import gameSlice from './slices/gameSlice';
import lobbySlice from './slices/lobbySlice';
import websocketSlice from './slices/websocketSlice';
import uiSlice from './slices/uiSlice';
import leaderboardSlice from './slices/leaderboardSlice';

// Import middleware
import websocketMiddleware from './middleware/websocketMiddleware';
import persistenceMiddleware from './middleware/persistenceMiddleware';
import loggingMiddleware from './middleware/loggingMiddleware';

// Persist configuration
const persistConfig = {
  key: 'cardgame-root',
  storage,
  whitelist: ['auth', 'ui'], // Only persist auth and UI state
  blacklist: ['websocket', 'game', 'lobby'] // Don't persist real-time data
};

const authPersistConfig = {
  key: 'cardgame-auth',
  storage,
  whitelist: ['user', 'token', 'preferences']
};

const uiPersistConfig = {
  key: 'cardgame-ui',
  storage,
  whitelist: ['theme', 'settings', 'preferences']
};

// Root reducer
const rootReducer = combineReducers({
  auth: persistReducer(authPersistConfig, authSlice),
  game: gameSlice,
  lobby: lobbySlice,
  websocket: websocketSlice,
  ui: persistReducer(uiPersistConfig, uiSlice),
  leaderboard: leaderboardSlice
});

const persistedReducer = persistReducer(persistConfig, rootReducer);

// Configure store
export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE'],
        ignoredPaths: ['websocket.connection']
      }
    }).concat([
      websocketMiddleware,
      persistenceMiddleware,
      loggingMiddleware
    ]),
  devTools: process.env.NODE_ENV !== 'production'
});

export const persistor = persistStore(store);

// Export types for TypeScript
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
```

---

## 🔐 **AUTH SLICE - AUTHENTICATION MANAGEMENT**

```javascript
// src/store/slices/authSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { authService } from '../../services/api/authService';

// Async thunks
export const loginUser = createAsyncThunk(
  'auth/loginUser',
  async ({ username, password }, { rejectWithValue }) => {
    try {
      const response = await authService.login(username, password);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const registerUser = createAsyncThunk(
  'auth/registerUser',
  async (userData, { rejectWithValue }) => {
    try {
      const response = await authService.register(userData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const refreshToken = createAsyncThunk(
  'auth/refreshToken',
  async (_, { getState, rejectWithValue }) => {
    try {
      const { auth } = getState();
      const response = await authService.refreshToken(auth.refreshToken);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const logoutUser = createAsyncThunk(
  'auth/logoutUser',
  async (_, { getState }) => {
    const { auth } = getState();
    if (auth.token) {
      await authService.logout(auth.token);
    }
  }
);

// Initial state
const initialState = {
  user: null,
  token: null,
  refreshToken: null,
  isAuthenticated: false,
  loading: false,
  error: null,
  lastLoginTime: null,
  sessionExpiry: null,
  permissions: [],
  preferences: {
    theme: 'light',
    language: 'en',
    notifications: true,
    soundEnabled: true
  }
};

// Auth slice
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    
    updateUserPreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
    },
    
    updateUserProfile: (state, action) => {
      if (state.user) {
        state.user = { ...state.user, ...action.payload };
      }
    },
    
    setSessionExpiry: (state, action) => {
      state.sessionExpiry = action.payload;
    },
    
    clearAuth: (state) => {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.sessionExpiry = null;
      state.permissions = [];
    }
  },
  
  extraReducers: (builder) => {
    // Login
    builder
      .addCase(loginUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
        state.isAuthenticated = true;
        state.lastLoginTime = new Date().toISOString();
        state.sessionExpiry = action.payload.expiresAt;
        state.permissions = action.payload.permissions || [];
        state.error = null;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
        state.isAuthenticated = false;
      });

    // Register
    builder
      .addCase(registerUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
        state.isAuthenticated = true;
        state.lastLoginTime = new Date().toISOString();
        state.sessionExpiry = action.payload.expiresAt;
        state.permissions = action.payload.permissions || [];
        state.error = null;
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });

    // Token refresh
    builder
      .addCase(refreshToken.fulfilled, (state, action) => {
        state.token = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
        state.sessionExpiry = action.payload.expiresAt;
      })
      .addCase(refreshToken.rejected, (state) => {
        // Token refresh failed, clear auth
        state.user = null;
        state.token = null;
        state.refreshToken = null;
        state.isAuthenticated = false;
        state.sessionExpiry = null;
      });

    // Logout
    builder.addCase(logoutUser.fulfilled, (state) => {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.sessionExpiry = null;
      state.permissions = [];
    });
  }
});

// Export actions
export const {
  clearError,
  updateUserPreferences,
  updateUserProfile,
  setSessionExpiry,
  clearAuth
} = authSlice.actions;

// Selectors
export const selectAuth = (state) => state.auth;
export const selectUser = (state) => state.auth.user;
export const selectIsAuthenticated = (state) => state.auth.isAuthenticated;
export const selectAuthLoading = (state) => state.auth.loading;
export const selectAuthError = (state) => state.auth.error;
export const selectUserPreferences = (state) => state.auth.preferences;
export const selectUserPermissions = (state) => state.auth.permissions;
export const selectSessionExpiry = (state) => state.auth.sessionExpiry;

// Check if user has specific permission
export const selectHasPermission = (permission) => (state) => {
  return state.auth.permissions.includes(permission);
};

export default authSlice.reducer;
```

---

## 🎮 **GAME SLICE - GAME STATE MANAGEMENT**

```javascript
// src/store/slices/gameSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { gameService } from '../../services/api/gameService';

// Async thunks
export const createGame = createAsyncThunk(
  'game/createGame',
  async (gameSettings, { rejectWithValue }) => {
    try {
      const response = await gameService.createGame(gameSettings);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const joinGame = createAsyncThunk(
  'game/joinGame',
  async (gameId, { rejectWithValue }) => {
    try {
      const response = await gameService.joinGame(gameId);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const fetchGameHistory = createAsyncThunk(
  'game/fetchGameHistory',
  async (userId, { rejectWithValue }) => {
    try {
      const response = await gameService.getGameHistory(userId);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

// Initial state
const initialState = {
  currentGame: null,
  gameHistory: [],
  playerHand: [],
  opponentHand: [],
  playedCards: [],
  gameSettings: {
    gameMode: 'QUICK',
    timeLimit: 300,
    difficulty: 'NORMAL',
    maxPlayers: 2
  },
  gameStats: {
    totalGames: 0,
    wins: 0,
    losses: 0,
    winRate: 0,
    currentStreak: 0,
    bestStreak: 0
  },
  loading: false,
  error: null,
  actionInProgress: false,
  lastAction: null,
  roundHistory: []
};

// Game slice
const gameSlice = createSlice({
  name: 'game',
  initialState,
  reducers: {
    // Game state updates from WebSocket
    updateGameState: (state, action) => {
      const { gameData } = action.payload;
      state.currentGame = { ...state.currentGame, ...gameData };
    },
    
    setPlayerHand: (state, action) => {
      state.playerHand = action.payload;
    },
    
    setOpponentHand: (state, action) => {
      state.opponentHand = action.payload;
    },
    
    addPlayedCard: (state, action) => {
      const { playerId, card, roundNumber } = action.payload;
      state.playedCards.push({
        playerId,
        card,
        roundNumber,
        timestamp: new Date().toISOString()
      });
    },
    
    updatePlayerAction: (state, action) => {
      const { playerId, action: actionType, cardIndex, roundNumber } = action.payload;
      
      state.lastAction = {
        playerId,
        actionType,
        cardIndex,
        roundNumber,
        timestamp: new Date().toISOString()
      };
      
      // Remove card from player hand if it's the current player
      if (playerId === state.currentGame?.currentUserId && cardIndex !== undefined) {
        state.playerHand.splice(cardIndex, 1);
      }
    },
    
    updateRoundResult: (state, action) => {
      const { roundNumber, winner, scores, cards } = action.payload;
      
      state.roundHistory.push({
        roundNumber,
        winner,
        scores,
        cards,
        timestamp: new Date().toISOString()
      });
      
      // Update current game round
      if (state.currentGame) {
        state.currentGame.currentRound = roundNumber + 1;
        state.currentGame.scores = scores;
      }
      
      // Clear played cards for new round
      state.playedCards = [];
    },
    
    setGameResult: (state, action) => {
      const { winner, finalScores, gameStats } = action.payload;
      
      if (state.currentGame) {
        state.currentGame.status = 'COMPLETED';
        state.currentGame.winner = winner;
        state.currentGame.finalScores = finalScores;
        state.currentGame.completedAt = new Date().toISOString();
      }
      
      // Update game statistics
      state.gameStats = { ...state.gameStats, ...gameStats };
      
      // Add to game history
      if (state.currentGame) {
        state.gameHistory.unshift({
          ...state.currentGame,
          roundHistory: [...state.roundHistory]
        });
      }
      
      // Keep only last 50 games in history
      if (state.gameHistory.length > 50) {
        state.gameHistory = state.gameHistory.slice(0, 50);
      }
    },
    
    addPlayer: (state, action) => {
      if (state.currentGame && state.currentGame.players) {
        const existingIndex = state.currentGame.players.findIndex(
          p => p.id === action.payload.id
        );
        
        if (existingIndex === -1) {
          state.currentGame.players.push(action.payload);
        } else {
          state.currentGame.players[existingIndex] = action.payload;
        }
      }
    },
    
    removePlayer: (state, action) => {
      if (state.currentGame && state.currentGame.players) {
        state.currentGame.players = state.currentGame.players.filter(
          p => p.id !== action.payload
        );
      }
    },
    
    updateGameSettings: (state, action) => {
      state.gameSettings = { ...state.gameSettings, ...action.payload };
    },
    
    setActionInProgress: (state, action) => {
      state.actionInProgress = action.payload;
    },
    
    clearCurrentGame: (state) => {
      state.currentGame = null;
      state.playerHand = [];
      state.opponentHand = [];
      state.playedCards = [];
      state.roundHistory = [];
      state.lastAction = null;
      state.actionInProgress = false;
    },
    
    clearError: (state) => {
      state.error = null;
    }
  },
  
  extraReducers: (builder) => {
    // Create game
    builder
      .addCase(createGame.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createGame.fulfilled, (state, action) => {
        state.loading = false;
        state.currentGame = action.payload;
        state.error = null;
      })
      .addCase(createGame.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });

    // Join game
    builder
      .addCase(joinGame.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(joinGame.fulfilled, (state, action) => {
        state.loading = false;
        state.currentGame = action.payload;
        state.error = null;
      })
      .addCase(joinGame.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });

    // Fetch game history
    builder
      .addCase(fetchGameHistory.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchGameHistory.fulfilled, (state, action) => {
        state.loading = false;
        state.gameHistory = action.payload;
      })
      .addCase(fetchGameHistory.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  }
});

// Export actions
export const {
  updateGameState,
  setPlayerHand,
  setOpponentHand,
  addPlayedCard,
  updatePlayerAction,
  updateRoundResult,
  setGameResult,
  addPlayer,
  removePlayer,
  updateGameSettings,
  setActionInProgress,
  clearCurrentGame,
  clearError
} = gameSlice.actions;

// Selectors
export const selectGame = (state) => state.game;
export const selectCurrentGame = (state) => state.game.currentGame;
export const selectPlayerHand = (state) => state.game.playerHand;
export const selectOpponentHand = (state) => state.game.opponentHand;
export const selectPlayedCards = (state) => state.game.playedCards;
export const selectGameHistory = (state) => state.game.gameHistory;
export const selectGameSettings = (state) => state.game.gameSettings;
export const selectGameStats = (state) => state.game.gameStats;
export const selectGameLoading = (state) => state.game.loading;
export const selectGameError = (state) => state.game.error;
export const selectActionInProgress = (state) => state.game.actionInProgress;
export const selectLastAction = (state) => state.game.lastAction;
export const selectRoundHistory = (state) => state.game.roundHistory;

// Computed selectors
export const selectGameStatus = (state) => state.game.currentGame?.status;
export const selectCurrentRound = (state) => state.game.currentGame?.currentRound;
export const selectCurrentPlayerTurn = (state) => state.game.currentGame?.currentPlayerTurn;
export const selectIsMyTurn = (state) => {
  const game = state.game.currentGame;
  const user = state.auth.user;
  return game?.currentPlayerTurn === user?.id;
};
export const selectCanPlayCard = (state) => {
  const game = state.game.currentGame;
  const user = state.auth.user;
  return game?.status === 'IN_PROGRESS' && 
         game?.currentPlayerTurn === user?.id &&
         !state.game.actionInProgress;
};

export const selectGameScore = (playerId) => (state) => {
  return state.game.currentGame?.scores?.[playerId] || 0;
};

export const selectWinRate = (state) => {
  const { wins, totalGames } = state.game.gameStats;
  return totalGames > 0 ? Math.round((wins / totalGames) * 100) : 0;
};

export default gameSlice.reducer;
```

---

## 🔌 **WEBSOCKET MIDDLEWARE - REAL-TIME INTEGRATION**

```javascript
// src/store/middleware/websocketMiddleware.js
import { createListenerMiddleware } from '@reduxjs/toolkit';
import { 
  updateGameState, 
  addPlayer, 
  removePlayer, 
  updatePlayerAction,
  setGameResult
} from '../slices/gameSlice';
import { 
  setConnectionState,
  addMessage,
  setReconnecting
} from '../slices/websocketSlice';
import { showNotification } from '../slices/uiSlice';

// Create listener middleware
const websocketMiddleware = createListenerMiddleware();

// WebSocket connection management
websocketMiddleware.startListening({
  actionCreator: setConnectionState,
  effect: async (action, listenerApi) => {
    const { payload: connectionState } = action;
    const { dispatch } = listenerApi;
    
    switch (connectionState) {
      case 'CONNECTED':
        dispatch(showNotification({
          type: 'success',
          message: 'Connected to game server',
          duration: 2000
        }));
        break;
        
      case 'DISCONNECTED':
        dispatch(showNotification({
          type: 'warning',
          message: 'Connection lost. Trying to reconnect...',
          duration: 0
        }));
        dispatch(setReconnecting(true));
        break;
        
      case 'FAILED':
        dispatch(showNotification({
          type: 'error',
          message: 'Failed to connect to server. Please refresh the page.',
          duration: 0
        }));
        break;
    }
  }
});

// Game state synchronization
websocketMiddleware.startListening({
  type: 'websocket/messageReceived',
  effect: async (action, listenerApi) => {
    const { message } = action.payload;
    const { dispatch } = listenerApi;
    
    switch (message.messageType) {
      case 'GAME_STATE_UPDATE':
        dispatch(updateGameState({ gameData: message.payload }));
        break;
        
      case 'PLAYER_JOINED':
        dispatch(addPlayer(message.payload.player));
        dispatch(showNotification({
          type: 'info',
          message: `${message.payload.player.username} joined the game`,
          duration: 3000
        }));
        break;
        
      case 'PLAYER_LEFT':
        dispatch(removePlayer(message.payload.playerId));
        dispatch(showNotification({
          type: 'warning',
          message: 'Player left the game',
          duration: 3000
        }));
        break;
        
      case 'CARD_PLAYED':
        dispatch(updatePlayerAction({
          playerId: message.payload.playerId,
          action: 'CARD_PLAYED',
          cardIndex: message.payload.cardIndex,
          roundNumber: message.payload.roundNumber
        }));
        break;
        
      case 'GAME_COMPLETED':
        dispatch(setGameResult({
          winner: message.payload.winner,
          finalScores: message.payload.finalScores,
          gameStats: message.payload.gameStats
        }));
        
        dispatch(showNotification({
          type: 'success',
          message: `Game completed! Winner: ${message.payload.winner.username}`,
          duration: 5000
        }));
        break;
        
      case 'ERROR':
        dispatch(showNotification({
          type: 'error',
          message: message.payload.message || 'An error occurred',
          duration: 5000
        }));
        break;
        
      case 'NOTIFICATION':
        dispatch(showNotification({
          type: message.payload.type || 'info',
          message: message.payload.message,
          duration: message.payload.duration || 4000
        }));
        break;
    }
    
    // Store message for debugging
    dispatch(addMessage(message));
  }
});

// Optimistic updates for game actions
websocketMiddleware.startListening({
  actionCreator: updatePlayerAction,
  effect: async (action, listenerApi) => {
    const { getState } = listenerApi;
    const state = getState();
    const { playerId, actionType, cardIndex } = action.payload;
    
    // If it's the current user's action, apply optimistic update
    if (playerId === state.auth.user?.id && actionType === 'CARD_PLAYED') {
      // Visual feedback immediately
      console.log(`Optimistically updating card play: ${cardIndex}`);
    }
  }
});

export default websocketMiddleware.middleware;
```

---

## 🔄 **DATA PERSISTENCE MIDDLEWARE**

```javascript
// src/store/middleware/persistenceMiddleware.js
import { createListenerMiddleware } from '@reduxjs/toolkit';
import { updateUserPreferences } from '../slices/authSlice';
import { updateGameSettings } from '../slices/gameSlice';
import { updateTheme } from '../slices/uiSlice';

const persistenceMiddleware = createListenerMiddleware();

// Auto-save user preferences
persistenceMiddleware.startListening({
  actionCreator: updateUserPreferences,
  effect: async (action, listenerApi) => {
    const { getState } = listenerApi;
    const state = getState();
    
    // Save to localStorage for immediate persistence
    localStorage.setItem('userPreferences', JSON.stringify(state.auth.preferences));
    
    // Sync với server if authenticated
    if (state.auth.isAuthenticated && state.auth.token) {
      try {
        // API call để sync preferences với server
        await fetch('/api/user/preferences', {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${state.auth.token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(state.auth.preferences)
        });
      } catch (error) {
        console.warn('Failed to sync preferences với server:', error);
      }
    }
  }
});

// Auto-save game settings
persistenceMiddleware.startListening({
  actionCreator: updateGameSettings,
  effect: async (action, listenerApi) => {
    const { getState } = listenerApi;
    const state = getState();
    
    localStorage.setItem('gameSettings', JSON.stringify(state.game.gameSettings));
  }
});

// Theme persistence
persistenceMiddleware.startListening({
  actionCreator: updateTheme,
  effect: async (action, listenerApi) => {
    const { payload: theme } = action;
    
    // Apply theme to document
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }
});

export default persistenceMiddleware.middleware;
```

---

## 🎯 **PERFORMANCE OPTIMIZATION PATTERNS**

### **Memoized Selectors**

```javascript
// src/store/selectors/gameSelectors.js
import { createSelector } from '@reduxjs/toolkit';

// Memoized selectors for expensive computations
export const selectSortedPlayerHand = createSelector(
  [state => state.game.playerHand],
  (playerHand) => {
    return [...playerHand].sort((a, b) => {
      const suitOrder = { spades: 0, hearts: 1, diamonds: 2, clubs: 3 };
      const rankOrder = { 
        A: 1, '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7,
        '8': 8, '9': 9, '10': 10, J: 11, Q: 12, K: 13
      };
      
      if (suitOrder[a.suit] !== suitOrder[b.suit]) {
        return suitOrder[a.suit] - suitOrder[b.suit];
      }
      return rankOrder[a.rank] - rankOrder[b.rank];
    });
  }
);

export const selectPlayableCards = createSelector(
  [
    state => state.game.playerHand,
    state => state.game.currentGame?.currentSuit,
    state => state.game.currentGame?.gameRules
  ],
  (playerHand, currentSuit, gameRules) => {
    if (!playerHand || !currentSuit) return [];
    
    // Complex logic để determine playable cards
    const hasSuit = playerHand.some(card => card.suit === currentSuit);
    
    return playerHand.map((card, index) => {
      if (hasSuit) {
        return card.suit === currentSuit ? index : null;
      }
      return index; // Can play any card if no suit cards
    }).filter(index => index !== null);
  }
);

export const selectGameStatistics = createSelector(
  [state => state.game.gameHistory],
  (gameHistory) => {
    const totalGames = gameHistory.length;
    const wins = gameHistory.filter(game => game.winner?.id === game.currentUserId).length;
    const winRate = totalGames > 0 ? (wins / totalGames) * 100 : 0;
    
    // Calculate streak
    let currentStreak = 0;
    let maxStreak = 0;
    let tempStreak = 0;
    
    for (let i = 0; i < gameHistory.length; i++) {
      const game = gameHistory[i];
      const isWin = game.winner?.id === game.currentUserId;
      
      if (isWin) {
        tempStreak++;
        if (i === 0) currentStreak = tempStreak; // Most recent games
      } else {
        if (i === 0) currentStreak = 0;
        tempStreak = 0;
      }
      
      maxStreak = Math.max(maxStreak, tempStreak);
    }
    
    return {
      totalGames,
      wins,
      losses: totalGames - wins,
      winRate: Math.round(winRate),
      currentStreak,
      maxStreak
    };
  }
);
```

### **Optimized Component Subscriptions**

```javascript
// src/hooks/useOptimizedSelector.js
import { useSelector, shallowEqual } from 'react-redux';
import { useMemo } from 'react';

export const useOptimizedSelector = (selector, equalityFn = shallowEqual) => {
  return useSelector(selector, equalityFn);
};

// Custom hook cho game-specific data
export const useGameData = () => {
  return useOptimizedSelector(state => ({
    currentGame: state.game.currentGame,
    playerHand: state.game.playerHand,
    isMyTurn: state.game.currentGame?.currentPlayerTurn === state.auth.user?.id,
    canPlayCard: state.game.currentGame?.status === 'IN_PROGRESS' && 
                 state.game.currentGame?.currentPlayerTurn === state.auth.user?.id &&
                 !state.game.actionInProgress
  }));
};

// Optimized leaderboard data
export const useLeaderboardData = () => {
  return useOptimizedSelector(state => ({
    rankings: state.leaderboard.globalRankings,
    playerStats: state.leaderboard.playerStats,
    loading: state.leaderboard.loading
  }));
};
```

---

## 🔄 **STATE NORMALIZATION PATTERNS**

### **Normalized Game Data Structure**

```javascript
// Example normalized state structure for complex data
const normalizedGameState = {
  games: {
    byId: {
      'game-1': { id: 'game-1', status: 'IN_PROGRESS', players: ['player-1', 'player-2'] },
      'game-2': { id: 'game-2', status: 'COMPLETED', players: ['player-3', 'player-4'] }
    },
    allIds: ['game-1', 'game-2']
  },
  players: {
    byId: {
      'player-1': { id: 'player-1', username: 'alice', score: 10 },
      'player-2': { id: 'player-2', username: 'bob', score: 8 },
      'player-3': { id: 'player-3', username: 'charlie', score: 15 },
      'player-4': { id: 'player-4', username: 'diana', score: 12 }
    },
    allIds: ['player-1', 'player-2', 'player-3', 'player-4']
  }
};

// Selectors for normalized data
export const selectGameById = (gameId) => (state) => {
  return state.game.games.byId[gameId];
};

export const selectGamePlayers = (gameId) => createSelector(
  [selectGameById(gameId), state => state.game.players.byId],
  (game, playersById) => {
    if (!game) return [];
    return game.players.map(playerId => playersById[playerId]);
  }
);
```

---

**State management architecture này provides efficient, scalable, và maintainable data flow cho entire frontend application với optimized performance và real-time synchronization capabilities! Next: Security best practices! 🛡️**