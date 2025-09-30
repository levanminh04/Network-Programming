# WebSocket Communication Strategy - Frontend Architecture

## 🔌 **WEBSOCKET INTEGRATION ARCHITECTURE**

Comprehensive guide để implement enterprise-grade WebSocket communication cho frontend của **Game Rút Bài May Mắn** với robust connection management, real-time event handling, và error recovery mechanisms.

---

## 🌐 **COMMUNICATION ARCHITECTURE OVERVIEW**

### **System Communication Flow**

```
┌─────────────────┐     WebSocket      ┌─────────────────┐     TCP Socket    ┌─────────────────┐
│                 │     (ws://...)     │                 │     (tcp://...)   │                 │
│   React App     │ ◄─────────────────► │   Gateway       │ ◄───────────────► │   Core Server   │
│   (Frontend)    │   JSON Messages    │   (Spring Boot) │   JSON Protocol   │   (Game Logic)  │
│                 │                    │                 │                   │                 │
└─────────────────┘                    └─────────────────┘                   └─────────────────┘
        │                                       │                                       │
        │                                       │                                       │
   ┌────▼────┐                            ┌────▼────┐                            ┌────▼────┐
   │ Browser │                            │   HTTP  │                            │Database │
   │ Storage │                            │   APIs  │                            │ MySQL   │
   │         │                            │         │                            │         │
   └─────────┘                            └─────────┘                            └─────────┘
```

### **Why Frontend Only Uses WebSocket (Not TCP)**

1. **Browser Security Model**: Browsers không allow direct TCP socket connections
2. **Abstraction Layer**: Gateway handles TCP complexity và protocol translation
3. **Simplified Development**: Focus on UI/UX thay vì network protocols
4. **Cross-platform Compatibility**: WebSocket works across all modern browsers
5. **Built-in Features**: WebSocket provides framing, text/binary support, và compression

---

## 🏗️ **WEBSOCKET MANAGER ARCHITECTURE**

### **Core WebSocket Service Implementation**

