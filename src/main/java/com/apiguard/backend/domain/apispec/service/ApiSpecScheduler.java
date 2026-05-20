package com.apiguard.backend.domain.apispec.service;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import com.apiguard.backend.domain.apispec.repository.ApiSpecSourceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSpecScheduler {

    private final ApiSpecSourceRepository specSourceRepository;
    private final ApiSpecService apiSpecService;

    @Value("${apiguard.apispec.auto-check-enabled:true}")
    private boolean autoCheckEnabled;

    @Scheduled(fixedDelayString = "${apiguard.apispec.check-fixed-delay-ms:300000}")
    public void scheduleSpecChecks() {
        if (!autoCheckEnabled) {
            return;
        }

        List<Long> sourceIds = specSourceRepository.findSchedulableActiveSources()
            .stream()
            .map(ApiSpecSource::getId)
            .toList();

        if (sourceIds.isEmpty()) {
            return;
        }

        log.info("OpenAPI 자동 검사 시작: {}개 소스", sourceIds.size());
        for (Long sourceId : sourceIds) {
            try {
                apiSpecService.checkActiveSource(sourceId);
            } catch (Exception e) {
                log.error("OpenAPI 자동 검사 실패: sourceId={}", sourceId, e);
            }
        }
        log.info("OpenAPI 자동 검사 완료: {}개 소스", sourceIds.size());
    }
}
