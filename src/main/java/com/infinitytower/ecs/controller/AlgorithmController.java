package com.infinitytower.ecs.controller;

import com.infinitytower.ecs.dto.Dto;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/algorithm")
public class AlgorithmController {

    private final SimulationService service;

    public AlgorithmController(SimulationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Dto.AlgorithmInfoDto> list() {
        return service.listAlgorithms();
    }

    @GetMapping("/current")
    public Map<String, String> current() {
        return Map.of("algorithm", service.getAlgorithm());
    }

    @PostMapping
    public Map<String, String> set(@RequestBody Dto.AlgorithmChangeDto dto) {
        service.setAlgorithm(dto.algorithm);
        return Map.of("algorithm", service.getAlgorithm());
    }
}
