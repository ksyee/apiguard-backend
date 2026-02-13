package com.apiguard.backend.domain.alert.controller;

import com.apiguard.backend.domain.alert.dto.AlertResponse;
import com.apiguard.backend.domain.alert.dto.CreateAlertRequest;
import com.apiguard.backend.domain.alert.dto.UpdateAlertRequest;
import com.apiguard.backend.domain.alert.service.AlertService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/endpoints/{endpointId}/alerts")
    public ApiResponse<AlertResponse> createAlert(
        @PathVariable Long endpointId,
        @RequestBody @Valid CreateAlertRequest request
    ) {
        return ApiResponse.ok(alertService.createAlert(endpointId, request));
    }

    @GetMapping("/endpoints/{endpointId}/alerts")
    public ApiResponse<List<AlertResponse>> getAlerts(@PathVariable Long endpointId) {
        return ApiResponse.ok(alertService.getAlerts(endpointId));
    }

    @PutMapping("/alerts/{id}")
    public ApiResponse<AlertResponse> updateAlert(
        @PathVariable Long id,
        @RequestBody @Valid UpdateAlertRequest request
    ) {
        return ApiResponse.ok(alertService.updateAlert(id, request));
    }

    @DeleteMapping("/alerts/{id}")
    public ApiResponse<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/alerts/{id}/toggle")
    public ApiResponse<AlertResponse> toggleAlert(@PathVariable Long id) {
        return ApiResponse.ok(alertService.toggleAlert(id));
    }
}
