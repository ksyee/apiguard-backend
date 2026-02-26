package com.apiguard.backend.domain.payment.dto;

import com.apiguard.backend.domain.subscription.entity.PlanType;
import com.apiguard.backend.domain.subscription.entity.Subscription;
import com.apiguard.backend.domain.subscription.service.PlanLimitPolicy;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
    PlanType planType,
    boolean active,
    LocalDateTime expiredAt,
    int maxEndpointsPerProject,
    int minCheckIntervalSeconds,
    int maxAlertChannels,
    int maxMembers,
    int dataRetentionDays
) {
    public static SubscriptionStatusResponse from(Subscription subscription, PlanLimitPolicy policy) {
        return new SubscriptionStatusResponse(
            subscription.getPlanType(),
            subscription.isActive(),
            subscription.getExpiredAt(),
            policy.maxEndpointsPerProject(),
            policy.minCheckIntervalSeconds(),
            policy.maxAlertChannels() == Integer.MAX_VALUE ? -1 : policy.maxAlertChannels(),
            policy.maxMembers() == Integer.MAX_VALUE ? -1 : policy.maxMembers(),
            policy.dataRetentionDays()
        );
    }
}
