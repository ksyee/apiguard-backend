package com.apiguard.backend.domain.statuspage.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateStatusPageRequest(
    @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
    String title,

    String description,

    Boolean isPublic,

    Boolean allEndpoints,

    List<Long> endpointIds
) {}
