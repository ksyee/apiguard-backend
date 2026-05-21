package com.apiguard.backend.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apiguard.backend.domain.payment.dto.ConfirmPaymentRequest;
import com.apiguard.backend.domain.payment.dto.PreparePaymentResponse;
import com.apiguard.backend.domain.payment.service.PaymentService;
import com.apiguard.backend.global.config.JwtTokenProvider;
import com.apiguard.backend.global.config.SecurityConfig;
import com.apiguard.backend.global.config.SecurityProperties;
import com.apiguard.backend.global.exception.ExternalPaymentException;
import com.apiguard.backend.global.exception.GlobalExceptionHandler;
import com.apiguard.backend.global.exception.PaymentConflictException;
import com.apiguard.backend.global.exception.PaymentNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = PaymentControllerTest.TestApplication.class)
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        given(securityProperties.getWhitelist()).willReturn(List.of());
    }

    @Test
    @DisplayName("인증 없이 결제 이력 조회 요청 시 차단된다")
    void getPaymentHistory_withoutAuthentication_isBlocked() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/payment/history"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("preparePayment 성공 시 ApiResponse 형식으로 반환한다")
    void preparePayment_returnsApiResponse() throws Exception {
        givenAuthenticatedUser();
        given(paymentService.preparePayment(1L))
            .willReturn(new PreparePaymentResponse(
                "order-1",
                19_900L,
                "ApiGuard PRO 플랜 (1개월)",
                "client-key",
                "apiguard_mock_customer_1",
                "owner@example.com",
                "Owner"
            ));

        mockMvc.perform(post("/api/workspaces/1/payment/prepare")
                .with(csrf())
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderId").value("order-1"))
            .andExpect(jsonPath("$.data.amount").value(19900))
            .andExpect(jsonPath("$.data.clientKey").value("client-key"))
            .andExpect(jsonPath("$.data.customerKey").value("apiguard_mock_customer_1"))
            .andExpect(jsonPath("$.data.customerEmail").value("owner@example.com"))
            .andExpect(jsonPath("$.data.customerName").value("Owner"));
    }

    @Test
    @DisplayName("confirmPayment 요청 본문이 비어 있으면 400을 반환한다")
    void confirmPayment_invalidRequest_returnsBadRequest() throws Exception {
        givenAuthenticatedUser();

        mockMvc.perform(post("/api/workspaces/1/payment/confirm")
                .with(csrf())
                .header("Authorization", "Bearer valid-token")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("confirmPayment 충돌 예외는 409로 반환한다")
    void confirmPayment_conflict_returnsConflict() throws Exception {
        givenAuthenticatedUser();
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);
        given(paymentService.confirmPayment(eq(1L), any(ConfirmPaymentRequest.class)))
            .willThrow(new PaymentConflictException("이미 처리된 주문입니다."));

        mockMvc.perform(post("/api/workspaces/1/payment/confirm")
                .with(csrf())
                .header("Authorization", "Bearer valid-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("이미 처리된 주문입니다."));
    }

    @Test
    @DisplayName("confirmPayment 주문 없음 예외는 404로 반환한다")
    void confirmPayment_notFound_returnsNotFound() throws Exception {
        givenAuthenticatedUser();
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);
        given(paymentService.confirmPayment(eq(1L), any(ConfirmPaymentRequest.class)))
            .willThrow(new PaymentNotFoundException("주문 정보를 찾을 수 없습니다. orderId: order-1"));

        mockMvc.perform(post("/api/workspaces/1/payment/confirm")
                .with(csrf())
                .header("Authorization", "Bearer valid-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("주문 정보를 찾을 수 없습니다. orderId: order-1"));
    }

    @Test
    @DisplayName("confirmPayment 외부 결제 예외는 502로 반환한다")
    void confirmPayment_externalError_returnsBadGateway() throws Exception {
        givenAuthenticatedUser();
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("payment-key", "order-1", 19_900L);
        given(paymentService.confirmPayment(eq(1L), any(ConfirmPaymentRequest.class)))
            .willThrow(new ExternalPaymentException("토스 결제 승인에 실패했습니다."));

        mockMvc.perform(post("/api/workspaces/1/payment/confirm")
                .with(csrf())
                .header("Authorization", "Bearer valid-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("토스 결제 승인에 실패했습니다."));
    }

    private void givenAuthenticatedUser() {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                "user@email.com",
                "",
                List.of(new SimpleGrantedAuthority("USER"))
            );
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getAuthentication("valid-token")).willReturn(authentication);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({PaymentController.class, SecurityConfig.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }
}
