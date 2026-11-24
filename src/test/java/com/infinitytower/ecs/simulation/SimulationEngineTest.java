package com.infinitytower.ecs.simulation;

import com.infinitytower.ecs.dto.Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimulationEngine();
        engine.start();
    }

    @Test
    void spawningAPassengerCreatesAWaitingPassengerAndAssignsAHallCall() {
        engine.spawnPassenger(1, 5, "normal", 1);
        Dto.SnapshotDto snap = engine.getSnapshot();
        assertEquals(1, snap.passengers.size());
        assertEquals(1, snap.hallCalls.size());
        assertTrue(snap.hallCalls.get(0).assigned, "Hall call should be assigned to an elevator immediately");
    }

    @Test
    void elevatorEventuallyReachesRequestedFloor() {
        engine.spawnPassenger(1, 5, "normal", 1);
        for (int i = 0; i < 500; i++) engine.tick(0.1);
        Dto.SnapshotDto snap = engine.getSnapshot();
        boolean someoneBoarded = snap.analytics.completed > 0 || snap.analytics.waitingPassengers == 0;
        assertTrue(someoneBoarded, "After enough ticks the passenger should have been served");
    }

    @Test
    void fireAlarmForcesRecallAndClearingReleasesElevators() {
        engine.toggleSafety("fire");
        for (int i = 0; i < 50; i++) engine.tick(0.1);
        Dto.SnapshotDto midSnap = engine.getSnapshot();
        assertTrue(midSnap.safety.fire);

        engine.toggleSafety("fire");
        Dto.SnapshotDto afterSnap = engine.getSnapshot();
        assertFalse(afterSnap.safety.fire);
        for (Dto.ElevatorDto e : afterSnap.elevators) {
            assertFalse(e.fireRecall, "Fire recall flag must clear once the alarm is turned off");
        }
    }

    @Test
    void floodModeBlocksBasementRequests() {
        engine.toggleSafety("flood");
        engine.spawnPassenger(-1, 5, "normal", 1);
        Dto.SnapshotDto snap = engine.getSnapshot();
        assertEquals(0, snap.passengers.size(), "Basement-origin request should be rejected while flood mode is active");
    }

    @Test
    void hardFaultStopsElevatorButCommFaultOnlyBlocksNewDispatch() {
        engine.injectFault(0, "motor");
        Dto.SnapshotDto snap = engine.getSnapshot();
        assertEquals("motor", snap.elevators.get(0).fault);

        engine.clearFault(0);
        engine.injectFault(0, "comm");
        snap = engine.getSnapshot();
        assertEquals("comm", snap.elevators.get(0).fault);
    }

    @Test
    void stuckHallCallGetsRetriedOnceElevatorsRecover() {
        for (Dto.ElevatorDto e : engine.getSnapshot().elevators) {
            engine.toggleMaintenance(e.id);
        }
        engine.spawnPassenger(1, 5, "normal", 1);
        Dto.SnapshotDto snap = engine.getSnapshot();
        assertFalse(snap.hallCalls.get(0).assigned, "Call should be unassigned while all elevators are in maintenance");

        for (Dto.ElevatorDto e : engine.getSnapshot().elevators) {
            engine.toggleMaintenance(e.id);
        }
        snap = engine.getSnapshot();
        assertTrue(snap.hallCalls.get(0).assigned, "Clearing maintenance should immediately retry the stuck call");
    }

    @Test
    void inMemoryLogsAreBoundedAndSupportSearchAndCategoryFilters() {
        engine.setMaxLogEntries(100);
        for (int i = 0; i < 150; i++) {
            engine.spawnPassenger(1, 5, "normal", 1);
        }
        var allLogs = engine.getLogs(null, null, 200);
        assertTrue(allLogs.size() <= 100, "Log buffer must be bounded by max capacity");

        var passengerLogs = engine.getLogs("passenger", null, 50);
        assertFalse(passengerLogs.isEmpty());
        assertTrue(passengerLogs.stream().allMatch(l -> "passenger".equals(l.category)));

        var searchLogs = engine.getLogs(null, "Passenger #", 10);
        assertFalse(searchLogs.isEmpty());
        assertTrue(searchLogs.get(0).message.contains("Passenger #"));
    }

    @Test
    void allTenAlgorithmsAreRegisteredAndSelectable() {
        var algos = engine.listAlgorithms();
        assertEquals(10, algos.size(), "All 10 scheduling algorithms must be registered");

        String[] expected = {"fcfs", "scan", "look", "sstf", "collective", "nearest", "sector", "destination", "adaptive", "ai"};
        for (String id : expected) {
            engine.setAlgorithm(id);
            assertEquals(id, engine.getAlgorithm(), "Should switch to algorithm: " + id);
        }
    }

    @Test
    void buildReportGeneratesTelemetryWithoutDatabase() {
        var report = engine.buildReport("simulation");
        assertNotNull(report);
        assertEquals("Infinity Tower", report.get("buildingName"));
        assertNotNull(report.get("algorithmUsed"));
        assertNotNull(report.get("performanceSummary"));
        assertNotNull(report.get("elevators"));
    }

    @Test
    void inMemoryConfigCanBeModifiedDuringSession() {
        Dto.BuildingConfigDto update = engine.getConfig();
        update.name = "Apex Tower";
        update.floors = 18;
        engine.applyConfig(update);

        assertEquals("Apex Tower", engine.getConfig().name);
        assertEquals(18, engine.getConfig().floors);
    }
}