```javascript
// src/services/websocket/WebSocketManager.js
import { EventEmitter } from 'events';

class WebSocketManager extends EventEmitter {
  constructor(config) {
    super();
    this.config = {
      url: config.url || 'ws://localhost:8080/ws/game',
      reconnectAttempts: config.reconnectAttempts || 5,
      reconnectInterval: config.reconnectInterval || 3000,
      heartbeatInterval: config.heartbeatInterval || 30000,
      messageTimeout: config.messageTimeout || 10000,
      ...config
    };
    
    this.socket = null;
    this.isConnected = false;
    this.isConnecting = false;
    this.reconnectCount = 0;
    this.messageQueue = [];
    this.pendingMessages = new Map(); // For request-response tracking
    this.heartbeatTimer = null;
    this.reconnectTimer = null;
    
    this.connect();
  }
  
  /**
   * Establish WebSocket connection với retry logic
   */
  async connect() {
    if (this.isConnecting || this.isConnected) {
      return Promise.resolve();
    }
    
    this.isConnecting = true;
    
    return new Promise((resolve, reject) => {
      try {
        this.socket = new WebSocket(this.config.url);
        
        const connectionTimeout = setTimeout(() => {
          this.socket.close();
          reject(new Error('Connection timeout'));
        }, 10000);
        
        this.socket.onopen = (event) => {
          clearTimeout(connectionTimeout);
          this.isConnected = true;
          this.isConnecting = false;
          this.reconnectCount = 0;
          
          this.emit('connected', event);
          this.startHeartbeat();
          this.processMessageQueue();
          
          console.log('🔌 WebSocket connected successfully');
          resolve();
        };
        
        this.socket.onmessage = (event) => {
          this.handleMessage(event.data);
        };
        
        this.socket.onclose = (event) => {
          this.handleDisconnection(event);
        };
        
        this.socket.onerror = (error) => {
          console.error('🚨 WebSocket error:', error);
          this.emit('error', error);
          
          if (this.isConnecting) {
            clearTimeout(connectionTimeout);
            reject(error);
          }
        };
        
      } catch (error) {
        this.isConnecting = false;
        reject(error);
      }
    });
  }
  
  /**
   * Send message với guaranteed delivery và timeout handling
   */
  async sendMessage(messageType, payload, options = {}) {
    const message = {
      messageType,
      payload,
      timestamp: new Date().toISOString(),
      requestId: this.generateRequestId(),
      sessionId: this.getSessionId(),
      userId: this.getUserId(),
      ...options
    };
    
    if (!this.isConnected) {
      if (options.queueWhenDisconnected !== false) {
        this.messageQueue.push(message);
        return Promise.resolve({ queued: true });
      } else {
        throw new Error('WebSocket not connected');
      }
    }
    
    return new Promise((resolve, reject) => {
      // Setup timeout for response
      const timeout = setTimeout(() => {
        this.pendingMessages.delete(message.requestId);
        reject(new Error(`Message timeout: ${messageType}`));
      }, options.timeout || this.config.messageTimeout);
      
      // Track pending message for response matching
      this.pendingMessages.set(message.requestId, {
        resolve,
        reject,
        timeout,
        messageType,
        timestamp: Date.now()
      });
      
      try {
        this.socket.send(JSON.stringify(message));
        this.emit('messageSent', message);
      } catch (error) {
        clearTimeout(timeout);
        this.pendingMessages.delete(message.requestId);
        reject(error);
      }
    });
  }
  
  /**
   * Handle incoming messages với type-based routing
   */
  handleMessage(rawData) {
    try {
      const message = JSON.parse(rawData);
      
      // Handle response messages
      if (message.requestId && this.pendingMessages.has(message.requestId)) {
        const pending = this.pendingMessages.get(message.requestId);
        clearTimeout(pending.timeout);
        this.pendingMessages.delete(message.requestId);
        
        if (message.error) {
          pending.reject(new Error(message.error));
        } else {
          pending.resolve(message);
        }
        return;
      }
      
      // Handle event messages
      this.emit('message', message);
      this.emit(`message:${message.messageType}`, message);
      
      // Specific message type handlers
      switch (message.messageType) {
        case 'HEARTBEAT_RESPONSE':
          this.handleHeartbeatResponse(message);
          break;
        case 'GAME_STATE_UPDATE':
          this.handleGameStateUpdate(message);
          break;
        case 'PLAYER_ACTION':
          this.handlePlayerAction(message);
          break;
        case 'ERROR':
          this.handleServerError(message);
          break;
        default:
          console.log('📨 Received message:', message.messageType, message);
      }
      
    } catch (error) {
      console.error('🚨 Failed to parse message:', error, rawData);
      this.emit('parseError', { error, rawData });
    }
  }
  
  /**
   * Handle connection loss với intelligent reconnection
   */
  handleDisconnection(event) {
    this.isConnected = false;
    this.isConnecting = false;
    this.stopHeartbeat();
    
    console.log('🔌 WebSocket disconnected:', event.code, event.reason);
    this.emit('disconnected', event);
    
    // Don't reconnect if it was a clean close
    if (event.code === 1000) {
      return;
    }
    
    // Attempt reconnection với exponential backoff
    if (this.reconnectCount < this.config.reconnectAttempts) {
      const delay = Math.min(
        this.config.reconnectInterval * Math.pow(2, this.reconnectCount),
        30000 // Max 30 seconds
      );
      
      console.log(`🔄 Reconnecting in ${delay}ms (attempt ${this.reconnectCount + 1}/${this.config.reconnectAttempts})`);
      
      this.reconnectTimer = setTimeout(() => {
        this.reconnectCount++;
        this.connect().catch(error => {
          console.error('🚨 Reconnection failed:', error);
        });
      }, delay);
    } else {
      console.error('🚨 Max reconnection attempts reached');
      this.emit('reconnectionFailed');
    }
  }
  
  /**
   * Heartbeat mechanism để detect connection issues
   */
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        this.sendMessage('HEARTBEAT', {}, { 
          timeout: 5000,
          queueWhenDisconnected: false 
        }).catch(error => {
          console.warn('💓 Heartbeat failed:', error);
        });
      }
    }, this.config.heartbeatInterval);
  }
  
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }
  
  /**
   * Process queued messages khi reconnected
   */
  processMessageQueue() {
    while (this.messageQueue.length > 0 && this.isConnected) {
      const message = this.messageQueue.shift();
      this.socket.send(JSON.stringify(message));
    }
  }
  
  /**
   * Graceful disconnection
   */
  disconnect() {
    this.stopHeartbeat();
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    if (this.socket) {
      this.socket.close(1000, 'Client disconnecting');
      this.socket = null;
    }
    
    this.isConnected = false;
    this.messageQueue = [];
    
    // Reject all pending messages
    this.pendingMessages.forEach(pending => {
      clearTimeout(pending.timeout);
      pending.reject(new Error('Connection closed'));
    });
    this.pendingMessages.clear();
  }
  
  // Helper methods
  generateRequestId() {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
  
  getSessionId() {
    return localStorage.getItem('sessionId') || null;
  }
  
  getUserId() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    return user.id || null;
  }
  
  // Game-specific helper methods
  async joinGame(gameId) {
    return this.sendMessage('JOIN_GAME', { gameId });
  }
  
  async playCard(gameId, cardIndex, roundNumber) {
    return this.sendMessage('PLAY_CARD', { 
      gameId, 
      cardIndex, 
      roundNumber 
    });
  }
  
  async leaveGame(gameId) {
    return this.sendMessage('LEAVE_GAME', { gameId });
  }
}

export default WebSocketManager;
```

