package com.apiguard.backend.domain.statuspage.repository;

import com.apiguard.backend.domain.statuspage.entity.StatusPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusPageRepository extends JpaRepository<StatusPage, Long> {

    Optional<StatusPage> findBySlugAndDeletedFalse(String slug);

    Optional<StatusPage> findByWorkspaceIdAndDeletedFalse(Long workspaceId);

    boolean existsBySlug(String slug);
}
