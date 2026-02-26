package com.apiguard.backend.domain.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
    String paymentKey,
    String orderId,
    String status,
    Long totalAmount,
    String method
) {
    public boolean isApproved() {
        return "DONE".equals(status);
    }
}
