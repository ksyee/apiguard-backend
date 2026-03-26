package com.apiguard.backend.domain.statuspage.dto;

import com.apiguard.backend.domain.statuspage.entity.StatusPage;

import java.time.LocalDateTime;

public record StatusPageResponse(
    Long id,
    String slug,
    String title,
    String description,
    boolean isPublic,
    LocalDateTime createdAt
) {
    public static StatusPageResponse from(StatusPage statusPage) {
        return new StatusPageResponse(
            statusPage.getId(),
            statusPage.getSlug(),
            statusPage.getTitle(),
            statusPage.getDescription(),
            statusPage.isPublic(),
            statusPage.getCreatedAt()
        );
    }
}
