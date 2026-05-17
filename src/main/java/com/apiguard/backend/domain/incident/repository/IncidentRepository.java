package com.apiguard.backend.domain.incident.repository;

import com.apiguard.backend.domain.incident.entity.Incident;
import com.apiguard.backend.domain.incident.entity.IncidentStatus;
import com.apiguard.backend.domain.incident.entity.IncidentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
        Long endpointId,
        IncidentType type,
        IncidentStatus status
    );

    Optional<Incident> findFirstByProjectIdAndTypeAndStatusAndTitleOrderByStartedAtDesc(
        Long projectId,
        IncidentType type,
        IncidentStatus status,
        String title
    );

    List<Incident> findByEndpointIdOrderByStartedAtDesc(Long endpointId);

    @Query("""
        SELECT i FROM Incident i
        LEFT JOIN i.endpoint e
        LEFT JOIN i.project p
        WHERE p.id = :projectId OR e.project.id = :projectId
        ORDER BY i.startedAt DESC
        """)
    List<Incident> findByProjectIdOrderByStartedAtDesc(@Param("projectId") Long projectId);

    @Query("""
        SELECT i FROM Incident i
        LEFT JOIN i.endpoint e
        LEFT JOIN i.project p
        WHERE (p.id = :projectId OR e.project.id = :projectId)
            AND i.status = :status
        ORDER BY i.startedAt DESC
        """)
    List<Incident> findByProjectIdAndStatusOrderByStartedAtDesc(
        @Param("projectId") Long projectId,
        @Param("status") IncidentStatus status
    );
}
