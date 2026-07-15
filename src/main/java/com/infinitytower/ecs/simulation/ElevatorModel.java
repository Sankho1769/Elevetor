package com.infinitytower.ecs.simulation;

import java.util.ArrayList;
import java.util.List;

class ElevatorModel {
    static final List<String> HARD_FAULTS = List.of("sensor", "motor", "cable", "brake");

    int id;
    String label;
    double currentFloor;
    double speed = 0;
    int direction = 0;
    List<Integer> queue = new ArrayList<>();
    List<PassengerModel> passengers = new ArrayList<>();
    String doorState = "closed";
    double doorProgress = 0;
    double doorTimer = 0;
    String state = "idle";
    double weight = 0;
    String fault = null;
    boolean maintenance = false;
    boolean inspection = false;
    boolean independentService = false;
    boolean fireRecall = false;
    boolean earthquakeHold = false;
    int sectorMin;
    int sectorMax;
    int tripCount = 0;
    double idleSince = 0;

    boolean hasHardFault() {
        return fault != null && HARD_FAULTS.contains(fault);
    }

    boolean isAvailable(SafetyStateModel safety) {
        return !hasHardFault() && !maintenance && !fireRecall
                && !safety.estop && !safety.power && !safety.earthquake;
    }

    boolean isDispatchEligible(SafetyStateModel safety) {
        return isAvailable(safety) && !inspection && !independentService
                && !"comm".equals(fault) && !"door".equals(fault);
    }

    Integer firstQueueFloor() {
        return queue.isEmpty() ? null : queue.get(0);
    }

    void addStop(int floor) {
        if (!queue.contains(floor)) queue.add(floor);
    }
}
