package com.apiguard.backend.global.exception;

import com.apiguard.backend.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 정적 리소스(favicon 등)가 없을 때 발생하는 예외 처리 (로그 레벨 낮춤)
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFoundException(
        org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return ApiResponse.error("Resource not found: " + e.getResourcePath());
    }
    
    // 중복 이메일 에러
    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicateEmailException(DuplicateEmailException e) {
        // 로그
        log.warn("중복 이메일 에러: {}", e.getMessage());
        
        return ApiResponse.error(e.getMessage());
    }
    
    // 로그인 실패 에러
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidCredentialsException(InvalidCredentialsException e) {
        log.warn("로그인 실패: {}", e.getMessage());
        
        return ApiResponse.error(e.getMessage());
    }
    
    // 사용자 조회 실패 에러
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("사용자 조회 실패: {}", e.getMessage());
        
        return ApiResponse.error(e.getMessage());
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("인증 실패: {}", e.getMessage());
        
        return ApiResponse.error(e.getMessage());
    }
    
    // 모든 예외를 처리하는 메서드
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생: ", e);
        
        // 우리가 만든 ApiResponse 규격으로 에러 메시지를 담아서 리턴
        return ApiResponse.error(e.getMessage());
    }
}
