package com.infinitytower.ecs.simulation;

import java.util.List;

class LookScheduler implements SchedulingAlgorithm {
    public String id() { return "look"; }
    public String displayName() { return "LOOK"; }
    public String description() { return "Reverses at the last request, not the shaft end."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        List<ElevatorModel> sameDir = available.stream().filter(e -> DispatchContext.sameDirectionMatch(e, call)).toList();
        List<ElevatorModel> pool = sameDir.isEmpty() ? available : sameDir;
        return pool.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
