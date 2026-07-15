package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/safety")
public class SafetyController {

    private final SimulationService service;

    public SafetyController(SimulationService service) {
        this.service = service;
    }

    @GetMapping
    public Dto.SafetyStateDto get() {
        return service.getSnapshot().safety;
    }

    @PostMapping("/toggle")
    public Dto.SafetyStateDto toggle(@RequestBody Dto.SafetyToggleDto dto) {
        service.toggleSafety(dto.key);
        return service.getSnapshot().safety;
    }

    @PostMapping("/clear-all")
    public Dto.SafetyStateDto clearAll() {
        service.clearAllSafety();
        return service.getSnapshot().safety;
    }

    @PostMapping("/fault")
    public Map<String, String> fault(@RequestBody Dto.FaultActionDto dto) {
        switch (dto.action) {
            case "inject" -> service.injectFault(dto.elevatorId, dto.faultType);
            case "clear" -> service.clearFault(dto.elevatorId);
            case "maintenance" -> service.toggleMaintenance(dto.elevatorId);
            case "inspection" -> service.toggleInspection(dto.elevatorId);
            case "independent" -> service.toggleIndependentService(dto.elevatorId);
            default -> { }
        }
        return Map.of("status", "ok");
    }
}
