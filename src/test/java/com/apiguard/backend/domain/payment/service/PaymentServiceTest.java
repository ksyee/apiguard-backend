package com.apiguard.backend.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.payment.client.TossConfirmResponse;
import com.apiguard.backend.domain.payment.client.TossPaymentClient;
import com.apiguard.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.apiguard.backend.domain.payment.dto.PaymentResponse;
import com.apiguard.backend.domain.payment.dto.PreparePaymentResponse;
import com.apiguard.backend.domain.payment.entity.Payment;
import com.apiguard.backend.domain.payment.entity.PaymentStatus;
import com.apiguard.backend.domain.payment.repository.PaymentRepository;
import com.apiguard.backend.domain.subscription.entity.PlanType;
import com.apiguard.backend.domain.subscription.entity.Subscription;
import com.apiguard.backend.domain.subscription.repository.SubscriptionRepository;
import com.apiguard.backend.domain.subscription.service.ProPlanLimitPolicy;
import com.apiguard.backend.domain.subscription.service.SubscriptionService;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.entity.WorkspaceRole;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.global.exception.ExternalPaymentException;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.PaymentConflictException;
import com.apiguard.backend.global.exception.PaymentNotFoundException;
import com.apiguard.backend.global.exception.PaymentValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "clientKey", "test-client-key");
    }

    @Test
    @DisplayName("preparePayment 성공 시 새 PENDING 주문을 생성한다")
    void preparePayment_success() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(workspaceService.getWorkspaceWithMemberCheck(1L)).willReturn(workspace);
        given(paymentRepository.findByWorkspaceIdAndStatus(1L, PaymentStatus.PENDING)).willReturn(List.of());
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

        PreparePaymentResponse response = paymentService.preparePayment(1L);

        assertThat(response.orderId()).startsWith("apiguard-1-");
        assertThat(response.amount()).isEqualTo(19_900L);
        assertThat(response.clientKey()).isEqualTo("test-client-key");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("preparePayment 호출 시 기존 PENDING 주문은 CANCELLED 처리된다")
    void preparePayment_cancelsExistingPendingPayments() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment pending1 = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        Payment pending2 = createPayment(workspace, "order-2", 19_900L, PaymentStatus.PENDING);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(workspaceService.getWorkspaceWithMemberCheck(1L)).willReturn(workspace);
        given(paymentRepository.findByWorkspaceIdAndStatus(1L, PaymentStatus.PENDING))
            .willReturn(List.of(pending1, pending2));
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

        paymentService.preparePayment(1L);

        assertThat(pending1.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(pending2.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("이미 활성 PRO 플랜이면 preparePayment는 409를 던진다")
    void preparePayment_activePro_throwsConflict() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.PRO, LocalDateTime.now().plusDays(30));

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> paymentService.preparePayment(1L))
            .isInstanceOf(PaymentConflictException.class)
            .hasMessage("이미 PRO 플랜을 구독 중입니다.");
    }

    @Test
    @DisplayName("OWNER가 아니면 preparePayment는 403을 던진다")
    void preparePayment_notOwner_throwsForbidden() {
        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> paymentService.preparePayment(1L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("결제는 워크스페이스 OWNER만 가능합니다.");
    }

    @Test
    @DisplayName("confirmPayment 성공 시 결제 성공과 PRO 업그레이드를 반영한다")
    void confirmPayment_success() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(tossPaymentClient.confirmPayment("payment-key", "order-1", 19_900L))
            .willReturn(new TossConfirmResponse("payment-key", "order-1", "DONE", 19_900L, "CARD"));

        PaymentResponse response = paymentService.confirmPayment(1L, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(subscription.getPlanType()).isEqualTo(PlanType.PRO);
        assertThat(subscription.getExternalSubscriptionId()).isEqualTo("payment-key");
        assertThat(subscription.getExpiredAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("존재하지 않는 주문이면 confirmPayment는 404를 던진다")
    void confirmPayment_orderNotFound() {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "missing-order", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("missing-order")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(PaymentNotFoundException.class)
            .hasMessage("주문 정보를 찾을 수 없습니다. orderId: missing-order");
    }

    @Test
    @DisplayName("다른 워크스페이스 주문이면 confirmPayment는 403을 던진다")
    void confirmPayment_otherWorkspace_throwsForbidden() {
        Workspace workspace = createWorkspace(2L);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("해당 주문에 대한 권한이 없습니다.");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SUCCESS", "FAILED", "CANCELLED"})
    @DisplayName("이미 처리된 주문이면 confirmPayment는 409를 던진다")
    void confirmPayment_alreadyProcessed_throwsConflict(PaymentStatus status) {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, status);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(PaymentConflictException.class)
            .hasMessage("이미 처리된 주문입니다.");
    }

    @Test
    @DisplayName("결제 금액이 다르면 confirmPayment는 FAILED 처리 후 400을 던진다")
    void confirmPayment_amountMismatch_marksFailed() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 9_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("결제 금액이 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("토스 응답 상태가 DONE이 아니면 confirmPayment는 FAILED 처리 후 502를 던진다")
    void confirmPayment_statusNotDone_marksFailed() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(tossPaymentClient.confirmPayment("payment-key", "order-1", 19_900L))
            .willReturn(new TossConfirmResponse("payment-key", "order-1", "CANCELED", 19_900L, "CARD"));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(ExternalPaymentException.class)
            .hasMessage("결제 승인이 거부되었습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("토스 응답 orderId가 다르면 confirmPayment는 FAILED 처리 후 502를 던진다")
    void confirmPayment_orderIdMismatch_marksFailed() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(tossPaymentClient.confirmPayment("payment-key", "order-1", 19_900L))
            .willReturn(new TossConfirmResponse("payment-key", "other-order", "DONE", 19_900L, "CARD"));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(ExternalPaymentException.class)
            .hasMessage("토스 결제 응답의 orderId가 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("토스 응답 결제 금액이 다르면 confirmPayment는 FAILED 처리 후 502를 던진다")
    void confirmPayment_totalAmountMismatch_marksFailed() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(tossPaymentClient.confirmPayment("payment-key", "order-1", 19_900L))
            .willReturn(new TossConfirmResponse("payment-key", "order-1", "DONE", 29_900L, "CARD"));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(ExternalPaymentException.class)
            .hasMessage("토스 결제 응답의 결제 금액이 일치하지 않습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("토스 클라이언트 오류면 confirmPayment는 FAILED 처리 후 502를 던진다")
    void confirmPayment_externalClientError_marksFailed() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.FREE, null);
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(tossPaymentClient.confirmPayment("payment-key", "order-1", 19_900L))
            .willThrow(new ExternalPaymentException("토스 결제 승인에 실패했습니다."));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(ExternalPaymentException.class)
            .hasMessage("토스 결제 승인에 실패했습니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("이미 활성 PRO 플랜이면 confirmPayment는 주문을 취소하고 409를 던진다")
    void confirmPayment_activePro_cancelsPendingPayment() {
        Workspace workspace = createWorkspace(1L);
        Subscription subscription = createSubscription(workspace, PlanType.PRO, LocalDateTime.now().plusDays(30));
        Payment payment = createPayment(workspace, "order-1", 19_900L, PaymentStatus.PENDING);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
            .isInstanceOf(PaymentConflictException.class)
            .hasMessage("이미 PRO 플랜을 구독 중입니다.");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelSubscription 성공 시 PRO 구독을 기간 종료 취소로 예약한다")
    void cancelSubscription_success() {
        Workspace workspace = createWorkspace(1L);
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(20);
        Subscription subscription = createSubscription(workspace, PlanType.PRO, expiredAt);

        given(workspaceService.getMemberRole(1L)).willReturn(WorkspaceRole.OWNER);
        given(subscriptionRepository.findByWorkspaceId(1L)).willReturn(Optional.of(subscription));
        given(subscriptionService.getPolicyForWorkspace(1L)).willReturn(new ProPlanLimitPolicy());

        var response = paymentService.cancelSubscription(1L);

        assertThat(subscription.getPlanType()).isEqualTo(PlanType.PRO);
        assertThat(subscription.isCancelAtPeriodEnd()).isTrue();
        assertThat(subscription.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(response.planType()).isEqualTo(PlanType.PRO);
        assertThat(response.cancelAtPeriodEnd()).isTrue();
    }

    private User createUser(Long id) {
        return User.builder()
            .id(id)
            .email("user" + id + "@email.com")
            .password("encodedPassword")
            .nickname("tester" + id)
            .role(Role.USER)
            .build();
    }

    private Workspace createWorkspace(Long id) {
        return Workspace.builder()
            .id(id)
            .name("Workspace " + id)
            .slug("workspace-" + id)
            .owner(createUser(id))
            .build();
    }

    private Subscription createSubscription(Workspace workspace, PlanType planType, LocalDateTime expiredAt) {
        return Subscription.builder()
            .id(workspace.getId())
            .workspace(workspace)
            .planType(planType)
            .startedAt(LocalDateTime.now().minusDays(1))
            .expiredAt(expiredAt)
            .build();
    }

    private Payment createPayment(Workspace workspace, String orderId, Long amount, PaymentStatus status) {
        return Payment.builder()
            .id(1L)
            .workspace(workspace)
            .orderId(orderId)
            .planType(PlanType.PRO)
            .amount(amount)
            .status(status)
            .build();
    }
}
