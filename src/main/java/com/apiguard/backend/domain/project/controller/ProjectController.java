package com.apiguard.backend.domain.project.controller;

import com.apiguard.backend.domain.project.dto.CreateProjectRequest;
import com.apiguard.backend.domain.project.dto.ProjectResponse;
import com.apiguard.backend.domain.project.dto.UpdateProjectRequest;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/workspaces/{workspaceId}/projects")
    public ApiResponse<ProjectResponse> createProject(
        @PathVariable Long workspaceId,
        @RequestBody @Valid CreateProjectRequest request
    ) {
        return ApiResponse.ok(projectService.createProject(workspaceId, request));
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public ApiResponse<List<ProjectResponse>> getProjects(@PathVariable Long workspaceId) {
        return ApiResponse.ok(projectService.getProjects(workspaceId));
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProject(id));
    }

    @PatchMapping("/projects/{id}")
    public ApiResponse<ProjectResponse> updateProject(
        @PathVariable Long id,
        @RequestBody @Valid UpdateProjectRequest request
    ) {
        return ApiResponse.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.ok();
    }
}
