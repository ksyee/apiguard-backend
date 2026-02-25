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
    public ApiResponse<WorkspaceResponse> createWorkspace(@RequestBody @Valid CreateWorkspaceRequest request) {
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

    @GetMapping("/{workspaceId}/members")
    public ApiResponse<List<WorkspaceMemberResponse>> getMembers(@PathVariable Long workspaceId) {
        return ApiResponse.ok(workspaceService.getMembers(workspaceId));
    }

    @PostMapping("/{workspaceId}/members/invite")
    public ApiResponse<WorkspaceMemberResponse> inviteMember(
        @PathVariable Long workspaceId,
        @RequestBody @Valid InviteMemberRequest request
    ) {
        return ApiResponse.ok(workspaceService.inviteMember(workspaceId, request));
    }

    @PutMapping("/{workspaceId}/members/{memberId}/role")
    public ApiResponse<WorkspaceMemberResponse> updateMemberRole(
        @PathVariable Long workspaceId,
        @PathVariable Long memberId,
        @RequestBody @Valid UpdateMemberRoleRequest request
    ) {
        return ApiResponse.ok(workspaceService.updateMemberRole(workspaceId, memberId, request));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ApiResponse<Void> removeMember(
        @PathVariable Long workspaceId,
        @PathVariable Long memberId
    ) {
        workspaceService.removeMember(workspaceId, memberId);
        return ApiResponse.ok();
    }
}
