package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        service.start();
        return Map.of("running", service.isRunning());
    }

    @PostMapping("/pause")
    public Map<String, Object> pause() {
        service.pause();
        return Map.of("running", service.isRunning());
    }

    @PostMapping("/resume")
    public Map<String, Object> resume() {
        service.resume();
        return Map.of("running", service.isRunning());
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        service.reset();
        return Map.of("status", "reset");
    }

    @PostMapping("/speed")
    public Map<String, Object> setSpeed(@RequestBody Map<String, Double> body) {
        double mult = body.getOrDefault("multiplier", 1.0);
        service.setSpeed(mult);
        return Map.of("speed", service.getSpeed());
    }

    @GetMapping("/snapshot")
    public Dto.SnapshotDto snapshot() {
        return service.getSnapshot();
    }
}
