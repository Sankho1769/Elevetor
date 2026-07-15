package com.infinitytower.ecs.simulation;

import java.util.List;

class DispatchContext {
    final List<ElevatorModel> allElevators;
    final int pendingLoad;
    final SafetyStateModel safety;

    DispatchContext(List<ElevatorModel> allElevators, int pendingLoad, SafetyStateModel safety) {
        this.allElevators = allElevators;
        this.pendingLoad = pendingLoad;
        this.safety = safety;
    }

    static double distanceCost(ElevatorModel e, int floor) {
        return Math.abs(e.currentFloor - floor);
    }

    static boolean sameDirectionMatch(ElevatorModel e, HallCallModel call) {
        if (e.direction == 0) return true;
        if (e.direction > 0) return call.floor >= e.currentFloor && "up".equals(call.dir);
        return call.floor <= e.currentFloor && "down".equals(call.dir);
    }

    static boolean enRouteMatch(ElevatorModel e, HallCallModel call) {
        if (e.direction == 0) return false;
        if (e.direction > 0) return call.floor >= e.currentFloor && "up".equals(call.dir);
        return call.floor <= e.currentFloor && "down".equals(call.dir);
    }

    static double scoreElevator(ElevatorModel e, HallCallModel call, BuildingModel building, SafetyStateModel safety) {
        if (!e.isDispatchEligible(safety)) return Double.NEGATIVE_INFINITY;
        double score = 0;
        score -= distanceCost(e, call.floor) * 2.2;
        if (sameDirectionMatch(e, call)) score += 6;
        score -= e.queue.size() * 1.8;
        score -= ((double) e.passengers.size() / Math.max(1, building.capacity)) * 5;
        score += PassengerModel.priorityWeight(call.priority) * 2.5;
        if ("idle".equals(e.state)) score += 3;
        return score;
    }
}
