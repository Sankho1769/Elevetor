package com.infinitytower.ecs.simulation;

import java.util.List;

class SstfScheduler implements SchedulingAlgorithm {
    public String id() { return "sstf"; }
    public String displayName() { return "SSTF"; }
    public String description() { return "Always serves the physically closest request."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        return available.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
