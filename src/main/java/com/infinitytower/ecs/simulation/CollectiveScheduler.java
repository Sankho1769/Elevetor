package com.infinitytower.ecs.simulation;

import java.util.List;

class CollectiveScheduler implements SchedulingAlgorithm {
    public String id() { return "collective"; }
    public String displayName() { return "Collective Control"; }
    public String description() { return "Picks up same-direction calls along the way."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        List<ElevatorModel> enRoute = available.stream().filter(e -> DispatchContext.enRouteMatch(e, call)).toList();
        List<ElevatorModel> idle = available.stream().filter(e -> e.direction == 0).toList();
        List<ElevatorModel> pool = !enRoute.isEmpty() ? enRoute : (!idle.isEmpty() ? idle : available);
        return pool.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
