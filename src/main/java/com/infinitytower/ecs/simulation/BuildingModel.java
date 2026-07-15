package com.infinitytower.ecs.simulation;

import java.util.ArrayList;
import java.util.List;

class BuildingModel {
    String name = "Infinity Tower";
    int floors = 12;
    int basements = 2;
    int lobby = 1;
    int idleFloor = 1;
    List<Integer> parkingFloors = List.of(-1, -2);
    List<Integer> vipFloors = List.of(10, 11, 12);
    List<Integer> serviceFloors = List.of(-2);
    List<Integer> mechanicalFloors = List.of();
    int elevatorCount = 4;
    int capacity = 8;
    int weightLimit = 900;
    double maxSpeed = 2.2;
    double acceleration = 1.1;
    double deceleration = 1.1;
    double doorOpenTime = 1.6;
    double doorCloseTime = 1.6;
    double loadTime = 1.2;
    double unloadTime = 1.0;
    boolean energySaving = false;

    List<Integer> floorRange() {
        List<Integer> result = new ArrayList<>();
        int bottom = basements > 0 ? -basements : 1;
        for (int f = floors; f >= bottom; f--) {
            if (f == 0) continue;
            result.add(f);
        }
        return result;
    }

    List<Integer> travelableFloors(boolean floodLocked) {
        List<Integer> all = floorRange();
        if (!floodLocked) return all;
        List<Integer> result = new ArrayList<>();
        for (int f : all) if (f > 0) result.add(f);
        return result;
    }

    int topFloor() {
        return floorRange().stream().max(Integer::compareTo).orElse(floors);
    }

    int bottomFloor() {
        return floorRange().stream().min(Integer::compareTo).orElse(-basements);
    }
}
