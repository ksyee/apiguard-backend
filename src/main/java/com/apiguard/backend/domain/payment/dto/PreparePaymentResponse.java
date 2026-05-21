package com.apiguard.backend.domain.payment.dto;

public record PreparePaymentResponse(
    String orderId,
    Long amount,
    String orderName,
    String clientKey,
    String customerKey,
    String customerEmail,
    String customerName
) {}
