package com.infinitytower.ecs.simulation;

import java.util.List;

class DestinationDispatchScheduler implements SchedulingAlgorithm {
    public String id() { return "destination"; }
    public String displayName() { return "Destination Dispatch"; }
    public String description() { return "Groups riders by destination before boarding."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        return available.stream().min((a, b) -> {
            double ca = DispatchContext.distanceCost(a, call.floor) - groupBonus(a, call) * 1.5;
            double cb = DispatchContext.distanceCost(b, call.floor) - groupBonus(b, call) * 1.5;
            return Double.compare(ca, cb);
        }).orElse(null);
    }

    private long groupBonus(ElevatorModel e, HallCallModel call) {
        int callSign = Integer.signum(call.floor - (int) Math.round(e.currentFloor));
        return e.queue.stream().filter(q -> Integer.signum(q - (int) Math.round(e.currentFloor)) == callSign).count();
    }
}
