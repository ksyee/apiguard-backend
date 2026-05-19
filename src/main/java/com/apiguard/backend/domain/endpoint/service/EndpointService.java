package com.apiguard.backend.domain.endpoint.service;

import com.apiguard.backend.domain.endpoint.dto.CreateEndpointRequest;
import com.apiguard.backend.domain.endpoint.dto.EndpointResponse;
import com.apiguard.backend.domain.endpoint.dto.UpdateEndpointRequest;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.domain.workspace.entity.WorkspaceMember;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.global.exception.EndpointNotFoundException;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.ProjectNotFoundException;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public EndpointResponse createEndpoint(Long projectId, CreateEndpointRequest request) {
        Project project = projectService.getProjectWithMemberCheck(projectId);

        if (project.getWorkspace() != null) {
            Long workspaceId = project.getWorkspace().getId();
            subscriptionService.validateEndpointCount(workspaceId, projectId);
            int checkInterval = request.checkInterval() != null ? request.checkInterval() : 60;
            subscriptionService.validateCheckInterval(workspaceId, checkInterval);
        }

        Endpoint endpoint = Endpoint.builder()
            .project(project)
            .url(request.url())
            .httpMethod(request.httpMethod())
            .headers(request.headers())
            .body(request.body())
            .expectedStatusCode(request.expectedStatusCode() != null ? request.expectedStatusCode() : 200)
            .checkInterval(request.checkInterval() != null ? request.checkInterval() : 60)
            .build();

        Endpoint saved = endpointRepository.save(endpoint);
        return EndpointResponse.from(saved);
    }

    public List<EndpointResponse> getEndpoints(Long projectId) {
        projectService.getProjectWithAccessCheck(projectId);
        return endpointRepository.findByProjectIdAndDeletedFalse(projectId).stream()
            .map(EndpointResponse::from)
            .toList();
    }

    public EndpointResponse getEndpoint(Long id) {
        Endpoint endpoint = getEndpointWithAccessCheck(id);
        return EndpointResponse.from(endpoint);
    }

    @Transactional
    public EndpointResponse updateEndpoint(Long id, UpdateEndpointRequest request) {
        Endpoint endpoint = getEndpointWithWriteCheck(id);

        if (endpoint.getProject().getWorkspace() != null && request.checkInterval() != null) {
            Long workspaceId = endpoint.getProject().getWorkspace().getId();
            subscriptionService.validateCheckInterval(workspaceId, request.checkInterval());
        }

        endpoint.update(
            request.url(),
            request.httpMethod(),
            request.headers(),
            request.body(),
            request.expectedStatusCode(),
            request.checkInterval()
        );
        return EndpointResponse.from(endpoint);
    }

    @Transactional
    public void deleteEndpoint(Long id) {
        Endpoint endpoint = getEndpointWithDeleteCheck(id);
        endpoint.softDelete();
    }

    @Transactional
    public EndpointResponse toggleEndpoint(Long id) {
        Endpoint endpoint = getEndpointWithWriteCheck(id);
        endpoint.toggleActive();
        return EndpointResponse.from(endpoint);
    }

    public Endpoint getEndpointWithAccessCheck(Long endpointId) {
        return getEndpointWithPermissionCheck(endpointId, WorkspaceRole.VIEWER, null);
    }

    public Endpoint getEndpointWithWriteCheck(Long endpointId) {
        return getEndpointWithPermissionCheck(endpointId, WorkspaceRole.MEMBER, "VIEWER는 쓰기 작업을 수행할 수 없습니다.");
    }

    public Endpoint getEndpointWithDeleteCheck(Long endpointId) {
        return getEndpointWithPermissionCheck(endpointId, WorkspaceRole.ADMIN, "엔드포인트 삭제는 ADMIN 이상만 가능합니다.");
    }

    private Endpoint getEndpointWithPermissionCheck(Long endpointId, WorkspaceRole requiredRole, String forbiddenMessage) {
        User user = userService.getUserDetail();
        Endpoint endpoint = endpointRepository.findByIdAndDeletedFalse(endpointId)
            .orElseThrow(() -> new EndpointNotFoundException("엔드포인트를 찾을 수 없습니다."));

        if (endpoint.getProject().isDeleted()) {
            throw new ProjectNotFoundException("프로젝트를 찾을 수 없습니다.");
        }

        if (endpoint.getProject().getWorkspace() != null) {
            Long workspaceId = endpoint.getProject().getWorkspace().getId();
            WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .orElseThrow(() -> new ForbiddenException("해당 엔드포인트에 대한 권한이 없습니다."));
            if (!member.getRole().isAtLeast(requiredRole)) {
                throw new ForbiddenException(forbiddenMessage != null ? forbiddenMessage : "해당 엔드포인트에 대한 권한이 없습니다.");
            }
        } else {
            if (!endpoint.getProject().getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("해당 엔드포인트에 대한 권한이 없습니다.");
            }
        }

        return endpoint;
    }
}
