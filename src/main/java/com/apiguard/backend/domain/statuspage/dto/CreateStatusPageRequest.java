package com.apiguard.backend.domain.statuspage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStatusPageRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
    String title,

    String description,

    @NotBlank(message = "슬러그는 필수입니다.")
    @Size(min = 3, max = 100, message = "슬러그는 3~100자 이내여야 합니다.")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "슬러그는 영소문자, 숫자, 하이픈만 사용 가능합니다.")
    String slug
) {}
