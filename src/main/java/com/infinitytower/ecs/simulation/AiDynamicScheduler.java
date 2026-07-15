package com.infinitytower.ecs.simulation;

import java.util.List;

class AiDynamicScheduler implements SchedulingAlgorithm {
    public String id() { return "ai"; }
    public String displayName() { return "AI-Inspired Dynamic"; }
    public String description() { return "Weighted multi-factor scoring per elevator."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        return available.stream().max((a, b) -> Double.compare(
                DispatchContext.scoreElevator(a, call, building, ctx.safety),
                DispatchContext.scoreElevator(b, call, building, ctx.safety))).orElse(null);
    }
}
