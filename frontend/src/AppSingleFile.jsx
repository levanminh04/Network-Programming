import React, { createContext, useContext, useReducer, useEffect, useRef, useState } from 'react';

// ============================================================================
// PROTOCOL CONSTANTS - Tuân thủ MessageProtocol.java
// ============================================================================
const MessageType = {
  // AUTH DOMAIN
  AUTH_REGISTER_REQUEST: 'AUTH.REGISTER_REQUEST',
  AUTH_REGISTER_SUCCESS: 'AUTH.REGISTER_SUCCESS',
  AUTH_REGISTER_FAILURE: 'AUTH.REGISTER_FAILURE',
  AUTH_LOGIN_REQUEST: 'AUTH.LOGIN_REQUEST',
  AUTH_LOGIN_SUCCESS: 'AUTH.LOGIN_SUCCESS',
  AUTH_LOGIN_FAILURE: 'AUTH.LOGIN_FAILURE',
  AUTH_LOGOUT_REQUEST: 'AUTH.LOGOUT_REQUEST',
  
  // LOBBY DOMAIN
  LOBBY_MATCH_REQUEST: 'LOBBY.MATCH_REQUEST',
  LOBBY_MATCH_REQUEST_ACK: 'LOBBY.MATCH_REQUEST_ACK',
  LOBBY_MATCH_CANCEL: 'LOBBY.MATCH_CANCEL',
  
  // GAME DOMAIN
  GAME_MATCH_FOUND: 'GAME.MATCH_FOUND',
  GAME_START: 'GAME.START',
  GAME_ROUND_START: 'GAME.ROUND_START',
  GAME_ROUND_REVEAL: 'GAME.ROUND_REVEAL',
  GAME_CARD_PLAY_REQUEST: 'GAME.CARD_PLAY_REQUEST',
  GAME_CARD_PLAY_SUCCESS: 'GAME.CARD_PLAY_SUCCESS',
  GAME_CARD_PLAY_FAILURE: 'GAME.CARD_PLAY_FAILURE',
  GAME_OPPONENT_READY: 'GAME.OPPONENT_READY',
  GAME_END: 'GAME.END',
  GAME_OPPONENT_LEFT: 'GAME.OPPONENT_LEFT',
  
  // SYSTEM DOMAIN
  SYSTEM_WELCOME: 'SYSTEM.WELCOME',
  SYSTEM_PING: 'SYSTEM.PING',
  SYSTEM_PONG: 'SYSTEM.PONG',
  SYSTEM_ERROR: 'SYSTEM.ERROR'
};

