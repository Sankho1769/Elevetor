package com.infinitytower.ecs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitytower.ecs.domain.BuildingConfigEntity;
import com.infinitytower.ecs.domain.SimulationLogEntity;
import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.repository.BuildingConfigRepository;
import com.infinitytower.ecs.repository.SimulationLogRepository;
import com.infinitytower.ecs.simulation.SimulationEngine;
import com.infinitytower.ecs.websocket.SimulationSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SimulationService {

    private final SimulationEngine engine;
    private final SimulationSocketHandler socketHandler;
    private final ObjectMapper objectMapper;
    private final SimulationLogRepository logRepository;
    private final BuildingConfigRepository buildingConfigRepository;

    private final double tickIntervalMs;
    private volatile double speedMultiplier = 1.0;

    public SimulationService(SimulationEngine engine,
                              SimulationSocketHandler socketHandler,
                              ObjectMapper objectMapper,
                              SimulationLogRepository logRepository,
                              BuildingConfigRepository buildingConfigRepository,
                              @Value("${ecs.simulation.tick-interval-ms}") double tickIntervalMs) {
        this.engine = engine;
        this.socketHandler = socketHandler;
        this.objectMapper = objectMapper;
        this.logRepository = logRepository;
        this.buildingConfigRepository = buildingConfigRepository;
        this.tickIntervalMs = tickIntervalMs;
        loadPersistedConfig();
    }

    private void loadPersistedConfig() {
        Optional<BuildingConfigEntity> existing = buildingConfigRepository.findById(1L);
        existing.ifPresent(cfg -> engine.applyConfig(toDto(cfg)));
    }

    private Dto.BuildingConfigDto toDto(BuildingConfigEntity e) {
        Dto.BuildingConfigDto dto = new Dto.BuildingConfigDto();
        dto.name = e.getName();
        dto.floors = e.getFloors();
        dto.basements = e.getBasements();
        dto.lobby = e.getLobby();
        dto.idleFloor = e.getIdleFloor();
        dto.parkingFloors = parseCsv(e.getParkingFloors());
        dto.vipFloors = parseCsv(e.getVipFloors());
        dto.serviceFloors = parseCsv(e.getServiceFloors());
        dto.mechanicalFloors = parseCsv(e.getMechanicalFloors());
        dto.elevatorCount = e.getElevatorCount();
        dto.capacity = e.getCapacity();
        dto.weightLimit = e.getWeightLimit();
        dto.maxSpeed = e.getMaxSpeed();
        dto.acceleration = e.getAcceleration();
        dto.deceleration = e.getDeceleration();
        dto.doorOpenTime = e.getDoorOpenTime();
        dto.doorCloseTime = e.getDoorCloseTime();
        dto.loadTime = e.getLoadTime();
        dto.unloadTime = e.getUnloadTime();
        dto.energySaving = e.isEnergySaving();
        dto.algorithm = e.getAlgorithm();
        return dto;
    }

    private List<Integer> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).toList();
    }

    private String toCsv(List<Integer> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    // ---------- Scheduled tasks ----------

    @Scheduled(fixedRateString = "${ecs.simulation.tick-interval-ms}")
    public void tickTask() {
        engine.tick((tickIntervalMs / 1000.0) * speedMultiplier);
    }

    @Scheduled(fixedRateString = "${ecs.simulation.broadcast-interval-ms}")
    public void broadcastTask() {
        if (socketHandler.connectedClients() == 0) return;
        try {
            String json = objectMapper.writeValueAsString(engine.getSnapshot());
            socketHandler.broadcast(json);
        } catch (Exception ignored) { }
    }

    @Scheduled(fixedRateString = "${ecs.simulation.log-persist-interval-ms}")
    public void persistLogsTask() {
        List<Dto.LogEntryDto> batch = engine.drainNewLogsForPersistence();
        if (batch.isEmpty()) return;
        List<SimulationLogEntity> entities = batch.stream()
                .map(l -> new SimulationLogEntity(l.simDay, l.simClock, l.category, l.level, l.message))
                .toList();
        logRepository.saveAll(entities);
    }

    // ---------- Passthrough API used by controllers ----------

    public void start() { engine.start(); }
    public void pause() { engine.pause(); }
    public void resume() { engine.resume(); }
    public void reset() { engine.resetSimulation(); }
    public boolean isRunning() { return engine.isRunning(); }
    public void setSpeed(double multiplier) { this.speedMultiplier = multiplier; }
    public double getSpeed() { return speedMultiplier; }

    public Dto.SnapshotDto getSnapshot() { return engine.getSnapshot(); }

    public Dto.BuildingConfigDto getConfig() { return engine.getConfig(); }

    public void applyConfig(Dto.BuildingConfigDto dto) {
        engine.applyConfig(dto);
        persistConfig(dto);
    }

    private void persistConfig(Dto.BuildingConfigDto dto) {
        BuildingConfigEntity entity = buildingConfigRepository.findById(1L).orElseGet(BuildingConfigEntity::new);
        entity.setId(1L);
        entity.setName(dto.name);
        entity.setFloors(dto.floors);
        entity.setBasements(dto.basements);
        entity.setLobby(dto.lobby);
        entity.setIdleFloor(dto.idleFloor);
        entity.setParkingFloors(toCsv(dto.parkingFloors));
        entity.setVipFloors(toCsv(dto.vipFloors));
        entity.setServiceFloors(toCsv(dto.serviceFloors));
        entity.setMechanicalFloors(toCsv(dto.mechanicalFloors));
        entity.setElevatorCount(dto.elevatorCount);
        entity.setCapacity(dto.capacity);
        entity.setWeightLimit(dto.weightLimit);
        entity.setMaxSpeed(dto.maxSpeed);
        entity.setAcceleration(dto.acceleration);
        entity.setDeceleration(dto.deceleration);
        entity.setDoorOpenTime(dto.doorOpenTime);
        entity.setDoorCloseTime(dto.doorCloseTime);
        entity.setLoadTime(dto.loadTime);
        entity.setUnloadTime(dto.unloadTime);
        entity.setEnergySaving(dto.energySaving);
        entity.setAlgorithm(dto.algorithm);
        buildingConfigRepository.save(entity);
    }

    public void setAlgorithm(String id) {
        engine.setAlgorithm(id);
        BuildingConfigEntity entity = buildingConfigRepository.findById(1L).orElse(null);
        if (entity != null) { entity.setAlgorithm(id); buildingConfigRepository.save(entity); }
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

    public java.util.Map<String, Object> buildReport(String type) { return engine.buildReport(type); }

    public List<SimulationLogEntity> getLogs(String category, String search, int limit) {
        org.springframework.data.domain.Pageable page = org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(500, limit)));
        if (search != null && !search.isBlank()) {
            return logRepository.findByMessageContainingIgnoreCaseOrderByIdDesc(search, page);
        }
        if (category != null && !category.isBlank() && !"all".equals(category)) {
            return logRepository.findByCategoryOrderByIdDesc(category, page);
        }
        return logRepository.findAllByOrderByIdDesc(page);
    }
}
