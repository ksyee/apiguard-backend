package com.apiguard.backend.domain.endpoint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.apiguard.backend.domain.endpoint.dto.CreateEndpointRequest;
import com.apiguard.backend.domain.endpoint.dto.EndpointResponse;
import com.apiguard.backend.domain.endpoint.dto.UpdateEndpointRequest;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.domain.subscription.service.FreePlanLimitPolicy;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.global.exception.EndpointNotFoundException;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.ProjectNotFoundException;
import java.util.List;
import java.util.Map;
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

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

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

    private Workspace createWorkspace(Long id, User owner) {
        return Workspace.builder()
            .id(id)
            .name("Test Workspace")
            .slug("test-workspace")
            .owner(owner)
            .build();
    }

    private Project createWorkspaceProject(Long id, User user, Workspace workspace) {
        return Project.builder()
            .id(id)
            .workspace(workspace)
            .user(user)
            .name("Workspace Project")
            .description("Workspace Project Description")
            .build();
    }

    private WorkspaceMember createMember(Long id, Workspace workspace, User user, WorkspaceRole role) {
        return WorkspaceMember.builder()
            .id(id)
            .workspace(workspace)
            .user(user)
            .role(role)
            .build();
    }

    private Endpoint createEndpoint(Long id, Project project) {
        return Endpoint.builder()
            .id(id)
            .project(project)
            .url("https://api.example.com/health")
            .httpMethod(HttpMethod.GET)
            .headers(Map.of("Authorization", "Bearer token"))
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

        given(projectService.getProjectWithMemberCheck(1L)).willReturn(project);

        CreateEndpointRequest request = new CreateEndpointRequest(
            "https://api.example.com/health", HttpMethod.GET, Map.of("Authorization", "Bearer token"), null, 200, 60
        );

        Endpoint saved = createEndpoint(1L, project);
        given(endpointRepository.save(any(Endpoint.class))).willReturn(saved);

        // when
        EndpointResponse response = endpointService.createEndpoint(1L, request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.url()).isEqualTo("https://api.example.com/health");
        assertThat(response.httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(response.headers()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    @DisplayName("워크스페이스 엔드포인트 생성 시 체크 주기 기본값은 플랜 최소 주기를 따른다")
    void createEndpoint_workspaceDefaultInterval_usesPlanMinimum() {
        // given
        User owner = createUser(1L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);
        given(projectService.getProjectWithMemberCheck(1L)).willReturn(project);
        given(subscriptionService.getPolicyForWorkspace(1L)).willReturn(new FreePlanLimitPolicy());
        given(endpointRepository.save(any(Endpoint.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        CreateEndpointRequest request = new CreateEndpointRequest(
            "https://api.example.com/health", HttpMethod.GET, null, null, 200, null
        );

        // when
        EndpointResponse response = endpointService.createEndpoint(1L, request);

        // then
        assertThat(response.checkInterval()).isEqualTo(300);
    }

    @Test
    @DisplayName("워크스페이스 VIEWER도 엔드포인트 조회 가능")
    void getEndpoint_workspaceViewer_canRead() {
        // given
        User owner = createUser(1L);
        User viewer = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(viewer);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, viewer, WorkspaceRole.VIEWER)));

        // when
        EndpointResponse response = endpointService.getEndpoint(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("워크스페이스 VIEWER는 엔드포인트 수정 불가")
    void updateEndpoint_workspaceViewer_throwsForbiddenException() {
        // given
        User owner = createUser(1L);
        User viewer = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(viewer);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, viewer, WorkspaceRole.VIEWER)));

        UpdateEndpointRequest request = new UpdateEndpointRequest(
            "https://api.example.com/new-health", null, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> endpointService.updateEndpoint(1L, request))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("VIEWER는 쓰기 작업을 수행할 수 없습니다.");
    }

    @Test
    @DisplayName("워크스페이스 MEMBER는 엔드포인트 삭제 불가")
    void deleteEndpoint_workspaceMember_throwsForbiddenException() {
        // given
        User owner = createUser(1L);
        User member = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(member);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, member, WorkspaceRole.MEMBER)));

        // when & then
        assertThatThrownBy(() -> endpointService.deleteEndpoint(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("엔드포인트 삭제는 ADMIN 이상만 가능합니다.");
    }

    @Test
    @DisplayName("워크스페이스 ADMIN은 엔드포인트 삭제 가능")
    void deleteEndpoint_workspaceAdmin_success() {
        // given
        User owner = createUser(1L);
        User admin = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(admin);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, admin, WorkspaceRole.ADMIN)));

        // when
        endpointService.deleteEndpoint(1L);

        // then
        assertThat(endpoint.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("프로젝트 내 엔드포인트 목록 조회")
    void getEndpoints_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);

        given(projectService.getProjectWithAccessCheck(1L)).willReturn(project);

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
    void getEndpoint_otherUser_throwsForbiddenException() {
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
    void getEndpoint_missingEndpoint_throwsEndpointNotFoundException() {
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

    @Test
    @DisplayName("엔드포인트 수정 시 빈 headers를 전달하면 기존 headers가 비워진다")
    void updateEndpoint_clearHeaders() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = createEndpoint(1L, project);

        given(userService.getUserDetail()).willReturn(user);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));

        UpdateEndpointRequest request = new UpdateEndpointRequest(
            null, null, Map.of(), null, null, null
        );

        // when
        EndpointResponse response = endpointService.updateEndpoint(1L, request);

        // then
        assertThat(response.headers()).isEmpty();
    }

    @Test
    @DisplayName("엔드포인트 수정 시 headers와 body에 null을 전달하면 기존 값이 비워진다")
    void updateEndpoint_clearNullableRequestFields() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);
        Endpoint endpoint = Endpoint.builder()
            .id(1L)
            .project(project)
            .url("https://api.example.com/health")
            .httpMethod(HttpMethod.POST)
            .headers(Map.of("Authorization", "Bearer token"))
            .body("{\"enabled\":true}")
            .expectedStatusCode(200)
            .checkInterval(60)
            .isActive(true)
            .build();

        given(userService.getUserDetail()).willReturn(user);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));

        UpdateEndpointRequest request = new UpdateEndpointRequest(
            "https://api.example.com/health", HttpMethod.POST, null, null, 200, 60
        );

        // when
        EndpointResponse response = endpointService.updateEndpoint(1L, request);

        // then
        assertThat(response.headers()).isNull();
        assertThat(response.body()).isNull();
    }

    @Test
    @DisplayName("삭제된 프로젝트의 엔드포인트 접근 시 404 예외")
    void getEndpoint_deletedProject_throwsProjectNotFoundException() {
        // given
        User user = createUser(1L);
        Project deletedProject = createProject(1L, user);
        deletedProject.softDelete();
        Endpoint endpoint = createEndpoint(1L, deletedProject);

        given(userService.getUserDetail()).willReturn(user);
        given(endpointRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(endpoint));

        // when & then
        assertThatThrownBy(() -> endpointService.getEndpoint(1L))
            .isInstanceOf(ProjectNotFoundException.class)
            .hasMessage("프로젝트를 찾을 수 없습니다.");
    }
}
