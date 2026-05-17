package com.apiguard.backend.domain.apispec.dto;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import java.time.LocalDateTime;

public record ApiSpecSourceResponse(
    Long id,
    Long projectId,
    String name,
    String specUrl,
    boolean active,
    LocalDateTime lastCheckedAt,
    LocalDateTime createdAt
) {
    public static ApiSpecSourceResponse from(ApiSpecSource source) {
        return new ApiSpecSourceResponse(
            source.getId(),
            source.getProject().getId(),
            source.getName(),
            source.getSpecUrl(),
            source.isActive(),
            source.getLastCheckedAt(),
            source.getCreatedAt()
        );
    }
}
