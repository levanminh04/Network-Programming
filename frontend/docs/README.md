# Frontend Architecture Documentation - Card Game Lucky Draw

## 🎯 **STRATEGIC FRONTEND OVERVIEW**

Documentation này cung cấp **comprehensive frontend architecture** cho dự án **Game Rút Bài May Mắn** với modern React patterns, enterprise-grade WebSocket management, và scalable component architecture.

---

## 📁 **ENTERPRISE FRONTEND STRUCTURE**

### **Recommended Project Architecture**

```
frontend/
├── docs/                           # 📚 Architecture & Implementation Docs
│   ├── README.md                   # Strategic overview & roadmap
│   ├── architecture/               # System design & patterns
│   │   ├── component-design.md     # Component architecture
│   │   ├── websocket-strategy.md   # Real-time communication
│   │   ├── state-management.md     # Redux/Context patterns
│   │   └── security-guide.md       # Frontend security practices
│   ├── implementation/             # Development guides
│   │   ├── setup-guide.md          # Environment & tooling setup
│   │   ├── websocket-integration.md # WebSocket implementation
│   │   ├── component-library.md    # Reusable components
│   │   └── api-integration.md      # Service layer patterns
│   └── testing/                    # QA & testing strategies
│       ├── testing-strategy.md     # Testing framework
│       ├── mock-server.md          # Development testing
│       └── e2e-testing.md          # End-to-end testing
├── public/                         # 🌐 Static assets
│   ├── index.html
│   ├── favicon.ico
│   ├── manifest.json
│   └── images/                     # Game assets & illustrations
├── src/                           # 🚀 Source code
│   ├── components/                # 🧩 Reusable UI components
│   │   ├── ui/                    # Basic UI elements
│   │   │   ├── Button/
│   │   │   ├── Card/
│   │   │   ├── Modal/
│   │   │   ├── Input/
│   │   │   └── Loading/
│   │   ├── game/                  # Game-specific components
│   │   │   ├── GameBoard/
│   │   │   ├── PlayerHand/
│   │   │   ├── ScoreDisplay/
│   │   │   ├── GameControls/
│   │   │   └── RoundIndicator/
│   │   ├── layout/                # Layout components
│   │   │   ├── Header/
│   │   │   ├── Sidebar/
│   │   │   ├── Footer/
│   │   │   └── MainLayout/
│   │   └── features/              # Feature-specific components
│   │       ├── auth/              # Authentication components
│   │       ├── lobby/             # Game lobby & matchmaking
│   │       ├── leaderboard/       # Rankings & statistics
│   │       └── profile/           # User profile management
│   ├── pages/                     # 📄 Page-level components
│   │   ├── HomePage/
│   │   ├── LoginPage/
│   │   ├── GameLobbyPage/
│   │   ├── GamePage/
│   │   ├── LeaderboardPage/
│   │   └── ProfilePage/
│   ├── hooks/                     # 🎣 Custom React hooks
│   │   ├── useWebSocket/          # WebSocket management
│   │   ├── useGameState/          # Game state management
│   │   ├── useAuth/               # Authentication logic
│   │   ├── useLocalStorage/       # Persistent storage
│   │   └── useAsyncOperation/     # Async operation handling
│   ├── services/                  # 🔧 Business logic & API
│   │   ├── websocket/             # WebSocket service layer
│   │   │   ├── WebSocketManager.js
│   │   │   ├── MessageHandler.js
│   │   │   └── ConnectionMonitor.js
│   │   ├── api/                   # REST API services
│   │   │   ├── authService.js
│   │   │   ├── gameService.js
│   │   │   └── userService.js
│   │   ├── game/                  # Game logic services
│   │   │   ├── GameStateManager.js
│   │   │   ├── CardLogic.js
│   │   │   └── ScoreCalculator.js
│   │   └── storage/               # Data persistence
│   │       ├── LocalStorageService.js
│   │       └── SessionManager.js
│   ├── store/                     # 🏪 State management
│   │   ├── index.js               # Store configuration
│   │   ├── slices/                # Redux Toolkit slices
│   │   │   ├── authSlice.js
│   │   │   ├── gameSlice.js
│   │   │   ├── lobbySlice.js
│   │   │   └── uiSlice.js
│   │   └── middleware/            # Custom middleware
│   │       ├── websocketMiddleware.js
│   │       └── persistenceMiddleware.js
│   ├── utils/                     # 🔨 Utility functions
│   │   ├── constants.js           # Application constants
│   │   ├── validators.js          # Input validation
│   │   ├── formatters.js          # Data formatting
│   │   ├── helpers.js             # General helpers
│   │   └── errorHandlers.js       # Error handling utilities
│   ├── config/                    # ⚙️ Configuration
│   │   ├── websocket.config.js    # WebSocket configuration
│   │   ├── api.config.js          # API endpoints & settings
│   │   ├── game.config.js         # Game rules & settings
│   │   └── environment.config.js  # Environment variables
│   ├── assets/                    # 🎨 Static resources
│   │   ├── images/                # Image assets
│   │   │   ├── cards/             # Card images
│   │   │   ├── backgrounds/       # Game backgrounds
│   │   │   └── icons/             # UI icons
│   │   ├── sounds/                # Audio assets
│   │   │   ├── card-flip.mp3
│   │   │   ├── win-sound.mp3
│   │   │   └── notification.mp3
│   │   └── fonts/                 # Custom fonts
│   ├── styles/                    # 🎨 Styling & themes
│   │   ├── globals.css            # Global styles
│   │   ├── variables.css          # CSS variables
│   │   ├── themes/                # Theme definitions
│   │   │   ├── light.css
│   │   │   └── dark.css
│   │   └── components/            # Component-specific styles
│   ├── types/                     # 📝 TypeScript type definitions
│   │   ├── game.types.ts
│   │   ├── user.types.ts
│   │   ├── websocket.types.ts
│   │   └── api.types.ts
│   ├── __tests__/                 # 🧪 Test files
│   │   ├── components/
│   │   ├── services/
│   │   ├── hooks/
│   │   └── utils/
│   ├── App.jsx                    # Main application component
│   ├── main.jsx                   # Application entry point
│   └── index.css                  # Root styles
├── tests/                         # 🧪 Test configuration & utilities
│   ├── setup.js                   # Test setup configuration
│   ├── mocks/                     # Mock data & services
│   │   ├── mockWebSocket.js
│   │   ├── mockGameData.js
│   │   └── mockServer.js
│   └── fixtures/                  # Test fixtures
├── .env.example                   # Environment variables template
├── .env.development               # Development environment
├── .env.production                # Production environment
├── .gitignore                     # Git ignore patterns
├── package.json                   # Dependencies & scripts
├── vite.config.js                 # Vite configuration
├── tailwind.config.js             # Tailwind CSS configuration
├── postcss.config.js              # PostCSS configuration
├── eslint.config.js               # ESLint configuration
├── prettier.config.js             # Prettier configuration
└── README.md                      # Project documentation
```

