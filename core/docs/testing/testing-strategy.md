# Core Module Testing Strategy & Quality Assurance

## 🧪 **COMPREHENSIVE TESTING FRAMEWORK**

Testing strategy cho Core module theo enterprise standards với comprehensive coverage, automation, và quality gates để đảm bảo robust và reliable system.

---

## 📊 **TESTING PYRAMID & STRATEGY OVERVIEW**

### **Testing Pyramid Architecture**
```
                   E2E Tests (5%)
                  ↗             ↖
            Integration Tests (25%)
           ↗                     ↖
      Unit Tests (70%)
     ↗                           ↖
Static Analysis & Code Quality (Base Layer)
```

### **Quality Metrics Targets**
| Metric | Target | Measurement |
|--------|--------|-------------|
| Unit Test Coverage | 90%+ | JaCoCo |
| Integration Test Coverage | 80%+ | Spring Boot Test |
| Code Quality Score | A Grade | SonarQube |
| Performance SLA | < 50ms response | Load testing |
| Security Score | 95%+ | Security scanning |

---

## 🎯 **1. UNIT TESTING FRAMEWORK**

### **Core Service Layer Testing**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Game Service Unit Tests")
class GameServiceTest {
    
    @Mock private GameRepository gameRepository;
    @Mock private UserService userService;
    @Mock private StatisticsService statisticsService;
    @Mock private NotificationService notificationService;
    @InjectMocks private GameService gameService;
    
    @Nested
    @DisplayName("Game Creation Tests")
    class GameCreationTests {
        
        @Test
        @DisplayName("Should create game successfully with valid players")
        void shouldCreateGameWithValidPlayers() {
            // Given
            String player1Id = "player-1";
            String player2Id = "player-2";
            String gameMode = "QUICK";
            
            when(userService.validateUserAvailable(player1Id)).thenReturn(true);
            when(userService.validateUserAvailable(player2Id)).thenReturn(true);
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
                Game game = invocation.getArgument(0);
                game.setId("game-123");
                return game;
            });
            
            // When
            Game result = gameService.createGame(player1Id, player2Id, gameMode);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("game-123");
            assertThat(result.getPlayer1Id()).isEqualTo(player1Id);
            assertThat(result.getPlayer2Id()).isEqualTo(player2Id);
            assertThat(result.getGameMode()).isEqualTo(gameMode);
            assertThat(result.getStatus()).isEqualTo(GameStatus.WAITING_TO_START);
            
