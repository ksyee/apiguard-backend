package com.apiguard.backend.domain.check.dto;

import java.time.LocalDateTime;

public record HourlyStatResponse(
    LocalDateTime hour,
    long checkCount,
    long successCount,
    double avgResponseTimeMs
) {
}
