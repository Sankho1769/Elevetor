package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.domain.SimulationLogEntity;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final SimulationService service;

    public LogController(SimulationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SimulationLogEntity> logs(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "150") int limit) {
        return service.getLogs(category, search, limit);
    }
}
