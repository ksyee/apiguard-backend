package com.apiguard.backend.domain.auth.dto;

/*
 * 토큰을 두 개로 나누는 이유:
 * 보안 + 사용성의 균형 -> accessToken만 쓰면? 수명이 짧아서 자주 로그인해야 함
 * refreshToken만 쓰면? 수명이 길어지면 탈취당했을 때 피해 기간이 길어짐(쏘 댄저러스~)
 */
public record LoginResponse(String accessToken, String refreshToken) {

}
