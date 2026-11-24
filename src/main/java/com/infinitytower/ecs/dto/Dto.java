package com.infinitytower.ecs.dto;

import java.util.List;

public final class Dto {

    private Dto() {}

    public static class BuildingConfigDto {
        public String name;
        public int floors;
        public int basements;
        public int lobby;
        public int idleFloor;
        public List<Integer> parkingFloors;
        public List<Integer> vipFloors;
        public List<Integer> serviceFloors;
        public List<Integer> mechanicalFloors;
        public int elevatorCount;
        public int capacity;
        public int weightLimit;
        public double maxSpeed;
        public double acceleration;
        public double deceleration;
        public double doorOpenTime;
        public double doorCloseTime;
        public double loadTime;
        public double unloadTime;
        public boolean energySaving;
        public String algorithm;
    }

    public static class ElevatorDto {
        public int id;
        public String label;
        public double currentFloor;
        public int direction;
        public String doorState;
        public double doorProgress;
        public String state;
        public int passengerCount;
        public double weight;
        public List<Integer> queue;
        public String fault;
        public boolean maintenance;
        public boolean inspection;
        public boolean independentService;
        public boolean fireRecall;
        public int sectorMin;
        public int sectorMax;
        public int tripCount;
    }

    public static class PassengerDto {
        public long id;
        public int origin;
        public int dest;
        public String priority;
        public String state;
        public double spawnTime;
        public double waitSoFar;
        public Integer assignedElevatorId;
    }

    public static class HallCallDto {
        public long id;
        public int floor;
        public String dir;
        public String priority;
        public boolean assigned;
        public Integer elevatorId;
    }

    public static class SafetyStateDto {
        public boolean fire;
        public boolean smoke;
        public boolean estop;
        public boolean power;
        public boolean earthquake;
        public boolean flood;
    }

    public static class AnalyticsDto {
        public int totalPassengers;
        public int activePassengers;
        public int waitingPassengers;
        public int completed;
        public int rejected;
        public int totalRequests;
        public double avgWaitTime;
        public double avgTravelTime;
        public double utilizationPct;
        public int queueLength;
        public int stops;
        public int trips;
        public double distance;
        public double energy;
        public int faultCount;
        public int activeAlerts;
    }

    public static class LogEntryDto {
        public long id;
        public int simDay;
        public String simClock;
        public String category;
        public String level;
        public String message;
        public String createdAt;

        public LogEntryDto() {}

        public LogEntryDto(long id, int simDay, String simClock, String category, String level, String message, String createdAt) {
            this.id = id;
            this.simDay = simDay;
            this.simClock = simClock;
            this.category = category;
            this.level = level;
            this.message = message;
            this.createdAt = createdAt;
        }
    }

    public static class SnapshotDto {
        public int simDay;
        public double simSeconds;
        public boolean running;
        public String algorithm;
        public BuildingConfigDto building;
        public List<ElevatorDto> elevators;
        public List<PassengerDto> passengers;
        public List<HallCallDto> hallCalls;
        public SafetyStateDto safety;
        public AnalyticsDto analytics;
        public List<LogEntryDto> recentLogs;
    }

    public static class RequestSpawnDto {
        public Integer origin;
        public Integer dest;
        public String priority;
        public Integer group;
    }

    public static class FaultActionDto {
        public int elevatorId;
        public String action;
        public String faultType;
    }

    public static class SafetyToggleDto {
        public String key;
    }

    public static class AlgorithmChangeDto {
        public String algorithm;
    }

    public static class AutoGenDto {
        public String mode;
        public double ratePerMinute;
    }

    public static class AlgorithmInfoDto {
        public String id;
        public String name;
        public String description;

        public AlgorithmInfoDto(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }
}
