package com.apiguard.backend.domain.subscription.service;

import com.apiguard.backend.domain.alert.repository.AlertConfigRepository;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.subscription.entity.PlanType;
import com.apiguard.backend.domain.subscription.entity.Subscription;
import com.apiguard.backend.domain.subscription.repository.SubscriptionRepository;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.repository.WorkspaceRepository;
import com.apiguard.backend.global.exception.PlanLimitExceededException;
import com.apiguard.backend.global.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final EndpointRepository endpointRepository;
    private final AlertConfigRepository alertConfigRepository;
    private final List<PlanLimitPolicy> planLimitPolicies;

    public PlanLimitPolicy getPolicyForWorkspace(Long workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
            .filter(Subscription::isActive)
            .map(sub -> getPolicyByPlanType(sub.getPlanType()))
            .orElseGet(this::getFreePlanPolicy);
    }

    public void validateEndpointCount(Long workspaceId, Long projectId) {
        PlanLimitPolicy policy = getPolicyForWorkspace(workspaceId);
        long count = endpointRepository.countByProjectIdAndDeletedFalse(projectId);
        if (count >= policy.maxEndpointsPerProject()) {
            throw new PlanLimitExceededException(
                "엔드포인트 수 제한에 도달했습니다. 현재 플랜의 최대 엔드포인트 수: " + policy.maxEndpointsPerProject()
            );
        }
    }

    public void validateCheckInterval(Long workspaceId, int seconds) {
        PlanLimitPolicy policy = getPolicyForWorkspace(workspaceId);
        if (seconds < policy.minCheckIntervalSeconds()) {
            throw new PlanLimitExceededException(
                "체크 주기가 너무 짧습니다. 현재 플랜의 최소 체크 주기: " + policy.minCheckIntervalSeconds() + "초"
            );
        }
    }

    public void validateAlertChannelCount(Long workspaceId, Long endpointId) {
        PlanLimitPolicy policy = getPolicyForWorkspace(workspaceId);
        if (policy.maxAlertChannels() == Integer.MAX_VALUE) {
            return;
        }
        long count = alertConfigRepository.countByEndpointIdAndDeletedFalse(endpointId);
        if (count >= policy.maxAlertChannels()) {
            throw new PlanLimitExceededException(
                "알림 채널 수 제한에 도달했습니다. 현재 플랜의 최대 알림 채널 수: " + policy.maxAlertChannels()
            );
        }
    }

    public void validateMemberCount(Long workspaceId) {
        PlanLimitPolicy policy = getPolicyForWorkspace(workspaceId);
        if (policy.maxMembers() == Integer.MAX_VALUE) {
            return;
        }
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));
        throw new PlanLimitExceededException(
            "멤버 수 제한에 도달했습니다. FREE 플랜은 혼자만 사용할 수 있습니다."
        );
    }

    @Transactional
    public Subscription createDefaultSubscription(Workspace workspace) {
        Subscription subscription = Subscription.builder()
            .workspace(workspace)
            .planType(PlanType.FREE)
            .startedAt(LocalDateTime.now())
            .expiredAt(null)
            .build();
        return subscriptionRepository.save(subscription);
    }

    private PlanLimitPolicy getPolicyByPlanType(PlanType planType) {
        return planLimitPolicies.stream()
            .filter(p -> p.planType() == planType)
            .findFirst()
            .orElseGet(this::getFreePlanPolicy);
    }

    private PlanLimitPolicy getFreePlanPolicy() {
        return planLimitPolicies.stream()
            .filter(p -> p.planType() == PlanType.FREE)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("FREE 플랜 정책을 찾을 수 없습니다."));
    }
}
