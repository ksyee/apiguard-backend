package com.apiguard.backend.domain.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.apiguard.backend.domain.workspace.dto.InviteMemberRequest;
import com.apiguard.backend.domain.workspace.dto.UpdateMemberRoleRequest;
import com.apiguard.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.apiguard.backend.domain.workspace.dto.WorkspaceResponse;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.domain.workspace.repository.WorkspaceRepository;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.WorkspaceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        // @Lazy 필드는 @InjectMocks 생성자 주입 후 자동 주입되지 않으므로 직접 주입
        ReflectionTestUtils.setField(workspaceService, "userService", userService);
    }

    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email("user" + id + "@email.com")
            .password("encodedPassword")
            .nickname("tester" + id)
            .role(Role.USER)
            .build();
    }

    private Workspace createWorkspace(Long id) {
        return Workspace.builder()
            .id(id)
            .name("My Workspace")
            .slug("my-workspace")
            .owner(createUser(1L))
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

    // -------------------------------------------------------------------------
    // createWorkspace
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("워크스페이스 생성 성공 — 생성자가 OWNER로 멤버에 추가됨")
    void createWorkspace_success() {
        // given
        User user = createUser(1L);
        given(userService.getUserDetail()).willReturn(user);

        Workspace saved = createWorkspace(1L);
        given(workspaceRepository.save(any(Workspace.class))).willReturn(saved);
        given(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        WorkspaceResponse response = workspaceService.createWorkspace(
            new CreateWorkspaceRequest("My Workspace")
        );

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("My Workspace");
        assertThat(response.role()).isEqualTo(WorkspaceRole.OWNER);
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
        verify(subscriptionService).createDefaultSubscription(saved);
    }

    // -------------------------------------------------------------------------
    // getMyWorkspaces
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("내 워크스페이스 목록 조회 성공")
    void getMyWorkspaces_success() {
        // given
        User user = createUser(1L);
        Workspace ws1 = createWorkspace(1L);
        Workspace ws2 = createWorkspace(2L);

        given(userService.getUserDetail()).willReturn(user);
        given(workspaceMemberRepository.findAllByUserId(1L)).willReturn(List.of(
            createMember(1L, ws1, user, WorkspaceRole.OWNER),
            createMember(2L, ws2, user, WorkspaceRole.MEMBER)
        ));

        // when
        List<WorkspaceResponse> result = workspaceService.getMyWorkspaces();

        // then
        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // getWorkspace
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("워크스페이스 상세 조회 성공")
    void getWorkspace_success() {
        // given
        User user = createUser(1L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember ownerMember = createMember(1L, workspace, user, WorkspaceRole.OWNER);

        given(userService.getUserDetail()).willReturn(user);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(ownerMember));

        // when
        WorkspaceResponse response = workspaceService.getWorkspace(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.slug()).isEqualTo("my-workspace");
        assertThat(response.role()).isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    @DisplayName("워크스페이스 멤버가 아닌 경우 조회 시 403 예외")
    void getWorkspace_notMember_throwsForbiddenException() {
        // given
        User user = createUser(1L);
        Workspace workspace = createWorkspace(1L);

        given(userService.getUserDetail()).willReturn(user);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workspaceService.getWorkspace(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("해당 워크스페이스에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 워크스페이스 조회 시 404 예외")
    void getWorkspace_missingWorkspace_throwsWorkspaceNotFoundException() {
        // given
        User user = createUser(1L);

        given(userService.getUserDetail()).willReturn(user);
        given(workspaceRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workspaceService.getWorkspace(999L))
            .isInstanceOf(WorkspaceNotFoundException.class)
            .hasMessage("워크스페이스를 찾을 수 없습니다.");
    }

    // -------------------------------------------------------------------------
    // deleteWorkspace
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("워크스페이스 삭제 성공 (owner)")
    void deleteWorkspace_success() {
        // given
        User user = createUser(1L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember ownerMember = createMember(1L, workspace, user, WorkspaceRole.OWNER);

        given(userService.getUserDetail()).willReturn(user);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(ownerMember));

        // when
        workspaceService.deleteWorkspace(1L);

        // then
        assertThat(workspace.isDeleted()).isTrue();
        assertThat(workspace.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("OWNER가 아닌 경우 워크스페이스 삭제 시 403 예외")
    void deleteWorkspace_notOwner_throwsForbiddenException() {
        // given
        User user = createUser(1L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember memberRole = createMember(1L, workspace, user, WorkspaceRole.MEMBER);

        given(userService.getUserDetail()).willReturn(user);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(memberRole));

        // when & then
        assertThatThrownBy(() -> workspaceService.deleteWorkspace(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("워크스페이스 삭제는 OWNER만 가능합니다.");
    }

    // -------------------------------------------------------------------------
    // getMembers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("멤버 목록 조회 성공")
    void getMembers_success() {
        // given
        User user1 = createUser(1L);
        User user2 = createUser(2L);
        Workspace workspace = createWorkspace(1L);

        given(userService.getUserDetail()).willReturn(user1);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(createMember(1L, workspace, user1, WorkspaceRole.OWNER)));
        given(workspaceMemberRepository.findByWorkspaceIdAndDeletedFalse(1L)).willReturn(List.of(
            createMember(1L, workspace, user1, WorkspaceRole.OWNER),
            createMember(2L, workspace, user2, WorkspaceRole.MEMBER)
        ));

        // when
        List<WorkspaceMemberResponse> result = workspaceService.getMembers(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(result.get(1).role()).isEqualTo(WorkspaceRole.MEMBER);
    }

    // -------------------------------------------------------------------------
    // inviteMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("멤버 초대 성공")
    void inviteMember_success() {
        // given
        User currentUser = createUser(1L);
        User targetUser = createUser(2L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember ownerMember = createMember(1L, workspace, currentUser, WorkspaceRole.OWNER);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(ownerMember));
        given(userRepository.findByEmailAndDeletedFalse("user2@email.com"))
            .willReturn(Optional.of(targetUser));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.empty());
        given(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        WorkspaceMemberResponse response = workspaceService.inviteMember(1L,
            new InviteMemberRequest("user2@email.com"));

        // then
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo(WorkspaceRole.MEMBER);
    }

    @Test
    @DisplayName("이미 멤버인 사용자 초대 시 400 예외")
    void inviteMember_alreadyMember_throwsIllegalArgumentException() {
        // given
        User currentUser = createUser(1L);
        User targetUser = createUser(2L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember ownerMember = createMember(1L, workspace, currentUser, WorkspaceRole.OWNER);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(ownerMember));
        given(userRepository.findByEmailAndDeletedFalse("user2@email.com"))
            .willReturn(Optional.of(targetUser));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(createMember(2L, workspace, targetUser, WorkspaceRole.MEMBER)));

        // when & then
        assertThatThrownBy(() -> workspaceService.inviteMember(1L,
            new InviteMemberRequest("user2@email.com")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 워크스페이스의 멤버입니다.");
    }

    @Test
    @DisplayName("ADMIN 미만 권한으로 멤버 초대 시 403 예외")
    void inviteMember_notAdminOrAbove_throwsForbiddenException() {
        // given
        User currentUser = createUser(1L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember viewerMember = createMember(1L, workspace, currentUser, WorkspaceRole.VIEWER);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(viewerMember));

        // when & then
        assertThatThrownBy(() -> workspaceService.inviteMember(1L,
            new InviteMemberRequest("user2@email.com")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("멤버 초대는 ADMIN 이상만 가능합니다.");
    }

    // -------------------------------------------------------------------------
    // updateMemberRole
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("멤버 역할 변경 성공")
    void updateMemberRole_success() {
        // given
        User currentUser = createUser(1L);
        User targetUser = createUser(2L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember ownerMember = createMember(1L, workspace, currentUser, WorkspaceRole.OWNER);
        WorkspaceMember targetMember = createMember(2L, workspace, targetUser, WorkspaceRole.MEMBER);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(ownerMember));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(targetMember));

        // when
        WorkspaceMemberResponse response = workspaceService.updateMemberRole(1L, 2L,
            new UpdateMemberRoleRequest(WorkspaceRole.ADMIN));

        // then
        assertThat(response.role()).isEqualTo(WorkspaceRole.ADMIN);
    }

    @Test
    @DisplayName("OWNER가 아닌 경우 역할 변경 시도 시 403 예외")
    void updateMemberRole_notOwner_throwsForbiddenException() {
        // given
        User currentUser = createUser(1L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember adminMember = createMember(1L, workspace, currentUser, WorkspaceRole.ADMIN);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(adminMember));

        // when & then
        assertThatThrownBy(() -> workspaceService.updateMemberRole(1L, 2L,
            new UpdateMemberRoleRequest(WorkspaceRole.MEMBER)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("역할 변경은 OWNER만 가능합니다.");
    }

    // -------------------------------------------------------------------------
    // removeMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("멤버 제거 성공")
    void removeMember_success() {
        // given
        User currentUser = createUser(1L);
        User targetUser = createUser(2L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember ownerMember = createMember(1L, workspace, currentUser, WorkspaceRole.OWNER);
        WorkspaceMember targetMember = createMember(2L, workspace, targetUser, WorkspaceRole.MEMBER);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(ownerMember));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L))
            .willReturn(Optional.of(targetMember));

        // when
        workspaceService.removeMember(1L, 2L);

        // then
        verify(workspaceMemberRepository).delete(targetMember);
    }

    @Test
    @DisplayName("OWNER가 아닌 경우 멤버 제거 시도 시 403 예외")
    void removeMember_notOwner_throwsForbiddenException() {
        // given
        User currentUser = createUser(1L);
        Workspace workspace = createWorkspace(1L);
        WorkspaceMember adminMember = createMember(1L, workspace, currentUser, WorkspaceRole.ADMIN);

        given(userService.getUserDetail()).willReturn(currentUser);
        given(workspaceRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 1L))
            .willReturn(Optional.of(adminMember));

        // when & then
        assertThatThrownBy(() -> workspaceService.removeMember(1L, 2L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("멤버 제거는 OWNER만 가능합니다.");
    }
}
