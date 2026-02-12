package com.apiguard.backend.domain.endpoint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.apiguard.backend.domain.endpoint.dto.CreateEndpointRequest;
import com.apiguard.backend.domain.endpoint.dto.EndpointResponse;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.global.exception.EndpointNotFoundException;
import com.apiguard.backend.global.exception.ForbiddenException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EndpointServiceTest {

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EndpointService endpointService;

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
    @DisplayName("엔드포인트 생성 성공")
    void createEndpoint_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);

        given(projectService.getProjectWithOwnerCheck(1L)).willReturn(project);

        CreateEndpointRequest request = new CreateEndpointRequest(
            "https://api.example.com/health", HttpMethod.GET, null, null, 200, 60
        );

        Endpoint saved = createEndpoint(1L, project);
        given(endpointRepository.save(any(Endpoint.class))).willReturn(saved);

        // when
        EndpointResponse response = endpointService.createEndpoint(1L, request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.url()).isEqualTo("https://api.example.com/health");
        assertThat(response.httpMethod()).isEqualTo(HttpMethod.GET);
    }

    @Test
    @DisplayName("프로젝트 내 엔드포인트 목록 조회")
    void getEndpoints_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);

        given(projectService.getProjectWithOwnerCheck(1L)).willReturn(project);

        List<Endpoint> endpoints = List.of(
            createEndpoint(1L, project),
            createEndpoint(2L, project)
        );
        given(endpointRepository.findByProjectIdAndDeletedFalse(1L)).willReturn(endpoints);

        // when
        List<EndpointResponse> result = endpointService.getEndpoints(1L);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("다른 사용자 엔드포인트 접근 시 403 예외")
    void getEndpoint_forbidden() {
        // given
        User owner = createUser(1L);
        User other = createUser(2L);
        Project project = createProject(1L, owner);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(other);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));

        // when & then
        assertThatThrownBy(() -> endpointService.getEndpoint(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("해당 엔드포인트에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 엔드포인트 조회 시 404 예외")
    void getEndpoint_notFound() {
        // given
        User user = createUser(1L);
        given(userService.getUserDetail()).willReturn(user);
        given(endpointRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> endpointService.getEndpoint(999L))
            .isInstanceOf(EndpointNotFoundException.class)
            .hasMessage("엔드포인트를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("엔드포인트 토글 성공")
    void toggleEndpoint_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(user);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));

        // when
        EndpointResponse response = endpointService.toggleEndpoint(1L);

        // then
        assertThat(response.isActive()).isFalse();
    }
}
