package com.apiguard.backend.domain.workspace.dto;

import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
    @NotNull(message = "역할은 필수입니다.")
    WorkspaceRole role
) {}
