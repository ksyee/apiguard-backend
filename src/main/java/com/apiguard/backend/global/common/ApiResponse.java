package com.apiguard.backend.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
    boolean success,
    @JsonInclude(JsonInclude.Include.NON_NULL) T data,

    @JsonInclude(JsonInclude.Include.NON_NULL) String message
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
