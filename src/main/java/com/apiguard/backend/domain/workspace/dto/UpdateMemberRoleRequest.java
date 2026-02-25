package com.apiguard.backend.domain.workspace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRoleRequest(
    @NotNull(message = "역할은 필수입니다.")
    @Pattern(regexp = "admin|member|viewer", message = "역할은 admin, member, viewer 중 하나여야 합니다.")
    String role
) {
}
