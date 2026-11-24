package com.infinitytower.ecs.metrics;

import com.infinitytower.ecs.simulation.SimulationEngine;
import com.infinitytower.ecs.websocket.SimulationSocketHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Enterprise Micrometer metrics instrumentation for SCADA telemetry,
 * JVM observability, and Prometheus/Grafana monitoring.
 */
@Component
public class SimulationMetrics {

    private final Timer tickTimer;
    private final Timer dispatchTimer;
    private final Counter completedPassengersCounter;

    public SimulationMetrics(MeterRegistry registry, SimulationEngine engine, SimulationSocketHandler socketHandler) {
        this.tickTimer = Timer.builder("ecs.simulation.tick.time")
                .description("Time taken per simulation physics tick")
                .register(registry);

        this.dispatchTimer = Timer.builder("ecs.simulation.dispatch.time")
                .description("Time taken to evaluate and assign scheduling algorithm")
                .register(registry);

        this.completedPassengersCounter = Counter.builder("ecs.passengers.completed.total")
                .description("Total passengers delivered to destination")
                .register(registry);

        Gauge.builder("ecs.passengers.waiting", engine, e -> {
            var snap = e.getSnapshot();
            return snap != null && snap.analytics != null ? snap.analytics.waitingPassengers : 0;
        })
        .description("Current waiting passengers in queue")
        .register(registry);

        Gauge.builder("ecs.websocket.active_sessions", socketHandler, SimulationSocketHandler::connectedClients)
        .description("Number of active WebSocket telemetry subscribers")
        .register(registry);

        Gauge.builder("ecs.command_queue.depth", engine, SimulationEngine::getQueueDepth)
        .description("Number of pending commands in lock-free queue")
        .register(registry);
    }

    public Timer getTickTimer() { return tickTimer; }
    public Timer getDispatchTimer() { return dispatchTimer; }
    public Counter getCompletedPassengersCounter() { return completedPassengersCounter; }
}
