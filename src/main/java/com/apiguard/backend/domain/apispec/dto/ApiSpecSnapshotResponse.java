package com.apiguard.backend.domain.apispec.dto;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSnapshot;
import java.time.LocalDateTime;

public record ApiSpecSnapshotResponse(
    Long id,
    Long specSourceId,
    String contentHash,
    LocalDateTime capturedAt
) {
    public static ApiSpecSnapshotResponse from(ApiSpecSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new ApiSpecSnapshotResponse(
            snapshot.getId(),
            snapshot.getSpecSource().getId(),
            snapshot.getContentHash(),
            snapshot.getCapturedAt()
        );
    }
}
