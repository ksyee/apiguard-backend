package com.apiguard.backend.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.project.dto.CreateProjectRequest;
import com.apiguard.backend.domain.project.dto.ProjectResponse;
import com.apiguard.backend.domain.project.dto.UpdateProjectRequest;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.repository.ProjectRepository;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.domain.workspace.repository.WorkspaceRepository;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.ProjectNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private ProjectService projectService;

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

    @Test
    @DisplayName("프로젝트 생성 성공")
    void createProject_success() {
        // given
        User user = createUser(1L);
        given(userService.getUserDetail()).willReturn(user);

        CreateProjectRequest request = new CreateProjectRequest("My Project", "My Description");
        Project saved = Project.builder()
            .id(1L)
            .user(user)
            .name(request.name())
            .description(request.description())
            .build();
        given(projectRepository.save(any(Project.class))).willReturn(saved);

        // when
        ProjectResponse response = projectService.createProject(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("My Project");
        assertThat(response.description()).isEqualTo("My Description");
    }

    @Test
    @DisplayName("워크스페이스 프로젝트 생성 시 플랜의 프로젝트 수 제한을 검증한다")
    void createWorkspaceProject_validatesProjectLimit() {
        // given
        User owner = createUser(1L);
        Workspace workspace = createWorkspace(1L, owner);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(userService.getUserDetail()).willReturn(owner);

        CreateProjectRequest request = new CreateProjectRequest("Workspace Project", "Description");
        Project saved = createWorkspaceProject(1L, owner, workspace);
        given(projectRepository.save(any(Project.class))).willReturn(saved);

        // when
        projectService.createProject(1L, request);

        // then
        verify(workspaceService).checkWritePermission(1L);
        verify(subscriptionService).validateProjectCount(1L);
    }

    @Test
    @DisplayName("워크스페이스 VIEWER도 프로젝트 조회 가능")
    void getProject_workspaceViewer_canRead() {
        // given
        User owner = createUser(1L);
        User viewer = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);

        given(userService.getUserDetail()).willReturn(viewer);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, viewer, WorkspaceRole.VIEWER)));

        // when
        ProjectResponse response = projectService.getProject(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("워크스페이스 VIEWER는 프로젝트 수정 불가")
    void updateProject_workspaceViewer_throwsForbiddenException() {
        // given
        User owner = createUser(1L);
        User viewer = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);

        given(userService.getUserDetail()).willReturn(viewer);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, viewer, WorkspaceRole.VIEWER)));

        // when & then
        assertThatThrownBy(() -> projectService.updateProject(1L, new UpdateProjectRequest("New Name", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("VIEWER는 쓰기 작업을 수행할 수 없습니다.");
    }

    @Test
    @DisplayName("워크스페이스 MEMBER는 프로젝트 삭제 불가")
    void deleteProject_workspaceMember_throwsForbiddenException() {
        // given
        User owner = createUser(1L);
        User member = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);

        given(userService.getUserDetail()).willReturn(member);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, member, WorkspaceRole.MEMBER)));

        // when & then
        assertThatThrownBy(() -> projectService.deleteProject(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("프로젝트 삭제는 ADMIN 이상만 가능합니다.");
    }

    @Test
    @DisplayName("워크스페이스 ADMIN은 프로젝트 삭제 가능")
    void deleteProject_workspaceAdmin_success() {
        // given
        User owner = createUser(1L);
        User admin = createUser(2L);
        Workspace workspace = createWorkspace(1L, owner);
        Project project = createWorkspaceProject(1L, owner, workspace);

        given(userService.getUserDetail()).willReturn(admin);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(1L, workspace, admin, WorkspaceRole.ADMIN)));

        // when
        projectService.deleteProject(1L);

        // then
        assertThat(project.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("내 프로젝트 목록 조회")
    void getMyProjects_success() {
        // given
        User user = createUser(1L);
        given(userService.getUserDetail()).willReturn(user);

        List<Project> projects = List.of(
            createProject(1L, user),
            createProject(2L, user)
        );
        given(projectRepository.findByUserIdAndDeletedFalse(1L)).willReturn(projects);

        // when
        List<ProjectResponse> result = projectService.getMyProjects();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("다른 사용자 프로젝트 접근 시 403 예외")
    void getProject_otherUser_throwsForbiddenException() {
        // given
        User owner = createUser(1L);
        User other = createUser(2L);
        Project project = createProject(1L, owner);

        given(userService.getUserDetail()).willReturn(other);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() -> projectService.getProject(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("해당 프로젝트에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 조회 시 404 예외")
    void getProject_missingProject_throwsProjectNotFoundException() {
        // given
        User user = createUser(1L);
        given(userService.getUserDetail()).willReturn(user);
        given(projectRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> projectService.getProject(999L))
            .isInstanceOf(ProjectNotFoundException.class)
            .hasMessage("프로젝트를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("프로젝트 삭제 성공")
    void deleteProject_success() {
        // given
        User user = createUser(1L);
        Project project = createProject(1L, user);

        given(userService.getUserDetail()).willReturn(user);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(project));

        // when
        projectService.deleteProject(1L);

        // then
        assertThat(project.isDeleted()).isTrue();
        assertThat(project.getDeletedAt()).isNotNull();
    }
}
