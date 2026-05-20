package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.global.security.OutboundUrlGuard;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HttpCheckerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OutboundUrlGuard outboundUrlGuard;

    @InjectMocks
    private HttpCheckerService httpCheckerService;

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
    @DisplayName("HTTP 체크 성공 - 200 OK")
    void check_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        URI uri = allowEndpointUrl(endpoint);

        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);
        given(restTemplate.exchange(
            eq(uri),
            eq(org.springframework.http.HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).willReturn(response);

        // when
        CheckResult result = httpCheckerService.check(endpoint);

        // then
        assertThat(result.getStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseTimeMs()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("상태코드 불일치 시 FAILURE")
    void check_statusCodeMismatch_failure() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        URI uri = allowEndpointUrl(endpoint);

        ResponseEntity<String> response = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        given(restTemplate.exchange(
            eq(uri),
            eq(org.springframework.http.HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).willReturn(response);

        // when
        CheckResult result = httpCheckerService.check(endpoint);

        // then
        assertThat(result.getStatus()).isEqualTo(CheckStatus.FAILURE);
        assertThat(result.getStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("타임아웃 발생 시 TIMEOUT")
    void check_timeout() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        URI uri = allowEndpointUrl(endpoint);

        given(restTemplate.exchange(
            eq(uri),
            eq(org.springframework.http.HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).willThrow(new ResourceAccessException("Connection timed out"));

        // when
        CheckResult result = httpCheckerService.check(endpoint);

        // then
        assertThat(result.getStatus()).isEqualTo(CheckStatus.TIMEOUT);
        assertThat(result.getErrorMessage()).contains("Connection timed out");
    }

    @Test
    @DisplayName("재시도 로직 - 첫 시도 실패 후 재시도 성공")
    void check_retrySuccess() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        URI uri = allowEndpointUrl(endpoint);

        ResponseEntity<String> failResponse = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);

        given(restTemplate.exchange(
            eq(uri),
            eq(org.springframework.http.HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).willReturn(failResponse).willReturn(successResponse);

        // when
        CheckResult result = httpCheckerService.check(endpoint);

        // then
        assertThat(result.getStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("재시도 로직 - 두 번 모두 실패")
    void check_retryAlsoFails() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        URI uri = allowEndpointUrl(endpoint);

        ResponseEntity<String> failResponse = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);

        given(restTemplate.exchange(
            eq(uri),
            eq(org.springframework.http.HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).willReturn(failResponse);

        // when
        CheckResult result = httpCheckerService.check(endpoint);

        // then
        assertThat(result.getStatus()).isEqualTo(CheckStatus.FAILURE);
        assertThat(result.getStatusCode()).isEqualTo(500);
    }

    private URI allowEndpointUrl(Endpoint endpoint) {
        URI uri = URI.create(endpoint.getUrl());
        given(outboundUrlGuard.validateHttpUrl(endpoint.getUrl(), "엔드포인트 URL"))
            .willReturn(uri);
        return uri;
    }
}
