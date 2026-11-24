package com.infinitytower.ecs.simulation;

import com.infinitytower.ecs.command.SimulationCommand;
import com.infinitytower.ecs.dto.Dto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class LockFreeCommandPipelineConcurrencyTest {

    @Test
    @DisplayName("High-concurrency benchmark: 50 threads enqueue 1,000 commands without blocking or locks")
    void testConcurrentCommandSubmissionAndDraining() throws Exception {
        SimulationEngine engine = new SimulationEngine();
        engine.start();

        int threadCount = 50;
        int commandsPerThread = 20;
        int totalCommands = threadCount * commandsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger submitted = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int originFloor = (t % 12) + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < commandsPerThread; i++) {
                        engine.submitCommand(new SimulationCommand.SpawnPassenger(originFloor, 1, "normal", 1));
                        submitted.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all 50 threads simultaneously
        long startTime = System.nanoTime();
        startLatch.countDown();
        boolean finishedInTime = doneLatch.await(5, TimeUnit.SECONDS);
        long elapsedMicros = (System.nanoTime() - startTime) / 1000;

        assertTrue(finishedInTime, "All 50 threads must complete submission without deadlocks");
        assertEquals(totalCommands, submitted.get());
        assertTrue(elapsedMicros < 500_000, "1,000 submissions across 50 threads must complete in < 500ms");

        // Verify that queue depth equals total submitted commands
        assertEquals(totalCommands, engine.getQueueDepth());

        // Perform one physics tick: drains all 1,000 commands in batch
        engine.tick(0.1);

        // Queue must be fully drained
        assertEquals(0, engine.getQueueDepth());

        // Snapshot must be available immediately without waiting
        Dto.SnapshotDto snap = engine.getSnapshot();
        assertNotNull(snap);
        assertTrue(snap.passengers.size() > 0, "Drained commands must have populated passenger state");

        executor.shutdown();
    }
}
