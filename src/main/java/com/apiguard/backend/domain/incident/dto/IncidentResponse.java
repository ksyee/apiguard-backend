package com.apiguard.backend.domain.incident.dto;

import com.apiguard.backend.domain.incident.entity.Incident;
import com.apiguard.backend.domain.incident.entity.IncidentSeverity;
import com.apiguard.backend.domain.incident.entity.IncidentStatus;
import com.apiguard.backend.domain.incident.entity.IncidentType;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.project.entity.Project;
import java.time.LocalDateTime;

public record IncidentResponse(
    Long id,
    Long endpointId,
    Long projectId,
    String endpointUrl,
    IncidentType type,
    IncidentStatus status,
    IncidentSeverity severity,
    String title,
    String description,
    int detectedCount,
    LocalDateTime startedAt,
    LocalDateTime lastDetectedAt,
    LocalDateTime resolvedAt
) {
    public static IncidentResponse from(Incident incident) {
        Endpoint endpoint = incident.getEndpoint();
        Project project = incident.getProject() != null
            ? incident.getProject()
            : endpoint.getProject();

        return new IncidentResponse(
            incident.getId(),
            endpoint != null ? endpoint.getId() : null,
            project.getId(),
            endpoint != null ? endpoint.getUrl() : null,
            incident.getType(),
            incident.getStatus(),
            incident.getSeverity(),
            incident.getTitle(),
            incident.getDescription(),
            incident.getDetectedCount(),
            incident.getStartedAt(),
            incident.getLastDetectedAt(),
            incident.getResolvedAt()
        );
    }
}
