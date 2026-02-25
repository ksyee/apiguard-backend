package com.apiguard.backend.domain.workspace.dto;

import com.apiguard.backend.domain.workspace.entity.Workspace;

import java.time.LocalDateTime;

public record WorkspaceResponse(
    Long id,
    String name,
    String slug,
    LocalDateTime createdAt
) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getSlug(),
            workspace.getCreatedAt()
        );
    }
}
