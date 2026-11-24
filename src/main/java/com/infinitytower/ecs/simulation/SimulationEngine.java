package com.infinitytower.ecs.simulation;

import com.infinitytower.ecs.dto.Dto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SimulationEngine {

    private final BuildingModel building = new BuildingModel();
    private final SafetyStateModel safety = new SafetyStateModel();
    private final SchedulerRegistry schedulerRegistry = new SchedulerRegistry();
    private final List<ElevatorModel> elevators = new ArrayList<>();
    private final List<PassengerModel> passengers = new ArrayList<>();
    private final List<HallCallModel> hallCalls = new ArrayList<>();
    private final Deque<Dto.LogEntryDto> inMemoryLogs = new ArrayDeque<>();
    private final AtomicLong logSeq = new AtomicLong(0);
    private int maxLogEntries = 5000;
    private final Random random = new Random();

    // Lock-free command pipeline and wait-free snapshot publishing
    private final Queue<com.infinitytower.ecs.command.SimulationCommand> commandQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private volatile Dto.SnapshotDto cachedSnapshot;

    private String algorithm = "look";
    private long passengerSeq = 1;
    private long requestSeq = 1;
    private double simSeconds = 0;
    private int simDay = 1;
    private boolean running = false;
    private boolean started = false;
    private String autoGenMode = "off";
    private double autoGenRatePerMinute = 6;
    private double autoGenAccum = 0;
    private double lastRedispatchSweep = 0;
    private int selectedElevatorForFault = 0;

    private int totalPassengers = 0;
    private int completed = 0;
    private int rejected = 0;
    private int totalRequests = 0;
    private final List<Double> waitTimes = new ArrayList<>();
    private final List<Double> travelTimes = new ArrayList<>();
    private int stops = 0;
    private double distance = 0;
    private double energy = 0;
    private int faultCount = 0;

    public SimulationEngine() {
        resetSimulation();
    }

    public synchronized void start() {
        started = true;
        running = true;
        if ("off".equals(autoGenMode)) {
            autoGenMode = "peak";
            log("Traffic generation enabled automatically (Peak-hour) on start.", "ok", "system");
        }
        log("Simulation started by operator.", "ok", "system");
        generateSnapshot();
    }

    public synchronized void pause() {
        if (!started) return;
        running = false;
        log("Simulation paused", "ok", "system");
        generateSnapshot();
    }

    public synchronized void resume() {
        if (!started) return;
        running = true;
        log("Simulation resumed", "ok", "system");
        generateSnapshot();
    }

    public synchronized boolean isRunning() { return running; }

    public synchronized void resetSimulation() {
        simSeconds = 0;
        simDay = 1;
        passengers.clear();
        hallCalls.clear();
        passengerSeq = 1;
        requestSeq = 1;
        totalPassengers = 0; completed = 0; rejected = 0; totalRequests = 0;
        waitTimes.clear(); travelTimes.clear();
        stops = 0; distance = 0; energy = 0; faultCount = 0;
        lastRedispatchSweep = 0;
        inMemoryLogs.clear();
        initElevators();
        log("Simulation reset. \"" + building.name + "\" initialized with " + building.elevatorCount
                + " elevators across " + building.floorRange().size() + " floors.", "ok", "system");
        generateSnapshot();
    }

    private void initElevators() {
        elevators.clear();
        for (int i = 0; i < building.elevatorCount; i++) {
            ElevatorModel e = new ElevatorModel();
            e.id = i;
            e.label = String.valueOf((char) ('A' + i));
            e.currentFloor = building.idleFloor;
            elevators.add(e);
        }
        assignSectors();
    }

    private void assignSectors() {
        List<Integer> floors = new ArrayList<>(building.floorRange());
        floors.sort(Integer::compareTo);
        int n = elevators.size();
        if (n == 0 || floors.isEmpty()) return;
        int chunk = (int) Math.ceil((double) floors.size() / n);
        for (int i = 0; i < n; i++) {
            int from = Math.min(i * chunk, floors.size());
            int to = Math.min(from + chunk, floors.size());
            List<Integer> slice = floors.subList(from, to);
            ElevatorModel e = elevators.get(i);
            e.sectorMin = slice.isEmpty() ? floors.get(0) : slice.get(0);
            e.sectorMax = slice.isEmpty() ? floors.get(floors.size() - 1) : slice.get(slice.size() - 1);
        }
    }

    // ---------- Config ----------

    public synchronized void applyConfig(Dto.BuildingConfigDto dto) {
        if (dto.name != null) building.name = dto.name;
        building.floors = dto.floors;
        building.basements = dto.basements;
        building.lobby = dto.lobby;
        building.idleFloor = dto.idleFloor;
        if (dto.parkingFloors != null) building.parkingFloors = dto.parkingFloors;
        if (dto.vipFloors != null) building.vipFloors = dto.vipFloors;
        if (dto.serviceFloors != null) building.serviceFloors = dto.serviceFloors;
        if (dto.mechanicalFloors != null) building.mechanicalFloors = dto.mechanicalFloors;
        building.elevatorCount = Math.max(1, Math.min(16, dto.elevatorCount));
        building.capacity = dto.capacity;
        building.weightLimit = dto.weightLimit;
        building.maxSpeed = dto.maxSpeed;
        building.acceleration = dto.acceleration;
        building.deceleration = dto.deceleration;
        building.doorOpenTime = dto.doorOpenTime;
        building.doorCloseTime = dto.doorCloseTime;
        building.loadTime = dto.loadTime;
        building.unloadTime = dto.unloadTime;
        building.energySaving = dto.energySaving;
        if (dto.algorithm != null) algorithm = dto.algorithm;
        resetSimulation();
        log("Configuration applied - building rebuilt.", "ok", "system");
    }

    public synchronized Dto.BuildingConfigDto getConfig() {
        Dto.BuildingConfigDto dto = new Dto.BuildingConfigDto();
        dto.name = building.name;
        dto.floors = building.floors;
        dto.basements = building.basements;
        dto.lobby = building.lobby;
        dto.idleFloor = building.idleFloor;
        dto.parkingFloors = building.parkingFloors;
        dto.vipFloors = building.vipFloors;
        dto.serviceFloors = building.serviceFloors;
        dto.mechanicalFloors = building.mechanicalFloors;
        dto.elevatorCount = building.elevatorCount;
        dto.capacity = building.capacity;
        dto.weightLimit = building.weightLimit;
        dto.maxSpeed = building.maxSpeed;
        dto.acceleration = building.acceleration;
        dto.deceleration = building.deceleration;
        dto.doorOpenTime = building.doorOpenTime;
        dto.doorCloseTime = building.doorCloseTime;
        dto.loadTime = building.loadTime;
        dto.unloadTime = building.unloadTime;
        dto.energySaving = building.energySaving;
        dto.algorithm = algorithm;
        return dto;
    }

    // ---------- Algorithm ----------

    public synchronized void setAlgorithm(String id) {
        this.algorithm = id;
        log("Scheduling algorithm switched to " + id.toUpperCase(), "ok", "system");
        invalidateSnapshot();
    }

    public synchronized String getAlgorithm() { return algorithm; }

    public List<Dto.AlgorithmInfoDto> listAlgorithms() {
        List<Dto.AlgorithmInfoDto> result = new ArrayList<>();
        for (SchedulingAlgorithm a : schedulerRegistry.all()) {
            result.add(new Dto.AlgorithmInfoDto(a.id(), a.displayName(), a.description()));
        }
        return result;
    }

    // ---------- Auto-generation ----------

    public synchronized void setAutoGen(String mode, double ratePerMinute) {
        this.autoGenMode = mode == null ? "off" : mode;
        if (ratePerMinute > 0) this.autoGenRatePerMinute = ratePerMinute;
        log("Auto-generation mode set to " + this.autoGenMode, "ok", "system");
    }

    private void handleAutoGen(double dt) {
        if ("off".equals(autoGenMode)) return;
        autoGenAccum += dt;
        double interval = 60.0 / Math.max(1, autoGenRatePerMinute);
        if (autoGenAccum < interval) return;
        autoGenAccum = 0;
        List<Integer> floors = building.travelableFloors(safety.flood);
        if (floors.isEmpty()) return;
        int lobby = building.lobby;
        switch (autoGenMode) {
            case "random" -> spawnPassenger(null, null, null, 1);
            case "peak" -> {
                if (random.nextDouble() < 0.7) spawnPassenger(lobby, randomFloor(floors), null, 1);
                else spawnPassenger(randomFloor(floors), lobby, null, 1);
            }
            case "rush" -> {
                if (random.nextDouble() < 0.9) spawnPassenger(lobby, randomFloor(floors), null, 1);
                else spawnPassenger(randomFloor(floors), lobby, null, 1);
            }
            case "office" -> spawnPassenger(lobby, randomFloor(floors), "staff", 1);
            case "residential" -> {
                String p = random.nextDouble() < 0.3 ? (random.nextBoolean() ? "elderly" : "disabled") : "normal";
                spawnPassenger(null, null, p, random.nextDouble() < 0.2 ? 2 : 1);
            }
            case "mall" -> spawnPassenger(randomFloor(floors), randomFloor(floors), null, 1);
            case "hospital" -> spawnPassenger(null, null, random.nextDouble() < 0.15 ? "medical" : "normal", 1);
            case "hotel" -> spawnPassenger(lobby, randomFloor(floors), random.nextDouble() < 0.2 ? "vip" : "normal", 1);
            case "airport" -> spawnPassenger(lobby, randomFloor(floors), random.nextDouble() < 0.1 ? "disabled" : "normal", 1);
            default -> { }
        }
    }

    private int randomFloor(List<Integer> floors) {
        return floors.get(random.nextInt(floors.size()));
    }

    private String randomPriority() {
        double r = random.nextDouble();
        if (r < 0.7) return "normal";
        if (r < 0.8) return "staff";
        if (r < 0.88) return "elderly";
        if (r < 0.93) return "disabled";
        if (r < 0.97) return "vip";
        return "medical";
    }

    // ---------- Passenger / request handling ----------

    private void invalidateSnapshot() {
        this.cachedSnapshot = null;
    }

    public synchronized void spawnPassenger(Integer origin, Integer dest, String priority, int group) {
        for (int i = 0; i < Math.max(1, group); i++) {
            spawnOne(origin, dest, priority, group);
        }
        invalidateSnapshot();
    }

    private void spawnOne(Integer originIn, Integer destIn, String priorityIn, int group) {
        List<Integer> floors = building.travelableFloors(safety.flood);
        if (floors.isEmpty()) return;
        int origin = originIn != null ? originIn : randomFloor(floors);
        int dest;
        if (destIn != null) {
            dest = destIn;
        } else {
            do { dest = randomFloor(floors); } while (dest == origin);
        }
        if (safety.flood && (origin < 0 || dest < 0)) {
            log("Request rejected — basement levels locked under flood protocol", "warn", "safety");
            return;
        }
        String priority = priorityIn != null ? priorityIn : randomPriority();
        PassengerModel p = new PassengerModel();
        p.id = passengerSeq++;
        p.origin = origin;
        p.dest = dest;
        p.priority = priority;
        p.weight = 55 + random.nextDouble() * 35;
        p.spawnTime = simSeconds;
        p.group = group;
        passengers.add(p);
        totalPassengers++;
        totalRequests++;
        createHallCall(origin, dest > origin ? "up" : "down", priority, p.id);
        log("Passenger #" + p.id + " (" + priority + ") waiting at " + floorLabel(origin) + " \u2192 " + floorLabel(dest), "info", "passenger");
    }

    private HallCallModel createHallCall(int floor, String dir, String priority, long passengerId) {
        for (HallCallModel h : hallCalls) {
            if (h.floor == floor && h.dir.equals(dir) && !h.assigned) {
                h.passengerIds.add(passengerId);
                return h;
            }
        }
        HallCallModel call = new HallCallModel();
        call.id = requestSeq++;
        call.floor = floor;
        call.dir = dir;
        call.priority = priority;
        call.time = simSeconds;
        call.passengerIds.add(passengerId);
        hallCalls.add(call);
        assignHallCallToElevator(call);
        return call;
    }

    private void assignHallCallToElevator(HallCallModel call) {
        if (safety.smoke) {
            log("Hall call at " + floorLabel(call.floor) + " rejected — smoke alarm active, dispatch suspended", "warn", "safety");
            rejected++;
            return;
        }
        List<ElevatorModel> available = elevators.stream().filter(e -> e.isDispatchEligible(safety)).toList();
        if (available.isEmpty()) {
            if (!call.everRejected) {
                log("Hall call at " + floorLabel(call.floor) + " could not be assigned — no elevators available", "error", "fault");
                rejected++;
                call.everRejected = true;
            }
            return;
        }
        int pendingLoad = (int) hallCalls.stream().filter(h -> !h.assigned).count()
                + (int) passengers.stream().filter(p -> "waiting".equals(p.state)).count();
        DispatchContext ctx = new DispatchContext(elevators, pendingLoad, safety);
        SchedulingAlgorithm scheduler = schedulerRegistry.get(algorithm);
        ElevatorModel chosen = scheduler.selectElevator(available, call, building, ctx);
        if (chosen == null) {
            rejected++;
            return;
        }
        call.assigned = true;
        call.elevatorId = chosen.id;
        chosen.addStop(call.floor);
        sortQueueForElevator(chosen);
        log("Hall call " + floorLabel(call.floor) + " (" + call.dir + ") \u2192 Car " + chosen.label + " [" + algorithm.toUpperCase() + "]", "info", "dispatch");
    }

    private void redispatchPendingCalls() {
        for (HallCallModel h : hallCalls) {
            if (!h.assigned) assignHallCallToElevator(h);
        }
    }

    private void sortQueueForElevator(ElevatorModel e) {
        double cur = e.currentFloor;
        if ("fcfs".equals(algorithm)) return;
        if ("scan".equals(algorithm) || "look".equals(algorithm) || "collective".equals(algorithm)) {
            List<Integer> up = e.queue.stream().filter(f -> f >= cur).sorted().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            List<Integer> down = e.queue.stream().filter(f -> f < cur).sorted(Collections.reverseOrder()).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            List<Integer> ordered = new ArrayList<>(e.direction >= 0 ? concat(up, down) : concat(down, up));
            if ("scan".equals(algorithm) && !ordered.isEmpty()) {
                int top = building.topFloor();
                int bottom = building.bottomFloor();
                boolean goingUp = e.direction >= 0;
                int terminal = goingUp ? top : bottom;
                List<Integer> leg1 = new ArrayList<>();
                List<Integer> leg2 = new ArrayList<>();
                for (int f : ordered) {
                    if (goingUp ? f >= cur : f <= cur) leg1.add(f); else leg2.add(f);
                }
                if (!leg1.isEmpty() && !leg1.get(leg1.size() - 1).equals(terminal)) leg1.add(terminal);
                ordered = concat(leg1, leg2);
            }
            e.queue = ordered;
        } else {
            e.queue.sort((a, b) -> Double.compare(Math.abs(a - cur), Math.abs(b - cur)));
        }
    }

    private static List<Integer> concat(List<Integer> a, List<Integer> b) {
        List<Integer> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }

    // ---------- Lock-free command pipeline & Tick physics ----------

    public void submitCommand(com.infinitytower.ecs.command.SimulationCommand command) {
        if (command != null) {
            commandQueue.offer(command);
        }
    }

    public int getQueueDepth() {
        return commandQueue.size();
    }

    private void drainCommands() {
        com.infinitytower.ecs.command.SimulationCommand cmd;
        while ((cmd = commandQueue.poll()) != null) {
            processCommand(cmd);
        }
    }

    private void processCommand(com.infinitytower.ecs.command.SimulationCommand cmd) {
        switch (cmd) {
            case com.infinitytower.ecs.command.SimulationCommand.SpawnPassenger sp ->
                    spawnPassenger(sp.origin(), sp.dest(), sp.priority(), sp.group());
            case com.infinitytower.ecs.command.SimulationCommand.SetAlgorithm sa ->
                    setAlgorithm(sa.algorithmId());
            case com.infinitytower.ecs.command.SimulationCommand.SetSpeed ss -> { }
            case com.infinitytower.ecs.command.SimulationCommand.Lifecycle lc -> {
                switch (lc.action()) {
                    case START -> start();
                    case PAUSE -> pause();
                    case RESUME -> resume();
                    case RESET -> resetSimulation();
                }
            }
            case com.infinitytower.ecs.command.SimulationCommand.ApplyConfig ac ->
                    applyConfig(ac.config());
            case com.infinitytower.ecs.command.SimulationCommand.ToggleSafety ts ->
                    toggleSafety(ts.key());
            case com.infinitytower.ecs.command.SimulationCommand.ClearAllSafety cas ->
                    clearAllSafety();
            case com.infinitytower.ecs.command.SimulationCommand.InjectFault ifcmd ->
                    injectFault(ifcmd.elevatorId(), ifcmd.faultType());
            case com.infinitytower.ecs.command.SimulationCommand.ClearFault cf ->
                    clearFault(cf.elevatorId());
            case com.infinitytower.ecs.command.SimulationCommand.ToggleMaintenance tm ->
                    toggleMaintenance(tm.elevatorId());
            case com.infinitytower.ecs.command.SimulationCommand.ToggleInspection ti ->
                    toggleInspection(ti.elevatorId());
            case com.infinitytower.ecs.command.SimulationCommand.SetAutoGen sag ->
                    setAutoGen(sag.mode(), sag.ratePerMinute());
        }
    }

    public synchronized void tick(double dt) {
        drainCommands();
        if (!running) {
            generateSnapshot();
            return;
        }
        simSeconds += dt;
        if (simSeconds >= 86400) { simSeconds -= 86400; simDay++; }
        for (ElevatorModel e : elevators) tickElevator(e, dt);
        handleAutoGen(dt);
        if (simSeconds - lastRedispatchSweep > 3) {
            redispatchPendingCalls();
            lastRedispatchSweep = simSeconds;
        }
        generateSnapshot();
    }

    private void tickElevator(ElevatorModel e, double dt) {
        if (!e.isAvailable(safety)) {
            if (safety.fire && !e.fireRecall) {
                e.fireRecall = true;
                e.queue = new ArrayList<>(List.of(building.lobby));
                log("Car " + e.label + " entering fire recall mode \u2192 returning to lobby", "error", "safety");
            }
            if (safety.earthquake && !e.earthquakeHold) {
                e.earthquakeHold = true;
                e.currentFloor = Math.round(e.currentFloor);
                e.queue = new ArrayList<>();
                e.speed = 0;
                e.doorState = "opening"; e.doorProgress = 0;
                log("Car " + e.label + " halted at nearest floor " + floorLabel((int) e.currentFloor) + " \u2014 earthquake protocol", "error", "safety");
            }
            if (e.speed > 0) e.speed = Math.max(0, e.speed - building.deceleration * dt);
            return;
        }
        if ("door".equals(e.fault)) {
            if ("closed".equals(e.doorState) || "closing".equals(e.doorState)) { e.doorState = "opening"; }
            if ("open".equals(e.doorState)) { e.doorTimer = Math.max(e.doorTimer, 1.2); }
        }
        if ("opening".equals(e.doorState) || "closing".equals(e.doorState)) {
            double dur = "opening".equals(e.doorState) ? building.doorOpenTime : building.doorCloseTime;
            e.doorProgress += dt / dur;
            if (e.doorProgress >= 1) {
                if ("opening".equals(e.doorState)) {
                    e.doorState = "open"; e.doorProgress = 1;
                    e.doorTimer = building.loadTime + building.unloadTime;
                    handleBoarding(e);
                } else {
                    e.doorState = "closed"; e.doorProgress = 0;
                }
            }
            return;
        }
        if ("open".equals(e.doorState)) {
            e.doorTimer -= dt;
            if (e.doorTimer <= 0) { e.doorState = "closing"; e.doorProgress = 0; }
            return;
        }
        if (e.queue.isEmpty()) {
            e.speed = Math.max(0, e.speed - building.deceleration * dt);
            e.direction = 0;
            if (!"idle".equals(e.state)) { e.state = "idle"; e.idleSince = simSeconds; }
            if (building.energySaving && Math.abs(e.currentFloor - building.idleFloor) > 0.5 && simSeconds - e.idleSince > 8) {
                e.addStop(building.idleFloor);
            }
            return;
        }
        int target = e.queue.get(0);
        e.direction = target > e.currentFloor ? 1 : (target < e.currentFloor ? -1 : 0);
        e.state = "moving";
        double distToTarget = Math.abs(target - e.currentFloor);
        double maxV = e.inspection ? building.maxSpeed * 0.2 : building.maxSpeed;
        double brakingDist = (e.speed * e.speed) / (2 * Math.max(0.01, building.deceleration));
        if (distToTarget <= brakingDist) {
            e.speed = Math.max(0.02, e.speed - building.deceleration * dt);
        } else if (e.speed < maxV) {
            e.speed = Math.min(maxV, e.speed + building.acceleration * dt);
        }
        double step = e.speed * dt * e.direction;
        double prevFloor = e.currentFloor;
        if (Math.abs(target - e.currentFloor) <= Math.abs(step)) {
            e.currentFloor = target;
            e.speed = 0;
            distance += Math.abs(target - prevFloor);
            arriveAtFloor(e);
        } else {
            e.currentFloor += step;
            distance += Math.abs(step);
        }
        energy += (0.4 + e.speed * 0.6) * dt * (e.passengers.size() * 0.15 + 1);
    }

    private void arriveAtFloor(ElevatorModel e) {
        if (!e.queue.isEmpty()) e.queue.remove(0);
        e.doorState = "opening"; e.doorProgress = 0;
        stops++;
        e.tripCount++;
        log("Car " + e.label + " arrived at " + floorLabel((int) e.currentFloor), "info", "elevator");
    }

    private void handleBoarding(ElevatorModel e) {
        int curFloor = (int) Math.round(e.currentFloor);
        List<PassengerModel> exiting = e.passengers.stream().filter(p -> p.dest == curFloor).toList();
        for (PassengerModel p : exiting) {
            p.exitTime = simSeconds;
            p.state = "arrived";
            travelTimes.add(p.exitTime - p.boardTime);
            completed++;
            e.weight -= p.weight;
            log("Passenger #" + p.id + " exited Car " + e.label + " at " + floorLabel(curFloor), "ok", "passenger");
        }
        e.passengers.removeIf(p -> p.dest == curFloor);

        List<PassengerModel> waiting = passengers.stream()
                .filter(p -> "waiting".equals(p.state) && p.origin == curFloor)
                .toList();
        List<PassengerModel> boardable = new ArrayList<>(waiting.stream().filter(p -> {
            for (HallCallModel h : hallCalls) {
                if (h.passengerIds.contains(p.id)) return h.elevatorId != null && h.elevatorId == e.id;
            }
            return false;
        }).toList());
        boardable.sort((a, b) -> Double.compare(PassengerModel.priorityWeight(b.priority), PassengerModel.priorityWeight(a.priority)));

        for (PassengerModel p : boardable) {
            if (e.passengers.size() >= building.capacity) continue;
            if (e.weight + p.weight > building.weightLimit) {
                log("Car " + e.label + " overweight \u2014 passenger #" + p.id + " denied boarding", "warn", "safety");
                continue;
            }
            p.state = "riding";
            p.boardTime = simSeconds;
            p.assignedElevatorId = e.id;
            waitTimes.add(p.boardTime - p.spawnTime);
            e.passengers.add(p);
            e.weight += p.weight;
            e.addStop(p.dest);
            log("Passenger #" + p.id + " boarded Car " + e.label + " \u2192 " + floorLabel(p.dest), "ok", "passenger");
        }
        sortQueueForElevator(e);
        final int cf = curFloor;
        hallCalls.removeIf(h -> h.floor == cf && h.elevatorId != null && h.elevatorId == e.id);

        passengers.removeIf(p -> "arrived".equals(p.state) && simSeconds - p.exitTime > 60);
    }

    private String floorLabel(int f) {
        if (f == building.lobby) return "LOBBY";
        if (f < 0) return "B" + Math.abs(f);
        return "F" + f;
    }

    // ---------- Safety ----------

    public synchronized void toggleSafety(String key) {
        boolean newVal = switch (key) {
            case "fire" -> !safety.fire;
            case "smoke" -> !safety.smoke;
            case "estop" -> !safety.estop;
            case "power" -> !safety.power;
            case "earthquake" -> !safety.earthquake;
            case "flood" -> !safety.flood;
            default -> false;
        };
        switch (key) {
            case "fire" -> safety.fire = newVal;
            case "smoke" -> safety.smoke = newVal;
            case "estop" -> safety.estop = newVal;
            case "power" -> safety.power = newVal;
            case "earthquake" -> safety.earthquake = newVal;
            case "flood" -> safety.flood = newVal;
            default -> { }
        }
        log("Safety condition \"" + key + "\" set to " + newVal, newVal ? "error" : "ok", "safety");
        if ("estop".equals(key) && newVal) elevators.forEach(e -> e.speed = 0);
        if ("fire".equals(key) && !newVal) {
            elevators.forEach(e -> e.fireRecall = false);
            log("Fire alarm cleared \u2014 all cars released from recall and returned to normal service.", "ok", "safety");
        }
        if ("earthquake".equals(key) && !newVal) {
            elevators.forEach(e -> e.earthquakeHold = false);
            log("Earthquake protocol cleared \u2014 cars resuming normal dispatch.", "ok", "safety");
        }
        if ("flood".equals(key)) {
            if (newVal) {
                elevators.forEach(e -> e.queue.removeIf(f -> f <= 0));
                log("Basement levels locked \u2014 existing basement stops cleared from all cars.", "error", "safety");
            } else {
                log("Flood protocol cleared \u2014 basement levels unlocked.", "ok", "safety");
            }
        }
        if (!newVal) redispatchPendingCalls();
        invalidateSnapshot();
    }

    public synchronized void clearAllSafety() {
        safety.clearAll();
        elevators.forEach(e -> { e.fireRecall = false; e.earthquakeHold = false; });
        redispatchPendingCalls();
        log("All building-wide safety conditions cleared \u2014 system returned to normal.", "ok", "safety");
        invalidateSnapshot();
    }

    public synchronized void injectFault(int elevatorId, String faultType) {
        ElevatorModel e = findElevator(elevatorId);
        if (e == null) return;
        e.fault = faultType;
        faultCount++;
        log("Car " + e.label + " fault injected: " + faultType, "error", "fault");
        invalidateSnapshot();
    }

    public synchronized void clearFault(int elevatorId) {
        ElevatorModel e = findElevator(elevatorId);
        if (e == null) return;
        e.fault = null;
        log("Car " + e.label + " fault cleared", "ok", "fault");
        redispatchPendingCalls();
        invalidateSnapshot();
    }

    public synchronized void toggleMaintenance(int elevatorId) {
        ElevatorModel e = findElevator(elevatorId);
        if (e == null) return;
        e.maintenance = !e.maintenance;
        log("Car " + e.label + " maintenance mode " + e.maintenance, "warn", "fault");
        redispatchPendingCalls();
        invalidateSnapshot();
    }

    public synchronized void toggleInspection(int elevatorId) {
        ElevatorModel e = findElevator(elevatorId);
        if (e == null) return;
        e.inspection = !e.inspection;
        log("Car " + e.label + " inspection mode " + e.inspection, "warn", "fault");
        redispatchPendingCalls();
        invalidateSnapshot();
    }

    public synchronized void toggleIndependentService(int elevatorId) {
        ElevatorModel e = findElevator(elevatorId);
        if (e == null) return;
        e.independentService = !e.independentService;
        log("Car " + e.label + " independent service " + e.independentService, "warn", "fault");
        redispatchPendingCalls();
        invalidateSnapshot();
    }

    private ElevatorModel findElevator(int id) {
        return elevators.stream().filter(e -> e.id == id).findFirst().orElse(null);
    }

    // ---------- Logging & In-Memory Log Store ----------

    public void setMaxLogEntries(int max) {
        this.maxLogEntries = Math.max(100, max);
    }

    private synchronized void log(String message, String level, String category) {
        Dto.LogEntryDto entry = new Dto.LogEntryDto(
                logSeq.incrementAndGet(),
                simDay,
                fmtClock(),
                category,
                level,
                message,
                Instant.now().toString()
        );
        inMemoryLogs.addLast(entry);
        while (inMemoryLogs.size() > maxLogEntries) {
            inMemoryLogs.removeFirst();
        }
    }

    private String fmtClock() {
        int s = (int) simSeconds;
        int hh = (s / 3600) % 24;
        int mm = (s / 60) % 60;
        int ss = s % 60;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }

    public synchronized List<Dto.LogEntryDto> getLogs(String category, String search, int limit) {
        int max = Math.max(1, Math.min(500, limit));
        List<Dto.LogEntryDto> result = new ArrayList<>();
        Iterator<Dto.LogEntryDto> it = inMemoryLogs.descendingIterator();
        boolean filterCategory = category != null && !category.isBlank() && !"all".equalsIgnoreCase(category);
        boolean filterSearch = search != null && !search.isBlank();
        String searchLower = filterSearch ? search.toLowerCase() : null;

        while (it.hasNext() && result.size() < max) {
            Dto.LogEntryDto item = it.next();
            if (filterCategory && !category.equalsIgnoreCase(item.category)) {
                continue;
            }
            if (filterSearch && (item.message == null || !item.message.toLowerCase().contains(searchLower))) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    // ---------- Snapshot / analytics ----------

    public Dto.SnapshotDto getSnapshot() {
        Dto.SnapshotDto snap = cachedSnapshot;
        if (snap != null) return snap;
        return generateSnapshot();
    }

    public synchronized Dto.SnapshotDto generateSnapshot() {
        Dto.SnapshotDto snap = new Dto.SnapshotDto();
        snap.simDay = simDay;
        snap.simSeconds = simSeconds;
        snap.running = running;
        snap.algorithm = algorithm;
        snap.building = getConfig();
        snap.safety = safetyDto();
        snap.elevators = elevators.stream().map(this::elevatorDto).toList();
        snap.passengers = passengers.stream()
                .filter(p -> !"arrived".equals(p.state))
                .map(this::passengerDto).toList();
        snap.hallCalls = hallCalls.stream().map(this::hallCallDto).toList();
        snap.analytics = analyticsDto();
        snap.recentLogs = new ArrayList<>(inMemoryLogs).stream()
                .skip(Math.max(0, inMemoryLogs.size() - 60)).toList();
        this.cachedSnapshot = snap;
        return snap;
    }

    private Dto.ElevatorDto elevatorDto(ElevatorModel e) {
        Dto.ElevatorDto d = new Dto.ElevatorDto();
        d.id = e.id; d.label = e.label; d.currentFloor = e.currentFloor;
        d.direction = e.direction; d.doorState = e.doorState; d.doorProgress = e.doorProgress;
        d.state = e.state; d.passengerCount = e.passengers.size(); d.weight = e.weight;
        d.queue = new ArrayList<>(e.queue); d.fault = e.fault; d.maintenance = e.maintenance;
        d.inspection = e.inspection; d.independentService = e.independentService;
        d.fireRecall = e.fireRecall; d.sectorMin = e.sectorMin; d.sectorMax = e.sectorMax;
        d.tripCount = e.tripCount;
        return d;
    }

    private Dto.PassengerDto passengerDto(PassengerModel p) {
        Dto.PassengerDto d = new Dto.PassengerDto();
        d.id = p.id; d.origin = p.origin; d.dest = p.dest; d.priority = p.priority;
        d.state = p.state; d.spawnTime = p.spawnTime;
        d.waitSoFar = p.boardTime != null ? p.boardTime - p.spawnTime : simSeconds - p.spawnTime;
        d.assignedElevatorId = p.assignedElevatorId;
        return d;
    }

    private Dto.HallCallDto hallCallDto(HallCallModel h) {
        Dto.HallCallDto d = new Dto.HallCallDto();
        d.id = h.id; d.floor = h.floor; d.dir = h.dir; d.priority = h.priority;
        d.assigned = h.assigned; d.elevatorId = h.elevatorId;
        return d;
    }

    private Dto.SafetyStateDto safetyDto() {
        Dto.SafetyStateDto d = new Dto.SafetyStateDto();
        d.fire = safety.fire; d.smoke = safety.smoke; d.estop = safety.estop;
        d.power = safety.power; d.earthquake = safety.earthquake; d.flood = safety.flood;
        return d;
    }

    public synchronized Dto.AnalyticsDto analyticsDto() {
        Dto.AnalyticsDto d = new Dto.AnalyticsDto();
        d.totalPassengers = totalPassengers;
        d.activePassengers = (int) passengers.stream().filter(p -> !"arrived".equals(p.state)).count();
        d.waitingPassengers = (int) passengers.stream().filter(p -> "waiting".equals(p.state)).count();
        d.completed = completed;
        d.rejected = rejected;
        d.totalRequests = totalRequests;
        d.avgWaitTime = avg(waitTimes);
        d.avgTravelTime = avg(travelTimes);
        long moving = elevators.stream().filter(e -> "moving".equals(e.state)).count();
        d.utilizationPct = elevators.isEmpty() ? 0 : (moving * 100.0 / elevators.size());
        d.queueLength = elevators.stream().mapToInt(e -> e.queue.size()).sum();
        d.stops = stops;
        d.trips = completed;
        d.distance = distance;
        d.energy = energy;
        d.faultCount = faultCount;
        int alerts = 0;
        if (safety.fire) alerts++; if (safety.smoke) alerts++; if (safety.estop) alerts++;
        if (safety.power) alerts++; if (safety.earthquake) alerts++; if (safety.flood) alerts++;
        d.activeAlerts = alerts;
        return d;
    }

    private double avg(List<Double> list) {
        if (list.isEmpty()) return 0;
        double sum = 0;
        for (double v : list) sum += v;
        return sum / list.size();
    }

    // ---------- Reports ----------

    public synchronized Map<String, Object> buildReport(String type) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", type);
        report.put("buildingName", building.name);
        report.put("systemVersion", "Infinity Tower ECS · v2.4.0 (backend)");
        report.put("simulatedTime", "Day " + simDay + " " + fmtClock());
        report.put("algorithmUsed", algorithm);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPassengers", totalPassengers);
        summary.put("completed", completed);
        summary.put("rejected", rejected);
        summary.put("trips", completed);
        summary.put("stops", stops);
        summary.put("distance", Math.round(distance * 100.0) / 100.0);
        summary.put("energy", Math.round(energy * 100.0) / 100.0);
        summary.put("avgWaitTime", Math.round(avg(waitTimes) * 100.0) / 100.0);
        summary.put("avgTravelTime", Math.round(avg(travelTimes) * 100.0) / 100.0);
        summary.put("faultCount", faultCount);
        report.put("performanceSummary", summary);
        List<Map<String, Object>> elevatorReports = new ArrayList<>();
        for (ElevatorModel e : elevators) {
            Map<String, Object> er = new LinkedHashMap<>();
            er.put("id", e.label); er.put("trips", e.tripCount);
            er.put("faults", e.fault != null ? 1 : 0); er.put("state", e.state);
            elevatorReports.add(er);
        }
        report.put("elevators", elevatorReports);
        return report;
    }
}
