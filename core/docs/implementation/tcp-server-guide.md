# TCP Server Implementation Guide - Enterprise Socket Architecture

## 🎯 **TCP SERVER OVERVIEW**

Core TCP Server là **backbone communication layer** giữa Gateway và Core business logic. Server được thiết kế để handle high-concurrent connections với low latency và high reliability.

### **Technical Requirements**
- **Protocol**: TCP với length-prefixed JSON messages
- **Concurrency**: Support 100+ simultaneous connections
- **Latency**: < 50ms response time cho game operations
- **Reliability**: Automatic reconnection và error recovery
- **Security**: Input validation và connection throttling

---

## 🌐 **NETWORK ARCHITECTURE**

### **Connection Flow**
```
Gateway → TCP Connect → Authentication → Message Exchange → Graceful Disconnect
```

### **Message Protocol Design**
```
[4 bytes: Message Length][Variable: JSON Message Content]

Example:
[0x00, 0x00, 0x01, 0x2A] + {"type":"GAME.CARD_PLAY_REQUEST",...}
```

### **Threading Model**
```
Main Thread → Accept Connections → Dispatch to Worker Threads
    ↓
Worker Thread Pool → Process Messages → Send Responses
    ↓
Database Thread Pool → Persist Data → Return Results
```

---

## 🔧 **IMPLEMENTATION COMPONENTS**

### **1. TcpServer.java - Main Server Class**

