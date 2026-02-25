package com.apiguard.backend.domain.workspace.dto;

import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;

import java.time.LocalDateTime;

public record WorkspaceMemberResponse(
    Long id,
    Long userId,
    String email,
    String nickname,
    String role,
    LocalDateTime joinedAt
) {
    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
            member.getId(),
            member.getUser().getId(),
            member.getUser().getEmail(),
            member.getUser().getNickname(),
            member.getRole().name().toLowerCase(),
            member.getJoinedAt()
        );
    }
}
