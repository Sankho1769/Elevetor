package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final SimulationService service;

    public RequestController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/spawn")
    public Map<String, String> spawn(@RequestBody Dto.RequestSpawnDto dto) {
        service.spawnPassenger(dto.origin, dto.dest, dto.priority, dto.group);
        return Map.of("status", "spawned");
    }

    @PostMapping("/batch")
    public Map<String, String> batch(@RequestParam(defaultValue = "10") int count) {
        service.spawnBatchRandom(count);
        return Map.of("status", "spawned", "count", String.valueOf(count));
    }

    @PostMapping("/autogen")
    public Map<String, String> autoGen(@RequestBody Dto.AutoGenDto dto) {
        service.setAutoGen(dto.mode, dto.ratePerMinute);
        return Map.of("status", "updated");
    }
}
