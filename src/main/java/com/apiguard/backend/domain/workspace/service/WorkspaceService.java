package com.apiguard.backend.domain.workspace.service;

import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.domain.workspace.dto.*;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.domain.workspace.repository.WorkspaceRepository;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.UserNotFoundException;
import com.apiguard.backend.global.exception.WorkspaceNotFoundException;
import com.apiguard.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        User currentUser = userService.getUserDetail();
        String slug = generateUniqueSlug(request.name());

        Workspace workspace = Workspace.builder()
            .name(request.name())
            .slug(slug)
            .build();
        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
            .workspace(saved)
            .user(currentUser)
            .role(WorkspaceRole.OWNER)
            .build();
        workspaceMemberRepository.save(ownerMember);

        return WorkspaceResponse.from(saved);
    }

    public List<WorkspaceResponse> getMyWorkspaces() {
        User currentUser = userService.getUserDetail();
        return workspaceMemberRepository.findByUserIdWithWorkspace(currentUser.getId()).stream()
            .map(member -> WorkspaceResponse.from(member.getWorkspace()))
            .toList();
    }

    public WorkspaceResponse getWorkspace(Long id) {
        Workspace workspace = getWorkspaceWithMemberCheck(id);
        return WorkspaceResponse.from(workspace);
    }

    @Transactional
    public void deleteWorkspace(Long id) {
        User currentUser = userService.getUserDetail();
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(id, currentUser.getId())
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("워크스페이스 삭제는 owner만 가능합니다.");
        }

        workspace.softDelete();
    }

    public List<WorkspaceMemberResponse> getMembers(Long workspaceId) {
        getWorkspaceWithMemberCheck(workspaceId);
        return workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
            .map(WorkspaceMemberResponse::from)
            .toList();
    }

    @Transactional
    public WorkspaceMemberResponse inviteMember(Long workspaceId, InviteMemberRequest request) {
        User currentUser = userService.getUserDetail();
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        checkAdminOrAbove(workspaceId, currentUser.getId());

        User targetUser = userRepository.findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new UserNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다."));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUser.getId())) {
            throw new IllegalArgumentException("이미 워크스페이스 멤버입니다.");
        }

        WorkspaceRole role = WorkspaceRole.valueOf(request.role().toUpperCase());
        WorkspaceMember newMember = WorkspaceMember.builder()
            .workspace(workspace)
            .user(targetUser)
            .role(role)
            .build();
        workspaceMemberRepository.save(newMember);

        return WorkspaceMemberResponse.from(newMember);
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long memberId, UpdateMemberRoleRequest request) {
        User currentUser = userService.getUserDetail();
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        checkAdminOrAbove(workspaceId, currentUser.getId());

        WorkspaceMember targetMember = workspaceMemberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
            .orElseThrow(() -> new ForbiddenException("해당 멤버를 찾을 수 없습니다."));

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("owner의 역할은 변경할 수 없습니다.");
        }

        WorkspaceRole newRole = WorkspaceRole.valueOf(request.role().toUpperCase());
        targetMember.updateRole(newRole);

        return WorkspaceMemberResponse.from(targetMember);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long memberId) {
        User currentUser = userService.getUserDetail();
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        checkAdminOrAbove(workspaceId, currentUser.getId());

        WorkspaceMember targetMember = workspaceMemberRepository.findByIdAndWorkspaceId(memberId, workspaceId)
            .orElseThrow(() -> new ForbiddenException("해당 멤버를 찾을 수 없습니다."));

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("owner는 제거할 수 없습니다.");
        }

        workspaceMemberRepository.delete(targetMember);
    }

    // --- private helpers ---

    private Workspace getWorkspaceWithMemberCheck(Long workspaceId) {
        User currentUser = userService.getUserDetail();
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다.");
        }

        return workspace;
    }

    private void checkAdminOrAbove(Long workspaceId, Long userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow(() -> new ForbiddenException("해당 워크스페이스에 대한 권한이 없습니다."));

        if (member.getRole() == WorkspaceRole.MEMBER || member.getRole() == WorkspaceRole.VIEWER) {
            throw new ForbiddenException("admin 이상의 권한이 필요합니다.");
        }
    }

    private String generateUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .toLowerCase()
            .trim()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");

        if (base.isEmpty()) {
            base = "workspace";
        }

        String slug = base;
        int suffix = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}
