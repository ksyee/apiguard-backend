package com.apiguard.backend.domain.auth.service;

import com.apiguard.backend.domain.auth.dto.LoginRequest;
import com.apiguard.backend.domain.auth.dto.LoginResponse;
import com.apiguard.backend.domain.auth.dto.LogoutRequest;
import com.apiguard.backend.domain.auth.dto.RefreshRequest;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.config.JwtTokenProvider;
import com.apiguard.backend.global.exception.InvalidCredentialsException;
import com.apiguard.backend.global.exception.UserNotFoundException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String RT_PREFIX = "RT:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        redisTemplate.opsForValue().set(
            RT_PREFIX + user.getEmail(),
            refreshToken,
            jwtTokenProvider.getRefreshExpiration(),
            TimeUnit.MILLISECONDS
        );

        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("유효하지 않은 Refresh Token입니다.");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        String storedToken = redisTemplate.opsForValue().get(RT_PREFIX + email);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidCredentialsException("이미 로그아웃된 사용자입니다.");
        }

        User user = userRepository.findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

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

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("유효하지 않은 Refresh Token입니다.");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        redisTemplate.delete(RT_PREFIX + email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(RT_PREFIX + email);
    }
}
