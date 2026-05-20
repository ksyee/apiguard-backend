package com.apiguard.backend.domain.apispec.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateApiSpecSourceRequest(
    @Size(max = 100, message = "스펙 이름은 100자 이내여야 합니다.")
    String name,

    @URL(message = "올바른 URL 형식이 아닙니다.")
    String specUrl,

    Boolean active
) {
}
