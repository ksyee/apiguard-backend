package com.apiguard.backend.domain.check.controller;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import com.apiguard.backend.domain.check.dto.EndpointStatsResponse;
import com.apiguard.backend.domain.check.dto.HourlyStatResponse;
import com.apiguard.backend.domain.check.dto.ProjectStatsResponse;
import com.apiguard.backend.domain.check.service.CheckService;
import com.apiguard.backend.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    @PostMapping("/endpoints/{id}/test")
    public ApiResponse<CheckResultResponse> testEndpoint(@PathVariable Long id) {
        return ApiResponse.ok(checkService.testEndpoint(id));
    }

    @GetMapping("/endpoints/{id}/stats")
    public ApiResponse<EndpointStatsResponse> getEndpointStats(@PathVariable Long id) {
        return ApiResponse.ok(checkService.getEndpointStats(id));
    }

    @GetMapping("/endpoints/{id}/stats/hourly")
    public ApiResponse<List<HourlyStatResponse>> getHourlyStats(@PathVariable Long id) {
        return ApiResponse.ok(checkService.getHourlyStats(id));
    }

    @GetMapping("/endpoints/{id}/checks")
    public ApiResponse<List<CheckResultResponse>> getRecentChecks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(checkService.getRecentChecks(id, limit));
    }

    @GetMapping("/projects/{id}/stats")
    public ApiResponse<ProjectStatsResponse> getProjectStats(@PathVariable Long id) {
        return ApiResponse.ok(checkService.getProjectStats(id));
    }
}
