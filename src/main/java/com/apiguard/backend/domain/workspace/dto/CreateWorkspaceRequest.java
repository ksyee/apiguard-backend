package com.apiguard.backend.domain.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
    @NotBlank(message = "워크스페이스 이름은 필수입니다.")
    String name
) {}
