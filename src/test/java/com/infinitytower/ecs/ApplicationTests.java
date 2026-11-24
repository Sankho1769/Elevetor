package com.infinitytower.ecs;

import com.infinitytower.ecs.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplicationTests {

    @Autowired
    private SimulationService simulationService;

    @Test
    void contextLoadsWithoutAnyDatabase() {
        assertNotNull(simulationService, "SimulationService should be loaded into Spring context without database");
        assertNotNull(simulationService.getConfig(), "Building config should be loaded in-memory");
        assertEquals("Infinity Tower", simulationService.getConfig().name);
        assertTrue(simulationService.listAlgorithms().size() >= 10, "All 10 algorithms should be registered");
    }
}
