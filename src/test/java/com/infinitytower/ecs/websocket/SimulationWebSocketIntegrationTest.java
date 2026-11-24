package com.infinitytower.ecs.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimulationWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("WebSocket /ws/simulation connects and streams live state broadcast frames")
    void testWebSocketBroadcastStream() throws Exception {
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        HttpClient client = HttpClient.newHttpClient();
        String wsUri = "ws://localhost:" + port + "/ws/simulation";

        WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUri), new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageFuture.complete(data.toString());
                        return null;
                    }
                }).get(5, TimeUnit.SECONDS);

        assertNotNull(ws, "WebSocket connection must be established");

        String receivedFrame = messageFuture.get(6, TimeUnit.SECONDS);
        assertNotNull(receivedFrame, "Should receive simulation snapshot frame from server");
        assertFalse(receivedFrame.isBlank());

        JsonNode snapshot = objectMapper.readTree(receivedFrame);
        assertTrue(snapshot.has("elevators"), "Snapshot frame must contain elevators list");
        assertTrue(snapshot.has("building"), "Snapshot frame must contain building configuration");
        assertTrue(snapshot.has("analytics"), "Snapshot frame must contain analytics telemetry");
        assertTrue(snapshot.has("safety"), "Snapshot frame must contain safety state");

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").get(2, TimeUnit.SECONDS);
    }
}
