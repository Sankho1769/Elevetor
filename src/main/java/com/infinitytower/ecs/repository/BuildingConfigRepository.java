package com.infinitytower.ecs.repository;

import com.infinitytower.ecs.domain.BuildingConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingConfigRepository extends JpaRepository<BuildingConfigEntity, Long> {
}
