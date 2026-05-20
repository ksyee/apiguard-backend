package com.apiguard.backend.domain.subscription.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean cancelAtPeriodEnd = false;

    public boolean isActive() {
        return expiredAt == null || expiredAt.isAfter(LocalDateTime.now());
    }

    public void upgradeTo(PlanType planType, String paymentKey, LocalDateTime expiredAt) {
        this.planType = planType;
        this.externalSubscriptionId = paymentKey;
        this.expiredAt = expiredAt;
        this.cancelAtPeriodEnd = false;
    }

    public void cancelToFree() {
        this.planType = PlanType.FREE;
        this.externalSubscriptionId = null;
        this.expiredAt = null;
        this.cancelAtPeriodEnd = false;
    }

    public void requestCancelAtPeriodEnd() {
        this.cancelAtPeriodEnd = true;
        if (this.expiredAt == null) {
            this.expiredAt = LocalDateTime.now();
        }
    }

    public void downgradeExpiredProToFree() {
        if (this.planType == PlanType.PRO && this.expiredAt != null && !this.expiredAt.isAfter(LocalDateTime.now())) {
            cancelToFree();
        }
    }
}