            verify(userService).validateUserAvailable(player1Id);
            verify(userService).validateUserAvailable(player2Id);
            verify(gameRepository).save(any(Game.class));
        }
        
        @Test
        @DisplayName("Should throw exception when player1 not available")
        void shouldThrowExceptionWhenPlayer1NotAvailable() {
            // Given
            String player1Id = "busy-player";
            String player2Id = "available-player";
            
            when(userService.validateUserAvailable(player1Id))
                .thenThrow(new PlayerNotAvailableException("Player is in another game"));
            
            // When & Then
            assertThatThrownBy(() -> gameService.createGame(player1Id, player2Id, "QUICK"))
                .isInstanceOf(PlayerNotAvailableException.class)
                .hasMessage("Player is in another game");
            
            verify(userService).validateUserAvailable(player1Id);
            verify(userService, never()).validateUserAvailable(player2Id);
            verify(gameRepository, never()).save(any(Game.class));
        }
        
        @Test
        @DisplayName("Should throw exception for same player")
        void shouldThrowExceptionForSamePlayer() {
            // Given
            String playerId = "same-player";
            
            // When & Then
            assertThatThrownBy(() -> gameService.createGame(playerId, playerId, "QUICK"))
                .isInstanceOf(InvalidGameConfigurationException.class)
                .hasMessage("Players cannot be the same");
            
            verifyNoInteractions(userService, gameRepository);
        }
    }
    
    @Nested
    @DisplayName("Game Move Processing Tests")
    class GameMoveTests {
        
        @Test
        @DisplayName("Should process valid move successfully")
        void shouldProcessValidMoveSuccessfully() {
            // Given
            String gameId = "game-123";
            String playerId = "player-1";
            int cardIndex = 5;
            int roundNumber = 1;
            
            Game game = createTestGame(gameId, playerId, "player-2");
            game.setStatus(GameStatus.IN_PROGRESS);
            game.setCurrentRound(1);
            game.setCurrentPlayerTurn(playerId);
            
            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // When
            GameMoveResult result = gameService.processMove(gameId, playerId, cardIndex, roundNumber);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getGameId()).isEqualTo(gameId);
            assertThat(result.getPlayerId()).isEqualTo(playerId);
            
            verify(gameRepository).findById(gameId);
            verify(gameRepository).save(game);
        }
        
        @Test
        @DisplayName("Should reject move when not player's turn")
        void shouldRejectMoveWhenNotPlayerTurn() {
            // Given
            String gameId = "game-123";
            String playerId = "player-1";
            String otherPlayerId = "player-2";
            
            Game game = createTestGame(gameId, playerId, otherPlayerId);
            game.setCurrentPlayerTurn(otherPlayerId); // Other player's turn
            
            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            
            // When & Then
            assertThatThrownBy(() -> gameService.processMove(gameId, playerId, 5, 1))
                .isInstanceOf(InvalidMoveException.class)
                .hasMessage("Not your turn");
            
            verify(gameRepository).findById(gameId);
            verify(gameRepository, never()).save(any(Game.class));
        }
    }
    
    private Game createTestGame(String gameId, String player1Id, String player2Id) {
        return Game.builder()
            .id(gameId)
            .player1Id(player1Id)
            .player2Id(player2Id)
            .gameMode("QUICK")
            .status(GameStatus.WAITING_TO_START)
            .currentRound(1)
            .build();
    }
}
```

### **Game Logic Engine Testing**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Game Engine Unit Tests")
class GameEngineTest {
    
    @Mock private GameSessionRepository sessionRepository;
    @Mock private CardDeckService cardDeckService;
    @Mock private ScoreCalculatorService scoreCalculator;
    @InjectMocks private GameEngine gameEngine;
    
    @Test
    @DisplayName("Should initialize game session correctly")
    void shouldInitializeGameSessionCorrectly() {
        // Given
        String player1Id = "player-1";
        String player2Id = "player-2";
        String gameMode = "QUICK";
        
        CardDeck mockDeck = createMockDeck();
        when(cardDeckService.createShuffledDeck()).thenReturn(mockDeck);
        
        // When
        GameSession session = gameEngine.initializeGame(player1Id, player2Id, gameMode);
        
        // Then
        assertThat(session).isNotNull();
        assertThat(session.getPlayer1Id()).isEqualTo(player1Id);
        assertThat(session.getPlayer2Id()).isEqualTo(player2Id);
        assertThat(session.getGameMode()).isEqualTo(gameMode);
        assertThat(session.getCurrentRound()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(GameStatus.READY_TO_START);
        assertThat(session.getPlayer1Hand()).hasSize(13);
        assertThat(session.getPlayer2Hand()).hasSize(13);
        
        verify(cardDeckService).createShuffledDeck();
    }
    
    @Test
    @DisplayName("Should calculate round winner correctly")
    void shouldCalculateRoundWinnerCorrectly() {
        // Given
        GameSession session = createTestSession();
        Card player1Card = new Card(Suit.HEARTS, Rank.KING);
        Card player2Card = new Card(Suit.SPADES, Rank.QUEEN);
        
        when(scoreCalculator.compareCards(player1Card, player2Card, Suit.HEARTS))
            .thenReturn(1); // Player 1 wins
        
        // When
        RoundResult result = gameEngine.processRound(session, player1Card, player2Card);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getWinnerId()).isEqualTo(session.getPlayer1Id());
        assertThat(result.getPlayer1Card()).isEqualTo(player1Card);
        assertThat(result.getPlayer2Card()).isEqualTo(player2Card);
        
        verify(scoreCalculator).compareCards(player1Card, player2Card, Suit.HEARTS);
    }
    
    private CardDeck createMockDeck() {
        // Create a standard 52-card deck for testing
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        return new CardDeck(cards);
    }
    
    private GameSession createTestSession() {
        return GameSession.builder()
            .sessionId("session-123")
            .player1Id("player-1")
            .player2Id("player-2")
            .gameMode("QUICK")
            .currentRound(1)
            .status(GameStatus.IN_PROGRESS)
            .trumpSuit(Suit.HEARTS)
            .build();
    }
}
```