### **Message Handler Service**

```javascript
// src/services/websocket/MessageHandler.js
import { store } from '../../store';
import { 
  updateGameState, 
  addPlayer, 
  removePlayer,
  updatePlayerAction,
  setGameResult
} from '../../store/slices/gameSlice';
import { showNotification } from '../../store/slices/uiSlice';

class MessageHandler {
  constructor(webSocketManager) {
    this.wsManager = webSocketManager;
    this.setupEventListeners();
  }
  
  setupEventListeners() {
    // Game state updates
    this.wsManager.on('message:GAME_STATE_UPDATE', this.handleGameStateUpdate.bind(this));
    this.wsManager.on('message:PLAYER_JOINED', this.handlePlayerJoined.bind(this));
    this.wsManager.on('message:PLAYER_LEFT', this.handlePlayerLeft.bind(this));
    this.wsManager.on('message:CARD_PLAYED', this.handleCardPlayed.bind(this));
    this.wsManager.on('message:ROUND_COMPLETED', this.handleRoundCompleted.bind(this));
    this.wsManager.on('message:GAME_COMPLETED', this.handleGameCompleted.bind(this));
    
    // System events
    this.wsManager.on('message:ERROR', this.handleError.bind(this));
    this.wsManager.on('message:NOTIFICATION', this.handleNotification.bind(this));
    
    // Connection events
    this.wsManager.on('connected', this.handleConnected.bind(this));
    this.wsManager.on('disconnected', this.handleDisconnected.bind(this));
    this.wsManager.on('reconnectionFailed', this.handleReconnectionFailed.bind(this));
  }
  
  handleGameStateUpdate(message) {
    const { payload } = message;
    store.dispatch(updateGameState(payload));
    
    console.log('🎮 Game state updated:', payload);
  }
  
  handlePlayerJoined(message) {
    const { payload } = message;
    store.dispatch(addPlayer(payload.player));
    store.dispatch(showNotification({
      type: 'info',
      message: `${payload.player.username} joined the game`,
      duration: 3000
    }));
  }
  
  handlePlayerLeft(message) {
    const { payload } = message;
    store.dispatch(removePlayer(payload.playerId));
    store.dispatch(showNotification({
      type: 'warning',
      message: `Player left the game`,
      duration: 3000
    }));
  }
  
  handleCardPlayed(message) {
    const { payload } = message;
    store.dispatch(updatePlayerAction({
      playerId: payload.playerId,
      action: 'CARD_PLAYED',
      cardIndex: payload.cardIndex,
      roundNumber: payload.roundNumber
    }));
    
    // Show visual feedback
    this.animateCardPlay(payload);
  }
  
  handleRoundCompleted(message) {
    const { payload } = message;
    store.dispatch(updateGameState({
      currentRound: payload.nextRound,
      roundResults: payload.results,
      scores: payload.scores
    }));
    
    // Show round result animation
    this.showRoundResult(payload);
  }
  
  handleGameCompleted(message) {
    const { payload } = message;
    store.dispatch(setGameResult({
      winner: payload.winner,
      finalScores: payload.finalScores,
      gameStats: payload.gameStats
    }));
    
    // Show game completion modal
    this.showGameCompletionModal(payload);
  }
  
  handleError(message) {
    const { payload } = message;
    console.error('🚨 Server error:', payload);
    
    store.dispatch(showNotification({
      type: 'error',
      message: payload.message || 'An error occurred',
      duration: 5000
    }));
  }
  
  handleNotification(message) {
    const { payload } = message;
    store.dispatch(showNotification({
      type: payload.type || 'info',
      message: payload.message,
      duration: payload.duration || 4000
    }));
  }
  
  handleConnected() {
    store.dispatch(showNotification({
      type: 'success',
      message: 'Connected to game server',
      duration: 2000
    }));
  }
  
  handleDisconnected() {
    store.dispatch(showNotification({
      type: 'warning',
      message: 'Connection lost. Trying to reconnect...',
      duration: 0 // Persistent until reconnected
    }));
  }
  
  handleReconnectionFailed() {
    store.dispatch(showNotification({
      type: 'error',
      message: 'Failed to reconnect. Please refresh the page.',
      duration: 0
    }));
  }
  
  // Animation helpers
  animateCardPlay(payload) {
    // Implement card play animation logic
    const cardElement = document.querySelector(`[data-card-index="${payload.cardIndex}"]`);
    if (cardElement) {
      cardElement.classList.add('card-played-animation');
      setTimeout(() => {
        cardElement.classList.remove('card-played-animation');
      }, 1000);
    }
  }
  
  showRoundResult(payload) {
    // Implement round result display logic
    const resultModal = document.getElementById('round-result-modal');
    if (resultModal) {
      resultModal.classList.add('show');
      setTimeout(() => {
        resultModal.classList.remove('show');
      }, 3000);
    }
  }
  
  showGameCompletionModal(payload) {
    // Implement game completion modal logic
    const modal = document.getElementById('game-completion-modal');
    if (modal) {
      modal.classList.add('show');
    }
  }
}

export default MessageHandler;
```

