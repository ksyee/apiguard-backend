package com.apiguard.backend.domain.alert.service;

import com.apiguard.backend.domain.alert.dto.AlertResponse;
import com.apiguard.backend.domain.alert.dto.CreateAlertRequest;
import com.apiguard.backend.domain.alert.dto.UpdateAlertRequest;
import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.repository.AlertConfigRepository;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.global.exception.AlertNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertService {

    private final AlertConfigRepository alertConfigRepository;
    private final CheckResultRepository checkResultRepository;
    private final EndpointService endpointService;
    private final List<NotificationService> notificationServices;
    private final StringRedisTemplate stringRedisTemplate;
    private final SubscriptionService subscriptionService;

    private static final String ALERT_SENT_KEY_PREFIX = "ALERT_SENT:";
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(30);

    @Transactional
    public AlertResponse createAlert(Long endpointId, CreateAlertRequest request) {
        Endpoint endpoint = endpointService.getEndpointWithWriteCheck(endpointId);

        if (endpoint.getProject().getWorkspace() != null) {
            Long workspaceId = endpoint.getProject().getWorkspace().getId();
            subscriptionService.validateAlertChannelCount(workspaceId, endpointId);
        }

        AlertConfig alertConfig = AlertConfig.builder()
            .endpoint(endpoint)
            .alertType(request.alertType())
            .target(request.target())
            .threshold(request.threshold() != null ? request.threshold() : 3)
            .build();

        AlertConfig saved = alertConfigRepository.save(alertConfig);
        return AlertResponse.from(saved);
    }

    public List<AlertResponse> getAlerts(Long endpointId) {
        endpointService.getEndpointWithAccessCheck(endpointId);
        return alertConfigRepository.findByEndpointIdAndDeletedFalse(endpointId).stream()
            .map(AlertResponse::from)
            .toList();
    }

    @Transactional
    public AlertResponse updateAlert(Long alertId, UpdateAlertRequest request) {
        AlertConfig alertConfig = getAlertWithWriteCheck(alertId);
        alertConfig.update(request.alertType(), request.target(), request.threshold());
        return AlertResponse.from(alertConfig);
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        AlertConfig alertConfig = getAlertWithWriteCheck(alertId);
        alertConfig.softDelete();
    }

    @Transactional
    public AlertResponse toggleAlert(Long alertId) {
        AlertConfig alertConfig = getAlertWithWriteCheck(alertId);
        alertConfig.toggleActive();
        return AlertResponse.from(alertConfig);
    }

    public void checkAndAlert(Long endpointId) {
        List<AlertConfig> activeAlerts = alertConfigRepository
            .findByEndpointIdAndIsActiveTrueAndDeletedFalse(endpointId);

        if (activeAlerts.isEmpty()) {
            return;
        }

        int maxThreshold = activeAlerts.stream()
            .mapToInt(AlertConfig::getThreshold)
            .max()
            .orElse(3);

        List<CheckResult> recentResults = checkResultRepository
            .findByEndpointIdOrderByCheckedAtDesc(endpointId, PageRequest.of(0, maxThreshold));

        if (recentResults.isEmpty()) {
            return;
        }

        int consecutiveFailures = 0;
        for (CheckResult result : recentResults) {
            if (result.getStatus() == CheckStatus.SUCCESS) {
                break;
            }
            consecutiveFailures++;
        }

        for (AlertConfig alertConfig : activeAlerts) {
            if (consecutiveFailures >= alertConfig.getThreshold()) {
                sendAlertIfNotDuplicate(alertConfig, alertConfig.getEndpoint(), recentResults);
            }
        }
    }

    private void sendAlertIfNotDuplicate(AlertConfig alertConfig, Endpoint endpoint,
                                          List<CheckResult> recentResults) {
        String redisKey = ALERT_SENT_KEY_PREFIX + alertConfig.getId();

        Boolean alreadySent = stringRedisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(alreadySent)) {
            log.debug("중복 알림 방지: alertConfigId={}", alertConfig.getId());
            return;
        }

        List<CheckResult> failures = recentResults.subList(
            0, Math.min(alertConfig.getThreshold(), recentResults.size()));

        for (NotificationService notificationService : notificationServices) {
            if (notificationService.supports(alertConfig.getAlertType())) {
                try {
                    notificationService.send(alertConfig, endpoint, failures);
                    stringRedisTemplate.opsForValue().set(redisKey, "1", ALERT_COOLDOWN);
                    log.info("알림 발송 완료: alertConfigId={}, type={}, target={}",
                        alertConfig.getId(), alertConfig.getAlertType(), alertConfig.getTarget());
                } catch (Exception e) {
                    log.error("알림 발송 실패: alertConfigId={}, type={}",
                        alertConfig.getId(), alertConfig.getAlertType(), e);
                }
                break;
            }
        }
    }

    private AlertConfig getAlertWithWriteCheck(Long alertId) {
        AlertConfig alertConfig = alertConfigRepository.findByIdAndDeletedFalse(alertId)
            .orElseThrow(() -> new AlertNotFoundException("알림 설정을 찾을 수 없습니다."));

        Endpoint endpoint = alertConfig.getEndpoint();
        if (endpoint.isDeleted()) {
            throw new AlertNotFoundException("알림 설정을 찾을 수 없습니다.");
        }

        endpointService.getEndpointWithWriteCheck(endpoint.getId());
        return alertConfig;
    }
}
