package com.apiguard.backend.domain.workspace.controller;

import com.apiguard.backend.domain.workspace.dto.*;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ApiResponse<WorkspaceResponse> createWorkspace(
        @RequestBody @Valid CreateWorkspaceRequest request
    ) {
        return ApiResponse.ok(workspaceService.createWorkspace(request));
    }

    @GetMapping
    public ApiResponse<List<WorkspaceResponse>> getMyWorkspaces() {
        return ApiResponse.ok(workspaceService.getMyWorkspaces());
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkspaceResponse> getWorkspace(@PathVariable Long id) {
        return ApiResponse.ok(workspaceService.getWorkspace(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/members")
    public ApiResponse<WorkspaceMemberResponse> inviteMember(
        @PathVariable Long id,
        @RequestBody @Valid InviteMemberRequest request
    ) {
        return ApiResponse.ok(workspaceService.inviteMember(id, request));
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<WorkspaceMemberResponse>> getMembers(@PathVariable Long id) {
        return ApiResponse.ok(workspaceService.getMembers(id));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ApiResponse<WorkspaceMemberResponse> updateMemberRole(
        @PathVariable Long id,
        @PathVariable Long userId,
        @RequestBody @Valid UpdateMemberRoleRequest request
    ) {
        return ApiResponse.ok(workspaceService.updateMemberRole(id, userId, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(
        @PathVariable Long id,
        @PathVariable Long userId
    ) {
        workspaceService.removeMember(id, userId);
        return ApiResponse.ok();
    }
}
