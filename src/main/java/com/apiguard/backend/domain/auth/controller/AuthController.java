package com.apiguard.backend.domain.auth.controller;

import com.apiguard.backend.domain.auth.dto.LoginRequest;
import com.apiguard.backend.domain.auth.dto.LoginResponse;
import com.apiguard.backend.domain.auth.dto.LogoutRequest;
import com.apiguard.backend.domain.auth.dto.RefreshRequest;
import com.apiguard.backend.domain.auth.service.AuthService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }
}
