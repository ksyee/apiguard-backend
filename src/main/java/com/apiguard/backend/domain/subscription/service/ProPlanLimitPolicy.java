package com.apiguard.backend.domain.subscription.service;

import com.apiguard.backend.domain.subscription.entity.PlanType;
import org.springframework.stereotype.Component;

@Component
public class ProPlanLimitPolicy implements PlanLimitPolicy {

    @Override
    public PlanType planType() {
        return PlanType.PRO;
    }

    @Override
    public int maxProjects() {
        return 50;
    }

    @Override
    public int maxEndpointsPerProject() {
        return 50;
    }

    @Override
    public int minCheckIntervalSeconds() {
        return 60;
    }

    @Override
    public int maxAlertChannels() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxMembers() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int dataRetentionDays() {
        return 90;
    }
}
