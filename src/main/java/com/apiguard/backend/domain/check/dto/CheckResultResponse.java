package com.apiguard.backend.domain.check.dto;

import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;

import java.time.LocalDateTime;

public record CheckResultResponse(
    Long id,
    Long endpointId,
    CheckStatus status,
    Integer statusCode,
    Long responseTimeMs,
    String errorMessage,
    LocalDateTime checkedAt
) {
    public static CheckResultResponse from(CheckResult checkResult) {
        return new CheckResultResponse(
            checkResult.getId(),
            checkResult.getEndpoint().getId(),
            checkResult.getStatus(),
            checkResult.getStatusCode(),
            checkResult.getResponseTimeMs(),
            checkResult.getErrorMessage(),
            checkResult.getCheckedAt()
        );
    }
}