### **Repository Layer Testing**

```java
@DataJpaTest
@TestPropertySource("classpath:application-test.yml")
@DisplayName("Game Repository Unit Tests")
class GameRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Test
    @DisplayName("Should find active games by player ID")
    void shouldFindActiveGamesByPlayerId() {
        // Given
        String playerId = "player-123";
        
        Game activeGame1 = createTestGame("game-1", playerId, "other-player-1", GameStatus.IN_PROGRESS);
        Game activeGame2 = createTestGame("game-2", "other-player-2", playerId, GameStatus.WAITING_TO_START);
        Game completedGame = createTestGame("game-3", playerId, "other-player-3", GameStatus.COMPLETED);
        
        entityManager.persistAndFlush(activeGame1);
        entityManager.persistAndFlush(activeGame2);
        entityManager.persistAndFlush(completedGame);
        
        // When
        List<Game> activeGames = gameRepository.findActiveGamesByPlayerId(playerId);
        
        // Then
        assertThat(activeGames).hasSize(2);
        assertThat(activeGames).extracting(Game::getId)
            .containsExactlyInAnyOrder("game-1", "game-2");
        assertThat(activeGames).allMatch(game -> 
            game.getStatus() == GameStatus.IN_PROGRESS || 
            game.getStatus() == GameStatus.WAITING_TO_START);
    }
    
    @Test
    @DisplayName("Should find games by status and date range")
    void shouldFindGamesByStatusAndDateRange() {
        // Given
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(Duration.ofDays(1));
        Instant twoDaysAgo = now.minus(Duration.ofDays(2));
        
        Game recentGame = createTestGame("recent-game", "p1", "p2", GameStatus.COMPLETED);
        recentGame.setCreatedAt(now.minus(Duration.ofHours(2)));
        
        Game oldGame = createTestGame("old-game", "p3", "p4", GameStatus.COMPLETED);
        oldGame.setCreatedAt(twoDaysAgo);
        
        entityManager.persistAndFlush(recentGame);
        entityManager.persistAndFlush(oldGame);
        
        // When
        List<Game> games = gameRepository.findGamesByStatusAndDateRange(
            GameStatus.COMPLETED, oneDayAgo, now);
        
        // Then
        assertThat(games).hasSize(1);
        assertThat(games.get(0).getId()).isEqualTo("recent-game");
    }
    
    private Game createTestGame(String id, String player1Id, String player2Id, GameStatus status) {
        return Game.builder()
            .id(id)
            .player1Id(player1Id)
            .player2Id(player2Id)
            .gameMode("QUICK")
            .status(status)
            .createdAt(Instant.now())
            .build();
    }
}
```

---

## 🔗 **2. INTEGRATION TESTING FRAMEWORK**

### **Service Integration Tests**