```java
package com.n9.core.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enterprise TCP Server for Core-Gateway communication.
 * Handles incoming connections and dispatches them to worker threads.
 * 
 * Features:
 * - Configurable thread pool for connection handling
 * - Graceful shutdown with connection cleanup
 * - Connection monitoring and metrics
 * - Error recovery and retry logic
 */
@Component
public class TcpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    
    @Value("${core.tcp.port:5000}")
    private int serverPort;
    
    @Value("${core.tcp.max-connections:100}")
    private int maxConnections;
    
    @Value("${core.tcp.thread-pool-size:20}")
    private int threadPoolSize;
    
    private ServerSocket serverSocket;
    private ExecutorService connectionPool;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private volatile boolean isRunning = false;
    
    private final ConnectionManager connectionManager;
    private final MessageProcessor messageProcessor;
    
    public TcpServer(ConnectionManager connectionManager, 
                    MessageProcessor messageProcessor) {
        this.connectionManager = connectionManager;
        this.messageProcessor = messageProcessor;
    }
    
    /**
     * Start the TCP server and begin accepting connections.
     */
    public void start() throws IOException {
        if (isRunning) {
            logger.warn("TCP Server is already running on port {}", serverPort);
            return;
        }
        
        logger.info("Starting TCP Server on port {}", serverPort);
        
        // Initialize server socket
        serverSocket = new ServerSocket(serverPort);
        serverSocket.setReuseAddress(true);
        
        // Initialize connection thread pool
        connectionPool = Executors.newFixedThreadPool(threadPoolSize, 
            r -> new Thread(r, "core-tcp-worker-" + System.currentTimeMillis()));
        
        isRunning = true;
        
        logger.info("TCP Server started successfully on port {}", serverPort);
        logger.info("Max connections: {}, Thread pool size: {}", maxConnections, threadPoolSize);
        
        // Main connection acceptance loop
        acceptConnections();
    }
    
    /**
     * Main connection acceptance loop.
     * Runs in the main thread and dispatches connections to worker threads.
     */
    private void acceptConnections() {
        while (isRunning && !serverSocket.isClosed()) {
            try {
                // Accept incoming connection
                Socket clientSocket = serverSocket.accept();
                
                // Check connection limits
                if (activeConnections.get() >= maxConnections) {
                    logger.warn("Connection limit reached ({}), rejecting connection from {}", 
                              maxConnections, clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    continue;
                }
                
                // Configure socket options
                configureSocket(clientSocket);
                
                // Create connection handler and submit to thread pool
                ClientConnectionHandler handler = new ClientConnectionHandler(
                    clientSocket, connectionManager, messageProcessor, this);
                
                connectionPool.submit(handler);
                
                int currentConnections = activeConnections.incrementAndGet();
                logger.info("New connection accepted from {}. Active connections: {}", 
                          clientSocket.getRemoteSocketAddress(), currentConnections);
                
            } catch (IOException e) {
                if (isRunning) {
                    logger.error("Error accepting connection", e);
                } else {
                    logger.info("Server stopped, ending connection acceptance loop");
                    break;
                }
            } catch (Exception e) {
                logger.error("Unexpected error in connection acceptance loop", e);
            }
        }
    }
    
    /**
     * Configure socket options for optimal performance.
     */
    private void configureSocket(Socket socket) throws IOException {
        socket.setTcpNoDelay(true);          // Disable Nagle's algorithm for low latency
        socket.setKeepAlive(true);           // Enable keep-alive
        socket.setSoTimeout(30000);          // 30-second read timeout
        socket.setSendBufferSize(64 * 1024); // 64KB send buffer
        socket.setReceiveBufferSize(64 * 1024); // 64KB receive buffer
    }
    
    /**
     * Handle connection closure and cleanup.
     */
    public void onConnectionClosed(String connectionId) {
        int currentConnections = activeConnections.decrementAndGet();
        logger.info("Connection {} closed. Active connections: {}", connectionId, currentConnections);
        
        // Notify connection manager
        connectionManager.removeConnection(connectionId);
    }
    
    /**
     * Gracefully stop the TCP server.
     */
    public void stop() {
        if (!isRunning) {
            logger.warn("TCP Server is not running");
            return;
        }
        
        logger.info("Stopping TCP Server...");
        isRunning = false;
        
        try {
            // Close server socket to stop accepting new connections
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Close all active connections
            connectionManager.closeAllConnections();
            
            // Shutdown thread pool
            if (connectionPool != null) {
                connectionPool.shutdown();
                if (!connectionPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("Thread pool did not terminate gracefully, forcing shutdown");
                    connectionPool.shutdownNow();
                }
            }
            
            logger.info("TCP Server stopped successfully");
            
        } catch (Exception e) {
            logger.error("Error stopping TCP Server", e);
        }
    }
    
    /**
     * Get current server statistics.
     */
    public ServerStats getStats() {
        return ServerStats.builder()
            .isRunning(isRunning)
            .serverPort(serverPort)
            .activeConnections(activeConnections.get())
            .maxConnections(maxConnections)
            .threadPoolSize(threadPoolSize)
            .build();
    }
    
    /**
     * Check if server is running and healthy.
     */
    public boolean isHealthy() {
        return isRunning && 
               serverSocket != null && 
               !serverSocket.isClosed() && 
               connectionPool != null && 
               !connectionPool.isShutdown();
    }
}
```

### **2. ClientConnectionHandler.java - Per-Connection Processing**

