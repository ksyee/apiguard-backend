package com.apiguard.backend.domain.apispec.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateApiSpecSourceRequest(
    @NotBlank(message = "스펙 이름은 필수입니다.")
    String name,

    @NotBlank(message = "OpenAPI JSON URL은 필수입니다.")
    @URL(message = "올바른 URL 형식이 아닙니다.")
    String specUrl
) {
}
