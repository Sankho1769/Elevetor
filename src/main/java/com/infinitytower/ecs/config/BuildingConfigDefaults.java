package com.infinitytower.ecs.config;

import com.infinitytower.ecs.dto.Dto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provides predefined, immutable building configuration defaults.
 * Loaded on application startup or when resetting to factory configuration.
 */
@Component
public class BuildingConfigDefaults {

    public Dto.BuildingConfigDto createDefaultConfig() {
        Dto.BuildingConfigDto dto = new Dto.BuildingConfigDto();
        dto.name = "Infinity Tower";
        dto.floors = 12;
        dto.basements = 2;
        dto.lobby = 1;
        dto.idleFloor = 1;
        dto.parkingFloors = List.of(-1, -2);
        dto.vipFloors = List.of(10, 11, 12);
        dto.serviceFloors = List.of(-2);
        dto.mechanicalFloors = List.of();
        dto.elevatorCount = 4;
        dto.capacity = 8;
        dto.weightLimit = 900;
        dto.maxSpeed = 2.2;
        dto.acceleration = 1.1;
        dto.deceleration = 1.1;
        dto.doorOpenTime = 1.6;
        dto.doorCloseTime = 1.6;
        dto.loadTime = 1.2;
        dto.unloadTime = 1.0;
        dto.energySaving = false;
        dto.algorithm = "look";
        return dto;
    }
}
