package com.apiguard.backend.domain.endpoint.repository;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
    List<Endpoint> findByProjectIdAndDeletedFalse(Long projectId);
    Optional<Endpoint> findByIdAndDeletedFalse(Long id);
    List<Endpoint> findByIsActiveTrueAndDeletedFalse();
    @Query("""
        SELECT e
        FROM Endpoint e
        WHERE e.isActive = true
          AND e.deleted = false
          AND e.project.deleted = false
          AND (e.project.workspace IS NULL OR e.project.workspace.deleted = false)
        """)
    List<Endpoint> findSchedulableActiveEndpoints();
    long countByProjectIdAndDeletedFalse(Long projectId);
    List<Endpoint> findAllByDeletedFalse();
    List<Endpoint> findByProject_Workspace_IdAndDeletedFalse(Long workspaceId);
    List<Endpoint> findByProject_Workspace_IdAndDeletedFalseAndIsActiveTrue(Long workspaceId);
}
