package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpCheckerService {

    private final RestTemplate restTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CheckResult check(Endpoint endpoint) {
        CheckResult firstAttempt = doCheck(endpoint);

        if (firstAttempt.getStatus() == CheckStatus.SUCCESS) {
            return firstAttempt;
        }

        log.info("첫 번째 시도 실패, 재시도 중: endpointId={}", endpoint.getId());
        return doCheck(endpoint);
    }

    private CheckResult doCheck(Endpoint endpoint) {
        HttpHeaders headers = buildHeaders(endpoint.getHeaders());
        HttpEntity<String> httpEntity = new HttpEntity<>(endpoint.getBody(), headers);

        long startTime = System.nanoTime();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint.getUrl(),
                org.springframework.http.HttpMethod.valueOf(endpoint.getHttpMethod().name()),
                httpEntity,
                String.class
            );

            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            int statusCode = response.getStatusCode().value();
            CheckStatus status = (statusCode == endpoint.getExpectedStatusCode())
                ? CheckStatus.SUCCESS : CheckStatus.FAILURE;

            return CheckResult.builder()
                .endpoint(endpoint)
                .status(status)
                .statusCode(statusCode)
                .responseTimeMs(responseTimeMs)
                .checkedAt(LocalDateTime.now())
                .build();

        } catch (ResourceAccessException e) {
            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            log.warn("타임아웃 발생: endpointId={}, url={}", endpoint.getId(), endpoint.getUrl(), e);

            return CheckResult.builder()
                .endpoint(endpoint)
                .status(CheckStatus.TIMEOUT)
                .responseTimeMs(responseTimeMs)
                .errorMessage(e.getMessage())
                .checkedAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("체크 중 오류 발생: endpointId={}, url={}", endpoint.getId(), endpoint.getUrl(), e);

            return CheckResult.builder()
                .endpoint(endpoint)
                .status(CheckStatus.ERROR)
                .responseTimeMs(responseTimeMs)
                .errorMessage(e.getMessage())
                .checkedAt(LocalDateTime.now())
                .build();
        }
    }

    private HttpHeaders buildHeaders(String headersJson) {
        HttpHeaders headers = new HttpHeaders();
        if (headersJson == null || headersJson.isBlank()) {
            return headers;
        }

        try {
            Map<String, String> headerMap = objectMapper.readValue(
                headersJson, new TypeReference<>() {}
            );
            headerMap.forEach(headers::set);
        } catch (Exception e) {
            log.warn("헤더 파싱 실패: {}", e.getMessage());
        }

        return headers;
    }
}
