package com.apiguard.backend.domain.apispec.repository;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiSpecSourceRepository extends JpaRepository<ApiSpecSource, Long> {

    Optional<ApiSpecSource> findByIdAndDeletedFalse(Long id);

    List<ApiSpecSource> findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(Long projectId);
}
