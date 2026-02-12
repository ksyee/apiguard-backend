package com.apiguard.backend.domain.endpoint.dto;

import com.apiguard.backend.domain.endpoint.entity.HttpMethod;

public record UpdateEndpointRequest(
    String url,
    HttpMethod httpMethod,
    String headers,
    String body,
    Integer expectedStatusCode,
    Integer checkInterval
) {}
