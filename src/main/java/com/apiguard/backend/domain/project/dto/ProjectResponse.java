package com.apiguard.backend.domain.project.dto;

import com.apiguard.backend.domain.project.entity.Project;

import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getCreatedAt()
        );
    }
}
