package com.apiguard.backend.domain.alert.service;

import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertType;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.global.security.OutboundUrlGuard;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WebhookNotificationService implements NotificationService {

    private final RestTemplate restTemplate;
    private final OutboundUrlGuard outboundUrlGuard;

    @Override
    public boolean supports(AlertType alertType) {
        return alertType == AlertType.WEBHOOK;
    }

    @Override
    public void send(AlertConfig config, Endpoint endpoint, List<CheckResult> recentFailures) {
        Map<String, Object> payload = Map.of(
            "event", "endpoint.alert",
            "endpointId", endpoint.getId(),
            "url", endpoint.getUrl(),
            "method", endpoint.getHttpMethod().name(),
            "threshold", config.getThreshold(),
            "failures", recentFailures.stream()
                .map(result -> Map.of(
                    "status", result.getStatus().name(),
                    "statusCode", result.getStatusCode() != null ? result.getStatusCode() : "",
                    "responseTimeMs", result.getResponseTimeMs() != null ? result.getResponseTimeMs() : "",
                    "errorMessage", result.getErrorMessage() != null ? result.getErrorMessage() : "",
                    "checkedAt", result.getCheckedAt().toString()
                ))
                .toList()
        );

        URI uri = outboundUrlGuard.validateHttpUrl(config.getTarget(), "웹훅 URL");
        restTemplate.postForEntity(uri, payload, String.class);
    }
}
