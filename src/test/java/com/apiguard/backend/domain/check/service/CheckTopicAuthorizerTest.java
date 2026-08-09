package com.apiguard.backend.domain.check.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.repository.ProjectRepository;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.UnauthorizedException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckTopicAuthorizerTest {

    private static final String EMAIL = "test@email.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private CheckTopicAuthorizer checkTopicAuthorizer;

    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email(EMAIL)
            .password("encodedPassword")
            .nickname("tester")
            .role(Role.USER)
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
            .build();
    }

    private Project createPersonalProject(Long id, User user) {
        return Project.builder()
            .id(id)
            .user(user)
            .name("Personal Project")
            .build();
    }

    private Endpoint createEndpoint(Long id, Project project) {
        return Endpoint.builder()
            .id(id)
            .project(project)
            .build();
    }

    private WorkspaceMember createMember(Workspace workspace, User user) {
        return WorkspaceMember.builder()
            .id(1L)
            .workspace(workspace)
            .user(user)
            .role(WorkspaceRole.MEMBER)
            .build();
    }

    @Test
    @DisplayName("엔드포인트 토픽: 워크스페이스 멤버는 구독할 수 있다")
    void endpointTopic_workspaceMember_allowed() {
        User user = createUser(1L);
        Workspace workspace = createWorkspace(10L, user);
        Project project = createWorkspaceProject(20L, user, workspace);
        Endpoint endpoint = createEndpoint(30L, project);

        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(user));
        given(endpointRepository.findByIdAndDeletedFalse(30L)).willReturn(Optional.of(endpoint));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
            .willReturn(Optional.of(createMember(workspace, user)));

        assertThatCode(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/endpoints/30/checks"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("엔드포인트 토픽: 워크스페이스 비멤버는 구독할 수 없다")
    void endpointTopic_nonMember_forbidden() {
        User user = createUser(1L);
        User owner = createUser(2L);
        Workspace workspace = createWorkspace(10L, owner);
        Project project = createWorkspaceProject(20L, owner, workspace);
        Endpoint endpoint = createEndpoint(30L, project);

        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(user));
        given(endpointRepository.findByIdAndDeletedFalse(30L)).willReturn(Optional.of(endpoint));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/endpoints/30/checks"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("엔드포인트 토픽: 존재하지 않는 엔드포인트는 권한 오류로 거부한다")
    void endpointTopic_notFound_forbidden() {
        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(createUser(1L)));
        given(endpointRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/endpoints/999/checks"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("프로젝트 토픽: 워크스페이스 멤버는 구독할 수 있다")
    void projectTopic_workspaceMember_allowed() {
        User user = createUser(1L);
        Workspace workspace = createWorkspace(10L, user);
        Project project = createWorkspaceProject(20L, user, workspace);

        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(user));
        given(projectRepository.findByIdAndDeletedFalse(20L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
            .willReturn(Optional.of(createMember(workspace, user)));

        assertThatCode(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/projects/20/checks"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("프로젝트 토픽: 워크스페이스 비멤버는 구독할 수 없다")
    void projectTopic_nonMember_forbidden() {
        User user = createUser(1L);
        User owner = createUser(2L);
        Workspace workspace = createWorkspace(10L, owner);
        Project project = createWorkspaceProject(20L, owner, workspace);

        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(user));
        given(projectRepository.findByIdAndDeletedFalse(20L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/projects/20/checks"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("개인 프로젝트 토픽: 소유자는 구독할 수 있다")
    void projectTopic_personalOwner_allowed() {
        User user = createUser(1L);
        Project project = createPersonalProject(20L, user);

        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(user));
        given(projectRepository.findByIdAndDeletedFalse(20L)).willReturn(Optional.of(project));

        assertThatCode(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/projects/20/checks"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("개인 프로젝트 토픽: 소유자가 아니면 구독할 수 없다")
    void projectTopic_personalNonOwner_forbidden() {
        User user = createUser(1L);
        Project project = createPersonalProject(20L, createUser(2L));

        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(user));
        given(projectRepository.findByIdAndDeletedFalse(20L)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/projects/20/checks"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("허용 목록에 없는 destination은 기본 거부한다")
    void unknownDestination_forbidden() {
        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.of(createUser(1L)));

        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/admin/secrets"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("destination이 null이면 거부한다")
    void nullDestination_forbidden() {
        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, null))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 인증 오류로 거부한다")
    void unknownUser_unauthorized() {
        given(userRepository.findByEmailAndDeletedFalse(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checkTopicAuthorizer.authorizeSubscription(EMAIL, "/topic/projects/20/checks"))
            .isInstanceOf(UnauthorizedException.class);
    }
}
