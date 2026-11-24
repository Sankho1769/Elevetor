package com.infinitytower.ecs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitytower.ecs.command.SimulationCommand;
import com.infinitytower.ecs.config.BuildingConfigDefaults;
import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.metrics.SimulationMetrics;
import com.infinitytower.ecs.simulation.SimulationEngine;
import com.infinitytower.ecs.websocket.SimulationSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SimulationEngine engine;
    private final SimulationSocketHandler socketHandler;
    private final ObjectMapper objectMapper;
    private final BuildingConfigDefaults configDefaults;
    private final SimulationMetrics metrics;

    private final double tickIntervalMs;
    private volatile double speedMultiplier = 1.0;

    public SimulationService(SimulationEngine engine,
                             SimulationSocketHandler socketHandler,
                             ObjectMapper objectMapper,
                             BuildingConfigDefaults configDefaults,
                             SimulationMetrics metrics,
                             @Value("${ecs.simulation.tick-interval-ms:100}") double tickIntervalMs,
                             @Value("${ecs.simulation.max-log-entries:5000}") int maxLogEntries) {
        this.engine = engine;
        this.socketHandler = socketHandler;
        this.objectMapper = objectMapper;
        this.configDefaults = configDefaults;
        this.metrics = metrics;
        this.tickIntervalMs = tickIntervalMs;
        this.engine.setMaxLogEntries(maxLogEntries);
        // Load default predefined in-memory building configuration
        this.engine.applyConfig(this.configDefaults.createDefaultConfig());
    }

    // ---------- Scheduled tasks ----------

    @Scheduled(fixedRateString = "${ecs.simulation.tick-interval-ms:100}")
    public void tickTask() {
        metrics.getTickTimer().record(() -> engine.tick((tickIntervalMs / 1000.0) * speedMultiplier));
    }

    @Scheduled(fixedRateString = "${ecs.simulation.broadcast-interval-ms:150}")
    public void broadcastTask() {
        if (socketHandler.connectedClients() == 0) return;
        try {
            String json = objectMapper.writeValueAsString(engine.getSnapshot());
            socketHandler.broadcast(json);
        } catch (Exception e) {
            log.warn("Telemetry broadcast error: {}", e.getMessage());
        }
    }

    // ---------- Command pipeline & Passthrough API ----------

    public void submitCommand(SimulationCommand command) {
        engine.submitCommand(command);
    }

    public void start() { engine.start(); }
    public void pause() { engine.pause(); }
    public void resume() { engine.resume(); }
    public void reset() {
        engine.resetSimulation();
    }
    public boolean isRunning() { return engine.isRunning(); }
    public void setSpeed(double multiplier) { this.speedMultiplier = multiplier; }
    public double getSpeed() { return speedMultiplier; }

    public Dto.SnapshotDto getSnapshot() { return engine.getSnapshot(); }

    public Dto.BuildingConfigDto getConfig() { return engine.getConfig(); }

    public void applyConfig(Dto.BuildingConfigDto dto) {
        engine.applyConfig(dto);
    }

    public void setAlgorithm(String id) {
        engine.setAlgorithm(id);
    }

    public String getAlgorithm() { return engine.getAlgorithm(); }
    public List<Dto.AlgorithmInfoDto> listAlgorithms() { return engine.listAlgorithms(); }

    public void setAutoGen(String mode, double ratePerMinute) { engine.setAutoGen(mode, ratePerMinute); }

    public void spawnPassenger(Integer origin, Integer dest, String priority, Integer group) {
        engine.spawnPassenger(origin, dest, priority, group == null ? 1 : group);
    }

    public void spawnBatchRandom(int count) {
        for (int i = 0; i < count; i++) engine.spawnPassenger(null, null, null, 1);
    }

    public void toggleSafety(String key) { engine.toggleSafety(key); }
    public void clearAllSafety() { engine.clearAllSafety(); }
    public void injectFault(int elevatorId, String faultType) { engine.injectFault(elevatorId, faultType); }
    public void clearFault(int elevatorId) { engine.clearFault(elevatorId); }
    public void toggleMaintenance(int elevatorId) { engine.toggleMaintenance(elevatorId); }
    public void toggleInspection(int elevatorId) { engine.toggleInspection(elevatorId); }
    public void toggleIndependentService(int elevatorId) { engine.toggleIndependentService(elevatorId); }

    public Dto.AnalyticsDto getAnalytics() { return engine.analyticsDto(); }

    public Map<String, Object> buildReport(String type) { return engine.buildReport(type); }

    public List<Dto.LogEntryDto> getLogs(String category, String search, int limit) {
        return engine.getLogs(category, search, limit);
    }
}
