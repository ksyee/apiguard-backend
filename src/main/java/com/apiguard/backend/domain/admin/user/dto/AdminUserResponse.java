package com.apiguard.backend.domain.admin.user.dto;

import com.apiguard.backend.domain.user.entity.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
    Long id,
    String email,
    String nickname,
    String role,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getRole().name(),
            user.getCreatedAt(),
            user.getDeletedAt()
        );
    }
}
