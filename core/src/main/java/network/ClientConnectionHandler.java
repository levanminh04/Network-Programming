package network;

import com.n9.core.service.GameService;
import com.n9.core.service.GameService.GameState;
import com.n9.shared.model.dto.game.*;
import com.n9.shared.model.dto.match.MatchStartDto;
import com.n9.shared.model.enums.ErrorCode;
import com.n9.shared.protocol.*;
import com.n9.shared.util.JsonUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * ClientConnectionHandler - Xử lý một TCP connection từ Gateway
 * 
 * Tận dụng shared module:
 * - MessageEnvelope: Wrapper chuẩn cho mọi message
 * - MessageFactory: Tạo request/response theo chuẩn
 * - MessageType: Định nghĩa các loại message
 * - DTOs: PlayCardRequestDto, RoundRevealDto, CardDto...
 * 
 * Message Flow:
 * Gateway -> Core: GAME.START (init game), GAME.CARD_PLAY_REQUEST
 * Core -> Gateway: GAME.CARD_PLAY_ACK, GAME.ROUND_REVEAL
 * 
 * @version 1.0.0 (MVP)
 */
public class ClientConnectionHandler implements Runnable {
    private final Socket socket;
    private final GameService gameService;

    // Connection state
    private BufferedReader reader;
    private BufferedWriter writer;
    
    public ClientConnectionHandler(Socket socket, GameService gameService) {
        this.socket = socket;
        this.gameService = gameService;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        System.out.println("✅ New connection from: " + clientAddress);
        
        try {
            reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8")
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("📨 Received from " + clientAddress + ": " + 
                    line.substring(0, Math.min(100, line.length())));
                
                String response = handleMessage(line);
                
                writer.write(response);
                writer.newLine();
                writer.flush();
                
                System.out.println("📤 Sent to " + clientAddress + ": " + 
                    response.substring(0, Math.min(100, response.length())));
            }
            
        } catch (SocketTimeoutException e) {
            System.err.println("⏱️ Timeout from " + clientAddress);
            // TODO: Implement auto-pick logic for timeout
        } catch (IOException e) {
            System.err.println("❌ Connection error from " + clientAddress + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error from " + clientAddress);
            e.printStackTrace();
        } finally {
            cleanup(clientAddress);
        }
    }

