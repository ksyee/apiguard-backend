package com.apiguard.backend.domain.endpoint.controller;

import com.apiguard.backend.domain.endpoint.dto.CreateEndpointRequest;
import com.apiguard.backend.domain.endpoint.dto.EndpointResponse;
import com.apiguard.backend.domain.endpoint.dto.UpdateEndpointRequest;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping("/projects/{projectId}/endpoints")
    public ApiResponse<EndpointResponse> createEndpoint(
        @PathVariable Long projectId,
        @RequestBody @Valid CreateEndpointRequest request
    ) {
        return ApiResponse.ok(endpointService.createEndpoint(projectId, request));
    }

    @GetMapping("/projects/{projectId}/endpoints")
    public ApiResponse<List<EndpointResponse>> getEndpoints(@PathVariable Long projectId) {
        return ApiResponse.ok(endpointService.getEndpoints(projectId));
    }

    @GetMapping("/endpoints/{id}")
    public ApiResponse<EndpointResponse> getEndpoint(@PathVariable Long id) {
        return ApiResponse.ok(endpointService.getEndpoint(id));
    }

    @PutMapping("/endpoints/{id}")
    public ApiResponse<EndpointResponse> updateEndpoint(
        @PathVariable Long id,
        @RequestBody @Valid UpdateEndpointRequest request
    ) {
        return ApiResponse.ok(endpointService.updateEndpoint(id, request));
    }

    @DeleteMapping("/endpoints/{id}")
    public ApiResponse<Void> deleteEndpoint(@PathVariable Long id) {
        endpointService.deleteEndpoint(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/endpoints/{id}/toggle")
    public ApiResponse<EndpointResponse> toggleEndpoint(@PathVariable Long id) {
        return ApiResponse.ok(endpointService.toggleEndpoint(id));
    }
}
