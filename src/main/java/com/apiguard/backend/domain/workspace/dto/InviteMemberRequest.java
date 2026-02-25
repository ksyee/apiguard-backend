package com.apiguard.backend.domain.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InviteMemberRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotNull(message = "역할은 필수입니다.")
    @Pattern(regexp = "admin|member|viewer", message = "역할은 admin, member, viewer 중 하나여야 합니다.")
    String role
) {
}
