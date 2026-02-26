package com.apiguard.backend.domain.endpoint.dto;

import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.URL;
import java.util.Map;

public record UpdateEndpointRequest(
    @URL(message = "올바른 URL 형식이 아닙니다.")
    String url,
    HttpMethod httpMethod,
    Map<String, String> headers,
    String body,

    @Min(value = 100, message = "예상 상태 코드는 100 이상이어야 합니다.")
    @Max(value = 599, message = "예상 상태 코드는 599 이하여야 합니다.")
    Integer expectedStatusCode,

    @Min(value = 1, message = "점검 주기는 1초 이상이어야 합니다.")
    Integer checkInterval
) {}
