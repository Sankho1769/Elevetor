package com.infinitytower.ecs.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "simulation_log")
public class SimulationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sim_day")
    private int simDay;

    @Column(name = "sim_clock")
    private String simClock;

    private String category;
    private String level;
    private String message;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public SimulationLogEntity() {}

    public SimulationLogEntity(int simDay, String simClock, String category, String level, String message) {
        this.simDay = simDay;
        this.simClock = simClock;
        this.category = category;
        this.level = level;
        this.message = message;
    }

    public Long getId() { return id; }
    public int getSimDay() { return simDay; }
    public String getSimClock() { return simClock; }
    public String getCategory() { return category; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
