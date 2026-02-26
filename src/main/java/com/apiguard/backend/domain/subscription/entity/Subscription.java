package com.apiguard.backend.domain.subscription.entity;

import com.apiguard.backend.domain.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanType planType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime expiredAt;

    @Column(length = 255)
    private String externalSubscriptionId;

    public boolean isActive() {
        return expiredAt == null || expiredAt.isAfter(LocalDateTime.now());
    }

    public void upgradeTo(PlanType planType, String paymentKey, LocalDateTime expiredAt) {
        this.planType = planType;
        this.externalSubscriptionId = paymentKey;
        this.expiredAt = expiredAt;
    }
}
