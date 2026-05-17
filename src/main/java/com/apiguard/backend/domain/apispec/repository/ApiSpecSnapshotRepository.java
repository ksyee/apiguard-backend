package com.apiguard.backend.domain.apispec.repository;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiSpecSnapshotRepository extends JpaRepository<ApiSpecSnapshot, Long> {

    Optional<ApiSpecSnapshot> findFirstBySpecSourceIdOrderByCapturedAtDesc(Long specSourceId);

    List<ApiSpecSnapshot> findBySpecSourceIdOrderByCapturedAtDesc(Long specSourceId);
}
