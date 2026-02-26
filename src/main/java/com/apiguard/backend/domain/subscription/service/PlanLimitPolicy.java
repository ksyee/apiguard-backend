package com.apiguard.backend.domain.subscription.service;

import com.apiguard.backend.domain.subscription.entity.PlanType;

public interface PlanLimitPolicy {

    PlanType planType();

    int maxEndpointsPerProject();

    int minCheckIntervalSeconds();

    int maxAlertChannels();

    int maxMembers();

    int dataRetentionDays();
}
