package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.global.exception.EndpointNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
