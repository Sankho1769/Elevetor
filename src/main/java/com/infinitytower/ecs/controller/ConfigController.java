package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final SimulationService service;

    public ConfigController(SimulationService service) {
        this.service = service;
    }

    @GetMapping
    public Dto.BuildingConfigDto getConfig() {
        return service.getConfig();
    }

    @PutMapping
    public Dto.BuildingConfigDto updateConfig(@RequestBody Dto.BuildingConfigDto dto) {
        service.applyConfig(dto);
        return service.getConfig();
    }
}
