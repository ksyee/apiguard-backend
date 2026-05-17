package com.apiguard.backend.domain.incident.controller;

import com.apiguard.backend.domain.incident.dto.IncidentResponse;
import com.apiguard.backend.domain.incident.entity.IncidentStatus;
import com.apiguard.backend.domain.incident.service.IncidentService;
import com.apiguard.backend.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/projects/{projectId}/incidents")
    public ApiResponse<List<IncidentResponse>> getProjectIncidents(
        @PathVariable Long projectId,
        @RequestParam(required = false) IncidentStatus status
    ) {
        return ApiResponse.ok(incidentService.getProjectIncidents(projectId, status));
    }

    @GetMapping("/endpoints/{endpointId}/incidents")
    public ApiResponse<List<IncidentResponse>> getEndpointIncidents(@PathVariable Long endpointId) {
        return ApiResponse.ok(incidentService.getEndpointIncidents(endpointId));
    }
}
