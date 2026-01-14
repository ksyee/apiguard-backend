package com.apiguard.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.apiguard.backend.domain.auth.dto.LoginRequest;
import com.apiguard.backend.domain.auth.dto.LoginResponse;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.config.JwtTokenProvider;
import com.apiguard.backend.global.exception.InvalidCredentialsException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    @DisplayName("로그인 성공")
    public void testLogin() {
        // given
        String email = "test@example.com";
        String password = "password";
        String encodedPassword = "encodedPassword"; // 인코딩된 패스워드 추가
        
        User user = User.builder()
            .email(email)
            .password(encodedPassword) // 저장된 비밀번호는 해시된 상태
            .role(Role.USER)
            .build();
        
        // Mock 동작 정의
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(email, user.getRole().name())).thenReturn(
            "accessToken");
        when(jwtTokenProvider.createRefreshToken(email)).thenReturn("refreshToken");
        
        // when
        LoginRequest request = new LoginRequest(email, password);
        LoginResponse response = authService.login(request);
        
        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
    }
    
    @Test
    @DisplayName("로그인 실패")
    public void login_WithNonExistentEmail_ThrowException() {
        // given
        when(userRepository.findByEmail("wrong@email.com")).thenReturn(Optional.empty());
        
        // when & then
        LoginRequest request = new LoginRequest("wrong@email.com", "password");
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(
            InvalidCredentialsException.class);
    }
    
    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    public void login_WithWrongPassword_ThrowException() {
        // given
        String email = "test@example.com";
        String wrongPassword = "wrongPassword";
        String correctEncodedPassword = "encodedPassword";
        
        User user = User.builder()
            .email(email)
            .password(correctEncodedPassword)
            .role(Role.USER)
            .build();
        
        // Mock 동작 정의
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongPassword, correctEncodedPassword)).thenReturn(false);
        
        // when & then
        LoginRequest request = new LoginRequest(email, wrongPassword);
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(
            InvalidCredentialsException.class);
    }
}
