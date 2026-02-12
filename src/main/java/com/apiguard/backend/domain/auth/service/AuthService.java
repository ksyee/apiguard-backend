package com.apiguard.backend.domain.auth.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apiguard.backend.domain.auth.dto.LoginRequest;
import com.apiguard.backend.domain.auth.dto.LoginResponse;
import com.apiguard.backend.domain.auth.dto.LogoutRequest;
import com.apiguard.backend.domain.auth.dto.RefreshRequest;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.config.JwtTokenProvider;
import com.apiguard.backend.global.exception.InvalidCredentialsException;
import com.apiguard.backend.global.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final StringRedisTemplate redisTemplate;

  private static final String RT_PREFIX = "RT:";

  public LoginResponse login(LoginRequest request) {
    // 1. 이메일로 사용자 조회
    User user = userRepository.findByEmailAndDeletedFalse(request.email())
        .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

    // 2. 비밀번호 확인
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new InvalidCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    // 3. JWT 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    // 4. Redis에 Refresh Token 저장
    redisTemplate.opsForValue().set(
        RT_PREFIX + user.getEmail(),
        refreshToken,
        jwtTokenProvider.getRefreshExpiration(),
        TimeUnit.MILLISECONDS
    );

    // 5. 응답 반환
    return new LoginResponse(accessToken, refreshToken);
  }

  public LoginResponse refresh(RefreshRequest request) {
    String refreshToken = request.refreshToken();

    // 1. RT 유효성 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new InvalidCredentialsException("유효하지 않은 Refresh Token입니다.");
    }

    // 2. 토큰에서 이메일 추출
    String email = jwtTokenProvider.getEmailFromToken(refreshToken);

    // 3. Redis에 저장된 RT와 비교
    String storedToken = redisTemplate.opsForValue().get(RT_PREFIX + email);
    if (storedToken == null || !storedToken.equals(refreshToken)) {
      throw new InvalidCredentialsException("이미 로그아웃된 사용자입니다.");
    }

    // 4. DB 사용자 조회
    User user = userRepository.findByEmailAndDeletedFalse(email)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

    // 5. 새 Access Token 발급
    String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
    String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    // 6. Redis에 새 RT 저장
    redisTemplate.opsForValue().set(
        RT_PREFIX + email,
        newRefreshToken,
        jwtTokenProvider.getRefreshExpiration(),
        TimeUnit.MILLISECONDS
    );

    return new LoginResponse(newAccessToken, newRefreshToken);
  }

  public void logout(LogoutRequest request) {
    String refreshToken = request.refreshToken();

    // 1. RT 유효성 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new InvalidCredentialsException("유효하지 않은 Refresh Token입니다.");
    }

    // 2. 토큰에서 이메일 추출
    String email = jwtTokenProvider.getEmailFromToken(refreshToken);

    // 3. Redis에서 RT 삭제
    redisTemplate.delete(RT_PREFIX + email);
  }

  public void deleteRefreshToken(String email) {
    redisTemplate.delete(RT_PREFIX + email);
  }
}
