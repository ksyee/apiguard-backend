package com.apiguard.backend.domain.project.service;

import com.apiguard.backend.domain.project.dto.CreateProjectRequest;
import com.apiguard.backend.domain.project.dto.ProjectResponse;
import com.apiguard.backend.domain.project.dto.UpdateProjectRequest;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.repository.ProjectRepository;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.ProjectNotFoundException;
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
        Project project = getProjectWithOwnerCheck(id);
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
}
