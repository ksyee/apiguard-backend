package com.apiguard.backend.domain.payment.service;

import com.apiguard.backend.domain.payment.client.TossConfirmResponse;
import com.apiguard.backend.domain.payment.client.TossPaymentClient;
import com.apiguard.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.apiguard.backend.domain.payment.dto.PaymentResponse;
import com.apiguard.backend.domain.payment.dto.PreparePaymentResponse;
import com.apiguard.backend.domain.payment.dto.SubscriptionStatusResponse;
import com.apiguard.backend.domain.payment.entity.Payment;
import com.apiguard.backend.domain.payment.entity.PaymentStatus;
import com.apiguard.backend.domain.payment.repository.PaymentRepository;
import com.apiguard.backend.domain.subscription.entity.PlanType;
import com.apiguard.backend.domain.subscription.entity.Subscription;
import com.apiguard.backend.domain.subscription.repository.SubscriptionRepository;
import com.apiguard.backend.domain.subscription.service.PlanLimitPolicy;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.PaymentException;
import com.apiguard.backend.global.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final long PRO_PLAN_AMOUNT = 19_900L;
    private static final int PRO_PLAN_DURATION_MONTHS = 1;

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TossPaymentClient tossPaymentClient;
    private final WorkspaceService workspaceService;
    private final SubscriptionService subscriptionService;

    @Value("${toss.payments.client-key}")
    private String clientKey;

    @Transactional
    public PreparePaymentResponse preparePayment(Long workspaceId) {
        checkOwnerPermission(workspaceId);

        Subscription subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("구독 정보를 찾을 수 없습니다."));

        if (subscription.getPlanType() == PlanType.PRO && subscription.isActive()) {
            throw new IllegalStateException("이미 PRO 플랜을 구독 중입니다.");
        }

        String orderId = "apiguard-" + workspaceId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Workspace workspace = workspaceService.getWorkspaceWithMemberCheck(workspaceId);

        Payment payment = Payment.builder()
            .workspace(workspace)
            .orderId(orderId)
            .planType(PlanType.PRO)
            .amount(PRO_PLAN_AMOUNT)
            .status(PaymentStatus.PENDING)
            .build();
        paymentRepository.save(payment);

        return new PreparePaymentResponse(
            orderId,
            PRO_PLAN_AMOUNT,
            "ApiGuard PRO 플랜 (1개월)",
            clientKey
        );
    }

    @Transactional
    public PaymentResponse confirmPayment(Long workspaceId, ConfirmPaymentRequest request) {
        checkOwnerPermission(workspaceId);

        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .orElseThrow(() -> new PaymentException("주문 정보를 찾을 수 없습니다. orderId: " + request.orderId()));

        if (!payment.getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("해당 주문에 대한 권한이 없습니다.");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("이미 처리된 주문입니다.");
        }

        if (!payment.getAmount().equals(request.amount())) {
            payment.markFailed();
            throw new PaymentException("결제 금액이 일치하지 않습니다.");
        }

        try {
            TossConfirmResponse tossResponse = tossPaymentClient.confirmPayment(
                request.paymentKey(), request.orderId(), request.amount()
            );

            if (!tossResponse.isApproved()) {
                payment.markFailed();
                throw new PaymentException("결제 승인이 거부되었습니다.");
            }

            payment.markSuccess(tossResponse.paymentKey());

            Subscription subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("구독 정보를 찾을 수 없습니다."));
            subscription.upgradeTo(PlanType.PRO, tossResponse.paymentKey(),
                LocalDateTime.now().plusMonths(PRO_PLAN_DURATION_MONTHS));

            return PaymentResponse.from(payment);

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            payment.markFailed();
            throw new PaymentException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public SubscriptionStatusResponse getSubscriptionStatus(Long workspaceId) {
        workspaceService.getWorkspaceWithMemberCheck(workspaceId);

        Subscription subscription = subscriptionRepository.findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("구독 정보를 찾을 수 없습니다."));

        PlanLimitPolicy policy = subscriptionService.getPolicyForWorkspace(workspaceId);
        return SubscriptionStatusResponse.from(subscription, policy);
    }

    public List<PaymentResponse> getPaymentHistory(Long workspaceId) {
        workspaceService.getWorkspaceWithMemberCheck(workspaceId);
        return paymentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
            .map(PaymentResponse::from)
            .toList();
    }

    private void checkOwnerPermission(Long workspaceId) {
        WorkspaceRole role = workspaceService.getMemberRole(workspaceId);
        if (role != WorkspaceRole.OWNER) {
            throw new ForbiddenException("결제는 워크스페이스 OWNER만 가능합니다.");
        }
    }
}
