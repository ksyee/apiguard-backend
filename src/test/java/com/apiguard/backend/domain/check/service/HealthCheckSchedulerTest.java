package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthCheckSchedulerTest {

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private CheckService checkService;

    private HealthCheckScheduler healthCheckScheduler;

    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        healthCheckScheduler = new HealthCheckScheduler(
            endpointRepository, checkService, directExecutor);
    }

    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email("test@email.com")
            .password("encodedPassword")
            .nickname("tester")
            .role(Role.USER)
            .build();
    }

    private Project createProject(Long id, User user) {
        return Project.builder()
            .id(id)
            .user(user)
            .name("Test Project")
            .description("Test Description")
            .build();
    }

    private Endpoint createEndpoint(Long id, Project project, LocalDateTime lastCheckedAt) {
        return Endpoint.builder()
            .id(id)
            .project(project)
            .url("https://api.example.com/health")
            .httpMethod(HttpMethod.GET)
            .expectedStatusCode(200)
            .checkInterval(60)
            .isActive(true)
            .lastCheckedAt(lastCheckedAt)
            .build();
    }

    @Test
    @DisplayName("체크 대상 엔드포인트가 있을 때 체크 실행 및 결과 저장")
    void scheduleHealthChecks_withDueEndpoints_executesChecks() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project, null);

        given(endpointRepository.findByIsActiveTrueAndDeletedFalse())
            .willReturn(List.of(endpoint));
        willDoNothing().given(checkService).performCheck(any(Endpoint.class));

        // when
        healthCheckScheduler.scheduleHealthChecks();

        // then
        verify(checkService).performCheck(endpoint);
    }

    @Test
    @DisplayName("체크 대상이 없을 때 아무것도 실행하지 않음")
    void scheduleHealthChecks_noDueEndpoints_skips() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project, LocalDateTime.now());

        given(endpointRepository.findByIsActiveTrueAndDeletedFalse())
            .willReturn(List.of(endpoint));

        // when
        healthCheckScheduler.scheduleHealthChecks();

        // then
        verify(checkService, never()).performCheck(any(Endpoint.class));
    }

    @Test
    @DisplayName("개별 체크 실패 시 다른 체크에 영향 없음")
    void scheduleHealthChecks_individualFailure_doesNotAffectOthers() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint1 = createEndpoint(1L, project, null);
        Endpoint endpoint2 = createEndpoint(2L, project, null);

        given(endpointRepository.findByIsActiveTrueAndDeletedFalse())
            .willReturn(List.of(endpoint1, endpoint2));

        willThrow(new RuntimeException("체크 실패")).given(checkService).performCheck(endpoint1);
        willDoNothing().given(checkService).performCheck(endpoint2);

        // when
        healthCheckScheduler.scheduleHealthChecks();

        // then
        verify(checkService).performCheck(endpoint1);
        verify(checkService).performCheck(endpoint2);
    }
}
