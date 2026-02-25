package com.apiguard.backend.domain.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
    @NotBlank(message = "워크스페이스 이름은 필수입니다.")
    @Size(max = 100, message = "워크스페이스 이름은 최대 100자입니다.")
    String name
) {
}
