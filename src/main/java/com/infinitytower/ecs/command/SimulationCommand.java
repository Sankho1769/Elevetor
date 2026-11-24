package com.infinitytower.ecs.command;

import com.infinitytower.ecs.dto.Dto;

/**
 * Immutable command records for the lock-free simulation command pipeline.
 * External HTTP requests enqueue these records in O(1) without acquiring monitor locks.
 */
public sealed interface SimulationCommand {

    record SpawnPassenger(Integer origin, Integer dest, String priority, int group) implements SimulationCommand {}
    record SetAlgorithm(String algorithmId) implements SimulationCommand {}
    record SetSpeed(double multiplier) implements SimulationCommand {}
    record Lifecycle(LifecycleAction action) implements SimulationCommand {}
    record ApplyConfig(Dto.BuildingConfigDto config) implements SimulationCommand {}
    record ToggleSafety(String key) implements SimulationCommand {}
    record ClearAllSafety() implements SimulationCommand {}
    record InjectFault(int elevatorId, String faultType) implements SimulationCommand {}
    record ClearFault(int elevatorId) implements SimulationCommand {}
    record ToggleMaintenance(int elevatorId) implements SimulationCommand {}
    record ToggleInspection(int elevatorId) implements SimulationCommand {}
    record SetAutoGen(String mode, double ratePerMinute) implements SimulationCommand {}

    enum LifecycleAction {
        START, PAUSE, RESUME, RESET
    }
}
