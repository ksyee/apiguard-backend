package com.apiguard.backend.domain.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.alert.dto.AlertResponse;
import com.apiguard.backend.domain.alert.dto.CreateAlertRequest;
import com.apiguard.backend.domain.alert.dto.UpdateAlertRequest;
import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertType;
import com.apiguard.backend.domain.alert.repository.AlertConfigRepository;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.global.exception.AlertNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertConfigRepository alertConfigRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private EndpointService endpointService;

    @Mock
    private List<NotificationService> notificationServices;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AlertService alertService;

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

    private AlertConfig createAlertConfig(Long id, Endpoint endpoint) {
        return AlertConfig.builder()
            .id(id)
            .endpoint(endpoint)
            .alertType(AlertType.EMAIL)
            .target("alert@example.com")
            .threshold(3)
            .isActive(true)
            .build();
    }

    private CheckResult createCheckResult(Endpoint endpoint, CheckStatus status) {
        return CheckResult.builder()
            .id(1L)
            .endpoint(endpoint)
            .status(status)
            .statusCode(status == CheckStatus.SUCCESS ? 200 : 500)
            .responseTimeMs(100L)
            .checkedAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("알림 설정 생성 성공")
    void createAlert_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);

        CreateAlertRequest request = new CreateAlertRequest(
            AlertType.EMAIL, "alert@example.com", 3
        );

        AlertConfig saved = createAlertConfig(1L, endpoint);
        given(alertConfigRepository.save(any(AlertConfig.class))).willReturn(saved);

        // when
        AlertResponse response = alertService.createAlert(1L, request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.alertType()).isEqualTo(AlertType.EMAIL);
        assertThat(response.target()).isEqualTo("alert@example.com");
        assertThat(response.threshold()).isEqualTo(3);
    }

    @Test
    @DisplayName("알림 설정 목록 조회 성공")
    void getAlerts_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);

        List<AlertConfig> alerts = List.of(
            createAlertConfig(1L, endpoint),
            createAlertConfig(2L, endpoint)
        );
        given(alertConfigRepository.findByEndpointIdAndDeletedFalse(1L)).willReturn(alerts);

        // when
        List<AlertResponse> result = alertService.getAlerts(1L);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("알림 설정 수정 성공")
    void updateAlert_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        AlertConfig alertConfig = createAlertConfig(1L, endpoint);

        given(alertConfigRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(alertConfig));
        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);

        UpdateAlertRequest request = new UpdateAlertRequest(
            AlertType.SLACK, "https://hooks.slack.com/test", 5
        );

        // when
        AlertResponse response = alertService.updateAlert(1L, request);

        // then
        assertThat(response.alertType()).isEqualTo(AlertType.SLACK);
        assertThat(response.target()).isEqualTo("https://hooks.slack.com/test");
        assertThat(response.threshold()).isEqualTo(5);
    }

    @Test
    @DisplayName("알림 설정 삭제 성공 (soft delete)")
    void deleteAlert_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        AlertConfig alertConfig = createAlertConfig(1L, endpoint);

        given(alertConfigRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(alertConfig));
        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);

        // when
        alertService.deleteAlert(1L);

        // then
        assertThat(alertConfig.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("알림 설정 토글 성공")
    void toggleAlert_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        AlertConfig alertConfig = createAlertConfig(1L, endpoint);

        given(alertConfigRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(alertConfig));
        given(endpointService.getEndpointWithOwnerCheck(1L)).willReturn(endpoint);

        // when
        AlertResponse response = alertService.toggleAlert(1L);

        // then
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 알림 설정 수정 시 404 예외")
    void updateAlert_missingAlert_throwsAlertNotFoundException() {
        // given
        given(alertConfigRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        UpdateAlertRequest request = new UpdateAlertRequest(AlertType.EMAIL, "test@test.com", 3);

        // when & then
        assertThatThrownBy(() -> alertService.updateAlert(999L, request))
            .isInstanceOf(AlertNotFoundException.class)
            .hasMessage("알림 설정을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("활성 알림이 없으면 알림 발송하지 않음")
    void checkAndAlert_noActiveAlerts() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        given(alertConfigRepository.findByEndpointIdAndIsActiveTrueAndDeletedFalse(1L))
            .willReturn(List.of());

        // when
        alertService.checkAndAlert(1L);

        // then
        verify(checkResultRepository, never())
            .findByEndpointIdOrderByCheckedAtDesc(any(), any());
    }

    @Test
    @DisplayName("연속 실패 횟수가 임계값 미만이면 알림 발송하지 않음")
    void checkAndAlert_belowThreshold() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        AlertConfig alertConfig = createAlertConfig(1L, endpoint);

        given(alertConfigRepository.findByEndpointIdAndIsActiveTrueAndDeletedFalse(1L))
            .willReturn(List.of(alertConfig));

        // 2 failures then 1 success (threshold is 3)
        List<CheckResult> results = List.of(
            createCheckResult(endpoint, CheckStatus.FAILURE),
            createCheckResult(endpoint, CheckStatus.FAILURE),
            createCheckResult(endpoint, CheckStatus.SUCCESS)
        );
        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
            .willReturn(results);

        // when
        alertService.checkAndAlert(1L);

        // then
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("연속 실패가 임계값 이상이고 중복 알림이 아닌 경우 알림 발송")
    void checkAndAlert_thresholdReached_sendsAlert() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        AlertConfig alertConfig = createAlertConfig(1L, endpoint);

        given(alertConfigRepository.findByEndpointIdAndIsActiveTrueAndDeletedFalse(1L))
            .willReturn(List.of(alertConfig));

        List<CheckResult> results = List.of(
            createCheckResult(endpoint, CheckStatus.FAILURE),
            createCheckResult(endpoint, CheckStatus.FAILURE),
            createCheckResult(endpoint, CheckStatus.FAILURE)
        );
        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
            .willReturn(results);

        given(stringRedisTemplate.hasKey("ALERT_SENT:1")).willReturn(false);

        NotificationService mockNotification = org.mockito.Mockito.mock(NotificationService.class);
        given(mockNotification.supports(AlertType.EMAIL)).willReturn(true);

        // Replace the mocked list with a real list containing the mock
        java.util.ArrayList<NotificationService> services = new java.util.ArrayList<>();
        services.add(mockNotification);

        // Use reflection to set the field since @InjectMocks already injected
        try {
            var field = AlertService.class.getDeclaredField("notificationServices");
            field.setAccessible(true);
            field.set(alertService, services);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        alertService.checkAndAlert(1L);

        // then
        verify(mockNotification).send(eq(alertConfig), eq(endpoint), any());
        verify(valueOperations).set(eq("ALERT_SENT:1"), eq("1"), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Redis에 중복 알림 키가 있으면 알림을 발송하지 않음")
    void checkAndAlert_duplicateAlert_skipped() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);
        AlertConfig alertConfig = createAlertConfig(1L, endpoint);

        given(alertConfigRepository.findByEndpointIdAndIsActiveTrueAndDeletedFalse(1L))
            .willReturn(List.of(alertConfig));

        List<CheckResult> results = List.of(
            createCheckResult(endpoint, CheckStatus.FAILURE),
            createCheckResult(endpoint, CheckStatus.FAILURE),
            createCheckResult(endpoint, CheckStatus.FAILURE)
        );
        given(checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(eq(1L), any(PageRequest.class)))
            .willReturn(results);

        given(stringRedisTemplate.hasKey("ALERT_SENT:1")).willReturn(true);

        // when
        alertService.checkAndAlert(1L);

        // then
        verify(stringRedisTemplate, never()).opsForValue();
    }
}
