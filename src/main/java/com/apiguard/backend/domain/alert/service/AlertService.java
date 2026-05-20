package com.apiguard.backend.domain.alert.service;

import com.apiguard.backend.domain.alert.dto.AlertDeliveryResponse;
import com.apiguard.backend.domain.alert.dto.AlertResponse;
import com.apiguard.backend.domain.alert.dto.CreateAlertRequest;
import com.apiguard.backend.domain.alert.dto.UpdateAlertRequest;
import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertDelivery;
import com.apiguard.backend.domain.alert.entity.AlertDeliveryStatus;
import com.apiguard.backend.domain.alert.entity.AlertType;
import com.apiguard.backend.domain.alert.repository.AlertConfigRepository;
import com.apiguard.backend.domain.alert.repository.AlertDeliveryRepository;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.global.exception.AlertNotFoundException;
import com.apiguard.backend.global.security.OutboundUrlGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertService {

    private final AlertConfigRepository alertConfigRepository;
    private final AlertDeliveryRepository alertDeliveryRepository;
    private final CheckResultRepository checkResultRepository;
    private final EndpointService endpointService;
    private final List<NotificationService> notificationServices;
    private final StringRedisTemplate stringRedisTemplate;
    private final SubscriptionService subscriptionService;
    private final OutboundUrlGuard outboundUrlGuard;

    private static final String ALERT_SENT_KEY_PREFIX = "ALERT_SENT:";
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(30);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

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
            .target(validateTarget(request.alertType(), request.target()))
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
        AlertType alertType = request.alertType() != null ? request.alertType() : alertConfig.getAlertType();
        String target = request.target() != null
            ? validateTarget(alertType, request.target())
            : null;
        alertConfig.update(request.alertType(), target, request.threshold());
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

    public List<AlertDeliveryResponse> getDeliveries(Long alertId, int limit) {
        getAlertWithAccessCheck(alertId);
        return alertDeliveryRepository.findByAlertConfigIdOrderByTriggeredAtDesc(
                alertId,
                PageRequest.of(0, Math.max(1, Math.min(limit, 50)))
            )
            .stream()
            .map(AlertDeliveryResponse::from)
            .toList();
    }

    @Transactional
    public AlertDeliveryResponse sendTestAlert(Long alertId) {
        AlertConfig alertConfig = getAlertWithWriteCheck(alertId);
        Endpoint endpoint = alertConfig.getEndpoint();
        CheckResult syntheticFailure = CheckResult.builder()
            .endpoint(endpoint)
            .status(CheckStatus.ERROR)
            .responseTimeMs(0L)
            .errorMessage("Test alert from APIGuard")
            .checkedAt(LocalDateTime.now())
            .build();

        return AlertDeliveryResponse.from(sendAlert(alertConfig, endpoint, List.of(syntheticFailure), true));
    }

    @Transactional
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

        AlertDelivery delivery = sendAlert(alertConfig, endpoint, failures, false);
        if (delivery.getStatus() == AlertDeliveryStatus.SUCCESS) {
            stringRedisTemplate.opsForValue().set(redisKey, "1", ALERT_COOLDOWN);
            log.info("알림 발송 완료: alertConfigId={}, type={}, target={}",
                alertConfig.getId(), alertConfig.getAlertType(), alertConfig.getTarget());
        }
    }

    private AlertDelivery sendAlert(
        AlertConfig alertConfig,
        Endpoint endpoint,
        List<CheckResult> failures,
        boolean testDelivery
    ) {
        for (NotificationService notificationService : notificationServices) {
            if (!notificationService.supports(alertConfig.getAlertType())) {
                continue;
            }

            try {
                notificationService.send(alertConfig, endpoint, failures);
                return saveDelivery(alertConfig, endpoint, AlertDeliveryStatus.SUCCESS, testDelivery, null);
            } catch (Exception e) {
                log.error("알림 발송 실패: alertConfigId={}, type={}",
                    alertConfig.getId(), alertConfig.getAlertType(), e);
                return saveDelivery(alertConfig, endpoint, AlertDeliveryStatus.FAILED, testDelivery, e.getMessage());
            }
        }

        return saveDelivery(
            alertConfig,
            endpoint,
            AlertDeliveryStatus.FAILED,
            testDelivery,
            "지원하지 않는 알림 유형입니다: " + alertConfig.getAlertType()
        );
    }

    private AlertDelivery saveDelivery(
        AlertConfig alertConfig,
        Endpoint endpoint,
        AlertDeliveryStatus status,
        boolean testDelivery,
        String errorMessage
    ) {
        return alertDeliveryRepository.save(AlertDelivery.builder()
            .alertConfig(alertConfig)
            .endpoint(endpoint)
            .alertType(alertConfig.getAlertType())
            .target(alertConfig.getTarget())
            .status(status)
            .testDelivery(testDelivery)
            .errorMessage(errorMessage)
            .triggeredAt(LocalDateTime.now())
            .build());
    }

    private String validateTarget(AlertType alertType, String rawTarget) {
        String target = rawTarget == null ? "" : rawTarget.trim();
        if (target.isBlank()) {
            throw new IllegalArgumentException("알림 대상은 필수입니다.");
        }

        if (alertType == AlertType.EMAIL) {
            if (!EMAIL_PATTERN.matcher(target).matches()) {
                throw new IllegalArgumentException("이메일 알림 대상은 올바른 이메일 주소여야 합니다.");
            }
            return target;
        }

        if (alertType == AlertType.SLACK) {
            outboundUrlGuard.validateHttpUrl(target, "Slack Webhook URL");
            return target;
        }

        if (alertType == AlertType.WEBHOOK) {
            outboundUrlGuard.validateHttpUrl(target, "웹훅 URL");
            return target;
        }

        throw new IllegalArgumentException("지원하지 않는 알림 유형입니다: " + alertType);
    }

    private AlertConfig getAlertWithAccessCheck(Long alertId) {
        AlertConfig alertConfig = alertConfigRepository.findByIdAndDeletedFalse(alertId)
            .orElseThrow(() -> new AlertNotFoundException("알림 설정을 찾을 수 없습니다."));

        Endpoint endpoint = alertConfig.getEndpoint();
        if (endpoint.isDeleted()) {
            throw new AlertNotFoundException("알림 설정을 찾을 수 없습니다.");
        }

        endpointService.getEndpointWithAccessCheck(endpoint.getId());
        return alertConfig;
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
