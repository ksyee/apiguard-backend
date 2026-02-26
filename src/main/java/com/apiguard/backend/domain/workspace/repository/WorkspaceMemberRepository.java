package com.apiguard.backend.domain.workspace.repository;

import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.workspace.deleted = false")
    List<WorkspaceMember> findByWorkspaceIdAndDeletedFalse(@Param("workspaceId") Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    List<WorkspaceMember> findAllByUserId(Long userId);
}
