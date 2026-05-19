package com.apiguard.backend.domain.workspace.entity;

public enum WorkspaceRole {
    OWNER(4),
    ADMIN(3),
    MEMBER(2),
    VIEWER(1);

    private final int rank;

    WorkspaceRole(int rank) {
        this.rank = rank;
    }

    public boolean isAtLeast(WorkspaceRole requiredRole) {
        return rank >= requiredRole.rank;
    }
}
