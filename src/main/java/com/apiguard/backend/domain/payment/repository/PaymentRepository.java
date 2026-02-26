package com.apiguard.backend.domain.payment.repository;

import com.apiguard.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