```java
@SpringBootTest
@Transactional
@DisplayName("Game Service Integration Tests")
class GameServiceIntegrationTest {
    
    @Autowired private GameService gameService;
    @Autowired private UserService userService;
    @Autowired private GameRepository gameRepository;
    @Autowired private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        // Create test users
        User player1 = createTestUser("player-1", "player1", "player1@test.com");
        User player2 = createTestUser("player-2", "player2", "player2@test.com");
        
        userRepository.save(player1);
        userRepository.save(player2);
    }
    
    @Test
    @DisplayName("Should handle complete game flow")
    void shouldHandleCompleteGameFlow() {
        // Given
        String player1Id = "player-1";
        String player2Id = "player-2";
        
        // When - Create game
        Game game = gameService.createGame(player1Id, player2Id, "QUICK");
        
        // Then - Verify game creation
        assertThat(game).isNotNull();
        assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING_TO_START);
        
        // When - Start game
        gameService.startGame(game.getId());
        
        // Then - Verify game started
        Game startedGame = gameRepository.findById(game.getId()).orElseThrow();
        assertThat(startedGame.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        
        // When - Process moves for complete round
        GameMoveResult move1 = gameService.processMove(game.getId(), player1Id, 0, 1);
        GameMoveResult move2 = gameService.processMove(game.getId(), player2Id, 0, 1);
        
        // Then - Verify moves processed
        assertThat(move1.isSuccess()).isTrue();
        assertThat(move2.isSuccess()).isTrue();
        
        Game updatedGame = gameRepository.findById(game.getId()).orElseThrow();
        assertThat(updatedGame.getCurrentRound()).isEqualTo(2); // Round completed
    }
    
    @Test
    @DisplayName("Should handle concurrent game creation")
    void shouldHandleConcurrentGameCreation() throws InterruptedException {
        // Given
        String player1Id = "player-1";
        String player2Id = "player-2";
        
        CountDownLatch latch = new CountDownLatch(2);
        List<Game> createdGames = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        // When - Attempt concurrent game creation
        Runnable createGame = () -> {
            try {
                Game game = gameService.createGame(player1Id, player2Id, "QUICK");
                createdGames.add(game);
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        };
        
        Thread thread1 = new Thread(createGame);
        Thread thread2 = new Thread(createGame);
        
        thread1.start();
        thread2.start();
        
        latch.await(5, TimeUnit.SECONDS);
        
        // Then - Only one game should be created
        assertThat(createdGames).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.get(0)).isInstanceOf(PlayerNotAvailableException.class);
    }
    
    private User createTestUser(String id, String username, String email) {
        return User.builder()
            .id(id)
            .username(username)
            .email(email)
            .passwordHash("hashed-password")
            .createdAt(Instant.now())
            .build();
    }
}
```

### **TCP Server Integration Tests**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("TCP Server Integration Tests")
class TcpServerIntegrationTest {
    
    @Value("${app.tcp.port:8081}")
    private int tcpPort;
    
    @Autowired private TcpServer tcpServer;
    @Autowired private MessageProcessor messageProcessor;
    
    private Socket clientSocket;
    private DataInputStream input;
    private DataOutputStream output;
    
    @BeforeEach
    void setUp() throws IOException {
        clientSocket = new Socket("localhost", tcpPort);
        input = new DataInputStream(clientSocket.getInputStream());
        output = new DataOutputStream(clientSocket.getOutputStream());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }
    
