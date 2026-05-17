package com.apiguard.backend.domain.apispec.repository;

import com.apiguard.backend.domain.apispec.entity.ApiSpecDiff;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiSpecDiffRepository extends JpaRepository<ApiSpecDiff, Long> {

    List<ApiSpecDiff> findBySpecSourceIdOrderByCheckedAtDesc(Long specSourceId);
}
