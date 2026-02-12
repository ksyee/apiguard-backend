package com.apiguard.backend.domain.endpoint.repository;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
    List<Endpoint> findByProjectIdAndDeletedFalse(Long projectId);
    Optional<Endpoint> findByIdAndDeletedFalse(Long id);
}