    @Test
    @DisplayName("Should handle message exchange correctly")
    void shouldHandleMessageExchangeCorrectly() throws IOException {
        // Given
        CreateGameRequest request = CreateGameRequest.builder()
            .player1Id("player-1")
            .player2Id("player-2")
            .gameMode("QUICK")
            .build();
        
        MessageEnvelope envelope = MessageEnvelope.builder()
            .messageType("CREATE_GAME")
            .payload(JsonUtils.toJson(request))
            .timestamp(Instant.now())
            .build();
        
        String message = JsonUtils.toJson(envelope);
        
        // When - Send message
        sendMessage(message);
        
        // Then - Receive response
        String response = receiveMessage();
        
        assertThat(response).isNotNull();
        
        MessageEnvelope responseEnvelope = JsonUtils.fromJson(response, MessageEnvelope.class);
        assertThat(responseEnvelope.getMessageType()).isEqualTo("CREATE_GAME_RESPONSE");
        
        CreateGameResponse gameResponse = JsonUtils.fromJson(
            responseEnvelope.getPayload(), CreateGameResponse.class);
        assertThat(gameResponse.isSuccess()).isTrue();
        assertThat(gameResponse.getGameId()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle multiple concurrent connections")
    void shouldHandleMultipleConcurrentConnections() throws InterruptedException {
        // Given
        int numberOfConnections = 10;
        CountDownLatch latch = new CountDownLatch(numberOfConnections);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());
        
        // When
        for (int i = 0; i < numberOfConnections; i++) {
            final int connectionId = i;
            new Thread(() -> {
                try (Socket socket = new Socket("localhost", tcpPort);
                     DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                    
                    // Send ping message
                    MessageEnvelope ping = MessageEnvelope.builder()
                        .messageType("PING")
                        .payload("{}")
                        .timestamp(Instant.now())
                        .build();
                    
                    sendMessage(out, JsonUtils.toJson(ping));
                    String response = receiveMessage(in);
                    
                    results.add(response.contains("PONG"));
                    
                } catch (IOException e) {
                    results.add(false);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(10, TimeUnit.SECONDS);
        
        // Then
        assertThat(results).hasSize(numberOfConnections);
        assertThat(results).allMatch(result -> result);
    }
    
    private void sendMessage(String message) throws IOException {
        sendMessage(output, message);
    }
    
    private void sendMessage(DataOutputStream out, String message) throws IOException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(messageBytes.length); // Length prefix
        out.write(messageBytes);
        out.flush();
    }
    
    private String receiveMessage() throws IOException {
        return receiveMessage(input);
    }
    
    private String receiveMessage(DataInputStream in) throws IOException {
        int messageLength = in.readInt();
        byte[] messageBytes = new byte[messageLength];
        in.readFully(messageBytes);
        return new String(messageBytes, StandardCharsets.UTF_8);
    }
}
```

---

## 🚀 **3. PERFORMANCE TESTING FRAMEWORK**

### **Load Testing with Custom Framework**

```java
@SpringBootTest
@DisplayName("Performance Tests")
class PerformanceTestSuite {
    
    @Autowired private GameService gameService;
    @Autowired private UserService userService;
    
    @Test
    @DisplayName("Should handle concurrent game creation under load")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentGameCreationUnderLoad() throws InterruptedException {
        // Given
        int numberOfConcurrentGames = 50;
        CountDownLatch latch = new CountDownLatch(numberOfConcurrentGames);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        // Create test users
        for (int i = 0; i < numberOfConcurrentGames * 2; i++) {
            User user = createTestUser("user-" + i, "user" + i, "user" + i + "@test.com");
            userService.createUser(user);
        }
        
        // When
        for (int i = 0; i < numberOfConcurrentGames; i++) {
            final int gameIndex = i;
            new Thread(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    String player1Id = "user-" + (gameIndex * 2);
                    String player2Id = "user-" + (gameIndex * 2 + 1);
                    
                    Game game = gameService.createGame(player1Id, player2Id, "QUICK");
                    
                    long endTime = System.currentTimeMillis();
                    responseTimes.add(endTime - startTime);
                    
                    if (game != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        
        // Then
        assertThat(successCount.get()).isEqualTo(numberOfConcurrentGames);
        assertThat(errorCount.get()).isEqualTo(0);
        
        // Performance assertions
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        assertThat(averageResponseTime).isLessThan(50.0); // < 50ms average
        assertThat(maxResponseTime).isLessThan(200); // < 200ms max
        
        System.out.printf("Performance Results: Avg=%.2fms, Max=%dms, Games=%d%n",
            averageResponseTime, maxResponseTime, numberOfConcurrentGames);
    }
    
    @Test
    @DisplayName("Should maintain performance under sustained load")
    void shouldMaintainPerformanceUnderSustainedLoad() throws InterruptedException {
        // Given
        int durationMinutes = 5;
        int requestsPerSecond = 10;
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        // Start background load
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(requestsPerSecond);
        
        Runnable loadTask = () -> {
            if (stopFlag.get()) return;
            
            try {
                long startTime = System.currentTimeMillis();
                
                // Perform lightweight operation
                List<Game> games = gameService.getRecentGames(Duration.ofHours(1));
                
                long endTime = System.currentTimeMillis();
                responseTimes.add(endTime - startTime);
                
                totalRequests.incrementAndGet();
                successfulRequests.incrementAndGet();
                
            } catch (Exception e) {
                totalRequests.incrementAndGet();
            }
        };
        
        // When
        executor.scheduleAtFixedRate(loadTask, 0, 1000 / requestsPerSecond, TimeUnit.MILLISECONDS);
        
        Thread.sleep(durationMinutes * 60 * 1000); // Run for specified duration
        stopFlag.set(true);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // Then
        double successRate = (double) successfulRequests.get() / totalRequests.get() * 100;
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        assertThat(successRate).isGreaterThan(99.0); // > 99% success rate
        assertThat(averageResponseTime).isLessThan(50.0); // < 50ms average
        
        System.out.printf("Sustained Load Results: Success Rate=%.2f%%, Avg Response=%.2fms, Total Requests=%d%n",
            successRate, averageResponseTime, totalRequests.get());
    }
}
```

### **Memory and Resource Testing**

```java
@SpringBootTest
@DisplayName("Resource Usage Tests")
class ResourceUsageTest {
    
    @Autowired private GameService gameService;
    
    @Test
    @DisplayName("Should not have memory leaks during game operations")
    void shouldNotHaveMemoryLeaksDuringGameOperations() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection để get baseline
        System.gc();
        Thread.yield();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Create and complete many games
        for (int i = 0; i < 100; i++) {
            createAndCompleteGame("player-" + (i * 2), "player-" + (i * 2 + 1));
            
            // Periodic cleanup suggestion
            if (i % 20 == 0) {
                System.gc();
                Thread.yield();
            }
        }
        
        // Force final garbage collection
        System.gc();
        Thread.yield();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Then
        long memoryIncrease = finalMemory - baselineMemory;
        long memoryIncreasePerGame = memoryIncrease / 100;
        
        // Memory increase should be reasonable
        assertThat(memoryIncreasePerGame).isLessThan(1024 * 1024); // < 1MB per game
        
        System.out.printf("Memory Usage: Baseline=%d bytes, Final=%d bytes, Increase=%d bytes%n",
            baselineMemory, finalMemory, memoryIncrease);
    }
    
    private void createAndCompleteGame(String player1Id, String player2Id) {
        try {
            // Create users if they don't exist
            createUserIfNotExists(player1Id);
            createUserIfNotExists(player2Id);
            
            // Create và complete game
            Game game = gameService.createGame(player1Id, player2Id, "QUICK");
            gameService.startGame(game.getId());
            
            // Simulate game completion
            for (int round = 1; round <= 13; round++) {
                gameService.processMove(game.getId(), player1Id, 0, round);
                gameService.processMove(game.getId(), player2Id, 0, round);
            }
            
        } catch (Exception e) {
            // Ignore errors for this test
        }
    }
}
```

---

## 🔍 **4. END-TO-END TESTING FRAMEWORK**

### **Complete System Flow Tests**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("End-to-End System Tests")
class EndToEndSystemTest {
    
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private TcpTestClient tcpTestClient;
    
    @Test
    @DisplayName("Should handle complete user journey")
    void shouldHandleCompleteUserJourney() throws Exception {
        // Given - User registration
        UserRegistrationRequest registrationRequest = UserRegistrationRequest.builder()
            .username("testuser")
            .email("testuser@example.com")
            .password("SecurePassword123!")
            .build();
        
        ResponseEntity<UserRegistrationResponse> registrationResponse = 
            restTemplate.postForEntity("/api/auth/register", registrationRequest, UserRegistrationResponse.class);
        
        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // When - User login
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePassword123!");
        ResponseEntity<AuthenticationResponse> loginResponse = 
            restTemplate.postForEntity("/api/auth/login", loginRequest, AuthenticationResponse.class);
        
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = loginResponse.getBody().getAccessToken();
        
        // Then - Create game via TCP
        CreateGameRequest gameRequest = CreateGameRequest.builder()
            .player1Id(registrationResponse.getBody().getUserId())
            .player2Id("ai-player")
            .gameMode("QUICK")
            .build();
        
        CreateGameResponse gameResponse = tcpTestClient.sendMessage(gameRequest, accessToken);
        assertThat(gameResponse.isSuccess()).isTrue();
        
        // And - Play complete game
        String gameId = gameResponse.getGameId();
        playCompleteGame(gameId, registrationResponse.getBody().getUserId(), accessToken);
        
        // Finally - Verify game completion
        GameStatusResponse statusResponse = tcpTestClient.getGameStatus(gameId, accessToken);
        assertThat(statusResponse.getStatus()).isEqualTo("COMPLETED");
    }
    
    private void playCompleteGame(String gameId, String playerId, String accessToken) throws Exception {
        for (int round = 1; round <= 13; round++) {
            // Player move
            GameMoveRequest moveRequest = GameMoveRequest.builder()
                .gameId(gameId)
                .playerId(playerId)
                .cardIndex(0) // Always play first card
                .roundNumber(round)
                .build();
            
            GameMoveResponse moveResponse = tcpTestClient.sendMessage(moveRequest, accessToken);
            assertThat(moveResponse.isSuccess()).isTrue();
            
            // Wait for AI move (simulated)
            Thread.sleep(100);
        }
    }
}
```

---

## 📊 **5. TEST AUTOMATION & CI/CD INTEGRATION**

### **Maven Test Configuration**

```xml
<!-- pom.xml testing configuration -->
<properties>
    <maven.surefire.version>3.0.0-M7</maven.surefire.version>
    <maven.failsafe.version>3.0.0-M7</maven.failsafe.version>
    <jacoco.version>0.8.8</jacoco.version>
</properties>

<dependencies>
    <!-- Testing dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Surefire for unit tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${maven.surefire.version}</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                    <exclude>**/*IT.java</exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Failsafe for integration tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>${maven.failsafe.version}</version>
            <configuration>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        
        <!-- JaCoCo for code coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>${jacoco.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.90</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### **GitHub Actions CI Pipeline**

```yaml
# .github/workflows/ci.yml
name: Core Module CI/CD

on:
  push:
    branches: [ main, develop ]
    paths: [ 'core/**' ]
  pull_request:
    branches: [ main ]
    paths: [ 'core/**' ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: cardgame_test
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=3
      
      redis:
        image: redis:6.2
        ports:
          - 6379:6379
        options: --health-cmd="redis-cli ping" --health-interval=10s --health-timeout=5s --health-retries=3
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'adopt'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Run unit tests
      run: mvn -f core/pom.xml clean test
      env:
        SPRING_PROFILES_ACTIVE: test
    
    - name: Run integration tests
      run: mvn -f core/pom.xml verify
      env:
        SPRING_PROFILES_ACTIVE: test
        DATABASE_URL: jdbc:mysql://localhost:3306/cardgame_test
        DATABASE_USERNAME: root
        DATABASE_PASSWORD: root
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: always()
      with:
        name: Core Module Tests
        path: 'core/target/surefire-reports/*.xml,core/target/failsafe-reports/*.xml'
        reporter: java-junit
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: core/target/site/jacoco/jacoco.xml
        flags: core-module
        name: core-coverage
```

---

## 📈 **6. QUALITY GATES & METRICS**

### **Quality Gates Configuration**

```java
@Configuration
public class QualityGatesConfig {
    
    public static final double MINIMUM_CODE_COVERAGE = 0.90; // 90%
    public static final double MINIMUM_BRANCH_COVERAGE = 0.85; // 85%
    public static final int MAXIMUM_CYCLOMATIC_COMPLEXITY = 10;
    public static final int MAXIMUM_METHOD_LENGTH = 50;
    public static final int MAXIMUM_CLASS_LENGTH = 500;
    
    @Bean
    public QualityGateValidator qualityGateValidator() {
        return new QualityGateValidator();
    }
}

public class QualityGateValidator {
    
    public QualityReport validateQualityGates(String projectPath) {
        QualityReport report = new QualityReport();
        
        // Code coverage validation
        double coverage = calculateCodeCoverage(projectPath);
        report.addMetric("code_coverage", coverage, coverage >= MINIMUM_CODE_COVERAGE);
        
        // Complexity validation
        int maxComplexity = calculateMaxComplexity(projectPath);
        report.addMetric("cyclomatic_complexity", maxComplexity, 
                        maxComplexity <= MAXIMUM_CYCLOMATIC_COMPLEXITY);
        
        // Code quality validation
        int codeSmells = countCodeSmells(projectPath);
        report.addMetric("code_smells", codeSmells, codeSmells == 0);
        
        return report;
    }
}
```

**Comprehensive testing framework này đảm bảo Core module có robust quality assurance với automated testing, continuous integration, và clear quality metrics để đáp ứng enterprise standards cho university project.**