    /**
     * Handle incoming message and route to appropriate handler
     * Sử dụng MessageEnvelope từ shared module
     */
    private String handleMessage(String jsonLine) {
        try {
            // Parse message envelope
            MessageEnvelope envelope = JsonUtils.fromJson(jsonLine, MessageEnvelope.class);
            if (envelope == null) {
                return createErrorResponse(null, "Invalid JSON format");
            }
            
            String type = envelope.getType();
            String correlationId = envelope.getCorrelationId();
            
            // Route based on message type
            switch (type) {
                case MessageType.GAME_START:
                    return handleGameStart(envelope);
                    
                case MessageType.GAME_CARD_PLAY_REQUEST:
                    return handlePlayCard(envelope);
                    
                case MessageType.GAME_STATE_SYNC:
                    return handleGameState(envelope);
                    
                default:
                    return createErrorResponse(correlationId, "Unknown message type: " + type);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error handling message: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(null, "Server error: " + e.getMessage());
        }
    }

    /**
     * Handle GAME.START request - Initialize new game
     * 
     * Request payload: MatchStartDto {matchId, gameId, playerPosition...}
     * Response: GAME.CARD_PLAY_ACK with player's hand
     */
    private String handleGameStart(MessageEnvelope envelope) {
        try {
            // Parse request payload
            MatchStartDto startDto = JsonUtils.getObjectMapper().convertValue(
                envelope.getPayload(), 
                MatchStartDto.class
            );
            
            // Validate
            if (startDto.getMatchId() == null) {
                return createErrorResponse(envelope.getCorrelationId(), 
                    "Missing matchId in GAME.START");
            }
            
            // Extract player IDs from envelope
            String player1Id = envelope.getUserId(); // Current player
            String matchId = startDto.getMatchId();
            
            // Note: Trong MVP, Gateway sẽ gọi 2 lần với player1 và player2
            // Hoặc cần có thêm field player2Id trong MatchStartDto
            
            // Initialize game (hoặc retrieve nếu đã init)
            GameState game = gameService.getGameState(matchId);
            if (game == null) {
                // Cần có cả 2 player IDs để init
                // Tạm thời throw error, Gateway cần gửi đầy đủ thông tin
                return createErrorResponse(envelope.getCorrelationId(),
                    "Game not initialized. Need both player IDs.");
            }
            
            // Get player's hand
            java.util.List<CardDto> playerHand = gameService.getPlayerHand(matchId, player1Id);
            
            // Create response - ACK that player received their cards
            PlayCardAckDto ackDto = new PlayCardAckDto();
            ackDto.setGameId(matchId);
            ackDto.setRoundNumber(game.getCurrentRound());
            ackDto.setCardId(null); // No specific card yet
            ackDto.setWaitingForOpponent(false);
            
            MessageEnvelope response = MessageFactory.createResponse(
                MessageType.GAME_CARD_PLAY_ACK,
                envelope.getCorrelationId(),
                ackDto
            );
            
            System.out.println("🎮 Game started for player: " + player1Id + " in match: " + matchId);
            
            return JsonUtils.toJson(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error starting game: " + e.getMessage());
            return createErrorResponse(envelope.getCorrelationId(), 
                "Failed to start game: " + e.getMessage());
        }
    }

    /**
     * Handle GAME.CARD_PLAY_REQUEST - Player plays a card
     * 
     * Request payload: PlayCardRequestDto {gameId, cardId, isAutoPick...}
     * Response: GAME.CARD_PLAY_ACK with played card info
     */
    private String handlePlayCard(MessageEnvelope envelope) {
        try {
            // Parse request
            PlayCardRequestDto request = JsonUtils.getObjectMapper().convertValue(
                envelope.getPayload(),
                PlayCardRequestDto.class
            );
            
            // Validate
            if (request.getGameId() == null || envelope.getUserId() == null) {
                return createErrorResponse(envelope.getCorrelationId(),
                    "Missing gameId or userId");
            }
            
            String matchId = request.getGameId();
            String playerId = envelope.getUserId();
            
            // Play card (không có auto-pick trong PlayCardRequestDto hiện tại)
            CardDto playedCard = gameService.playCard(matchId, playerId, request.getCardId());
            System.out.println("🃏 Player " + playerId + " played: " + 
                gameService.formatCard(playedCard));
            
            // Create ACK response
            PlayCardAckDto ackDto = new PlayCardAckDto();
            ackDto.setGameId(matchId);
            ackDto.setRoundNumber(request.getRoundNumber());
            ackDto.setCardId(playedCard.getCardId());
            ackDto.setWaitingForOpponent(true);
            
            MessageEnvelope response = MessageFactory.createResponse(
                MessageType.GAME_CARD_PLAY_ACK,
                envelope.getCorrelationId(),
                ackDto
            );
            
            return JsonUtils.toJson(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Invalid card play: " + e.getMessage());
            return createErrorResponse(envelope.getCorrelationId(), e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error playing card: " + e.getMessage());
            return createErrorResponse(envelope.getCorrelationId(), 
                "Failed to play card: " + e.getMessage());
        }
    }

    /**
     * Handle GAME.STATE_SYNC - Get current game state
     * 
     * Response: Current round, scores, player's remaining hand
     */
    private String handleGameState(MessageEnvelope envelope) {
        try {
            String matchId = envelope.getMatchId();
            String playerId = envelope.getUserId();
            
            // Validate
            if (matchId == null || playerId == null) {
                return createErrorResponse(envelope.getCorrelationId(),
                    "Missing matchId or userId");
            }
            
            // Get game state
            GameState game = gameService.getGameState(matchId);
            if (game == null) {
                return createErrorResponse(envelope.getCorrelationId(),
                    "Game not found: " + matchId);
            }
            
            // Determine player perspective
            boolean isPlayer1 = playerId.equals(game.getPlayer1Id());
            
            // Build response using RoundStartDto
            RoundStartDto stateDto = new RoundStartDto();
            stateDto.setGameId(matchId);
            stateDto.setRoundNumber(game.getCurrentRound());
            stateDto.setPlayerScore(isPlayer1 ? game.getPlayer1Score() : game.getPlayer2Score());
            stateDto.setOpponentScore(isPlayer1 ? game.getPlayer2Score() : game.getPlayer1Score());
            stateDto.setHand(isPlayer1 ? game.getPlayer1Hand() : game.getPlayer2Hand());
            
            MessageEnvelope response = MessageFactory.createResponse(
                MessageType.GAME_ROUND_START,
                envelope.getCorrelationId(),
                stateDto
            );
            
            return JsonUtils.toJson(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting game state: " + e.getMessage());
            return createErrorResponse(envelope.getCorrelationId(),
                "Failed to get game state: " + e.getMessage());
        }
    }

    /**
     * Create error response using shared protocol
     */
    private String createErrorResponse(String correlationId, String errorMessage) {
        try {
            MessageEnvelope errorEnvelope = MessageFactory.createError(
                correlationId != null ? correlationId : "unknown",
                ErrorCode.SYSTEM_INTERNAL_ERROR,
                errorMessage
            );
            return JsonUtils.toJson(errorEnvelope);
        } catch (Exception e) {
            // Fallback to simple JSON if MessageFactory fails
            return "{\"type\":\"SYSTEM.ERROR\",\"error\":{\"message\":\"" + errorMessage + "\"}}";
        }
    }

    /**
     * Cleanup resources when connection closes
     */
    private void cleanup(String clientAddress) {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🧹 Connection closed: " + clientAddress);
        } catch (IOException e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
        }
    }
}
