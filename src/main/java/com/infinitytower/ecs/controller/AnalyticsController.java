package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final SimulationService service;

    public AnalyticsController(SimulationService service) {
        this.service = service;
    }

    @GetMapping
    public Dto.AnalyticsDto get() {
        return service.getAnalytics();
    }
}
