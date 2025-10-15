import com.n9.shared.protocol.*;
import com.n9.shared.model.dto.game.PlayCardRequestDto;
import com.n9.shared.model.dto.match.MatchStartDto;
import com.n9.shared.util.JsonUtils;

import java.io.*;
import java.net.Socket;

/**
 * Test Client để kiểm thử Core Server
 * Sử dụng MessageEnvelope và DTOs từ shared module
 * 
 * Usage:
 * 1. Start Core Server: java com.n9.core.CoreServer
 * 2. Run: java -cp "target/classes:target/test-classes:..." TestCoreClient
 * 3. Xem kết quả test các message types
 * 
 * @version 2.0.0 (Updated for shared protocol)
 */
public class TestCoreClient {
    
    private static final String HOST = "localhost";
    private static final int PORT = 9090;
    
    public static void main(String[] args) {
        System.out.println("🧪 Test Core Client Starting...");
        System.out.println("📡 Using shared protocol: MessageEnvelope + MessageFactory");
        System.out.println();
        
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            
            System.out.println("✅ Connected to Core Server at " + HOST + ":" + PORT);
            System.out.println();
            
            // Test 1: GAME.START
            testGameStart(in, out);
            
            // Test 2: GAME.CARD_PLAY_REQUEST
            testPlayCard(in, out, "match-123", "player1", 1);
            
            // Test 3: GAME.STATE_SYNC
            testGameState(in, out, "match-123", "player1");
            
            System.out.println("\n✅ All tests completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test GAME.START message
     */
    private static void testGameStart(BufferedReader in, BufferedWriter out) throws Exception {
        System.out.println("=== Test 1: GAME.START ===");
        
        // Create MatchStartDto payload
        MatchStartDto startDto = new MatchStartDto();
        startDto.setMatchId("match-123");
        startDto.setGameId("match-123");
        startDto.setGameMode("QUICK_MATCH");
        startDto.setPlayerPosition(1);
        startDto.setTotalRounds(3);
        startDto.setRoundTimeout(10);
        
        // Create MessageEnvelope using MessageFactory
        MessageEnvelope request = MessageFactory.createRequest(
            MessageType.GAME_START,
            "player1",  // userId
            "session-123",  // sessionId
            "match-123",  // matchId
            startDto
        );
        
        // Send message
        String jsonRequest = JsonUtils.toJson(request);
        System.out.println("📤 Sending: " + jsonRequest);
        out.write(jsonRequest);
        out.newLine();
        out.flush();
        
        // Receive response
        String response = in.readLine();
        System.out.println("📥 Received: " + response);
        
        // Parse response
        MessageEnvelope responseEnvelope = JsonUtils.fromJson(response, MessageEnvelope.class);
        System.out.println("   Type: " + responseEnvelope.getType());
        System.out.println("   CorrelationId: " + responseEnvelope.getCorrelationId());
        System.out.println();
        
        Thread.sleep(1000);
    }
    
    /**
     * Test GAME.CARD_PLAY_REQUEST message
     */
    private static void testPlayCard(BufferedReader in, BufferedWriter out, 
                                      String matchId, String playerId, int cardId) throws Exception {
        System.out.println("=== Test 2: GAME.CARD_PLAY_REQUEST ===");
        
        // Create PlayCardRequestDto payload
        PlayCardRequestDto playDto = new PlayCardRequestDto();
        playDto.setGameId(matchId);
        playDto.setRoundNumber(1);
        playDto.setCardId(cardId);
        
        // Create MessageEnvelope
        MessageEnvelope request = MessageFactory.createRequest(
            MessageType.GAME_CARD_PLAY_REQUEST,
            playerId,  // userId
            "session-123",  // sessionId
            matchId,  // matchId
            playDto
        );
        
        // Send message
        String jsonRequest = JsonUtils.toJson(request);
        System.out.println("📤 Sending: " + jsonRequest);
        out.write(jsonRequest);
        out.newLine();
        out.flush();
        
        // Receive response
        String response = in.readLine();
        System.out.println("📥 Received: " + response);
        
        // Parse response
        MessageEnvelope responseEnvelope = JsonUtils.fromJson(response, MessageEnvelope.class);
        System.out.println("   Type: " + responseEnvelope.getType());
        System.out.println("   CorrelationId: " + responseEnvelope.getCorrelationId());
        System.out.println();
        
        Thread.sleep(1000);
    }
    
    /**
     * Test GAME.STATE_SYNC message
     */
    private static void testGameState(BufferedReader in, BufferedWriter out,
                                       String matchId, String playerId) throws Exception {
        System.out.println("=== Test 3: GAME.STATE_SYNC ===");
        
        // Create MessageEnvelope (no payload needed for state sync)
        MessageEnvelope request = MessageFactory.createRequest(
            MessageType.GAME_STATE_SYNC,
            playerId,  // userId
            "session-123",  // sessionId
            matchId,  // matchId
            null  // No payload
        );
        
        // Send message
        String jsonRequest = JsonUtils.toJson(request);
        System.out.println("📤 Sending: " + jsonRequest);
        out.write(jsonRequest);
        out.newLine();
        out.flush();
        
        // Receive response
        String response = in.readLine();
        System.out.println("📥 Received: " + response);
        
        // Parse response
        MessageEnvelope responseEnvelope = JsonUtils.fromJson(response, MessageEnvelope.class);
        System.out.println("   Type: " + responseEnvelope.getType());
        System.out.println("   CorrelationId: " + responseEnvelope.getCorrelationId());
        if (responseEnvelope.getPayload() != null) {
            System.out.println("   Payload: " + responseEnvelope.getPayload().toString());
        }
        System.out.println();
    }
}
