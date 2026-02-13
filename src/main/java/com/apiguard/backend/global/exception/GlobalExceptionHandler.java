package com.apiguard.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoResourceFoundException(
        org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return buildProblemDetail(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "Resource not found: " + e.getResourcePath()
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDuplicateEmailException(DuplicateEmailException e) {
        log.warn("중복 이메일 에러: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleInvalidCredentialsException(InvalidCredentialsException e) {
        log.warn("로그인 실패: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUserNotFoundException(UserNotFoundException e) {
        log.warn("사용자 조회 실패: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUnauthorizedException(UnauthorizedException e) {
        log.warn("인증 실패: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProjectNotFoundException(ProjectNotFoundException e) {
        log.warn("프로젝트 조회 실패: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(EndpointNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleEndpointNotFoundException(EndpointNotFoundException e) {
        log.warn("엔드포인트 조회 실패: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(AlertNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleAlertNotFoundException(AlertNotFoundException e) {
        log.warn("알림 설정 조회 실패: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleForbiddenException(ForbiddenException e) {
        log.warn("권한 없음: {}", e.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("잘못된 요청입니다.");
        log.warn("Validation 실패: {}", message);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleException(Exception e) {
        log.error("처리되지 않은 예외 발생: ", e);
        return buildProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
        );
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setProperty("code", code);
        return problemDetail;
    }
}
