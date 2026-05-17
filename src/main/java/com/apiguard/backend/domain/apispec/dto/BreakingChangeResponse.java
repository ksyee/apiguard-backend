package com.apiguard.backend.domain.apispec.dto;

import com.apiguard.backend.domain.apispec.entity.BreakingChange;
import com.apiguard.backend.domain.apispec.entity.BreakingChangeRule;

public record BreakingChangeResponse(
    Long id,
    BreakingChangeRule rule,
    String location,
    String description
) {
    public static BreakingChangeResponse from(BreakingChange breakingChange) {
        return new BreakingChangeResponse(
            breakingChange.getId(),
            breakingChange.getRule(),
            breakingChange.getLocation(),
            breakingChange.getDescription()
        );
    }
}
