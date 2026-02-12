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
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
        return ApiResponse.ok(projectService.createProject(request));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> getMyProjects() {
        return ApiResponse.ok(projectService.getMyProjects());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> getProject(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProject(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> updateProject(
        @PathVariable Long id,
        @RequestBody @Valid UpdateProjectRequest request
    ) {
        return ApiResponse.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.ok();
    }
}
