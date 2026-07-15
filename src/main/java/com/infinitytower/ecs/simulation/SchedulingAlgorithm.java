package com.infinitytower.ecs.simulation;

import java.util.List;

interface SchedulingAlgorithm {
    String id();
    String displayName();
    String description();
    ElevatorModel selectElevator(List<ElevatorModel> available, HallCallModel call, BuildingModel building, DispatchContext ctx);
}
