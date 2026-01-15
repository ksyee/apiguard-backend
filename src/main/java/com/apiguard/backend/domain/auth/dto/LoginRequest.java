package com.apiguard.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,
    @NotBlank(message = "비밀번호는 필수입니다.") String password // 로그인 시 비밀번호는 입력된 값이 맞는지만 확인하면 됨(틀렸으면 틀렸다고만 응답하면 됨)
) {
}