---

## 🔧 **WEBSOCKET CONFIGURATION**

### **Environment-based Configuration**

```javascript
// src/config/websocket.config.js
const getWebSocketConfig = () => {
  const environment = import.meta.env.VITE_NODE_ENV || 'development';
  
  const configs = {
    development: {
      url: 'ws://localhost:8080/ws/game',
      reconnectAttempts: 5,
      reconnectInterval: 3000,
      heartbeatInterval: 30000,
      messageTimeout: 10000,
      debug: true
    },
    production: {
      url: `wss://${window.location.host}/ws/game`,
      reconnectAttempts: 10,
      reconnectInterval: 5000,
      heartbeatInterval: 30000,
      messageTimeout: 15000,
      debug: false
    },
    test: {
      url: 'ws://localhost:8080/ws/game',
      reconnectAttempts: 3,
      reconnectInterval: 1000,
      heartbeatInterval: 10000,
      messageTimeout: 5000,
      debug: true
    }
  };
  
  return configs[environment] || configs.development;
};

export const WEBSOCKET_CONFIG = getWebSocketConfig();

export const MESSAGE_TYPES = {
  // Authentication
  LOGIN: 'LOGIN',
  LOGOUT: 'LOGOUT',
  TOKEN_REFRESH: 'TOKEN_REFRESH',
  
  // Game Management
  CREATE_GAME: 'CREATE_GAME',
  JOIN_GAME: 'JOIN_GAME',
  LEAVE_GAME: 'LEAVE_GAME',
  START_GAME: 'START_GAME',
  
  // Game Actions
  PLAY_CARD: 'PLAY_CARD',
  END_ROUND: 'END_ROUND',
  GAME_OVER: 'GAME_OVER',
  
  // Real-time Updates
  GAME_STATE_UPDATE: 'GAME_STATE_UPDATE',
  PLAYER_JOINED: 'PLAYER_JOINED',
  PLAYER_LEFT: 'PLAYER_LEFT',
  CARD_PLAYED: 'CARD_PLAYED',
  ROUND_COMPLETED: 'ROUND_COMPLETED',
  GAME_COMPLETED: 'GAME_COMPLETED',
  
  // System Events
  HEARTBEAT: 'HEARTBEAT',
  HEARTBEAT_RESPONSE: 'HEARTBEAT_RESPONSE',
  ERROR: 'ERROR',
  NOTIFICATION: 'NOTIFICATION'
};

