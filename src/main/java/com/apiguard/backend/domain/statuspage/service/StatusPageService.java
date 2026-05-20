package com.apiguard.backend.domain.statuspage.service;

import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.statuspage.dto.CreateStatusPageRequest;
import com.apiguard.backend.domain.statuspage.dto.PublicStatusPageResponse;
import com.apiguard.backend.domain.statuspage.dto.StatusPageResponse;
import com.apiguard.backend.domain.statuspage.dto.UpdateStatusPageRequest;
import com.apiguard.backend.domain.statuspage.entity.StatusPage;
import com.apiguard.backend.domain.statuspage.repository.StatusPageRepository;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.StatusPageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusPageService {

    private final StatusPageRepository statusPageRepository;
    private final EndpointRepository endpointRepository;
    private final CheckResultRepository checkResultRepository;
    private final WorkspaceService workspaceService;

    @Transactional
    public StatusPageResponse create(Long workspaceId, CreateStatusPageRequest request) {
        Workspace workspace = workspaceService.getWorkspaceWithMemberCheck(workspaceId);
        workspaceService.checkWritePermission(workspaceId);

        statusPageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
            .ifPresent(existing -> {
                throw new IllegalArgumentException("이 워크스페이스에는 이미 상태 페이지가 있습니다.");
            });

        if (statusPageRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("이미 사용 중인 슬러그입니다.");
        }

        StatusPage statusPage = StatusPage.builder()
            .workspace(workspace)
            .slug(request.slug())
            .title(request.title())
            .description(request.description())
            .isPublic(true)
            .build();
        statusPage.updateEndpointSelection(
            shouldPublishAll(request.allEndpoints(), request.endpointIds()),
            validateEndpointIds(workspaceId, request.endpointIds())
        );

        return StatusPageResponse.from(statusPageRepository.save(statusPage));
    }

    public StatusPageResponse getByWorkspaceId(Long workspaceId) {
        workspaceService.getWorkspaceWithMemberCheck(workspaceId);

        StatusPage statusPage = statusPageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new StatusPageNotFoundException("상태 페이지를 찾을 수 없습니다."));

        return StatusPageResponse.from(statusPage);
    }

    @Transactional
    public StatusPageResponse update(Long workspaceId, UpdateStatusPageRequest request) {
        workspaceService.checkWritePermission(workspaceId);

        StatusPage statusPage = statusPageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new StatusPageNotFoundException("상태 페이지를 찾을 수 없습니다."));

        statusPage.update(request.title(), request.description(), request.isPublic());
        if (request.allEndpoints() != null || request.endpointIds() != null) {
            statusPage.updateEndpointSelection(
                shouldPublishAll(request.allEndpoints(), request.endpointIds()),
                validateEndpointIds(workspaceId, request.endpointIds())
            );
        }

        return StatusPageResponse.from(statusPage);
    }

    @Transactional
    public void delete(Long workspaceId) {
        workspaceService.checkWritePermission(workspaceId);

        StatusPage statusPage = statusPageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
            .orElseThrow(() -> new StatusPageNotFoundException("상태 페이지를 찾을 수 없습니다."));

        statusPage.softDelete();
    }

    /**
     * 공개 상태 페이지 조회 (인증 불필요)
     */
    public PublicStatusPageResponse getPublicPage(String slug) {
        StatusPage statusPage = statusPageRepository.findBySlugAndDeletedFalse(slug)
            .orElseThrow(() -> new StatusPageNotFoundException("상태 페이지를 찾을 수 없습니다."));

        if (!statusPage.isPublic()) {
            throw new ForbiddenException("비공개 상태 페이지입니다.");
        }

        Long workspaceId = statusPage.getWorkspace().getId();
        List<Endpoint> endpoints = resolvePublicEndpoints(statusPage, workspaceId);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<PublicStatusPageResponse.EndpointStatus> endpointStatuses = new ArrayList<>();

        for (Endpoint endpoint : endpoints) {
            long totalChecks = checkResultRepository.countByEndpointIdAndCheckedAtAfter(endpoint.getId(), since);
            long successCount = checkResultRepository.countByEndpointIdAndStatusAndCheckedAtAfter(
                endpoint.getId(), CheckStatus.SUCCESS, since);
            Double avgResponseTime = checkResultRepository.findAvgResponseTimeByEndpointIdAndCheckedAtAfter(
                endpoint.getId(), since);

            double uptimePercent = totalChecks > 0 ? (double) successCount / totalChecks * 100 : 100.0;

            List<CheckResult> recent = checkResultRepository.findByEndpointIdOrderByCheckedAtDesc(
                endpoint.getId(), PageRequest.of(0, 1));
            String status = recent.isEmpty() ? "UNKNOWN"
                : recent.get(0).getStatus() == CheckStatus.SUCCESS ? "UP" : "DOWN";

            endpointStatuses.add(new PublicStatusPageResponse.EndpointStatus(
                endpoint.getUrl(),
                endpoint.getHttpMethod().name(),
                status,
                Math.round(uptimePercent * 100.0) / 100.0,
                avgResponseTime != null ? Math.round(avgResponseTime * 100.0) / 100.0 : 0.0,
                endpoint.getLastCheckedAt()
            ));
        }

        long downCount = endpointStatuses.stream().filter(e -> "DOWN".equals(e.status())).count();
        String overallStatus;
        if (endpointStatuses.isEmpty()) {
            overallStatus = "NO_DATA";
        } else if (downCount == 0) {
            overallStatus = "OPERATIONAL";
        } else if (downCount < endpointStatuses.size()) {
            overallStatus = "DEGRADED";
        } else {
            overallStatus = "MAJOR_OUTAGE";
        }

        return new PublicStatusPageResponse(
            statusPage.getTitle(),
            statusPage.getDescription(),
            overallStatus,
            endpointStatuses
        );
    }

    private Set<Long> validateEndpointIds(Long workspaceId, List<Long> endpointIds) {
        if (endpointIds == null || endpointIds.isEmpty()) {
            return Set.of();
        }

        List<Endpoint> workspaceEndpoints = endpointRepository.findByProject_Workspace_IdAndDeletedFalse(workspaceId);
        Set<Long> allowedIds = workspaceEndpoints.stream()
            .map(Endpoint::getId)
            .collect(java.util.stream.Collectors.toSet());

        LinkedHashSet<Long> selectedIds = new LinkedHashSet<>(endpointIds);
        if (!allowedIds.containsAll(selectedIds)) {
            throw new IllegalArgumentException("상태 페이지에 포함할 수 없는 엔드포인트가 있습니다.");
        }
        return selectedIds;
    }

    private List<Endpoint> resolvePublicEndpoints(StatusPage statusPage, Long workspaceId) {
        List<Endpoint> endpoints = endpointRepository
            .findByProject_Workspace_IdAndDeletedFalseAndIsActiveTrue(workspaceId);
        if (statusPage.isAllEndpoints()) {
            return endpoints;
        }

        Set<Long> selectedIds = statusPage.getSelectedEndpointIds();
        if (selectedIds == null || selectedIds.isEmpty()) {
            return List.of();
        }
        return endpoints.stream()
            .filter(endpoint -> selectedIds.contains(endpoint.getId()))
            .toList();
    }

    private boolean shouldPublishAll(Boolean allEndpoints, List<Long> endpointIds) {
        if (allEndpoints != null) {
            return allEndpoints;
        }
        return endpointIds == null;
    }
}