---

## 🏗️ **ARCHITECTURAL PRINCIPLES**

### **1. Separation of Concerns**
- **Presentation Layer**: React components focused on UI rendering
- **Business Logic Layer**: Services handling game logic và data processing
- **Communication Layer**: WebSocket management và API integration
- **State Management Layer**: Centralized state với Redux Toolkit

### **2. Component Architecture Patterns**
- **Atomic Design**: Button → Card → PlayerHand → GameBoard
- **Container vs Presentational**: Smart containers, dumb components
- **Composition over Inheritance**: Flexible component composition
- **Single Responsibility**: Each component has one clear purpose

### **3. Real-time Communication Strategy**
- **WebSocket Only**: No direct TCP socket connection
- **Gateway Integration**: Frontend ↔ Gateway ↔ Core architecture
- **Event-driven Messaging**: Reactive to server events
- **Resilient Connection**: Auto-reconnect và error recovery

---

## 🔌 **WEBSOCKET INTEGRATION STRATEGY**

### **Frontend ↔ Gateway Communication Flow**

```
┌─────────────────┐    WebSocket     ┌─────────────────┐    TCP    ┌─────────────────┐
│                 │    (Port 8080)   │                 │           │                 │
│   React App     │ ◄──────────────► │   Gateway       │ ◄───────► │   Core Server   │
│   (Port 3000)   │    JSON/Events   │   (Spring Boot) │   JSON    │   (Port 5000)   │
│                 │                  │                 │           │                 │
└─────────────────┘                  └─────────────────┘           └─────────────────┘
```

### **Why Frontend Doesn't Handle TCP/Threading**
- **Abstraction Layer**: Gateway handles complex TCP communication
- **Browser Limitations**: No direct TCP socket support trong browsers
- **Simplified Development**: Focus on UI/UX instead of network protocols
- **Security**: Gateway handles authentication và protocol translation

---

## 📊 **DATA FLOW & MESSAGE PATTERNS**

### **Message Type Classifications**

| Category | Message Types | Description |
|----------|---------------|-------------|
| **Authentication** | `LOGIN`, `LOGOUT`, `TOKEN_REFRESH` | User authentication |
| **Game Management** | `CREATE_GAME`, `JOIN_GAME`, `START_GAME` | Game lifecycle |
| **Game Actions** | `PLAY_CARD`, `END_ROUND`, `GAME_OVER` | Real-time gameplay |
| **Real-time Updates** | `PLAYER_JOINED`, `CARD_PLAYED`, `SCORE_UPDATE` | Live notifications |
| **System Events** | `HEARTBEAT`, `ERROR`, `RECONNECT` | Connection management |

### **JSON Message Schema Example**

```json
{
  "messageType": "PLAY_CARD",
  "timestamp": "2025-10-01T10:30:00.000Z",
  "sessionId": "session-123",
  "userId": "user-456",
  "payload": {
    "gameId": "game-789",
    "cardIndex": 5,
    "roundNumber": 3
  },
  "metadata": {
    "version": "1.0",
    "source": "frontend",
    "requestId": "req-abc123"
  }
}
```

---

