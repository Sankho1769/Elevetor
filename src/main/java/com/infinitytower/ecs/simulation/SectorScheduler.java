package com.infinitytower.ecs.simulation;

import java.util.List;

class SectorScheduler implements SchedulingAlgorithm {
    public String id() { return "sector"; }
    public String displayName() { return "Sector Based"; }
    public String description() { return "Each car owns a floor band; calls route by sector."; }

    public ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx) {
        List<ElevatorModel> inSector = available.stream()
                .filter(e -> call.floor >= e.sectorMin && call.floor <= e.sectorMax).toList();
        List<ElevatorModel> pool = inSector.isEmpty() ? available : inSector;
        return pool.stream().min((a, b) -> Double.compare(
                DispatchContext.distanceCost(a, call.floor), DispatchContext.distanceCost(b, call.floor))).orElse(null);
    }
}
