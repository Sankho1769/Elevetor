package com.infinitytower.ecs.simulation;

import java.util.List;

class AdaptiveScheduler implements SchedulingAlgorithm {
    public String id() { return "adaptive"; }
    public String displayName() { return "Adaptive"; }
    public String description() { return "Switches strategy with live traffic conditions."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        int threshold = ctx.allElevators.size() * 2;
        if (ctx.pendingLoad > threshold) {
            List<ElevatorModel> inSector = available.stream()
                    .filter(e -> call.floor >= e.sectorMin && call.floor <= e.sectorMax).toList();
            List<ElevatorModel> pool = inSector.isEmpty() ? available : inSector;
            return pool.stream().max((a, b) -> Double.compare(
                    DispatchContext.scoreElevator(a, call, building, ctx.safety),
                    DispatchContext.scoreElevator(b, call, building, ctx.safety))).orElse(null);
        }
        return available.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