## 🛠️ **TECHNOLOGY STACK & DEPENDENCIES**

### **Core Dependencies**
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.8.0",
    "@reduxjs/toolkit": "^1.9.1",
    "react-redux": "^8.0.5",
    "socket.io-client": "^4.6.1",
    "axios": "^1.3.0",
    "tailwindcss": "^3.2.0",
    "framer-motion": "^8.5.0",
    "react-hook-form": "^7.43.0",
    "zod": "^3.20.0",
    "@hookform/resolvers": "^2.9.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^3.1.0",
    "vite": "^4.1.0",
    "vitest": "^0.28.0",
    "@testing-library/react": "^13.4.0",
    "@testing-library/jest-dom": "^5.16.0",
    "msw": "^1.0.0",
    "cypress": "^12.5.0",
    "eslint": "^8.34.0",
    "prettier": "^2.8.0"
  }
}
```

---

## 🎯 **IMPLEMENTATION ROADMAP**

### **Week 1: Foundation & WebSocket Setup** 🏗️
**Days 1-2: Project Structure & Configuration**
- [ ] Setup Vite + React + TypeScript project structure
- [ ] Configure Tailwind CSS với custom design system
- [ ] Setup ESLint, Prettier, và development tooling
- [ ] Create basic routing với React Router

**Days 3-4: WebSocket Infrastructure**
- [ ] Implement WebSocketManager service
- [ ] Create connection monitoring và auto-reconnect logic
- [ ] Setup message handler với type-safe dispatching
- [ ] Test basic WebSocket connection với Gateway

**Days 5-7: State Management Foundation**
- [ ] Configure Redux Toolkit với proper store structure
- [ ] Create auth, game, và UI slices
- [ ] Implement WebSocket middleware for real-time updates
- [ ] Setup persistence middleware for local storage

### **Week 2: Core UI Components & Authentication** 🎨
**Days 8-10: UI Component Library**
- [ ] Build atomic UI components (Button, Input, Card, Modal)
- [ ] Create game-specific components (PlayerHand, GameBoard)
- [ ] Implement responsive layout components
- [ ] Setup component testing với React Testing Library

**Days 11-14: Authentication Flow**
- [ ] Build Login/Register pages với form validation
- [ ] Implement JWT token management
- [ ] Create protected route wrapper
- [ ] Setup user profile management

### **Week 3: Game Interface & Real-time Features** 🎮
**Days 15-17: Game Lobby & Matchmaking**
- [ ] Build game lobby interface
- [ ] Implement matchmaking UI
- [ ] Create game room management
- [ ] Add real-time player status updates

**Days 18-21: Game Gameplay Interface**
- [ ] Build main game board component
- [ ] Implement card playing interactions
- [ ] Create score display và round indicators
- [ ] Add game result và winner announcement

### **Week 4: Polish, Testing & Production Ready** ✨
**Days 22-24: Performance & Optimization**
- [ ] Implement lazy loading và code splitting
- [ ] Optimize WebSocket message handling
- [ ] Add loading states và skeleton components
- [ ] Performance testing với realistic data loads

**Days 25-28: Testing & Quality Assurance**
- [ ] Complete unit testing coverage (80%+)
- [ ] Implement E2E testing với Cypress
- [ ] Setup mock server for development testing
- [ ] Cross-browser compatibility testing

---

## 📚 **DOCUMENTATION STRUCTURE**

### **Architecture Documents**
1. **`architecture/component-design.md`** - Component patterns và design system
2. **`architecture/websocket-strategy.md`** - Real-time communication architecture
3. **`architecture/state-management.md`** - Redux patterns và data flow
4. **`architecture/security-guide.md`** - Frontend security best practices

### **Implementation Guides**
1. **`implementation/setup-guide.md`** - Development environment setup
2. **`implementation/websocket-integration.md`** - WebSocket implementation details
3. **`implementation/component-library.md`** - Reusable component guidelines
4. **`implementation/api-integration.md`** - Service layer patterns

### **Testing Documentation**
1. **`testing/testing-strategy.md`** - Comprehensive testing approach
2. **`testing/mock-server.md`** - Development testing setup
3. **`testing/e2e-testing.md`** - End-to-end testing strategies

---

## 🚀 **SUCCESS METRICS & QUALITY GATES**

### **Performance Targets**
- **First Contentful Paint**: < 1.5s
- **Time to Interactive**: < 3s
- **WebSocket Connection**: < 500ms
- **Game Action Response**: < 100ms

### **Quality Standards**
- **Test Coverage**: 80%+ unit tests, 90%+ critical paths
- **TypeScript Coverage**: 95%+ type safety
- **Accessibility**: WCAG 2.1 AA compliance
- **Performance Score**: 90+ Lighthouse score

### **User Experience Metrics**
- **Game Session Duration**: Average 10+ minutes
- **User Retention**: 70%+ return rate
- **Error Rate**: < 1% of user actions
- **Real-time Responsiveness**: < 100ms perceived latency

---

**Next step: Tôi sẽ tạo detailed architecture documents để provide comprehensive implementation guidance cho team. Let's start với component design strategy! 🎯**