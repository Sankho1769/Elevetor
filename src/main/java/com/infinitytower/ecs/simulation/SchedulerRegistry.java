package com.infinitytower.ecs.simulation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SchedulerRegistry {
    private final Map<String, SchedulingAlgorithm> byId = new LinkedHashMap<>();

    SchedulerRegistry() {
        register(new FcfsScheduler());
        register(new ScanScheduler());
        register(new LookScheduler());
        register(new SstfScheduler());
        register(new CollectiveScheduler());
        register(new NearestCarScheduler());
        register(new SectorScheduler());
        register(new DestinationDispatchScheduler());
        register(new AdaptiveScheduler());
        register(new AiDynamicScheduler());
    }

    private void register(SchedulingAlgorithm algo) {
        byId.put(algo.id(), algo);
    }

    SchedulingAlgorithm get(String id) {
        return byId.getOrDefault(id, byId.get("ai"));
    }

    List<SchedulingAlgorithm> all() {
        return List.copyOf(byId.values());
    }
}
