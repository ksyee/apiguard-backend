package com.apiguard.backend.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
    boolean success,
    @JsonInclude(JsonInclude.Include.NON_NULL) T data,
    
    @JsonInclude(JsonInclude.Include.NON_NULL) String message
) {
    
    // 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    
    // 성공 응답 (데이터 없음)
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }
    
    // 실패 응답
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
