package com.apiguard.backend.domain.payment.controller;

import com.apiguard.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.apiguard.backend.domain.payment.dto.PaymentResponse;
import com.apiguard.backend.domain.payment.dto.PreparePaymentResponse;
import com.apiguard.backend.domain.payment.dto.SubscriptionStatusResponse;
import com.apiguard.backend.domain.payment.service.PaymentService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/subscription")
    public ApiResponse<SubscriptionStatusResponse> getSubscriptionStatus(
            @PathVariable Long workspaceId) {
        return ApiResponse.ok(paymentService.getSubscriptionStatus(workspaceId));
    }

    @PostMapping("/payment/prepare")
    public ApiResponse<PreparePaymentResponse> preparePayment(
            @PathVariable Long workspaceId) {
        return ApiResponse.ok(paymentService.preparePayment(workspaceId));
    }

    @PostMapping("/payment/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(
            @PathVariable Long workspaceId,
            @RequestBody @Valid ConfirmPaymentRequest request) {
        return ApiResponse.ok(paymentService.confirmPayment(workspaceId, request));
    }

    @GetMapping("/payment/history")
    public ApiResponse<List<PaymentResponse>> getPaymentHistory(
            @PathVariable Long workspaceId) {
        return ApiResponse.ok(paymentService.getPaymentHistory(workspaceId));
    }
}
