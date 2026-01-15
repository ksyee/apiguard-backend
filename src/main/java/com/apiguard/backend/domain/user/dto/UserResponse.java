package com.apiguard.backend.domain.user.dto;

import com.apiguard.backend.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String email,
    String nickname,
    LocalDateTime createdAt
) {
    
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getCreatedAt()
        );
    }
}
