package com.apiguard.backend.domain.apispec.controller;

import com.apiguard.backend.domain.apispec.dto.ApiSpecDiffDetailResponse;
import com.apiguard.backend.domain.apispec.dto.ApiSpecDiffResponse;
import com.apiguard.backend.domain.apispec.dto.ApiSpecSourceResponse;
import com.apiguard.backend.domain.apispec.dto.CreateApiSpecSourceRequest;
import com.apiguard.backend.domain.apispec.service.ApiSpecService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiSpecController {

    private final ApiSpecService apiSpecService;

    @PostMapping("/projects/{projectId}/spec-sources")
    public ApiResponse<ApiSpecSourceResponse> createSource(
        @PathVariable Long projectId,
        @RequestBody @Valid CreateApiSpecSourceRequest request
    ) {
        return ApiResponse.ok(apiSpecService.createSource(projectId, request));
    }

    @GetMapping("/projects/{projectId}/spec-sources")
    public ApiResponse<List<ApiSpecSourceResponse>> getSources(@PathVariable Long projectId) {
        return ApiResponse.ok(apiSpecService.getSources(projectId));
    }

    @PostMapping("/spec-sources/{sourceId}/check")
    public ApiResponse<ApiSpecDiffDetailResponse> checkSource(@PathVariable Long sourceId) {
        return ApiResponse.ok(apiSpecService.checkSource(sourceId));
    }

    @GetMapping("/spec-sources/{sourceId}/diffs")
    public ApiResponse<List<ApiSpecDiffResponse>> getDiffs(@PathVariable Long sourceId) {
        return ApiResponse.ok(apiSpecService.getDiffs(sourceId));
    }

    @GetMapping("/spec-diffs/{diffId}")
    public ApiResponse<ApiSpecDiffDetailResponse> getDiff(@PathVariable Long diffId) {
        return ApiResponse.ok(apiSpecService.getDiff(diffId));
    }
}
