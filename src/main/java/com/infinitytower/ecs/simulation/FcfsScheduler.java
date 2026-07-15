package com.infinitytower.ecs.simulation;

import java.util.List;

class FcfsScheduler implements SchedulingAlgorithm {
    private int pointer = 0;

    public String id() { return "fcfs"; }
    public String displayName() { return "FCFS"; }
    public String description() { return "First request, first served, in arrival order."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        List<ElevatorModel> all = ctx.allElevators;
        int n = all.size();
        if (n == 0) return null;
        int idx = pointer % n;
        int tries = 0;
        while (!available.contains(all.get(idx)) && tries < n) {
            idx = (idx + 1) % n;
            tries++;
        }
        pointer = (idx + 1) % n;
        return available.contains(all.get(idx)) ? all.get(idx) : null;
    }
}
