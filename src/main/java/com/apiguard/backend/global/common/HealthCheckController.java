package com.apiguard.backend.global.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    
    // 서버 상태 확인 API (정상)
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("APIGuard 서버가 정상적으로 구동중입니다.");
    }
    
    // 서버 상태 확인 API (에러)
    @GetMapping("/test-error")
    public ApiResponse<String> testError() {
        throw new RuntimeException("서버가 터져버렸습니다!!!");
    }
}

