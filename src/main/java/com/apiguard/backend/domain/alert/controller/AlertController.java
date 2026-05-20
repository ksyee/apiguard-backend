package com.apiguard.backend.domain.alert.controller;

import com.apiguard.backend.domain.alert.dto.AlertDeliveryResponse;
import com.apiguard.backend.domain.alert.dto.AlertResponse;
import com.apiguard.backend.domain.alert.dto.CreateAlertRequest;
import com.apiguard.backend.domain.alert.dto.UpdateAlertRequest;
import com.apiguard.backend.domain.alert.service.AlertService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/alerts/{id}/test")
    public ApiResponse<AlertDeliveryResponse> sendTestAlert(@PathVariable Long id) {
        return ApiResponse.ok(alertService.sendTestAlert(id));
    }

    @GetMapping("/alerts/{id}/deliveries")
    public ApiResponse<List<AlertDeliveryResponse>> getDeliveries(
        @PathVariable Long id,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(alertService.getDeliveries(id, limit));
    }
}
