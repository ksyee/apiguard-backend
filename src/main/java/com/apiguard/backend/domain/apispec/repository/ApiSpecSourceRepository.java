package com.apiguard.backend.domain.apispec.repository;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApiSpecSourceRepository extends JpaRepository<ApiSpecSource, Long> {

    Optional<ApiSpecSource> findByIdAndDeletedFalse(Long id);

    List<ApiSpecSource> findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(Long projectId);

    @Query("""
        SELECT s
        FROM ApiSpecSource s
        WHERE s.active = true
          AND s.deleted = false
          AND s.project.deleted = false
          AND (s.project.workspace IS NULL OR s.project.workspace.deleted = false)
        ORDER BY s.createdAt ASC
        """)
    List<ApiSpecSource> findSchedulableActiveSources();
}
