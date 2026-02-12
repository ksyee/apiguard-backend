package com.apiguard.backend.domain.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @Size(max = 100, message = "프로젝트 이름은 100자 이하로 입력해주세요.")
    String name,

    @Size(max = 500, message = "프로젝트 설명은 500자 이하로 입력해주세요.")
    String description
) {}