```java
package com.n9.core.network;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handles individual client connections in dedicated threads.
 * Responsible for message reading, processing, and response sending.
 * 
 * Features:
 * - Length-prefixed message protocol
 * - Automatic message validation
 * - Error handling and recovery
 * - Connection lifecycle management
 */
public class ClientConnectionHandler implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionHandler.class);
    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64KB
    
    private final Socket clientSocket;
    private final String connectionId;
    private final ConnectionManager connectionManager;
    private final MessageProcessor messageProcessor;
    private final TcpServer tcpServer;
    
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private volatile boolean isConnected = false;
    
    public ClientConnectionHandler(Socket clientSocket,
                                 ConnectionManager connectionManager,
                                 MessageProcessor messageProcessor,
                                 TcpServer tcpServer) {
        this.clientSocket = clientSocket;
        this.connectionId = generateConnectionId();
        this.connectionManager = connectionManager;
        this.messageProcessor = messageProcessor;
        this.tcpServer = tcpServer;
    }
    
    @Override
    public void run() {
        try {
            initializeStreams();
            registerConnection();
            processMessages();
            
        } catch (Exception e) {
            logger.error("Error in connection handler for {}", connectionId, e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * Initialize input/output streams for the connection.
     */
    private void initializeStreams() throws IOException {
        inputStream = new DataInputStream(
            new BufferedInputStream(clientSocket.getInputStream()));
        outputStream = new DataOutputStream(
            new BufferedOutputStream(clientSocket.getOutputStream()));
        
        isConnected = true;
        logger.debug("Streams initialized for connection {}", connectionId);
    }
    
    /**
     * Register this connection with the connection manager.
     */
    private void registerConnection() {
        connectionManager.addConnection(connectionId, this);
        logger.info("Connection {} registered from {}", 
                   connectionId, clientSocket.getRemoteSocketAddress());
    }
    
    /**
     * Main message processing loop.
     */
    private void processMessages() {
        while (isConnected && !clientSocket.isClosed()) {
            try {
                // Read message length
                int messageLength = inputStream.readInt();
                
                // Validate message size
                if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
                    logger.warn("Invalid message length {} from connection {}", 
                              messageLength, connectionId);
                    sendErrorResponse("INVALID_MESSAGE_SIZE", 
                                    "Message size must be between 1 and " + MAX_MESSAGE_SIZE);
                    continue;
                }
                
                // Read message content
                byte[] messageBytes = new byte[messageLength];
                inputStream.readFully(messageBytes);
                
                String messageJson = new String(messageBytes, StandardCharsets.UTF_8);
                logger.debug("Received message from {}: {}", connectionId, messageJson);
                
                // Parse and validate message
                MessageEnvelope envelope = parseMessage(messageJson);
                if (envelope == null) {
                    sendErrorResponse("INVALID_MESSAGE_FORMAT", "Failed to parse message");
                    continue;
                }
                
                // Add connection context
                envelope = envelope.toBuilder()
                    .sessionId(connectionId)
                    .build();
                
                // Process message and get response
                MessageEnvelope response = messageProcessor.processMessage(envelope);
                
                // Send response
                sendMessage(response);
                
            } catch (EOFException e) {
                logger.info("Connection {} closed by client", connectionId);
                break;
            } catch (SocketException e) {
                logger.info("Socket error for connection {}: {}", connectionId, e.getMessage());
                break;
            } catch (IOException e) {
                logger.error("IO error for connection {}", connectionId, e);
                break;
            } catch (Exception e) {
                logger.error("Unexpected error processing message for connection {}", connectionId, e);
                sendErrorResponse("INTERNAL_ERROR", "Unexpected server error");
            }
        }
    }
    
    /**
     * Parse JSON message to MessageEnvelope.
     */
    private MessageEnvelope parseMessage(String messageJson) {
        try {
            return JsonUtils.deserializeMessageEnvelope(messageJson);
        } catch (Exception e) {
            logger.warn("Failed to parse message from connection {}: {}", connectionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Send a message to the client.
     */
    public synchronized void sendMessage(MessageEnvelope envelope) {
        if (!isConnected || clientSocket.isClosed()) {
            logger.warn("Cannot send message to closed connection {}", connectionId);
            return;
        }
        
        try {
            String messageJson = JsonUtils.serializeMessageEnvelope(envelope);
            byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);
            
            // Send length-prefixed message
            outputStream.writeInt(messageBytes.length);
            outputStream.write(messageBytes);
            outputStream.flush();
            
            logger.debug("Sent message to {}: {}", connectionId, messageJson);
            
        } catch (IOException e) {
            logger.error("Failed to send message to connection {}", connectionId, e);
            close();
        }
    }
    
    /**
     * Send error response to client.
     */
    private void sendErrorResponse(String errorCode, String errorMessage) {
        MessageEnvelope errorResponse = MessageEnvelope.builder()
            .type("SYSTEM.ERROR")
            .correlationId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .sessionId(connectionId)
            .error(ErrorInfo.builder()
                .code(errorCode)
                .message(errorMessage)
                .retryable(false)
                .build())
            .build();
        
        sendMessage(errorResponse);
    }
    
    /**
     * Close the connection gracefully.
     */
    public void close() {
        if (!isConnected) {
            return;
        }
        
        isConnected = false;
        
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing connection {}", connectionId, e);
        }
        
        logger.info("Connection {} closed", connectionId);
    }
    
    /**
     * Cleanup resources and notify server.
     */
    private void cleanup() {
        close();
        tcpServer.onConnectionClosed(connectionId);
    }
    
    /**
     * Generate unique connection ID.
     */
    private String generateConnectionId() {
        return "conn-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(clientSocket.hashCode());
    }
    
    /**
     * Get connection information.
     */
    public ConnectionInfo getConnectionInfo() {
        return ConnectionInfo.builder()
            .connectionId(connectionId)
            .remoteAddress(clientSocket.getRemoteSocketAddress().toString())
            .isConnected(isConnected)
            .connectedAt(System.currentTimeMillis())
            .build();
    }
}
```

