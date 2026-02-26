package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.subscription.service.PlanLimitPolicy;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckResultRetentionScheduler {

    private final EndpointRepository endpointRepository;
    private final CheckResultRepository checkResultRepository;
    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredCheckResults() {
        log.info("체크 결과 보관 기간 만료 데이터 삭제 시작");

        List<Endpoint> endpoints = endpointRepository.findAllByDeletedFalse();
        int totalDeleted = 0;

        for (Endpoint endpoint : endpoints) {
            if (endpoint.getProject() == null || endpoint.getProject().getWorkspace() == null) {
                continue;
            }

            Long workspaceId = endpoint.getProject().getWorkspace().getId();

            try {
                PlanLimitPolicy policy = subscriptionService.getPolicyForWorkspace(workspaceId);
                int retentionDays = policy.dataRetentionDays();
                LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

                int deleted = checkResultRepository.deleteByEndpointIdAndCheckedAtBefore(
                    endpoint.getId(), cutoff
                );

                if (deleted > 0) {
                    log.debug("엔드포인트 {} 만료 데이터 {}건 삭제 (보관 기간: {}일)",
                        endpoint.getId(), deleted, retentionDays);
                    totalDeleted += deleted;
                }
            } catch (Exception e) {
                log.error("엔드포인트 {} 만료 데이터 삭제 중 오류 발생", endpoint.getId(), e);
            }
        }

        log.info("체크 결과 보관 기간 만료 데이터 삭제 완료: 총 {}건 삭제", totalDeleted);
    }
}
