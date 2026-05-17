package com.apiguard.backend.domain.incident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.apispec.entity.BreakingChange;
import com.apiguard.backend.domain.apispec.entity.BreakingChangeRule;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.incident.entity.Incident;
import com.apiguard.backend.domain.incident.entity.IncidentSeverity;
import com.apiguard.backend.domain.incident.entity.IncidentStatus;
import com.apiguard.backend.domain.incident.entity.IncidentType;
import com.apiguard.backend.domain.incident.repository.IncidentRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private EndpointService endpointService;

    @Mock
    private ProjectService projectService;

    @Test
    @DisplayName("3회 연속 실패 시 availability incident 생성")
    void syncIncidentState_consecutiveFailures_opensIncident() {
        IncidentService incidentService = new IncidentService(
            incidentRepository,
            checkResultRepository,
            endpointService,
            projectService
        );
        Endpoint endpoint = createEndpoint();
        List<CheckResult> failures = List.of(
            createResult(endpoint, CheckStatus.FAILURE),
            createResult(endpoint, CheckStatus.TIMEOUT),
            createResult(endpoint, CheckStatus.ERROR)
        );

        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
            .willReturn(failures);
        given(incidentRepository.findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
            1L,
            IncidentType.PERFORMANCE,
            IncidentStatus.OPEN
        )).willReturn(Optional.empty());
        given(incidentRepository.findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
            1L,
            IncidentType.AVAILABILITY,
            IncidentStatus.OPEN
        )).willReturn(Optional.empty());

        incidentService.syncIncidentState(failures.get(0));

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(IncidentType.AVAILABILITY);
        assertThat(captor.getValue().getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(captor.getValue().getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);
    }

    @Test
    @DisplayName("성공 체크가 들어오면 열린 availability incident를 resolved 처리")
    void syncIncidentState_success_resolvesAvailabilityIncident() {
        IncidentService incidentService = new IncidentService(
            incidentRepository,
            checkResultRepository,
            endpointService,
            projectService
        );
        Endpoint endpoint = createEndpoint();
        Incident openIncident = Incident.builder()
            .id(1L)
            .endpoint(endpoint)
            .type(IncidentType.AVAILABILITY)
            .status(IncidentStatus.OPEN)
            .severity(IncidentSeverity.CRITICAL)
            .title("Endpoint availability incident")
            .description("failure")
            .detectedCount(3)
            .startedAt(LocalDateTime.now().minusMinutes(3))
            .lastDetectedAt(LocalDateTime.now().minusMinutes(1))
            .build();

        given(incidentRepository.findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
            1L,
            IncidentType.AVAILABILITY,
            IncidentStatus.OPEN
        )).willReturn(Optional.of(openIncident));
        given(incidentRepository.findFirstByEndpointIdAndTypeAndStatusOrderByStartedAtDesc(
            1L,
            IncidentType.PERFORMANCE,
            IncidentStatus.OPEN
        )).willReturn(Optional.empty());

        incidentService.syncIncidentState(createResult(endpoint, CheckStatus.SUCCESS));

        assertThat(openIncident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(openIncident.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Breaking change 발생 시 project 단위 contract change incident 생성")
    void recordContractChange_opensProjectIncident() {
        IncidentService incidentService = new IncidentService(
            incidentRepository,
            checkResultRepository,
            endpointService,
            projectService
        );
        Project project = createProject();
        List<BreakingChange> changes = List.of(
            BreakingChange.builder()
                .rule(BreakingChangeRule.METHOD_REMOVED)
                .location("/users DELETE")
                .description("기존 method가 삭제되었습니다.")
                .build()
        );

        given(incidentRepository.findFirstByProjectIdAndTypeAndStatusAndTitleOrderByStartedAtDesc(
            1L,
            IncidentType.CONTRACT_CHANGE,
            IncidentStatus.OPEN,
            "API contract breaking change detected: Payments API"
        )).willReturn(Optional.empty());

        incidentService.recordContractChange(project, "Payments API", changes);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getProject()).isEqualTo(project);
        assertThat(captor.getValue().getEndpoint()).isNull();
        assertThat(captor.getValue().getType()).isEqualTo(IncidentType.CONTRACT_CHANGE);
        assertThat(captor.getValue().getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(captor.getValue().getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(captor.getValue().getDetectedCount()).isEqualTo(1);
    }

    private Endpoint createEndpoint() {
        Project project = createProject();
        return Endpoint.builder()
            .id(1L)
            .project(project)
            .url("https://api.example.com/health")
            .httpMethod(HttpMethod.GET)
            .expectedStatusCode(200)
            .checkInterval(60)
            .build();
    }

    private Project createProject() {
        User user = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("tester")
            .password("encoded")
            .role(Role.USER)
            .build();
        return Project.builder()
            .id(1L)
            .user(user)
            .name("Test Project")
            .build();
    }

    private CheckResult createResult(Endpoint endpoint, CheckStatus status) {
        return CheckResult.builder()
            .id(1L)
            .endpoint(endpoint)
            .status(status)
            .statusCode(status == CheckStatus.SUCCESS ? 200 : 500)
            .responseTimeMs(100L)
            .checkedAt(LocalDateTime.now())
            .build();
    }
}