### **3. MessageProcessor.java - Protocol Message Handling**

```java
package com.n9.core.network;

import com.n9.shared.protocol.MessageEnvelope;
import com.n9.shared.protocol.MessageType;
import com.n9.core.service.GameService;
import com.n9.core.service.UserService;
import com.n9.core.service.MatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Central message processing hub for all protocol messages.
 * Routes messages to appropriate service handlers.
 */
@Component
public class MessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    
    private final GameService gameService;
    private final UserService userService;
    private final MatchmakingService matchmakingService;
    
    public MessageProcessor(GameService gameService,
                          UserService userService,
                          MatchmakingService matchmakingService) {
        this.gameService = gameService;
        this.userService = userService;
        this.matchmakingService = matchmakingService;
    }
    
    /**
     * Process incoming message and return response.
     */
    public MessageEnvelope processMessage(MessageEnvelope envelope) {
        logger.debug("Processing message type: {} from session: {}", 
                    envelope.getType(), envelope.getSessionId());
        
        try {
            switch (envelope.getType()) {
                // Lobby & Matchmaking Messages
                case MessageType.LOBBY_JOIN_REQUEST:
                    return matchmakingService.handleJoinLobby(envelope);
                    
                case MessageType.LOBBY_MATCH_REQUEST:
                    return matchmakingService.handleMatchRequest(envelope);
                    
                case MessageType.LOBBY_LEAVE_REQUEST:
                    return matchmakingService.handleLeaveLobby(envelope);
                
                // Game Messages
                case MessageType.GAME_CARD_PLAY_REQUEST:
                    return gameService.handleCardPlay(envelope);
                    
                case MessageType.GAME_SURRENDER_REQUEST:
                    return gameService.handleSurrender(envelope);
                    
                case MessageType.GAME_REMATCH_REQUEST:
                    return gameService.handleRematchRequest(envelope);
                
                // User Messages
                case MessageType.USER_PROFILE_REQUEST:
                    return userService.handleProfileRequest(envelope);
                    
                case MessageType.USER_STATISTICS_REQUEST:
                    return userService.handleStatisticsRequest(envelope);
                
                // System Messages
                case MessageType.SYSTEM_HEARTBEAT:
                    return handleHeartbeat(envelope);
                    
                case MessageType.SYSTEM_PING:
                    return handlePing(envelope);
                
                default:
                    logger.warn("Unsupported message type: {}", envelope.getType());
                    return createErrorResponse(envelope, "UNSUPPORTED_MESSAGE_TYPE", 
                                             "Message type not supported by Core server");
            }
            
        } catch (Exception e) {
            logger.error("Error processing message type: {}", envelope.getType(), e);
            return createErrorResponse(envelope, "PROCESSING_ERROR", 
                                     "Failed to process message: " + e.getMessage());
        }
    }
    
    /**
     * Handle heartbeat messages for connection keep-alive.
     */
    private MessageEnvelope handleHeartbeat(MessageEnvelope envelope) {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_HEARTBEAT)
            .correlationId(envelope.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .sessionId(envelope.getSessionId())
            .payload(Map.of(
                "status", "alive",
                "serverTime", System.currentTimeMillis()
            ))
            .build();
    }
    
    /**
     * Handle ping messages for latency measurement.
     */
    private MessageEnvelope handlePing(MessageEnvelope envelope) {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_PONG)
            .correlationId(envelope.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .sessionId(envelope.getSessionId())
            .payload(Map.of(
                "clientTimestamp", envelope.getTimestamp(),
                "serverTimestamp", System.currentTimeMillis()
            ))
            .build();
    }
    
    /**
     * Create standardized error response.
     */
    private MessageEnvelope createErrorResponse(MessageEnvelope original, 
                                              String errorCode, String message) {
        return MessageEnvelope.builder()
            .type(MessageType.SYSTEM_ERROR)
            .correlationId(original.getCorrelationId())
            .timestamp(System.currentTimeMillis())
            .sessionId(original.getSessionId())
            .userId(original.getUserId())
            .error(ErrorInfo.builder()
                .code(errorCode)
                .message(message)
                .retryable(false)
                .build())
            .build();
    }
}
```

