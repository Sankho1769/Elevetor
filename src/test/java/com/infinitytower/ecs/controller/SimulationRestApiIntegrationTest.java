package com.infinitytower.ecs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimulationRestApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + port;
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");
        if (jsonBody != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("GET / returns 200 OK and serves index.html with Aubergine theme")
    void testRootEndpointServesStaticSpa() throws Exception {
        HttpResponse<String> res = get("/");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Infinity Tower"), "Should serve Infinity Tower SPA");
        assertTrue(res.body().contains("theme-aubergine"), "Should contain Aubergine theme definition");
    }

    @Test
    @DisplayName("GET and PUT /api/config manage in-memory building configuration")
    void testConfigEndpoints() throws Exception {
        HttpResponse<String> getRes = get("/api/config");
        assertEquals(200, getRes.statusCode());
        JsonNode getJson = objectMapper.readTree(getRes.body());
        assertNotNull(getJson.get("name"));
        assertTrue(getJson.get("floors").asInt() >= 1);

        String updatePayload = """
            {
              "name": "Integration Test Tower",
              "floors": 15,
              "basements": 2,
              "lobby": 1,
              "idleFloor": 1,
              "parkingFloors": [-1, -2],
              "vipFloors": [10, 11, 12],
              "serviceFloors": [-2],
              "mechanicalFloors": [],
              "elevatorCount": 4,
              "capacity": 8,
              "weightLimit": 900,
              "maxSpeed": 2.2,
              "acceleration": 1.1,
              "deceleration": 1.1,
              "doorOpenTime": 1.6,
              "doorCloseTime": 1.6,
              "loadTime": 1.2,
              "unloadTime": 1.0,
              "energySaving": false,
              "algorithm": "look"
            }
            """;
        HttpResponse<String> putRes = put("/api/config", updatePayload);
        assertEquals(200, putRes.statusCode());
        JsonNode putJson = objectMapper.readTree(putRes.body());
        assertEquals("Integration Test Tower", putJson.get("name").asText());
        assertEquals(15, putJson.get("floors").asInt());
    }

    @Test
    @DisplayName("POST /api/simulation lifecycle: start, pause, resume, speed, reset, snapshot")
    void testSimulationLifecycleEndpoints() throws Exception {
        HttpResponse<String> startRes = post("/api/simulation/start", null);
        assertEquals(200, startRes.statusCode());
        assertTrue(objectMapper.readTree(startRes.body()).get("running").asBoolean());

        HttpResponse<String> pauseRes = post("/api/simulation/pause", null);
        assertEquals(200, pauseRes.statusCode());
        assertFalse(objectMapper.readTree(pauseRes.body()).get("running").asBoolean());

        HttpResponse<String> resumeRes = post("/api/simulation/resume", null);
        assertEquals(200, resumeRes.statusCode());
        assertTrue(objectMapper.readTree(resumeRes.body()).get("running").asBoolean());

        HttpResponse<String> speedRes = post("/api/simulation/speed", "{\"multiplier\": 4.0}");
        assertEquals(200, speedRes.statusCode());
        assertEquals(4.0, objectMapper.readTree(speedRes.body()).get("speed").asDouble());

        HttpResponse<String> snapRes = get("/api/simulation/snapshot");
        assertEquals(200, snapRes.statusCode());
        JsonNode snapJson = objectMapper.readTree(snapRes.body());
        assertTrue(snapJson.has("elevators"));
        assertTrue(snapJson.get("elevators").isArray());
        assertTrue(snapJson.has("analytics"));

        HttpResponse<String> resetRes = post("/api/simulation/reset", null);
        assertEquals(200, resetRes.statusCode());
        assertEquals("reset", objectMapper.readTree(resetRes.body()).get("status").asText());
    }

    @Test
    @DisplayName("POST /api/requests: spawn manual, batch, and auto-generation")
    void testPassengerRequestEndpoints() throws Exception {
        String spawnPayload = "{\"origin\": 1, \"dest\": 6, \"priority\": \"vip\", \"group\": 1}";
        HttpResponse<String> spawnRes = post("/api/requests/spawn", spawnPayload);
        assertEquals(200, spawnRes.statusCode());
        assertEquals("spawned", objectMapper.readTree(spawnRes.body()).get("status").asText());

        HttpResponse<String> batchRes = post("/api/requests/batch?count=5", null);
        assertEquals(200, batchRes.statusCode());
        assertEquals("spawned", objectMapper.readTree(batchRes.body()).get("status").asText());
        assertEquals("5", objectMapper.readTree(batchRes.body()).get("count").asText());

        String autoGenPayload = "{\"mode\": \"peak\", \"ratePerMinute\": 10.0}";
        HttpResponse<String> autoRes = post("/api/requests/autogen", autoGenPayload);
        assertEquals(200, autoRes.statusCode());
        assertEquals("updated", objectMapper.readTree(autoRes.body()).get("status").asText());
    }

    @Test
    @DisplayName("GET /api/algorithm lists all 10 algorithms and POST /api/algorithm switches them")
    void testAllTenAlgorithmsRegisteredAndSwitchable() throws Exception {
        HttpResponse<String> listRes = get("/api/algorithm");
        assertEquals(200, listRes.statusCode());
        JsonNode listJson = objectMapper.readTree(listRes.body());
        assertTrue(listJson.isArray());
        assertEquals(10, listJson.size(), "Must expose exactly 10 scheduling algorithms");

        List<String> expectedAlgorithms = List.of(
                "fcfs", "scan", "look", "sstf", "collective",
                "nearest", "sector", "destination", "adaptive", "ai"
        );

        for (String algo : expectedAlgorithms) {
            String payload = "{\"algorithm\": \"" + algo + "\"}";
            HttpResponse<String> setRes = post("/api/algorithm", payload);
            assertEquals(200, setRes.statusCode());
            assertEquals(algo, objectMapper.readTree(setRes.body()).get("algorithm").asText());

            HttpResponse<String> currentRes = get("/api/algorithm/current");
            assertEquals(200, currentRes.statusCode());
            assertEquals(algo, objectMapper.readTree(currentRes.body()).get("algorithm").asText());
        }
    }

    @Test
    @DisplayName("Safety controls: toggle emergencies, clear-all, inject and clear faults")
    void testSafetyEndpoints() throws Exception {
        HttpResponse<String> toggleRes = post("/api/safety/toggle", "{\"key\": \"smoke\"}");
        assertEquals(200, toggleRes.statusCode());
        assertTrue(objectMapper.readTree(toggleRes.body()).get("smoke").asBoolean());

        HttpResponse<String> clearRes = post("/api/safety/clear-all", null);
        assertEquals(200, clearRes.statusCode());
        assertFalse(objectMapper.readTree(clearRes.body()).get("smoke").asBoolean());

        HttpResponse<String> injectRes = post("/api/safety/fault",
                "{\"elevatorId\": 0, \"action\": \"inject\", \"faultType\": \"motor\"}");
        assertEquals(200, injectRes.statusCode());
        assertEquals("ok", objectMapper.readTree(injectRes.body()).get("status").asText());

        HttpResponse<String> clearFaultRes = post("/api/safety/fault",
                "{\"elevatorId\": 0, \"action\": \"clear\"}");
        assertEquals(200, clearFaultRes.statusCode());
        assertEquals("ok", objectMapper.readTree(clearFaultRes.body()).get("status").asText());
    }

    @Test
    @DisplayName("GET /api/analytics returns real-time telemetry metrics")
    void testAnalyticsEndpoint() throws Exception {
        HttpResponse<String> res = get("/api/analytics");
        assertEquals(200, res.statusCode());
        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.has("totalPassengers"));
        assertTrue(json.has("waitingPassengers"));
        assertTrue(json.has("avgWaitTime"));
        assertTrue(json.has("energy"));
    }

    @Test
    @DisplayName("GET /api/logs supports in-memory filtering and search")
    void testLogsEndpoint() throws Exception {
        post("/api/requests/spawn", "{\"origin\": 1, \"dest\": 5, \"priority\": \"normal\", \"group\": 1}");
        HttpResponse<String> res = get("/api/logs?limit=25");
        assertEquals(200, res.statusCode());
        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.isArray());
        assertFalse(json.isEmpty(), "Logs should contain recent simulation events");

        HttpResponse<String> filterRes = get("/api/logs?category=system&limit=10");
        assertEquals(200, filterRes.statusCode());
        JsonNode filterJson = objectMapper.readTree(filterRes.body());
        assertTrue(filterJson.isArray());
        for (JsonNode entry : filterJson) {
            assertEquals("system", entry.get("category").asText());
        }
    }

    @Test
    @DisplayName("GET /api/reports on-demand downloads for JSON and CSV without database")
    void testReportsEndpoint() throws Exception {
        HttpResponse<String> jsonRes = get("/api/reports?type=simulation&format=json");
        assertEquals(200, jsonRes.statusCode());
        assertTrue(jsonRes.headers().firstValue("content-disposition").orElse("").contains("simulation-report.json"));
        JsonNode json = objectMapper.readTree(jsonRes.body());
        assertEquals("simulation", json.get("reportType").asText());
        assertNotNull(json.get("performanceSummary"));

        HttpResponse<String> csvRes = get("/api/reports?type=performance&format=csv");
        assertEquals(200, csvRes.statusCode());
        assertTrue(csvRes.headers().firstValue("content-disposition").orElse("").contains("performance-report.csv"));
        assertTrue(csvRes.body().startsWith("metric,value"), "CSV report should contain CSV header");
    }
}
