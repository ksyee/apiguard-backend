package com.apiguard.backend.domain.alert.service;

import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertType;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;

    @Override
    public boolean supports(AlertType alertType) {
        return alertType == AlertType.EMAIL;
    }

    @Override
    public void send(AlertConfig config, Endpoint endpoint, List<CheckResult> recentFailures) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(config.getTarget());
            helper.setSubject("[APIGuard] 엔드포인트 장애 알림 - " + endpoint.getUrl());
            helper.setText(buildHtmlContent(endpoint, recentFailures, config.getThreshold()), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    private String buildHtmlContent(Endpoint endpoint, List<CheckResult> failures, int threshold) {
        String failureRows = failures.stream()
            .map(f -> String.format(
                "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                f.getCheckedAt(),
                f.getStatus(),
                f.getStatusCode() != null ? f.getStatusCode() : "-",
                f.getErrorMessage() != null ? f.getErrorMessage() : "-"
            ))
            .collect(Collectors.joining());

        return """
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #e74c3c;">APIGuard 장애 알림</h2>
                <p>엔드포인트가 연속 <strong>%d회</strong> 실패했습니다.</p>
                <table style="border-collapse: collapse; margin-top: 10px;">
                    <tr>
                        <td style="padding: 8px; font-weight: bold;">URL</td>
                        <td style="padding: 8px;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; font-weight: bold;">HTTP 메서드</td>
                        <td style="padding: 8px;">%s</td>
                    </tr>
                </table>
                <h3 style="margin-top: 20px;">최근 실패 내역</h3>
                <table style="border-collapse: collapse; width: 100%%;">
                    <thead>
                        <tr style="background-color: #f8f9fa;">
                            <th style="border: 1px solid #dee2e6; padding: 8px;">시간</th>
                            <th style="border: 1px solid #dee2e6; padding: 8px;">상태</th>
                            <th style="border: 1px solid #dee2e6; padding: 8px;">상태 코드</th>
                            <th style="border: 1px solid #dee2e6; padding: 8px;">에러 메시지</th>
                        </tr>
                    </thead>
                    <tbody>%s</tbody>
                </table>
                <p style="margin-top: 20px; color: #6c757d; font-size: 12px;">
                    이 알림은 APIGuard에서 자동으로 발송되었습니다.
                </p>
            </body>
            </html>
            """.formatted(threshold, endpoint.getUrl(), endpoint.getHttpMethod(), failureRows);
    }
}
