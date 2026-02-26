package com.apiguard.backend.domain.project.repository;

import com.apiguard.backend.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserIdAndDeletedFalse(Long userId);
    Optional<Project> findByIdAndDeletedFalse(Long id);
    List<Project> findByWorkspaceIdAndDeletedFalse(Long workspaceId);
    long countByWorkspaceIdAndDeletedFalse(Long workspaceId);
}
