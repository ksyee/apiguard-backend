package com.apiguard.backend.domain.workspace.dto;

import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;

import java.time.LocalDateTime;

public record WorkspaceMemberResponse(
    Long userId,
    String nickname,
    String email,
    WorkspaceRole role,
    LocalDateTime joinedAt
) {
    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
            member.getUser().getId(),
            member.getUser().getNickname(),
            member.getUser().getEmail(),
            member.getRole(),
            member.getJoinedAt()
        );
    }
}
