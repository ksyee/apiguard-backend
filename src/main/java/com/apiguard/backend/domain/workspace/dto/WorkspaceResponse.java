package com.apiguard.backend.domain.workspace.dto;

import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;

import java.time.LocalDateTime;

public record WorkspaceResponse(
    Long id,
    String name,
    String slug,
    WorkspaceRole role,
    LocalDateTime createdAt
) {
    public static WorkspaceResponse from(Workspace workspace, WorkspaceRole role) {
        return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getSlug(),
            role,
            workspace.getCreatedAt()
        );
    }
}