---

## ⚙️ **CONFIGURATION**

### **application.yml - TCP Server Configuration**

```yaml
# Core TCP Server Configuration
core:
  tcp:
    port: 5000                    # TCP server port
    max-connections: 100          # Maximum concurrent connections
    thread-pool-size: 20          # Connection handling threads
    message-size-limit: 65536     # 64KB max message size
    connection-timeout: 30000     # Connection timeout (30 seconds)
    keep-alive: true              # Enable TCP keep-alive
    
  performance:
    socket-buffer-size: 65536     # 64KB socket buffers
    tcp-no-delay: true            # Disable Nagle's algorithm
    connection-queue-size: 50     # Server socket backlog
    
  monitoring:
    connection-stats: true        # Enable connection statistics
    message-logging: false        # Log all messages (debug only)
    performance-metrics: true     # Enable performance monitoring
```

---

## 📊 **MONITORING & METRICS**

### **Connection Statistics**
- Active connections count
- Total connections processed
- Average connection duration
- Message throughput (messages/second)
- Error rates

### **Performance Metrics**
- Message processing latency
- Memory usage per connection
- CPU utilization
- Network bandwidth usage

---

## 🧪 **TESTING STRATEGY**

### **Unit Tests**
- Message parsing and validation
- Connection lifecycle management
- Error handling scenarios

### **Integration Tests**
- End-to-end message flow
- Concurrent connection handling
- Gateway integration testing

### **Performance Tests**
- Load testing with 100+ connections
- Stress testing with message flooding
- Memory leak detection

### **Example Test Implementation**

```java
@Test
public void testConcurrentConnections() {
    int connectionCount = 50;
    CountDownLatch latch = new CountDownLatch(connectionCount);
    
    // Create multiple test connections
    for (int i = 0; i < connectionCount; i++) {
        executor.submit(() -> {
            try {
                TestTcpClient client = new TestTcpClient("localhost", 5000);
                client.connect();
                client.sendMessage(createTestMessage());
                MessageEnvelope response = client.receiveMessage();
                assertNotNull(response);
                client.disconnect();
            } finally {
                latch.countDown();
            }
        });
    }
    
    // Wait for all connections to complete
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    
    // Verify server stats
    ServerStats stats = tcpServer.getStats();
    assertEquals(0, stats.getActiveConnections()); // All connections closed
}
```

---

## 🔒 **SECURITY CONSIDERATIONS**

### **Input Validation**
- Message size limits to prevent DoS
- JSON structure validation
- Protocol compliance checking

### **Connection Security**
- Rate limiting per connection
- Connection timeout enforcement
- Graceful disconnection handling

### **Error Handling**
- Sanitized error messages
- No sensitive information exposure
- Comprehensive logging for debugging

---

Đây là foundation của TCP Server architecture. Bạn có muốn tôi tiếp tục với **Game Logic Engine** hoặc **Database Design** không?