package com.apiguard.backend.domain.project.service;

import com.apiguard.backend.domain.project.dto.CreateProjectRequest;
import com.apiguard.backend.domain.project.dto.ProjectResponse;
import com.apiguard.backend.domain.project.dto.UpdateProjectRequest;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.repository.ProjectRepository;
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
import com.apiguard.backend.global.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public ProjectResponse createProject(Long workspaceId, CreateProjectRequest request) {
        workspaceService.checkWritePermission(workspaceId);
        Workspace workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다."));

        User user = userService.getUserDetail();

        Project project = Project.builder()
            .workspace(workspace)
            .user(user)
            .name(request.name())
            .description(request.description())
            .build();

        Project saved = projectRepository.save(project);
        return ProjectResponse.from(saved);
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User user = userService.getUserDetail();

        Project project = Project.builder()
            .user(user)
            .name(request.name())
            .description(request.description())
            .build();

        Project saved = projectRepository.save(project);
        return ProjectResponse.from(saved);
    }

    public List<ProjectResponse> getProjects(Long workspaceId) {
        workspaceService.getWorkspaceWithMemberCheck(workspaceId);
        return projectRepository.findByWorkspaceIdAndDeletedFalse(workspaceId).stream()
            .map(ProjectResponse::from)
            .toList();
    }

    public List<ProjectResponse> getMyProjects() {
        User user = userService.getUserDetail();
        return projectRepository.findByUserIdAndDeletedFalse(user.getId()).stream()
            .map(ProjectResponse::from)
            .toList();
    }

    public ProjectResponse getProject(Long id) {
        Project project = getProjectWithOwnerCheck(id);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = getProjectWithMemberCheck(id);
        project.update(request.name(), request.description());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = getProjectWithOwnerCheck(id);
        project.softDelete();
    }

    public Project getProjectWithOwnerCheck(Long projectId) {
        User user = userService.getUserDetail();
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));

        if (!project.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("해당 프로젝트에 대한 권한이 없습니다.");
        }

        return project;
    }

    public Project getProjectWithAccessCheck(Long projectId) {
        User user = userService.getUserDetail();
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));

        if (project.getWorkspace() != null) {
            Long workspaceId = project.getWorkspace().getId();
            workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new ForbiddenException("해당 프로젝트에 대한 권한이 없습니다."));
        } else if (project.getUser() == null || !project.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("해당 프로젝트에 대한 권한이 없습니다.");
        }

        return project;
    }

    public Project getProjectWithMemberCheck(Long projectId) {
        User user = userService.getUserDetail();
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ProjectNotFoundException("프로젝트를 찾을 수 없습니다."));

        if (project.getWorkspace() != null) {
            Long workspaceId = project.getWorkspace().getId();
            WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new ForbiddenException("해당 프로젝트에 대한 권한이 없습니다."));

            if (member.getRole() == WorkspaceRole.VIEWER) {
                throw new ForbiddenException("VIEWER는 쓰기 작업을 수행할 수 없습니다.");
            }
        } else {
            if (project.getUser() == null || !project.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("해당 프로젝트에 대한 권한이 없습니다.");
            }
        }

        return project;
    }
}
