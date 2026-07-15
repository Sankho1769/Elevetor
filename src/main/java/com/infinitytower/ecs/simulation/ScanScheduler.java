package com.infinitytower.ecs.simulation;

import java.util.List;

class ScanScheduler implements SchedulingAlgorithm {
    public String id() { return "scan"; }
    public String displayName() { return "SCAN"; }
    public String description() { return "Sweeps to the end of the shaft, then reverses."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        List<ElevatorModel> sameDir = available.stream().filter(e -> DispatchContext.sameDirectionMatch(e, call)).toList();
        List<ElevatorModel> pool = sameDir.isEmpty() ? available : sameDir;
        return pool.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
