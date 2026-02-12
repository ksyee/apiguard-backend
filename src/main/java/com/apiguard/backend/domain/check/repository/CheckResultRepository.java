package com.apiguard.backend.domain.check.repository;

import com.apiguard.backend.domain.check.entity.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {
}
