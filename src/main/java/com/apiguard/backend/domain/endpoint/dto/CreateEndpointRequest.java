package com.apiguard.backend.domain.endpoint.dto;

import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEndpointRequest(
    @NotBlank(message = "URL은 필수입니다.")
    String url,

    @NotNull(message = "HTTP 메서드는 필수입니다.")
    HttpMethod httpMethod,

    String headers,

    String body,

    Integer expectedStatusCode,

    Integer checkInterval
) {}
