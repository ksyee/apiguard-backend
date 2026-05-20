package com.apiguard.backend.domain.alert.dto;

import com.apiguard.backend.domain.alert.entity.AlertDelivery;
import com.apiguard.backend.domain.alert.entity.AlertDeliveryStatus;
import com.apiguard.backend.domain.alert.entity.AlertType;
import java.time.LocalDateTime;

public record AlertDeliveryResponse(
    Long id,
    Long alertId,
    Long endpointId,
    AlertType alertType,
    String target,
    AlertDeliveryStatus status,
    boolean testDelivery,
    String errorMessage,
    LocalDateTime triggeredAt
) {
    public static AlertDeliveryResponse from(AlertDelivery delivery) {
        return new AlertDeliveryResponse(
            delivery.getId(),
            delivery.getAlertConfig().getId(),
            delivery.getEndpoint().getId(),
            delivery.getAlertType(),
            delivery.getTarget(),
            delivery.getStatus(),
            delivery.isTestDelivery(),
            delivery.getErrorMessage(),
            delivery.getTriggeredAt()
        );
    }
}
