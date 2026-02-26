package com.apiguard.backend.domain.subscription.repository;

import com.apiguard.backend.domain.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByWorkspaceId(Long workspaceId);
}
