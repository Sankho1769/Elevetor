package com.infinitytower.ecs.simulation;

import java.util.List;

class NearestCarScheduler implements SchedulingAlgorithm {
    public String id() { return "nearest"; }
    public String displayName() { return "Nearest Car"; }
    public String description() { return "Hall call goes to the nearest idle-capable car."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        List<ElevatorModel> idle = available.stream().filter(e -> e.queue.isEmpty()).toList();
        List<ElevatorModel> pool = idle.isEmpty() ? available : idle;
        return pool.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
