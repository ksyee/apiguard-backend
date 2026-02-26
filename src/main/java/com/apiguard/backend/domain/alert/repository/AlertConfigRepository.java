package com.apiguard.backend.domain.alert.repository;

import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertConfigRepository extends JpaRepository<AlertConfig, Long> {

    List<AlertConfig> findByEndpointIdAndDeletedFalse(Long endpointId);

    List<AlertConfig> findByEndpointIdAndIsActiveTrueAndDeletedFalse(Long endpointId);

    List<AlertConfig> findByEndpointAndIsActiveTrueAndDeletedFalse(Endpoint endpoint);

    Optional<AlertConfig> findByIdAndDeletedFalse(Long id);
}
