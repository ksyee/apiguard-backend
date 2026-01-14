package com.apiguard.backend.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apiguard.backend.domain.auth.dto.LoginRequest;
import com.apiguard.backend.domain.auth.dto.LoginResponse;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.config.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public LoginResponse login(LoginRequest request) {
    // 1. 이메일로 사용자 조회
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다."));

    // 2. 비밀번호 확인
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    // 3. JWT 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

    // 4. 응답 반환
    return new LoginResponse(accessToken, refreshToken);
  }
}