export const CONNECTION_STATES = {
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  RECONNECTING: 'RECONNECTING',
  FAILED: 'FAILED'
};
```

---

## ⚡ **REAL-TIME EVENT HANDLING PATTERNS**

### **Event-Driven Architecture**

```javascript
// src/hooks/useWebSocket.js
import { useEffect, useRef, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import WebSocketManager from '../services/websocket/WebSocketManager';
import MessageHandler from '../services/websocket/MessageHandler';
import { WEBSOCKET_CONFIG } from '../config/websocket.config';
import { setConnectionState } from '../store/slices/uiSlice';

export const useWebSocket = () => {
  const dispatch = useDispatch();
  const wsManager = useRef(null);
  const messageHandler = useRef(null);
  const connectionState = useSelector(state => state.ui.connectionState);
  
  useEffect(() => {
    // Initialize WebSocket Manager
    wsManager.current = new WebSocketManager(WEBSOCKET_CONFIG);
    messageHandler.current = new MessageHandler(wsManager.current);
    
    // Connection state management
    wsManager.current.on('connected', () => {
      dispatch(setConnectionState('CONNECTED'));
    });
    
    wsManager.current.on('disconnected', () => {
      dispatch(setConnectionState('DISCONNECTED'));
    });
    
    wsManager.current.on('reconnectionFailed', () => {
      dispatch(setConnectionState('FAILED'));
    });
    
    // Cleanup on unmount
    return () => {
      if (wsManager.current) {
        wsManager.current.disconnect();
      }
    };
  }, [dispatch]);
  
  // Public API methods
  const sendMessage = useCallback(async (messageType, payload, options) => {
    if (!wsManager.current) {
      throw new Error('WebSocket not initialized');
    }
    return wsManager.current.sendMessage(messageType, payload, options);
  }, []);
  
  const joinGame = useCallback(async (gameId) => {
    return sendMessage('JOIN_GAME', { gameId });
  }, [sendMessage]);
  
  const playCard = useCallback(async (gameId, cardIndex, roundNumber) => {
    return sendMessage('PLAY_CARD', { 
      gameId, 
      cardIndex, 
      roundNumber,
      timestamp: Date.now()
    });
  }, [sendMessage]);
  
  const leaveGame = useCallback(async (gameId) => {
    return sendMessage('LEAVE_GAME', { gameId });
  }, [sendMessage]);
  
  return {
    connectionState,
    sendMessage,
    joinGame,
    playCard,
    leaveGame,
    isConnected: connectionState === 'CONNECTED'
  };
};
```

### **Game State Synchronization**

```javascript
// src/hooks/useGameState.js
import { useSelector, useDispatch } from 'react-redux';
import { useCallback } from 'react';
import { useWebSocket } from './useWebSocket';
import { 
  selectCurrentGame, 
  selectPlayerHand, 
  selectGameStatus,
  selectCurrentRound
} from '../store/slices/gameSlice';

export const useGameState = () => {
  const dispatch = useDispatch();
  const { playCard, leaveGame } = useWebSocket();
  
  // Selectors
  const currentGame = useSelector(selectCurrentGame);
  const playerHand = useSelector(selectPlayerHand);
  const gameStatus = useSelector(selectGameStatus);
  const currentRound = useSelector(selectCurrentRound);
  
  // Actions
  const handlePlayCard = useCallback(async (cardIndex) => {
    if (!currentGame?.id || !currentRound) {
      throw new Error('No active game or round');
    }
    
    try {
      const result = await playCard(currentGame.id, cardIndex, currentRound);
      return result;
    } catch (error) {
      console.error('Failed to play card:', error);
      throw error;
    }
  }, [currentGame, currentRound, playCard]);
  
  const handleLeaveGame = useCallback(async () => {
    if (!currentGame?.id) {
      throw new Error('No active game');
    }
    
    try {
      const result = await leaveGame(currentGame.id);
      return result;
    } catch (error) {
      console.error('Failed to leave game:', error);
      throw error;
    }
  }, [currentGame, leaveGame]);
  
  return {
    // State
    currentGame,
    playerHand,
    gameStatus,
    currentRound,
    
    // Actions
    playCard: handlePlayCard,
    leaveGame: handleLeaveGame,
    
    // Computed state
    isGameActive: gameStatus === 'IN_PROGRESS',
    isMyTurn: currentGame?.currentPlayerTurn === currentGame?.currentUserId,
    canPlayCard: gameStatus === 'IN_PROGRESS' && currentGame?.currentPlayerTurn === currentGame?.currentUserId
  };
};
```

---

## 🛡️ **ERROR HANDLING & RESILIENCE**

### **Comprehensive Error Recovery**

```javascript
// src/services/websocket/ConnectionMonitor.js
class ConnectionMonitor {
  constructor(webSocketManager) {
    this.wsManager = webSocketManager;
    this.connectionQuality = 'GOOD';
    this.latencyHistory = [];
    this.errorCount = 0;
    this.lastPingTime = null;
    
    this.setupMonitoring();
  }
  
  setupMonitoring() {
    // Monitor connection quality
    this.wsManager.on('message:HEARTBEAT_RESPONSE', (message) => {
      if (this.lastPingTime) {
        const latency = Date.now() - this.lastPingTime;
        this.updateLatencyHistory(latency);
        this.assessConnectionQuality();
      }
    });
    
    // Monitor errors
    this.wsManager.on('error', () => {
      this.errorCount++;
      this.assessConnectionQuality();
    });
    
    // Reset error count on successful connection
    this.wsManager.on('connected', () => {
      this.errorCount = 0;
      this.connectionQuality = 'GOOD';
    });
  }
  
  updateLatencyHistory(latency) {
    this.latencyHistory.push(latency);
    if (this.latencyHistory.length > 10) {
      this.latencyHistory.shift();
    }
  }
  
  assessConnectionQuality() {
    const avgLatency = this.latencyHistory.reduce((a, b) => a + b, 0) / this.latencyHistory.length;
    
    if (this.errorCount > 3 || avgLatency > 1000) {
      this.connectionQuality = 'POOR';
    } else if (this.errorCount > 1 || avgLatency > 500) {
      this.connectionQuality = 'FAIR';
    } else {
      this.connectionQuality = 'GOOD';
    }
    
    // Emit quality change events
    this.wsManager.emit('connectionQualityChanged', {
      quality: this.connectionQuality,
      latency: avgLatency,
      errorCount: this.errorCount
    });
  }
  
  getConnectionStats() {
    return {
      quality: this.connectionQuality,
      averageLatency: this.latencyHistory.reduce((a, b) => a + b, 0) / this.latencyHistory.length || 0,
      errorCount: this.errorCount,
      isConnected: this.wsManager.isConnected
    };
  }
}

export default ConnectionMonitor;
```

### **Message Retry Mechanism**

```javascript
// src/services/websocket/RetryManager.js
class RetryManager {
  constructor(webSocketManager) {
    this.wsManager = webSocketManager;
    this.retryQueue = new Map();
    this.maxRetries = 3;
    this.retryDelay = 1000;
  }
  
  async sendWithRetry(messageType, payload, options = {}) {
    const maxRetries = options.maxRetries || this.maxRetries;
    const retryDelay = options.retryDelay || this.retryDelay;
    
    let attempt = 0;
    let lastError;
    
    while (attempt <= maxRetries) {
      try {
        const result = await this.wsManager.sendMessage(messageType, payload, {
          ...options,
          timeout: options.timeout || 5000
        });
        
        return result;
      } catch (error) {
        lastError = error;
        attempt++;
        
        if (attempt <= maxRetries) {
          console.warn(`Retry attempt ${attempt} for ${messageType}:`, error.message);
          await this.delay(retryDelay * attempt); // Exponential backoff
        }
      }
    }
    
    throw new Error(`Failed after ${maxRetries} retries: ${lastError.message}`);
  }
  
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
```

---

## 📊 **PERFORMANCE OPTIMIZATION**

### **Message Throttling & Batching**

```javascript
// src/services/websocket/MessageOptimizer.js
class MessageOptimizer {
  constructor(webSocketManager) {
    this.wsManager = webSocketManager;
    this.messageBuffer = [];
    this.batchTimer = null;
    this.batchDelay = 100; // 100ms batching window
    this.throttleMap = new Map();
  }
  
  // Throttle frequent messages
  throttledSend(messageType, payload, throttleKey, throttleDelay = 200) {
    const now = Date.now();
    const lastSent = this.throttleMap.get(throttleKey);
    
    if (lastSent && (now - lastSent) < throttleDelay) {
      return Promise.resolve({ throttled: true });
    }
    
    this.throttleMap.set(throttleKey, now);
    return this.wsManager.sendMessage(messageType, payload);
  }
  
  // Batch multiple messages
  batchSend(messageType, payload) {
    this.messageBuffer.push({ messageType, payload, timestamp: Date.now() });
    
    if (this.batchTimer) {
      clearTimeout(this.batchTimer);
    }
    
    this.batchTimer = setTimeout(() => {
      this.flushBatch();
    }, this.batchDelay);
  }
  
  flushBatch() {
    if (this.messageBuffer.length === 0) return;
    
    const batch = [...this.messageBuffer];
    this.messageBuffer = [];
    
    this.wsManager.sendMessage('BATCH_MESSAGE', {
      messages: batch,
      batchId: this.generateBatchId()
    });
  }
  
  generateBatchId() {
    return `batch_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}
```

---

**WebSocket architecture này provides robust, scalable, và performant real-time communication foundation cho frontend application. Next, tôi sẽ create component design strategy để build maintainable UI components! 🎨**