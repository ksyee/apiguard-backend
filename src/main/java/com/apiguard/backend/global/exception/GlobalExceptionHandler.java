package com.apiguard.backend.global.exception;

import com.apiguard.backend.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 모든 예외를 처리하는 메서드
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생: ", e);
        
        // 우리가 만든 ApiResponse 규격으로 에러 메시지를 담아서 리턴
        return ApiResponse.error(e.getMessage());
    }
}
