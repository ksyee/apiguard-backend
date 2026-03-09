package com.apiguard.backend.domain.payment.entity;

import com.apiguard.backend.domain.subscription.entity.PlanType;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "payments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(length = 200)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanType planType;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    public void markSuccess(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}
