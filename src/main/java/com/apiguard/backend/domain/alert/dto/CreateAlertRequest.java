package com.apiguard.backend.domain.alert.dto;

import com.apiguard.backend.domain.alert.entity.AlertType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAlertRequest(
    @NotNull(message = "알림 유형은 필수입니다.")
    AlertType alertType,

    @NotBlank(message = "알림 대상은 필수입니다.")
    String target,

    @Min(value = 1, message = "임계값은 1 이상이어야 합니다.")
    Integer threshold
) {}
