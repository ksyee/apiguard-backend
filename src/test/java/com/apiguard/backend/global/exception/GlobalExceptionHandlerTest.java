package com.apiguard.backend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.apiguard.backend.global.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("도메인 예외는 ApiResponse 형식으로 반환")
    void domainException_returnsApiResponse() {
        ResponseEntity<ApiResponse<Void>> result = globalExceptionHandler.handleDuplicateEmailException(
            new DuplicateEmailException("이미 사용 중인 이메일입니다.")
        );

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getBody().success()).isFalse();
        assertThat(result.getBody().message()).isEqualTo("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("처리되지 않은 예외는 내부 메시지를 노출하지 않음")
    void unknownException_masksDetail() {
        ResponseEntity<ApiResponse<Void>> result = globalExceptionHandler.handleException(
            new RuntimeException("민감한 내부 메시지")
        );

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getBody().success()).isFalse();
        assertThat(result.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("JSON 파싱 실패는 400으로 반환")
    void invalidJson_returnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> result = globalExceptionHandler.handleHttpMessageNotReadableException(
            new HttpMessageNotReadableException("invalid json", new MockHttpInputMessage(new byte[0]))
        );

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getBody().success()).isFalse();
        assertThat(result.getBody().message()).isEqualTo("요청 본문 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("결제 충돌 예외는 409로 반환")
    void paymentConflict_returnsConflict() {
        ResponseEntity<ApiResponse<Void>> result = globalExceptionHandler.handlePaymentConflictException(
            new PaymentConflictException("이미 처리된 주문입니다.")
        );

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getBody().success()).isFalse();
        assertThat(result.getBody().message()).isEqualTo("이미 처리된 주문입니다.");
    }

    @Test
    @DisplayName("외부 결제 예외는 502로 반환")
    void externalPayment_returnsBadGateway() {
        ResponseEntity<ApiResponse<Void>> result = globalExceptionHandler.handleExternalPaymentException(
            new ExternalPaymentException("토스 결제 승인에 실패했습니다.")
        );

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(result.getBody().success()).isFalse();
        assertThat(result.getBody().message()).isEqualTo("토스 결제 승인에 실패했습니다.");
    }

    @Test
    @DisplayName("일반 결제 예외는 500으로 반환")
    void paymentException_returnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> result = globalExceptionHandler.handlePaymentException(
            new PaymentException("결제 처리 중 오류가 발생했습니다.")
        );

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getBody().success()).isFalse();
        assertThat(result.getBody().message()).isEqualTo("결제 처리 중 오류가 발생했습니다.");
    }
}
