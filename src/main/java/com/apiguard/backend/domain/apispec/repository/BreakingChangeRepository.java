package com.apiguard.backend.domain.apispec.repository;

import com.apiguard.backend.domain.apispec.entity.BreakingChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreakingChangeRepository extends JpaRepository<BreakingChange, Long> {

    List<BreakingChange> findByDiffIdOrderByIdAsc(Long diffId);
}
