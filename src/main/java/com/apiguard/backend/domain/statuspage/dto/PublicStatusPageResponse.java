package com.apiguard.backend.domain.statuspage.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicStatusPageResponse(
    String title,
    String description,
    String overallStatus,
    List<EndpointStatus> endpoints
) {
    public record EndpointStatus(
        String url,
        String httpMethod,
        String status,
        double uptimePercent,
        double avgResponseTimeMs,
        LocalDateTime lastCheckedAt
    ) {}
}
