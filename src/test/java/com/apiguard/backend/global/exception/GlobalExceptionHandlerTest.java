package com.apiguard.backend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("도메인 예외는 ProblemDetail 형식으로 반환")
    void domainException_ReturnsProblemDetail() {
        ProblemDetail result = globalExceptionHandler.handleDuplicateEmailException(
            new DuplicateEmailException("이미 사용 중인 이메일입니다.")
        );

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        assertThat(result.getDetail()).isEqualTo("이미 사용 중인 이메일입니다.");
        assertThat(result.getProperties()).containsEntry("code", "DUPLICATE_EMAIL");
    }

    @Test
    @DisplayName("처리되지 않은 예외는 내부 메시지를 노출하지 않음")
    void unknownException_MasksDetail() {
        ProblemDetail result = globalExceptionHandler.handleException(
            new RuntimeException("민감한 내부 메시지")
        );

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(result.getDetail()).isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(result.getProperties()).containsEntry("code", "INTERNAL_SERVER_ERROR");
    }
}
