package com.apiguard.backend.domain.admin.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull(message = "역할은 필수입니다.")
    String role
) {

}
