package com.apiguard.backend.domain.workspace.repository;

import com.apiguard.backend.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByIdAndDeletedFalse(Long id);
    boolean existsBySlug(String slug);
}
