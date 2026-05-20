package com.apiguard.backend.domain.workspace.service;

import com.apiguard.backend.domain.subscription.service.SubscriptionService;
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
import com.apiguard.backend.global.exception.UserNotFoundException;
import com.apiguard.backend.global.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    // UserService와 순환 참조가 있어 예외적으로 지연 필드 주입을 사용한다.
    @Autowired
    @Lazy
    private UserService userService;

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        User user = userService.getUserDetail();
        String slug = generateUniqueSlug(request.name(), user.getId());

        try {
            Workspace workspace = Workspace.builder()
                .name(request.name())
                .slug(slug)
                .owner(user)
                .build();

            Workspace saved = workspaceRepository.save(workspace);

            WorkspaceMember member = WorkspaceMember.builder()
                .workspace(saved)
                .user(user)
                .role(WorkspaceRole.OWNER)
                .build();
            workspaceMemberRepository.save(member);

            subscriptionService.createDefaultSubscription(saved);

            return WorkspaceResponse.from(saved, WorkspaceRole.OWNER);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("동일한 이름의 워크스페이스가 이미 존재합니다.");
        }
    }

    @Transactional
    public WorkspaceResponse createPersonalWorkspace(User savedUser) {
        String workspaceName = savedUser.getNickname() + "의 워크스페이스";
        String slug = generateUniqueSlug(savedUser.getNickname(), savedUser.getId());

        try {
            Workspace workspace = Workspace.builder()
                .name(workspaceName)
                .slug(slug)
                .owner(savedUser)
                .build();

            Workspace saved = workspaceRepository.save(workspace);

            WorkspaceMember member = WorkspaceMember.builder()
                .workspace(saved)
                .user(savedUser)
                .role(WorkspaceRole.OWNER)
                .build();
            workspaceMemberRepository.save(member);

            subscriptionService.createDefaultSubscription(saved);

            return WorkspaceResponse.from(saved, WorkspaceRole.OWNER);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("개인 워크스페이스 생성 중 충돌이 발생했습니다. 다시 시도해 주세요.");
        }
    }

    public List<WorkspaceResponse> getMyWorkspaces() {
        User user = userService.getUserDetail();
        return workspaceMemberRepository.findAllByUserId(user.getId()).stream()
            .filter(m -> !m.getWorkspace().isDeleted())
            .map(m -> WorkspaceResponse.from(m.getWorkspace(), m.getRole()))
            .toList();
    }

    public WorkspaceResponse getWorkspace(Long workspaceId) {
        User user = userService.getUserDetail();
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        return WorkspaceResponse.from(workspace, member.getRole());
    }

    @Transactional
    public void deleteWorkspace(Long workspaceId) {
        User user = userService.getUserDetail();
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("워크스페이스 삭제는 OWNER만 가능합니다.");
        }

        workspace.softDelete();
    }

    @Transactional
    public WorkspaceMemberResponse inviteMember(Long workspaceId, InviteMemberRequest request) {
        User currentUser = userService.getUserDetail();

        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (!currentMember.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new ForbiddenException("멤버 초대는 ADMIN 이상만 가능합니다.");
        }

        subscriptionService.validateMemberCount(workspaceId);

        User invitedUser = userRepository.findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, invitedUser.getId()).isPresent()) {
            throw new IllegalArgumentException("이미 워크스페이스의 멤버입니다.");
        }

        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceRole inviteRole = request.role() != null ? request.role() : WorkspaceRole.MEMBER;
        if (inviteRole == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("OWNER 역할은 초대 시 지정할 수 없습니다.");
        }
        if (inviteRole == WorkspaceRole.ADMIN && currentMember.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("ADMIN 초대는 OWNER만 가능합니다.");
        }

        WorkspaceMember newMember = WorkspaceMember.builder()
            .workspace(workspace)
            .user(invitedUser)
            .role(inviteRole)
            .build();

        WorkspaceMember saved = workspaceMemberRepository.save(newMember);
        return WorkspaceMemberResponse.from(saved);
    }

    public List<WorkspaceMemberResponse> getMembers(Long workspaceId) {
        User user = userService.getUserDetail();
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        return workspaceMemberRepository.findByWorkspaceIdAndDeletedFalse(workspaceId).stream()
            .map(WorkspaceMemberResponse::from)
            .toList();
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long userId, UpdateMemberRoleRequest request) {
        User currentUser = userService.getUserDetail();

        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (currentMember.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("역할 변경은 OWNER만 가능합니다.");
        }

        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("자기 자신의 역할은 변경할 수 없습니다.");
        }

        if (request.role() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("OWNER 역할은 역할 변경으로 지정할 수 없습니다.");
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow(() -> new UserNotFoundException("해당 멤버를 찾을 수 없습니다."));

        targetMember.updateRole(request.role());
        return WorkspaceMemberResponse.from(targetMember);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long userId) {
        User currentUser = userService.getUserDetail();

        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (currentMember.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("멤버 제거는 OWNER만 가능합니다.");
        }

        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("OWNER 본인은 제거할 수 없습니다.");
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow(() -> new UserNotFoundException("해당 멤버를 찾을 수 없습니다."));

        workspaceMemberRepository.delete(targetMember);
    }

    public Workspace getWorkspaceWithMemberCheck(Long workspaceId) {
        User user = userService.getUserDetail();
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        return workspace;
    }

    public void checkWritePermission(Long workspaceId) {
        User user = userService.getUserDetail();
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (!member.getRole().isAtLeast(WorkspaceRole.MEMBER)) {
            throw new ForbiddenException("VIEWER는 쓰기 작업을 수행할 수 없습니다.");
        }
    }

    public WorkspaceRole getMemberRole(Long workspaceId) {
        User user = userService.getUserDetail();
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));
        return member.getRole();
    }

    private String generateUniqueSlug(String name, Long userId) {
        String base = name.toLowerCase()
            .replaceAll("[^a-z0-9가-힣\\s]", "")
            .trim()
            .replaceAll("\\s+", "-");
        if (base.isEmpty()) {
            base = "workspace";
        }

        String prefix = base + "-" + userId;
        String candidate = prefix;
        int suffix = 2;

        while (workspaceRepository.existsBySlug(candidate)) {
            candidate = prefix + "-" + suffix;
            suffix++;
        }

        return candidate;
    }
}