// ============================================================================
// UTILITY FUNCTIONS - Tuân thủ MessageFactory.java & JsonUtils.java
// ============================================================================
const generateCorrelationId = () => `c-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

const createRequest = (type, payload, sessionId = null) => {
  const envelope = {
    type,
    correlationId: generateCorrelationId(),
    payload
  };
  if (sessionId) {
    envelope.sessionId = sessionId;
  }
  return envelope;
};

const parseMessage = (data) => {
  try {
    return JSON.parse(data);
  } catch (error) {
    console.error('Failed to parse message:', error);
    return null;
  }
};

// ============================================================================
// CARD UTILITIES
// ============================================================================
const CardSuit = {
  // Backend sends code: H, D, C, S (not HEARTS, DIAMONDS, etc.)
  H: { name: 'HEARTS', symbol: '♥', color: 'red', vietnamese: 'Cơ' },
  D: { name: 'DIAMONDS', symbol: '♦', color: 'red', vietnamese: 'Rô' },
  C: { name: 'CLUBS', symbol: '♣', color: 'black', vietnamese: 'Chuồn' },
  S: { name: 'SPADES', symbol: '♠', color: 'black', vietnamese: 'Bích' },
  
  // Keep full names for backward compatibility
  HEARTS: { name: 'HEARTS', symbol: '♥', color: 'red', vietnamese: 'Cơ' },
  DIAMONDS: { name: 'DIAMONDS', symbol: '♦', color: 'red', vietnamese: 'Rô' },
  CLUBS: { name: 'CLUBS', symbol: '♣', color: 'black', vietnamese: 'Chuồn' },
  SPADES: { name: 'SPADES', symbol: '♠', color: 'black', vietnamese: 'Bích' }
};

const getCardDisplay = (card) => {
  if (!card) return '??';
  
  // Use displayName from backend if available (e.g., "A♥", "9♣")
  if (card.displayName) {
    return card.displayName;
  }
  
  // Fallback: construct from rank and suit
  const suit = CardSuit[card.suit];
  return `${card.rank}${suit ? suit.symbol : ''}`;
};

const getCardColor = (card) => {
  if (!card || !card.suit) return 'gray';
  const suit = CardSuit[card.suit];
  const color = suit ? suit.color : 'gray';
  
  // Debug: uncomment to see card colors
  // console.log('Card:', card.displayName, 'Suit:', card.suit, 'Color:', color);
  
  return color;
};

// ============================================================================
// APP STATE MANAGEMENT - Context + Reducer
// ============================================================================
const initialState = {
  // Auth State
  isAuthenticated: false,
  user: null,
  sessionId: null,
  
  // View State
  currentView: 'AUTH', // 'AUTH' | 'LOBBY' | 'GAME'
  
  // WebSocket State
  ws: null,
  connected: false,
  
  // Lobby State
  matchmaking: false,
  matchFound: false,
  
  // Game State
  gameId: null,
  matchId: null,
  yourPosition: null, // 1 or 2 (player1 or player2)
  currentRound: 0,
  totalRounds: 3,
  playerScore: 0,
  opponentScore: 0,
  availableCards: [],
  selectedCardId: null,
  playerCard: null,
  opponentCard: null,
  opponentUsername: '',
  opponentReady: false,
  roundDeadline: null,
  roundResult: null,
  gameResult: null,
  
  // UI State
  error: null,
  message: null,
  loading: false
};

const appReducer = (state, action) => {
  switch (action.type) {
    // WebSocket Actions
    case 'WS_CONNECTED':
      return { ...state, ws: action.payload, connected: true, error: null };
    case 'WS_DISCONNECTED':
      return { ...state, connected: false, error: 'Mất kết nối với server' };
    
    // Auth Actions
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        isAuthenticated: true,
        user: action.payload.user,
        sessionId: action.payload.sessionId,
        currentView: 'LOBBY',
        error: null
      };
    case 'REGISTER_SUCCESS':
      return { ...state, message: 'Đăng ký thành công! Hãy đăng nhập.', error: null };
    case 'AUTH_ERROR':
      return { ...state, error: action.payload, loading: false };
    case 'LOGOUT':
      return { ...initialState, ws: state.ws, connected: state.connected };
    
    // Lobby Actions
    case 'MATCHMAKING_START':
      return { ...state, matchmaking: true, error: null };
    case 'MATCHMAKING_CANCEL':
      return { ...state, matchmaking: false };
    case 'MATCH_FOUND':
      console.log('🎉 MATCH_FOUND reducer triggered with payload:', action.payload);
      return {
        ...state,
        matchFound: true,
        matchId: action.payload.matchId,
        opponentUsername: action.payload.opponentUsername,
        message: `Đã tìm thấy đối thủ: ${action.payload.opponentUsername}!`
      };
    
    // Game Actions
    case 'GAME_START':
      console.log('🎮 GAME_START reducer triggered with payload:', action.payload);
      return {
        ...state,
        currentView: 'GAME',
        gameId: action.payload.gameId || action.payload.matchId,
        matchId: action.payload.matchId,
        yourPosition: action.payload.yourPosition, // 1 or 2
        opponentUsername: action.payload.opponentUsername || state.opponentUsername,
        matchmaking: false,
        matchFound: false,
        currentRound: 0,
        playerScore: 0,
        opponentScore: 0,
        message: null
      };
    
    case 'ROUND_START':
      console.log('🎯 ROUND_START reducer triggered with payload:', action.payload);
      // Backend sends "availableCards", not "hand"
      const handCards = action.payload.availableCards || action.payload.hand || [];
      console.log('📋 Hand cards received:', handCards);
      console.log('📋 Hand cards length:', handCards.length);
      return {
        ...state,
        // Use matchId as gameId since backend sends matchId
        gameId: action.payload.matchId || state.gameId,
        matchId: action.payload.matchId || state.matchId,
        currentRound: action.payload.roundNumber,
        totalRounds: action.payload.totalRounds || 3,
        availableCards: handCards,
        roundDeadline: action.payload.deadlineTimestamp || action.payload.deadline,
        selectedCardId: null,
        playerCard: null,
        opponentCard: null,
        opponentReady: false,
        roundResult: null,
        playerScore: action.payload.playerScore || state.playerScore,
        opponentScore: action.payload.opponentScore || state.opponentScore,
        message: action.payload.message || `Round ${action.payload.roundNumber} - Chọn bài của bạn!`
      };
    
    case 'CARD_SELECTED':
      // Show the selected card immediately
      const selectedCard = state.availableCards?.find(c => c.cardId === action.payload);
      return { 
        ...state, 
        selectedCardId: action.payload,
        playerCard: selectedCard,
        message: 'Đã chọn bài. Đang chờ đối thủ...'
      };
    
    case 'CARD_PLAY_SUCCESS':
      // Backend confirmed the card selection
      return {
        ...state,
        availableCards: action.payload.availableCards || state.availableCards,
        message: 'Đã gửi lựa chọn. Chờ đối thủ...'
      };
    
    case 'OPPONENT_READY':
      return {
        ...state,
        opponentReady: true,
        availableCards: action.payload.availableCards || state.availableCards,
        message: 'Đối thủ đã chọn bài!'
      };
    
    case 'ROUND_REVEAL':
      return {
        ...state,
        playerCard: action.payload.playerCard,
        opponentCard: action.payload.opponentCard,
        playerScore: action.payload.playerScore,
        opponentScore: action.payload.opponentScore,
        roundResult: action.payload.result,
        message: action.payload.message
      };
    
    case 'GAME_END':
      console.log('🏁 GAME_END payload:', action.payload);
      // Backend sends: player1Score, player2Score, winnerId
      // Use yourPosition to determine which score is ours
      const isPlayer1End = state.yourPosition === 1;
      
      const myFinalScore = isPlayer1End ? action.payload.player1Score : action.payload.player2Score;
      const opponentFinalScore = isPlayer1End ? action.payload.player2Score : action.payload.player1Score;
      
      let finalResult;
      if (!action.payload.winnerId) {
        finalResult = 'DRAW';
      } else if (String(action.payload.winnerId) === String(state.user?.userId)) {
        finalResult = 'WIN';
      } else {
        finalResult = 'LOSS';
      }
      
      console.log('🏁 Final result:', {
        yourPosition: state.yourPosition,
        isPlayer1: isPlayer1End,
        myScore: myFinalScore,
        opponentScore: opponentFinalScore,
        result: finalResult,
        winnerId: action.payload.winnerId,
        myUserId: state.user?.userId
      });
      
      return {
        ...state,
        gameResult: {
          ...action.payload,
          result: finalResult,
          playerScore: myFinalScore,
          opponentScore: opponentFinalScore,
          message: action.payload.message || 
            (finalResult === 'WIN' ? 'Chúc mừng! Bạn đã thắng!' :
             finalResult === 'LOSS' ? 'Tiếc quá! Bạn đã thua!' :
             'Trận đấu hòa!')
        },
        message: action.payload.message
      };
    
    case 'RETURN_TO_LOBBY':
      return {
        ...state,
        currentView: 'LOBBY',
        // Reset matchmaking states
        matchmaking: false,
        matchFound: false,
        // Reset game states
        gameId: null,
        matchId: null,
        yourPosition: null,
        currentRound: 0,
        totalRounds: 3,
        playerScore: 0,
        opponentScore: 0,
        availableCards: [],
        selectedCardId: null,
        playerCard: null,
        opponentCard: null,
        opponentUsername: '',
        opponentReady: false,
        roundDeadline: null,
        roundResult: null,
        gameResult: null,
        message: null,
        error: null
      };
    
    // UI Actions
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    case 'CLEAR_ERROR':
      return { ...state, error: null };
    case 'SET_MESSAGE':
      return { ...state, message: action.payload };
    case 'CLEAR_MESSAGE':
      return { ...state, message: null };
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    
    default:
      return state;
  }
};

// ============================================================================
// CONTEXT
// ============================================================================
const AppContext = createContext();

const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
};

// ============================================================================
// WEBSOCKET HOOK
// ============================================================================
const useWebSocket = (dispatch, sessionId) => {
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttemptsRef = useRef(0);
  const MAX_RECONNECT_ATTEMPTS = 5;

  useEffect(() => {
    const connect = () => {
      try {
        const ws = new WebSocket('ws://localhost:8080/ws');
        
        ws.onopen = () => {
          console.log('WebSocket connected');
          dispatch({ type: 'WS_CONNECTED', payload: ws });
          reconnectAttemptsRef.current = 0;
        };
        
        ws.onmessage = (event) => {
          const envelope = parseMessage(event.data);
          if (!envelope) return;
          
          console.log('Received:', envelope.type, envelope);
          
          // Route messages to appropriate handlers
          switch (envelope.type) {
            case MessageType.SYSTEM_WELCOME:
              console.log('Welcome message received');
              break;
            
            case MessageType.SYSTEM_PING:
              // Respond to ping with pong
              const pong = createRequest(MessageType.SYSTEM_PONG, {}, sessionId);
              ws.send(JSON.stringify(pong));
              break;
            
            case MessageType.AUTH_LOGIN_SUCCESS:
              dispatch({
                type: 'LOGIN_SUCCESS',
                payload: {
                  user: envelope.payload,
                  sessionId: envelope.sessionId
                }
              });
              break;
            
            case MessageType.AUTH_LOGIN_FAILURE:
              dispatch({
                type: 'AUTH_ERROR',
                payload: envelope.error?.message || 'Đăng nhập thất bại'
              });
              break;
            
            case MessageType.AUTH_REGISTER_SUCCESS:
              dispatch({ type: 'REGISTER_SUCCESS' });
              break;
            
            case MessageType.AUTH_REGISTER_FAILURE:
              dispatch({
                type: 'AUTH_ERROR',
                payload: envelope.error?.message || 'Đăng ký thất bại'
              });
              break;
            
            case MessageType.LOBBY_MATCH_REQUEST_ACK:
              console.log('Match request acknowledged');
              break;
            
            case MessageType.GAME_MATCH_FOUND:
              console.log('📨 Received GAME.MATCH_FOUND:', envelope.payload);
              dispatch({
                type: 'MATCH_FOUND',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_START:
              console.log('📨 Received GAME.START:', envelope.payload);
              dispatch({
                type: 'GAME_START',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_ROUND_START:
              console.log('📨 Received GAME.ROUND_START:', envelope.payload);
              console.log('   - Round:', envelope.payload.roundNumber);
              console.log('   - availableCards:', envelope.payload.availableCards);
              console.log('   - availableCards size:', envelope.payload.availableCards?.length);
              console.log('   - hand size:', envelope.payload.hand?.length);
              console.log('   - Deadline:', envelope.payload.deadlineTimestamp);
              dispatch({
                type: 'ROUND_START',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_CARD_PLAY_SUCCESS:
              dispatch({
                type: 'CARD_PLAY_SUCCESS',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_CARD_PLAY_FAILURE:
              dispatch({
                type: 'SET_ERROR',
                payload: envelope.error?.message || 'Không thể chơi bài'
              });
              break;
            
            case MessageType.GAME_OPPONENT_READY:
              dispatch({
                type: 'OPPONENT_READY',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_ROUND_REVEAL:
              dispatch({
                type: 'ROUND_REVEAL',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_END:
              dispatch({
                type: 'GAME_END',
                payload: envelope.payload
              });
              break;
            
            case MessageType.GAME_OPPONENT_LEFT:
              dispatch({
                type: 'SET_ERROR',
                payload: 'Đối thủ đã rời khỏi trận đấu'
              });
              setTimeout(() => {
                dispatch({ type: 'RETURN_TO_LOBBY' });
              }, 3000);
              break;
            
            case MessageType.SYSTEM_ERROR:
              dispatch({
                type: 'SET_ERROR',
                payload: envelope.error?.message || 'Lỗi hệ thống'
              });
              // Reset loading state to allow retry
              dispatch({ type: 'SET_LOADING', payload: false });
              break;
            
            default:
              console.log('Unhandled message type:', envelope.type);
          }
        };
        
        ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          dispatch({ type: 'SET_ERROR', payload: 'Lỗi kết nối WebSocket' });
        };
        
        ws.onclose = () => {
          console.log('WebSocket disconnected');
          dispatch({ type: 'WS_DISCONNECTED' });
          
          // Attempt reconnection
          if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttemptsRef.current++;
            const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 10000);
            console.log(`Reconnecting in ${delay}ms... (Attempt ${reconnectAttemptsRef.current})`);
            reconnectTimeoutRef.current = setTimeout(connect, delay);
          } else {
            dispatch({ type: 'SET_ERROR', payload: 'Không thể kết nối đến server' });
          }
        };
        
        wsRef.current = ws;
      } catch (error) {
        console.error('Failed to create WebSocket:', error);
        dispatch({ type: 'SET_ERROR', payload: 'Không thể tạo kết nối WebSocket' });
      }
    };
    
    connect();
    
    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [dispatch, sessionId]);
  
  return wsRef.current;
};

// ============================================================================
// AUTH VIEW
// ============================================================================
const AuthView = () => {
  const { state, dispatch, sendMessage } = useApp();
  const [activeTab, setActiveTab] = useState('login');
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    email: '',
    displayName: ''
  });

  const handleInputChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleLogin = (e) => {
    e.preventDefault();
    
    // Clear previous errors
    dispatch({ type: 'CLEAR_ERROR' });
    
    if (!formData.username || !formData.password) {
      dispatch({ type: 'SET_ERROR', payload: 'Vui lòng nhập username và password' });
      return;
    }
    
    const payload = {
      username: formData.username,
      password: formData.password,
      clientVersion: '1.0.0',
      rememberMe: false
    };
    
    sendMessage(MessageType.AUTH_LOGIN_REQUEST, payload);
    dispatch({ type: 'SET_LOADING', payload: true });
  };

  const handleRegister = (e) => {
    e.preventDefault();
    
    // Clear previous errors
    dispatch({ type: 'CLEAR_ERROR' });
    
    if (!formData.username || !formData.password || !formData.email) {
      dispatch({ type: 'SET_ERROR', payload: 'Vui lòng điền đầy đủ thông tin' });
      return;
    }
    
    const payload = {
      username: formData.username,
      password: formData.password,
      email: formData.email,
      displayName: formData.displayName || formData.username
    };
    
    sendMessage(MessageType.AUTH_REGISTER_REQUEST, payload);
    dispatch({ type: 'SET_LOADING', payload: true });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-md p-8">
        <h1 className="text-3xl font-bold text-center mb-6 text-gray-800">
          🎮 Card Game
        </h1>
        
        {/* Tab Buttons */}
        <div className="flex mb-6 border-b">
          <button
            className={`flex-1 py-2 text-center font-medium transition-colors ${
              activeTab === 'login'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => {
              setActiveTab('login');
              dispatch({ type: 'CLEAR_ERROR' });
              dispatch({ type: 'CLEAR_MESSAGE' });
            }}
          >
            Đăng nhập
          </button>
          <button
            className={`flex-1 py-2 text-center font-medium transition-colors ${
              activeTab === 'register'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => {
              setActiveTab('register');
              dispatch({ type: 'CLEAR_ERROR' });
              dispatch({ type: 'CLEAR_MESSAGE' });
            }}
          >
            Đăng ký
          </button>
        </div>

        {/* Error/Message Display */}
        {state.error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {state.error}
          </div>
        )}
        {state.message && (
          <div className="mb-4 p-3 bg-green-100 border border-green-400 text-green-700 rounded">
            {state.message}
          </div>
        )}

        {/* Login Form */}
        {activeTab === 'login' && (
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Username
              </label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
                placeholder="Nhập username"
                disabled={state.loading}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
                placeholder="Nhập password"
                disabled={state.loading}
              />
            </div>
            <button
              type="submit"
              disabled={state.loading || !state.connected}
              className="w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {state.loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
            </button>
          </form>
        )}

        {/* Register Form */}
        {activeTab === 'register' && (
          <form onSubmit={handleRegister} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Username *
              </label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
                placeholder="3-50 ký tự"
                disabled={state.loading}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email *
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
                placeholder="email@example.com"
                disabled={state.loading}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Password *
              </label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
                placeholder="Tối thiểu 6 ký tự"
                disabled={state.loading}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Tên hiển thị (Tùy chọn)
              </label>
              <input
                type="text"
                name="displayName"
                value={formData.displayName}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900 bg-white"
                placeholder="Mặc định là username"
                disabled={state.loading}
              />
            </div>
            <button
              type="submit"
              disabled={state.loading || !state.connected}
              className="w-full bg-purple-600 text-white py-2 rounded-lg font-medium hover:bg-purple-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {state.loading ? 'Đang đăng ký...' : 'Đăng ký'}
            </button>
          </form>
        )}

        {/* Connection Status */}
        <div className="mt-4 text-center text-sm">
          <span className={`inline-flex items-center ${state.connected ? 'text-green-600' : 'text-red-600'}`}>
            <span className={`w-2 h-2 rounded-full mr-2 ${state.connected ? 'bg-green-600' : 'bg-red-600'}`}></span>
            {state.connected ? 'Đã kết nối' : 'Mất kết nối'}
          </span>
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// LOBBY VIEW
// ============================================================================
const LobbyView = () => {
  const { state, dispatch, sendMessage } = useApp();

  const handleFindMatch = () => {
    const payload = {
      gameMode: 'QUICK_MATCH',
      timestamp: Date.now()
    };
    sendMessage(MessageType.LOBBY_MATCH_REQUEST, payload);
    dispatch({ type: 'MATCHMAKING_START' });
  };

  const handleCancelMatch = () => {
    const payload = {};
    sendMessage(MessageType.LOBBY_MATCH_CANCEL, payload);
    dispatch({ type: 'MATCHMAKING_CANCEL' });
  };

  const handleLogout = () => {
    sendMessage(MessageType.AUTH_LOGOUT_REQUEST, {});
    dispatch({ type: 'LOGOUT' });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-500 to-pink-500 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-2xl p-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-800">
              Sảnh chờ
            </h1>
            <p className="text-gray-600 mt-1">
              Chào mừng, <span className="font-semibold">{state.user?.username || 'Player'}</span>!
            </p>
          </div>
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
          >
            Đăng xuất
          </button>
        </div>

        {/* User Stats */}
        {state.user && (
          <div className="bg-gray-50 rounded-lg p-4 mb-6 grid grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">
                {state.user.gamesPlayed || 0}
              </div>
              <div className="text-sm text-gray-600">Trận đã chơi</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {state.user.gamesWon || 0}
              </div>
              <div className="text-sm text-gray-600">Trận thắng</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">
                {state.user.score || 0}
              </div>
              <div className="text-sm text-gray-600">Điểm</div>
            </div>
          </div>
        )}

        {/* Messages */}
        {state.message && (
          <div className="mb-4 p-4 bg-blue-100 border border-blue-400 text-blue-700 rounded-lg">
            {state.message}
          </div>
        )}
        {state.error && (
          <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
            {state.error}
          </div>
        )}

        {/* Matchmaking Section */}
        <div className="text-center">
          {!state.matchmaking && !state.matchFound && (
            <>
              <button
                onClick={handleFindMatch}
                disabled={!state.connected}
                className="px-8 py-4 bg-gradient-to-r from-green-500 to-blue-500 text-white text-xl font-bold rounded-lg hover:from-green-600 hover:to-blue-600 transition-all transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
              >
                🎯 Tìm trận
              </button>
              <p className="mt-4 text-gray-600">
                Nhấn để tìm đối thủ và bắt đầu trận đấu!
              </p>
            </>
          )}

          {state.matchmaking && !state.matchFound && (
            <div className="space-y-4">
              <div className="flex justify-center">
                <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-blue-600"></div>
              </div>
              <p className="text-xl font-medium text-gray-700">
                Đang tìm đối thủ...
              </p>
              <button
                onClick={handleCancelMatch}
                className="px-6 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
              >
                Hủy
              </button>
            </div>
          )}

          {state.matchFound && (
            <div className="space-y-4">
              <div className="text-2xl font-bold text-green-600 animate-pulse">
                ✅ Đã tìm thấy đối thủ!
              </div>
              <p className="text-lg">
                Đối thủ: <span className="font-bold">{state.opponentUsername}</span>
              </p>
              <p className="text-gray-600">Đang chuẩn bị trận đấu...</p>
            </div>
          )}
        </div>

        {/* Connection Status */}
        <div className="mt-8 text-center text-sm">
          <span className={`inline-flex items-center ${state.connected ? 'text-green-600' : 'text-red-600'}`}>
            <span className={`w-2 h-2 rounded-full mr-2 ${state.connected ? 'bg-green-600' : 'bg-red-600'}`}></span>
            {state.connected ? 'Đã kết nối' : 'Mất kết nối'}
          </span>
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// GAME VIEW
// ============================================================================
const GameView = () => {
  const { state, dispatch, sendMessage } = useApp();
  const [countdown, setCountdown] = useState(0);
  const [showAllCards, setShowAllCards] = useState(false); // Debug mode toggle

  // Debug: Log state changes (only when round changes)
  useEffect(() => {
    if (state.currentRound > 0) {
      console.log('🎮 GameView Round', state.currentRound, '- Cards:', state.availableCards?.length || 0);
    }
  }, [state.currentRound]);

  // Countdown timer
  useEffect(() => {
    if (!state.roundDeadline) {
      setCountdown(0);
      return;
    }

    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.floor((state.roundDeadline - Date.now()) / 1000));
      setCountdown(remaining);
      
      if (remaining === 0) {
        clearInterval(interval);
      }
    }, 100);

    return () => clearInterval(interval);
  }, [state.roundDeadline]);

  const handleCardClick = (cardId) => {
    console.log('🎴 Card clicked:', cardId);
    
    if (state.selectedCardId || state.playerCard) {
      console.log('   ⛔ Already selected, ignoring');
      return;
    }

    dispatch({ type: 'CARD_SELECTED', payload: cardId });

    const payload = {
      gameId: state.gameId,
      roundNumber: state.currentRound,
      cardId: cardId,
      timestamp: Date.now()
    };

    console.log('   📤 Sending:', payload);
    sendMessage(MessageType.GAME_CARD_PLAY_REQUEST, payload);
  };

  const handleReturnToLobby = () => {
    dispatch({ type: 'RETURN_TO_LOBBY' });
  };

  const isCardDisabled = (cardId) => {
    // Disable if already selected a card
    if (state.selectedCardId || state.playerCard) return true;
    
    // Disable if countdown is 0
    if (countdown === 0) return true;
    
    return false;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-800 to-blue-900 text-white p-4">
      {/* Debug Info - Remove in production */}
      <div className="bg-yellow-500 text-black p-2 mb-2 rounded text-xs">
        DEBUG: Round {state.currentRound}/{state.totalRounds} | 
        Cards: {state.availableCards?.length || 0} | 
        Countdown: {countdown}s |
        PlayerCard: {state.playerCard ? '✅' : '❌'}
      </div>
      
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="bg-gray-700 rounded-lg p-4 mb-4 flex justify-between items-center">
          <div className="flex items-center space-x-4">
            <div className="text-lg font-bold">
              Round {state.currentRound || 0} / {state.totalRounds || 3}
            </div>
            {/* Debug Toggle Button */}
            <button
              onClick={() => setShowAllCards(!showAllCards)}
              className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
                showAllCards 
                  ? 'bg-yellow-500 text-black hover:bg-yellow-600' 
                  : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
              }`}
              title="Toggle để hiển thị/ẩn tất cả bài (dành cho testing)"
            >
              {showAllCards ? '👁️ Debug: ON' : '👁️ Debug: OFF'}
            </button>
          </div>
          <div className="flex items-center space-x-8">
            <div className="text-center">
              <div className="text-sm text-gray-300">Bạn</div>
              <div className="text-2xl font-bold text-blue-400">{state.user?.username}</div>
              <div className="text-xl font-bold">{state.playerScore}</div>
            </div>
            <div className="text-3xl font-bold">VS</div>
            <div className="text-center">
              <div className="text-sm text-gray-300">Đối thủ</div>
              <div className="text-2xl font-bold text-red-400">{state.opponentUsername}</div>
              <div className="text-xl font-bold">{state.opponentScore}</div>
            </div>
          </div>
          <div className={`text-3xl font-bold ${countdown <= 3 ? 'text-red-500 animate-pulse' : 'text-yellow-400'}`}>
            ⏱️ {countdown}s
          </div>
        </div>

        {/* Messages */}
        {state.message && (
          <div className="mb-4 p-4 bg-blue-600 rounded-lg text-center text-lg font-medium">
            {state.message}
          </div>
        )}
        {state.error && (
          <div className="mb-4 p-4 bg-red-600 rounded-lg text-center text-lg font-medium">
            {state.error}
          </div>
        )}

        {/* Game Result */}
        {state.gameResult && (
          <div className="bg-gray-800 rounded-lg p-8 mb-4 text-center">
            <h2 className="text-4xl font-bold mb-4">
              {state.gameResult.result === 'WIN' ? '🎉 Chiến thắng!' : 
               state.gameResult.result === 'LOSS' ? '😢 Thất bại!' : 
               '🤝 Hòa!'}
            </h2>
            <div className="text-2xl mb-4">
              Tỷ số cuối: {state.gameResult.playerScore} - {state.gameResult.opponentScore}
            </div>
            <p className="text-gray-300 mb-6">{state.gameResult.message}</p>
            <button
              onClick={handleReturnToLobby}
              className="px-8 py-3 bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors text-lg font-medium"
            >
              Quay về sảnh
            </button>
          </div>
        )}

        {/* Round Reveal */}
        {state.roundResult && state.playerCard && state.opponentCard && (
          <div className="bg-gray-700 rounded-lg p-6 mb-4">
            <h3 className="text-2xl font-bold mb-4 text-center">Kết quả Round {state.currentRound}</h3>
            <div className="flex justify-center items-center space-x-8 mb-4">
              <div className="text-center">
                <div className="text-sm text-gray-300 mb-2">Bài của bạn</div>
                <div className={`text-6xl font-bold ${getCardColor(state.playerCard) === 'red' ? 'text-red-500' : 'text-gray-900'} bg-white rounded-lg p-4 shadow-lg`}>
                  {getCardDisplay(state.playerCard)}
                </div>
              </div>
              <div className="text-3xl font-bold">
                {state.roundResult === 'WIN' ? '>' : state.roundResult === 'LOSS' ? '<' : '='}
              </div>
              <div className="text-center">
                <div className="text-sm text-gray-300 mb-2">Bài đối thủ</div>
                <div className={`text-6xl font-bold ${getCardColor(state.opponentCard) === 'red' ? 'text-red-500' : 'text-gray-900'} bg-white rounded-lg p-4 shadow-lg`}>
                  {getCardDisplay(state.opponentCard)}
                </div>
              </div>
            </div>
            <div className="text-center">
              <span className={`text-xl font-bold ${
                state.roundResult === 'WIN' ? 'text-green-400' : 
                state.roundResult === 'LOSS' ? 'text-red-400' : 
                'text-yellow-400'
              }`}>
                {state.roundResult === 'WIN' ? '✅ Bạn thắng round này!' : 
                 state.roundResult === 'LOSS' ? '❌ Bạn thua round này!' : 
                 '🤝 Hòa!'}
              </span>
            </div>
          </div>
        )}

        {/* Player's Selected Card (if any) */}
        {state.playerCard && !state.roundResult && (
          <div className="mb-4 text-center">
            <div className="text-sm text-gray-300 mb-2">Bài bạn đã chọn</div>
            <div className={`inline-block text-5xl font-bold ${getCardColor(state.playerCard) === 'red' ? 'text-red-500' : 'text-gray-900'} bg-white rounded-lg p-4 shadow-lg`}>
              {getCardDisplay(state.playerCard)}
            </div>
            <div className="mt-2 text-gray-300">
              {state.opponentReady ? '✅ Đối thủ đã chọn bài' : '⏳ Đang chờ đối thủ...'}
            </div>
          </div>
        )}

        {/* Available Cards */}
        {state.availableCards && state.availableCards.length > 0 && !state.gameResult && (
          <div>
            <h3 className="text-xl font-bold mb-3 text-center">
              {state.playerCard ? 'Bộ bài chung (đã rút)' : 'Chọn một lá bài (mặt úp)'}
              {showAllCards && <span className="text-yellow-400 ml-2 text-sm">(Debug: Hiển thị tất cả)</span>}
            </h3>
            <div className="grid grid-cols-6 sm:grid-cols-9 md:grid-cols-12 gap-2">
              {state.availableCards.map((card) => {
                const disabled = isCardDisabled(card.cardId);
                const selected = state.selectedCardId === card.cardId;
                const isRevealed = state.playerCard && state.playerCard.cardId === card.cardId;
                const color = getCardColor(card);
                
                // Debug mode: show all cards OR normal mode: only show selected card
                const shouldShowCard = showAllCards || isRevealed;
                
                return (
                  <button
                    key={card.cardId}
                    onClick={() => handleCardClick(card.cardId)}
                    disabled={disabled}
                    className={`
                      aspect-[2/3] rounded-lg font-bold transition-all transform
                      ${disabled ? 'opacity-30 cursor-not-allowed' : 'hover:scale-110 hover:shadow-xl cursor-pointer'}
                      ${selected ? 'ring-4 ring-yellow-400 scale-105' : ''}
                      ${isRevealed 
                        ? (color === 'red' ? 'bg-white text-red-500 text-2xl' : 'bg-white text-gray-900 text-2xl')
                        : 'bg-gradient-to-br from-blue-600 to-purple-700 text-white text-4xl'
                      }
                      shadow-md flex items-center justify-center
                    `}
                  >
                    {(showAllCards || isRevealed) ? getCardDisplay(card) : '�'}
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// ============================================================================
// MAIN APP COMPONENT
// ============================================================================
const AppSingleFile = () => {
  const [state, dispatch] = useReducer(appReducer, initialState);
  const ws = useWebSocket(dispatch, state.sessionId);

  const sendMessage = (type, payload) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket not ready!');
      dispatch({ type: 'SET_ERROR', payload: 'Không có kết nối WebSocket' });
      return;
    }

    const message = createRequest(type, payload, state.sessionId);
    ws.send(JSON.stringify(message));
    console.log('📨 Sent:', type);
  };

  const contextValue = {
    state,
    dispatch,
    sendMessage
  };

  return (
    <AppContext.Provider value={contextValue}>
      {state.currentView === 'AUTH' && <AuthView />}
      {state.currentView === 'LOBBY' && <LobbyView />}
      {state.currentView === 'GAME' && <GameView />}
    </AppContext.Provider>
  );
};

export default AppSingleFile;

