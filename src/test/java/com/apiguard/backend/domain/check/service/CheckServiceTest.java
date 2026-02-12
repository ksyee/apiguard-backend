package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import com.apiguard.backend.domain.check.dto.EndpointStatsResponse;
import com.apiguard.backend.domain.check.dto.ProjectStatsResponse;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.global.exception.EndpointNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckServiceTest {

    @Mock
    private EndpointService endpointService;

    @Mock
    private HttpCheckerService httpCheckerService;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private CheckService checkService;

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

    private Endpoint createEndpoint(Long id, Project project) {
        return Endpoint.builder()
            .id(id)
            .project(project)
            .url("https://api.example.com/health")
            .httpMethod(HttpMethod.GET)
            .expectedStatusCode(200)
            .checkInterval(60)
            .isActive(true)
            .build();
    }

    @Test
    @DisplayName("수동 테스트 성공 - 체크 실행 후 저장 및 응답")
    void testEndpoint_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        CheckResult checkResult = CheckResult.builder()
            .id(1L)
            .endpoint(endpoint)
            .status(CheckStatus.SUCCESS)
            .statusCode(200)
            .responseTimeMs(150L)
            .checkedAt(LocalDateTime.now())
            .build();

        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);
        given(httpCheckerService.check(endpoint)).willReturn(checkResult);
        given(checkResultRepository.save(any(CheckResult.class))).willReturn(checkResult);

        // when
        CheckResultResponse response = checkService.testEndpoint(1L);

        // then
        assertThat(response.endpointId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.responseTimeMs()).isEqualTo(150L);
        verify(checkResultRepository).save(any(CheckResult.class));
    }

    @Test
    @DisplayName("존재하지 않는 엔드포인트 테스트 시 404 예외")
    void testEndpoint_notFound() {
        // given
        given(endpointService.getEndpointWithOwnerCheck(999L))
            .willThrow(new EndpointNotFoundException("엔드포인트를 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> checkService.testEndpoint(999L))
            .isInstanceOf(EndpointNotFoundException.class)
            .hasMessage("엔드포인트를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("엔드포인트 통계 조회 성공 - 성공률, 평균 응답시간 계산 검증")
    void getEndpointStats_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);
        given(checkResultRepository.countByEndpointIdAndCheckedAtAfter(eq(1L), any(LocalDateTime.class)))
            .willReturn(10L);
        given(checkResultRepository.countByEndpointIdAndStatusAndCheckedAtAfter(eq(1L), eq(CheckStatus.SUCCESS), any(LocalDateTime.class)))
            .willReturn(8L);
        given(checkResultRepository.findAvgResponseTimeByEndpointIdAndCheckedAtAfter(eq(1L), any(LocalDateTime.class)))
            .willReturn(250.5);

        // when
        EndpointStatsResponse stats = checkService.getEndpointStats(1L);

        // then
        assertThat(stats.totalChecks()).isEqualTo(10L);
        assertThat(stats.successCount()).isEqualTo(8L);
        assertThat(stats.successRate()).isEqualTo(80.0);
        assertThat(stats.avgResponseTimeMs()).isEqualTo(250.5);
        assertThat(stats.since()).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 통계 조회 성공 - UP/DOWN 카운트 검증")
    void getProjectStats_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint1 = createEndpoint(1L, project);
        Endpoint endpoint2 = createEndpoint(2L, project);

        CheckResult successResult = CheckResult.builder()
            .id(1L)
            .endpoint(endpoint1)
            .status(CheckStatus.SUCCESS)
            .statusCode(200)
            .responseTimeMs(100L)
            .checkedAt(LocalDateTime.now())
            .build();

        CheckResult failureResult = CheckResult.builder()
            .id(2L)
            .endpoint(endpoint2)
            .status(CheckStatus.FAILURE)
            .statusCode(500)
            .responseTimeMs(200L)
            .checkedAt(LocalDateTime.now())
            .build();

        given(projectService.getProjectWithOwnerCheck(1L)).willReturn(project);
        given(endpointRepository.findByProjectIdAndDeletedFalse(1L)).willReturn(List.of(endpoint1, endpoint2));
        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
            .willReturn(List.of(successResult));
        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(2L), any(PageRequest.class)))
            .willReturn(List.of(failureResult));

        // when
        ProjectStatsResponse stats = checkService.getProjectStats(1L);

        // then
        assertThat(stats.totalEndpoints()).isEqualTo(2);
        assertThat(stats.upCount()).isEqualTo(1);
        assertThat(stats.downCount()).isEqualTo(1);
        assertThat(stats.avgResponseTimeMs()).isEqualTo(150.0);
    }

    @Test
    @DisplayName("최근 체크 결과 조회 성공 - 결과 목록 반환 검증")
    void getRecentChecks_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        CheckResult result1 = CheckResult.builder()
            .id(1L)
            .endpoint(endpoint)
            .status(CheckStatus.SUCCESS)
            .statusCode(200)
            .responseTimeMs(100L)
            .checkedAt(LocalDateTime.now())
            .build();

        CheckResult result2 = CheckResult.builder()
            .id(2L)
            .endpoint(endpoint)
            .status(CheckStatus.FAILURE)
            .statusCode(500)
            .responseTimeMs(300L)
            .checkedAt(LocalDateTime.now().minusMinutes(5))
            .build();

        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);
        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
            .willReturn(List.of(result1, result2));

        // when
        List<CheckResultResponse> results = checkService.getRecentChecks(1L, 20);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).status()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(results.get(1).status()).isEqualTo(CheckStatus.FAILURE);
    }
}
