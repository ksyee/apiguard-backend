package com.apiguard.backend.domain.admin.notice.dto;

import com.apiguard.backend.domain.admin.notice.entity.Notice;
import java.time.LocalDateTime;

public record NoticeResponse(
    Long id,
    String title,
    String content,
    boolean pinned,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
            notice.getId(),
            notice.getTitle(),
            notice.getContent(),
            notice.isPinned(),
            notice.getCreatedAt(),
            notice.getUpdatedAt()
        );
    }
}
