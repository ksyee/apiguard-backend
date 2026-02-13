package com.apiguard.backend.domain.alert.dto;

import com.apiguard.backend.domain.alert.entity.AlertType;
import jakarta.validation.constraints.Min;

public record UpdateAlertRequest(
    AlertType alertType,

    String target,

    @Min(value = 1, message = "임계값은 1 이상이어야 합니다.")
    Integer threshold
) {}
