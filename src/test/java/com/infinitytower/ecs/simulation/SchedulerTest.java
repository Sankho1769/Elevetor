package com.infinitytower.ecs.simulation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    private ElevatorModel elevatorAt(int id, double floor) {
        ElevatorModel e = new ElevatorModel();
        e.id = id;
        e.label = String.valueOf((char) ('A' + id));
        e.currentFloor = floor;
        e.sectorMin = -2;
        e.sectorMax = 12;
        return e;
    }

    private HallCallModel callAt(int floor, String dir) {
        HallCallModel h = new HallCallModel();
        h.floor = floor;
        h.dir = dir;
        h.priority = "normal";
        return h;
    }

    @Test
    void nearestCarPrefersIdleCarsOverBusyCloserCars() {
        ElevatorModel idleFar = elevatorAt(0, 1);
        ElevatorModel busyClose = elevatorAt(1, 4);
        busyClose.queue.add(9);
        busyClose.direction = 1;

        List<ElevatorModel> available = List.of(idleFar, busyClose);
        DispatchContext ctx = new DispatchContext(available, 0, new SafetyStateModel());

        NearestCarScheduler scheduler = new NearestCarScheduler();
        ElevatorModel chosen = scheduler.selectElevator(available, callAt(5, "up"), new BuildingModel(), ctx);

        assertEquals(idleFar, chosen, "Nearest Car should prefer the idle car even though it is farther away");
    }

    @Test
    void sstfPicksPurelyByDistanceRegardlessOfIdleState() {
        ElevatorModel idleFar = elevatorAt(0, 1);
        ElevatorModel busyClose = elevatorAt(1, 4);
        busyClose.queue.add(9);
        busyClose.direction = 1;

        List<ElevatorModel> available = List.of(idleFar, busyClose);
        DispatchContext ctx = new DispatchContext(available, 0, new SafetyStateModel());

        SstfScheduler scheduler = new SstfScheduler();
        ElevatorModel chosen = scheduler.selectElevator(available, callAt(5, "up"), new BuildingModel(), ctx);

        assertEquals(busyClose, chosen, "SSTF should pick the physically closest car even if busy");
    }

    @Test
    void fcfsRotatesAcrossElevatorsRegardlessOfDistance() {
        ElevatorModel a = elevatorAt(0, 1);
        ElevatorModel b = elevatorAt(1, 2);
        ElevatorModel c = elevatorAt(2, 3);
        List<ElevatorModel> available = List.of(a, b, c);
        DispatchContext ctx = new DispatchContext(available, 0, new SafetyStateModel());

        FcfsScheduler scheduler = new FcfsScheduler();
        ElevatorModel first = scheduler.selectElevator(available, callAt(5, "up"), new BuildingModel(), ctx);
        ElevatorModel second = scheduler.selectElevator(available, callAt(5, "up"), new BuildingModel(), ctx);
        ElevatorModel third = scheduler.selectElevator(available, callAt(5, "up"), new BuildingModel(), ctx);

        assertNotEquals(first.id, second.id, "FCFS should rotate to a different car on each call");
        assertNotEquals(second.id, third.id, "FCFS should rotate to a different car on each call");
    }

    @Test
    void sectorSchedulerRestrictsToOwningSectorWhenPossible() {
        ElevatorModel lowSector = elevatorAt(0, 1);
        lowSector.sectorMin = -2; lowSector.sectorMax = 4;
        ElevatorModel highSector = elevatorAt(1, 2);
        highSector.sectorMin = 5; highSector.sectorMax = 12;

        List<ElevatorModel> available = List.of(lowSector, highSector);
        DispatchContext ctx = new DispatchContext(available, 0, new SafetyStateModel());

        SectorScheduler scheduler = new SectorScheduler();
        ElevatorModel chosen = scheduler.selectElevator(available, callAt(10, "up"), new BuildingModel(), ctx);

        assertEquals(highSector, chosen, "Sector scheduler should route to the car owning that floor band");
    }

    @Test
    void collectiveControlPrefersCarAlreadyEnRoute() {
        ElevatorModel enRoute = elevatorAt(0, 2);
        enRoute.direction = 1;
        ElevatorModel idleCloser = elevatorAt(1, 6);

        List<ElevatorModel> available = List.of(enRoute, idleCloser);
        DispatchContext ctx = new DispatchContext(available, 0, new SafetyStateModel());

        CollectiveScheduler scheduler = new CollectiveScheduler();
        ElevatorModel chosen = scheduler.selectElevator(available, callAt(7, "up"), new BuildingModel(), ctx);

        assertEquals(enRoute, chosen, "Collective control should favor a car already moving toward the call");
    }

    @Test
    void allTenAlgorithmsAreRegisteredAndDistinct() {
        SchedulerRegistry registry = new SchedulerRegistry();
        List<String> ids = new ArrayList<>();
        for (SchedulingAlgorithm a : registry.all()) ids.add(a.id());
        assertEquals(10, ids.size());
        assertEquals(10, ids.stream().distinct().count(), "All algorithm ids must be unique");
    }

    @Test
    void allTenAlgorithmsSuccessfullyDispatchCalls() {
        SchedulerRegistry registry = new SchedulerRegistry();
        BuildingModel building = new BuildingModel();
        ElevatorModel a = elevatorAt(0, 1);
        ElevatorModel b = elevatorAt(1, 5);
        ElevatorModel c = elevatorAt(2, 9);
        List<ElevatorModel> available = List.of(a, b, c);
        DispatchContext ctx = new DispatchContext(available, 3, new SafetyStateModel());

        for (SchedulingAlgorithm algo : registry.all()) {
            ElevatorModel chosenUp = algo.selectElevator(available, callAt(4, "up"), building, ctx);
            assertNotNull(chosenUp, algo.id() + " should select a car for UP call");
            assertTrue(available.contains(chosenUp), algo.id() + " must select from available cars");

            ElevatorModel chosenDown = algo.selectElevator(available, callAt(7, "down"), building, ctx);
            assertNotNull(chosenDown, algo.id() + " should select a car for DOWN call");
            assertTrue(available.contains(chosenDown), algo.id() + " must select from available cars");
        }
    }
}
