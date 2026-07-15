package com.infinitytower.ecs.repository;

import com.infinitytower.ecs.domain.SimulationLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationLogRepository extends JpaRepository<SimulationLogEntity, Long> {
    List<SimulationLogEntity> findByCategoryOrderByIdDesc(String category, Pageable pageable);
    List<SimulationLogEntity> findByMessageContainingIgnoreCaseOrderByIdDesc(String text, Pageable pageable);
    List<SimulationLogEntity> findAllByOrderByIdDesc(Pageable pageable);
}
