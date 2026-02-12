package com.apiguard.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.auth.dto.LoginRequest;
import com.apiguard.backend.domain.auth.dto.LoginResponse;
import com.apiguard.backend.domain.auth.dto.LogoutRequest;
import com.apiguard.backend.domain.auth.dto.RefreshRequest;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.config.JwtTokenProvider;
import com.apiguard.backend.global.exception.InvalidCredentialsException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공")
    void testLogin() {
        // given
        String email = "test@example.com";
        String password = "password";
        String encodedPassword = "encodedPassword";

        User user = User.builder()
            .email(email)
            .password(encodedPassword)
            .role(Role.USER)
            .build();

        given(userRepository.findByEmailAndDeletedFalse(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        given(jwtTokenProvider.createAccessToken(email, user.getRole().name())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(email)).willReturn("refreshToken");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(1209600000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        LoginRequest request = new LoginRequest(email, password);
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        verify(valueOperations).set(eq("RT:" + email), eq("refreshToken"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_WithNonExistentEmail_ThrowException() {
        // Given
        given(userRepository.findByEmailAndDeletedFalse("wrong@email.com")).willReturn(Optional.empty());

        // when & then
        LoginRequest request = new LoginRequest("wrong@email.com", "password");
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_WithWrongPassword_ThrowException() {
        // given
        String email = "test@example.com";
        String wrongPassword = "wrongPassword";
        String correctEncodedPassword = "encodedPassword";

        User user = User.builder()
            .email(email)
            .password(correctEncodedPassword)
            .role(Role.USER)
            .build();

        given(userRepository.findByEmailAndDeletedFalse(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(wrongPassword, correctEncodedPassword)).willReturn(false);

        // when & then
        LoginRequest request = new LoginRequest(email, wrongPassword);
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_Success() {
        // given
        String email = "test@example.com";
        String oldRefreshToken = "oldRefreshToken";

        User user = User.builder()
            .email(email)
            .password("encodedPassword")
            .role(Role.USER)
            .build();

        given(jwtTokenProvider.validateToken(oldRefreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmailFromToken(oldRefreshToken)).willReturn(email);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:" + email)).willReturn(oldRefreshToken);
        given(userRepository.findByEmailAndDeletedFalse(email)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(email, "USER")).willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(email)).willReturn("newRefreshToken");
        given(jwtTokenProvider.getRefreshExpiration()).willReturn(1209600000L);

        // when
        LoginResponse response = authService.refresh(new RefreshRequest(oldRefreshToken));

        // then
        assertThat(response.accessToken()).isEqualTo("newAccessToken");
        assertThat(response.refreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 RT")
    void refresh_ExpiredToken_ThrowException() {
        // given
        String expiredToken = "expiredToken";
        given(jwtTokenProvider.validateToken(expiredToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(expiredToken)))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("유효하지 않은 Refresh Token입니다.");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 로그아웃된 RT")
    void refresh_LoggedOutToken_ThrowException() {
        // given
        String refreshToken = "loggedOutRefreshToken";
        String email = "test@example.com";

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmailFromToken(refreshToken)).willReturn(email);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:" + email)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken)))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("이미 로그아웃된 사용자입니다.");
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        String refreshToken = "validRefreshToken";
        String email = "test@example.com";

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmailFromToken(refreshToken)).willReturn(email);

        // when
        authService.logout(new LogoutRequest(refreshToken));

        // then
        verify(redisTemplate).delete("RT:" + email);
    }
}
