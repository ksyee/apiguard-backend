package com.apiguard.backend.domain.alert.dto;

import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertType;

import java.time.LocalDateTime;

public record AlertResponse(
    Long id,
    Long endpointId,
    AlertType alertType,
    String target,
    int threshold,
    boolean isActive,
    LocalDateTime createdAt
) {
    public static AlertResponse from(AlertConfig alertConfig) {
        return new AlertResponse(
            alertConfig.getId(),
            alertConfig.getEndpoint().getId(),
            alertConfig.getAlertType(),
            alertConfig.getTarget(),
            alertConfig.getThreshold(),
            alertConfig.isActive(),
            alertConfig.getCreatedAt()
        );
    }
}
