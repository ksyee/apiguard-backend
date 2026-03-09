package com.apiguard.backend.domain.auth.dto;

public record LoginResponse(String accessToken, String refreshToken) {
}
