package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final EndpointRepository endpointRepository;
    private final CheckService checkService;
    private final Executor healthCheckExecutor;

    @Scheduled(fixedDelay = 60_000)
    public void scheduleHealthChecks() {
        List<Endpoint> activeEndpoints = endpointRepository.findByIsActiveTrueAndDeletedFalse();

        LocalDateTime now = LocalDateTime.now();
        List<Endpoint> dueEndpoints = activeEndpoints.stream()
            .filter(ep -> isDue(ep, now))
            .toList();

        if (dueEndpoints.isEmpty()) {
            return;
        }

        log.info("헬스체크 시작: {}개 엔드포인트", dueEndpoints.size());

        List<CompletableFuture<Void>> futures = dueEndpoints.stream()
            .map(endpoint -> CompletableFuture.runAsync(
                () -> executeCheck(endpoint), healthCheckExecutor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("헬스체크 완료: {}개 엔드포인트", dueEndpoints.size());
    }

    private boolean isDue(Endpoint endpoint, LocalDateTime now) {
        if (endpoint.getLastCheckedAt() == null) {
            return true;
        }
        return endpoint.getLastCheckedAt().plusSeconds(endpoint.getCheckInterval()).isBefore(now);
    }

    private void executeCheck(Endpoint endpoint) {
        try {
            checkService.performCheck(endpoint);
        } catch (Exception e) {
            log.error("헬스체크 실패: endpointId={}, url={}", endpoint.getId(), endpoint.getUrl(), e);
        }
    }
}
