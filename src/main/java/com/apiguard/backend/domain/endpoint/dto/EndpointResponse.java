package com.apiguard.backend.domain.endpoint.dto;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;

import java.time.LocalDateTime;

public record EndpointResponse(
    Long id,
    Long projectId,
    String url,
    HttpMethod httpMethod,
    String headers,
    String body,
    int expectedStatusCode,
    int checkInterval,
    boolean isActive,
    LocalDateTime lastCheckedAt,
    LocalDateTime createdAt
) {
    public static EndpointResponse from(Endpoint endpoint) {
        return new EndpointResponse(
            endpoint.getId(),
            endpoint.getProject().getId(),
            endpoint.getUrl(),
            endpoint.getHttpMethod(),
            endpoint.getHeaders(),
            endpoint.getBody(),
            endpoint.getExpectedStatusCode(),
            endpoint.getCheckInterval(),
            endpoint.isActive(),
            endpoint.getLastCheckedAt(),
            endpoint.getCreatedAt()
        );
    }
}
