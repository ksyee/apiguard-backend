package com.apiguard.backend.domain.check.dto;

public record ProjectStatsResponse(
    long totalEndpoints,
    long upCount,
    long downCount,
    double avgResponseTimeMs
) {
}
