package com.apiguard.backend.domain.apispec.dto;

import com.apiguard.backend.domain.apispec.entity.ApiSpecDiff;
import java.time.LocalDateTime;

public record ApiSpecDiffResponse(
    Long id,
    Long specSourceId,
    Long baseSnapshotId,
    Long headSnapshotId,
    boolean breaking,
    int breakingChangeCount,
    String summary,
    LocalDateTime checkedAt
) {
    public static ApiSpecDiffResponse from(ApiSpecDiff diff) {
        return new ApiSpecDiffResponse(
            diff.getId(),
            diff.getSpecSource().getId(),
            diff.getBaseSnapshot() != null ? diff.getBaseSnapshot().getId() : null,
            diff.getHeadSnapshot().getId(),
            diff.isBreaking(),
            diff.getBreakingChangeCount(),
            diff.getSummary(),
            diff.getCheckedAt()
        );
    }
}
