package com.apiguard.backend.domain.payment.dto;

import com.apiguard.backend.domain.payment.entity.Payment;
import com.apiguard.backend.domain.payment.entity.PaymentStatus;
import com.apiguard.backend.domain.subscription.entity.PlanType;

import java.time.LocalDateTime;

public record PaymentResponse(
    Long id,
    String orderId,
    String paymentKey,
    PlanType planType,
    Long amount,
    PaymentStatus status,
    LocalDateTime paidAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getPaymentKey(),
            payment.getPlanType(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getPaidAt()
        );
    }
}
