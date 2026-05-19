package com.apiguard.backend.domain.incident.service;

import com.apiguard.backend.domain.apispec.entity.BreakingChange;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.incident.dto.IncidentResponse;
import com.apiguard.backend.domain.incident.entity.Incident;
import com.apiguard.backend.domain.incident.entity.IncidentSeverity;
import com.apiguard.backend.domain.incident.entity.IncidentStatus;
import com.apiguard.backend.domain.incident.entity.IncidentType;
import com.apiguard.backend.domain.incident.repository.IncidentRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {

    private static final int FAILURE_THRESHOLD = 3;
    private static final int SLOW_RESPONSE_THRESHOLD = 3;
    private static final long DEGRADED_RESPONSE_TIME_MS = 1_000L;

    private final IncidentRepository incidentRepository;
    private final CheckResultRepository checkResultRepository;
    private final EndpointService endpointService;
    private final ProjectService projectService;

    @Transactional
    public void syncIncidentState(CheckResult latestResult) {
        Endpoint endpoint = latestResult.getEndpoint();

        if (latestResult.getStatus() == CheckStatus.SUCCESS) {
            handleSuccessfulCheck(endpoint, latestResult);
            return;
        }

        resolveOpenIncident(endpoint, IncidentType.PERFORMANCE);

        int consecutiveFailures = countConsecutiveFailures(endpoint.getId());
        if (consecutiveFailures >= FAILURE_THRESHOLD) {
            openOrUpdateIncident(
                endpoint,
                IncidentType.AVAILABILITY,
                IncidentSeverity.CRITICAL,
                "Endpoint availability incident",
                "최근 " + consecutiveFailures + "회 연속 상태 체크가 실패했습니다.",
                consecutiveFailures
            );
        }
    }

    public List<IncidentResponse> getEndpointIncidents(Long endpointId) {
        endpointService.getEndpointWithAccessCheck(endpointId);
        return incidentRepository.findByEndpointIdOrderByStartedAtDesc(endpointId).stream()
            .map(IncidentResponse::from)
            .toList();
    }

    public List<IncidentResponse> getProjectIncidents(Long projectId, IncidentStatus status) {
        projectService.getProjectWithAccessCheck(projectId);
        List<Incident> incidents = status == null
            ? incidentRepository.findByProjectIdOrderByStartedAtDesc(projectId)
            : incidentRepository.findByProjectIdAndStatusOrderByStartedAtDesc(projectId, status);

        return incidents.stream()
            .map(IncidentResponse::from)
            .toList();
    }

    @Transactional
    public void recordContractChange(Project project, String specSourceName, List<BreakingChange> changes) {
        if (changes.isEmpty()) {
            return;
        }

        String title = "API contract breaking change detected: " + specSourceName;
        String description = buildContractChangeDescription(specSourceName, changes);

        incidentRepository.findFirstByProjectIdAndTypeAndStatusAndTitleOrderByStartedAtDesc(
                project.getId(),
                IncidentType.CONTRACT_CHANGE,
                IncidentStatus.OPEN,
                title
            )
            .ifPresentOrElse(
                incident -> incident.markDetected(changes.size(), description),
                () -> incidentRepository.save(Incident.builder()
                    .project(project)
                    .type(IncidentType.CONTRACT_CHANGE)
                    .status(IncidentStatus.OPEN)
                    .severity(IncidentSeverity.CRITICAL)
                    .title(title)
                    .description(description)
                    .detectedCount(changes.size())
                    .startedAt(LocalDateTime.now())
                    .lastDetectedAt(LocalDateTime.now())
                    .build())
            );
    }

    private void handleSuccessfulCheck(Endpoint endpoint, CheckResult latestResult) {
        resolveOpenIncident(endpoint, IncidentType.AVAILABILITY);

        if (isSlow(latestResult)) {
            int consecutiveSlowResponses = countConsecutiveSlowResponses(endpoint.getId());
            if (consecutiveSlowResponses >= SLOW_RESPONSE_THRESHOLD) {
                openOrUpdateIncident(
                    endpoint,
                    IncidentType.PERFORMANCE,
                    IncidentSeverity.WARNING,
                    "Endpoint degraded performance",
                    "최근 " + consecutiveSlowResponses + "회 연속 응답 시간이 "
                        + DEGRADED_RESPONSE_TIME_MS + "ms를 초과했습니다.",
                    consecutiveSlowResponses
                );
            }
            return;
        }

        resolveOpenIncident(endpoint, IncidentType.PERFORMANCE);
    }

    private int countConsecutiveFailures(Long endpointId) {
        List<CheckResult> recentResults = checkResultRepository
            .findByEndpointIdOrderByCheckedAtDesc(endpointId, PageRequest.of(0, FAILURE_THRESHOLD));

        int count = 0;
        for (CheckResult result : recentResults) {
            if (result.getStatus() == CheckStatus.SUCCESS) {
                break;
            }
            count++;
        }
        return count;
    }

    private int countConsecutiveSlowResponses(Long endpointId) {
        List<CheckResult> recentResults = checkResultRepository
            .findByEndpointIdOrderByCheckedAtDesc(endpointId, PageRequest.of(0, SLOW_RESPONSE_THRESHOLD));

        int count = 0;
        for (CheckResult result : recentResults) {
            if (!isSlow(result)) {
                break;
            }
            count++;
        }
        return count;
    }

    private boolean isSlow(CheckResult result) {
        return result.getStatus() == CheckStatus.SUCCESS
            && result.getResponseTimeMs() != null
            && result.getResponseTimeMs() > DEGRADED_RESPONSE_TIME_MS;
    }

    private void openOrUpdateIncident(
        Endpoint endpoint,
        IncidentType type,
        IncidentSeverity severity,
        String title,
        String description,
        int detectedCount
    ) {
        incidentRepository.findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
                endpoint.getId(),
                type,
                IncidentStatus.OPEN
            )
            .ifPresentOrElse(
                incident -> incident.markDetected(detectedCount, description),
                () -> incidentRepository.save(Incident.builder()
                    .endpoint(endpoint)
                    .project(endpoint.getProject())
                    .type(type)
                    .status(IncidentStatus.OPEN)
                    .severity(severity)
                    .title(title)
                    .description(description)
                    .detectedCount(detectedCount)
                    .startedAt(LocalDateTime.now())
                    .lastDetectedAt(LocalDateTime.now())
                    .build())
            );
    }

    private void resolveOpenIncident(Endpoint endpoint, IncidentType type) {
        incidentRepository.findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
                endpoint.getId(),
                type,
                IncidentStatus.OPEN
            )
            .ifPresent(Incident::resolve);
    }

    private String buildContractChangeDescription(String specSourceName, List<BreakingChange> changes) {
        String details = changes.stream()
            .limit(5)
            .map(change -> "- " + change.getRule() + " " + change.getLocation())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");

        return "OpenAPI source '" + specSourceName + "'에서 "
            + changes.size()
            + "개의 breaking change가 감지되었습니다."
            + (details.isBlank() ? "" : "\n" + details);
    }
}
