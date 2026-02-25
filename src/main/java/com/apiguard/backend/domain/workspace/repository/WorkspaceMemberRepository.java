package com.apiguard.backend.domain.workspace.repository;

import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.workspace w WHERE wm.user.id = :userId AND w.deleted = false")
    List<WorkspaceMember> findByUserIdWithWorkspace(@Param("userId") Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    Optional<WorkspaceMember> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
