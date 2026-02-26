package com.apiguard.backend.domain.subscription.service;

import com.apiguard.backend.domain.subscription.entity.PlanType;
import org.springframework.stereotype.Component;

@Component
public class FreePlanLimitPolicy implements PlanLimitPolicy {

    @Override
    public PlanType planType() {
        return PlanType.FREE;
    }

    @Override
    public int maxEndpointsPerProject() {
        return 5;
    }

    @Override
    public int minCheckIntervalSeconds() {
        return 300;
    }

    @Override
    public int maxAlertChannels() {
        return 1;
    }

    @Override
    public int maxMembers() {
        return 1;
    }

    @Override
    public int dataRetentionDays() {
        return 7;
    }
}
