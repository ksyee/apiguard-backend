package com.apiguard.backend.domain.check.dto;

import java.time.LocalDateTime;

public record EndpointStatsResponse(
    long totalChecks,
    long successCount,
    double successRate,
    double avgResponseTimeMs,
    LocalDateTime since
) {
}
