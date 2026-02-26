package com.apiguard.backend.domain.endpoint.service;

import com.apiguard.backend.domain.endpoint.dto.CreateEndpointRequest;
import com.apiguard.backend.domain.endpoint.dto.EndpointResponse;
import com.apiguard.backend.domain.endpoint.dto.UpdateEndpointRequest;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.service.ProjectService;
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

    @Transactional
    public EndpointResponse createEndpoint(Long projectId, CreateEndpointRequest request) {
        Project project = projectService.getProjectWithOwnerCheck(projectId);

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
        projectService.getProjectWithOwnerCheck(projectId);
        return endpointRepository.findByProjectIdAndDeletedFalse(projectId).stream()
            .map(EndpointResponse::from)
            .toList();
    }

    public EndpointResponse getEndpoint(Long id) {
        Endpoint endpoint = getEndpointWithOwnerCheck(id);
        return EndpointResponse.from(endpoint);
    }

    @Transactional
    public EndpointResponse updateEndpoint(Long id, UpdateEndpointRequest request) {
        Endpoint endpoint = getEndpointWithOwnerCheck(id);

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
        Endpoint endpoint = getEndpointWithOwnerCheck(id);
        endpoint.softDelete();
    }

    @Transactional
    public EndpointResponse toggleEndpoint(Long id) {
        Endpoint endpoint = getEndpointWithOwnerCheck(id);
        endpoint.toggleActive();
        return EndpointResponse.from(endpoint);
    }

    public Endpoint getEndpointWithOwnerCheck(Long endpointId) {
        User user = userService.getUserDetail();
        Endpoint endpoint = endpointRepository.findByIdAndDeletedFalse(endpointId)
            .orElseThrow(() -> new EndpointNotFoundException("엔드포인트를 찾을 수 없습니다."));

        if (endpoint.getProject().isDeleted()) {
            throw new ProjectNotFoundException("프로젝트를 찾을 수 없습니다.");
        }

        if (!endpoint.getProject().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("해당 엔드포인트에 대한 권한이 없습니다.");
        }

        return endpoint;
    }
}
