package com.apiguard.backend.domain.apispec.dto;

import com.apiguard.backend.domain.apispec.entity.ApiSpecDiff;
import java.time.LocalDateTime;
import java.util.List;

public record ApiSpecDiffDetailResponse(
    Long id,
    Long specSourceId,
    Long baseSnapshotId,
    Long headSnapshotId,
    boolean breaking,
    int breakingChangeCount,
    String summary,
    LocalDateTime checkedAt,
    List<BreakingChangeResponse> changes
) {
    public static ApiSpecDiffDetailResponse from(
        ApiSpecDiff diff,
        List<BreakingChangeResponse> changes
    ) {
        return new ApiSpecDiffDetailResponse(
            diff.getId(),
            diff.getSpecSource().getId(),
            diff.getBaseSnapshot() != null ? diff.getBaseSnapshot().getId() : null,
            diff.getHeadSnapshot().getId(),
            diff.isBreaking(),
            diff.getBreakingChangeCount(),
            diff.getSummary(),
            diff.getCheckedAt(),
            changes
        );
    }
}
