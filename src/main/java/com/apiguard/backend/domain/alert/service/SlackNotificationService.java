package com.apiguard.backend.domain.alert.service;

import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertType;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService implements NotificationService {

    private final RestTemplate restTemplate;

    @Override
    public boolean supports(AlertType alertType) {
        return alertType == AlertType.SLACK;
    }

    @Override
    public void send(AlertConfig config, Endpoint endpoint, List<CheckResult> recentFailures) {
        String text = buildSlackMessage(endpoint, recentFailures, config.getThreshold());
        Map<String, String> payload = Map.of("text", text);

        restTemplate.postForEntity(config.getTarget(), payload, String.class);
    }

    private String buildSlackMessage(Endpoint endpoint, List<CheckResult> failures, int threshold) {
        String failureDetails = failures.stream()
            .map(f -> String.format("  - %s | %s | 상태코드: %s | %s",
                f.getCheckedAt(),
                f.getStatus(),
                f.getStatusCode() != null ? f.getStatusCode() : "-",
                f.getErrorMessage() != null ? f.getErrorMessage() : "-"
            ))
            .collect(Collectors.joining("\n"));

        return """
            :rotating_light: *APIGuard 장애 알림*

            엔드포인트가 연속 *%d회* 실패했습니다.

            *URL:* %s
            *HTTP 메서드:* %s

            *최근 실패 내역:*
            %s
            """.formatted(threshold, endpoint.getUrl(), endpoint.getHttpMethod(), failureDetails);
    }
}
