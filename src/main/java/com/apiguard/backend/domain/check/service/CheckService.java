package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import com.apiguard.backend.domain.check.dto.EndpointStatsResponse;
import com.apiguard.backend.domain.check.dto.HourlyStatResponse;
import com.apiguard.backend.domain.check.dto.ProjectStatsResponse;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.global.exception.EndpointNotFoundException;
import com.apiguard.backend.domain.incident.service.IncidentService;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckService {

    private final EndpointService endpointService;
    private final HttpCheckerService httpCheckerService;
    private final CheckResultRepository checkResultRepository;
    private final EndpointRepository endpointRepository;
    private final ProjectService projectService;
    private final CheckEventPublisher checkEventPublisher;
    private final IncidentService incidentService;

    @Transactional
    public CheckResultResponse testEndpoint(Long endpointId) {
        Endpoint endpoint = endpointService.getEndpointWithWriteCheck(endpointId);

        CheckResult result = httpCheckerService.check(endpoint);
        CheckResult saved = checkResultRepository.save(result);

        endpoint.updateLastCheckedAt();
        incidentService.syncIncidentState(saved);

        CheckResultResponse response = CheckResultResponse.from(saved);

        try {
            checkEventPublisher.publish(response, endpoint.getProject().getId());
        } catch (Exception e) {
            // WebSocket 발행 실패가 테스트 트랜잭션에 영향을 주지 않도록 한다.
        }

        return response;
    }

    @Transactional
    public void performCheck(Long endpointId) {
        Endpoint endpoint = endpointRepository.findByIdAndDeletedFalse(endpointId)
            .orElseThrow(() -> new EndpointNotFoundException("엔드포인트를 찾을 수 없습니다."));
        if (endpoint.getProject().isDeleted()
            || (endpoint.getProject().getWorkspace() != null && endpoint.getProject().getWorkspace().isDeleted())) {
            throw new EndpointNotFoundException("엔드포인트를 찾을 수 없습니다.");
        }
        CheckResult result = httpCheckerService.check(endpoint);
        CheckResult saved = checkResultRepository.save(result);
        endpoint.updateLastCheckedAt();
        incidentService.syncIncidentState(saved);

        try {
            checkEventPublisher.publish(
                CheckResultResponse.from(saved),
                endpoint.getProject().getId()
            );
        } catch (Exception e) {
            // WebSocket 발행 실패가 헬스체크 트랜잭션에 영향을 주지 않도록 한다.
        }
    }

    public EndpointStatsResponse getEndpointStats(Long endpointId) {
        endpointService.getEndpointWithAccessCheck(endpointId);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long totalChecks = checkResultRepository.countByEndpointIdAndCheckedAtAfter(endpointId, since);
        long successCount = checkResultRepository.countByEndpointIdAndStatusAndCheckedAtAfter(endpointId, CheckStatus.SUCCESS, since);
        double successRate = totalChecks > 0 ? (double) successCount / totalChecks * 100 : 0.0;
        Double avgResponseTime = checkResultRepository.findAvgResponseTimeByEndpointIdAndCheckedAtAfter(endpointId, since);

        return new EndpointStatsResponse(
            totalChecks,
            successCount,
            successRate,
            avgResponseTime != null ? avgResponseTime : 0.0,
            since
        );
    }

    public List<HourlyStatResponse> getHourlyStats(Long endpointId) {
        endpointService.getEndpointWithAccessCheck(endpointId);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Object[]> rows = checkResultRepository.findHourlyStatsByEndpointId(endpointId, since);

        return rows.stream()
            .map(row -> new HourlyStatResponse(
                row[0] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0
            ))
            .toList();
    }

    public ProjectStatsResponse getProjectStats(Long projectId) {
        Project project = projectService.getProjectWithAccessCheck(projectId);

        List<Endpoint> endpoints = endpointRepository.findByProjectIdAndDeletedFalse(project.getId());
        long totalEndpoints = endpoints.size();
        long upCount = 0;
        long downCount = 0;
        double totalAvg = 0.0;
        int endpointsWithChecks = 0;

        for (Endpoint endpoint : endpoints) {
            List<CheckResult> recent = checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(endpoint.getId(), PageRequest.of(0, 1));
            if (!recent.isEmpty()) {
                CheckResult lastCheck = recent.get(0);
                if (lastCheck.getStatus() == CheckStatus.SUCCESS) {
                    upCount++;
                } else {
                    downCount++;
                }
                if (lastCheck.getResponseTimeMs() != null) {
                    totalAvg += lastCheck.getResponseTimeMs();
                    endpointsWithChecks++;
                }
            }
        }

        double avgResponseTime = endpointsWithChecks > 0 ? totalAvg / endpointsWithChecks : 0.0;

        return new ProjectStatsResponse(totalEndpoints, upCount, downCount, avgResponseTime);
    }

    public List<CheckResultResponse> getRecentChecks(Long endpointId, int limit) {
        endpointService.getEndpointWithAccessCheck(endpointId);

        List<CheckResult> results = checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(endpointId, PageRequest.of(0, limit));
        return results.stream()
            .map(CheckResultResponse::from)
            .toList();
    }
}
