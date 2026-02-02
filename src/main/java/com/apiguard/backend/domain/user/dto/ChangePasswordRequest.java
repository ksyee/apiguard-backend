package com.apiguard.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    String currentPassword,
    
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8~16자, 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    String newPassword,
    
    @NotBlank(message = "새 비밀번호를 확인해주세요.")
    String newPasswordConfirm
    ) {
    
}
