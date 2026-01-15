package com.apiguard.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.exception.DuplicateEmailException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    @DisplayName("회원가입")
    void signUp() {
        // Given
        SignUpRequest request = new SignUpRequest("test@email.com", "test1234", "nickname");
        
        // When
        userService.signUp(request);
        
        // Then
        verify(userRepository).save(any());
    }
    
    @Test
    @DisplayName("사용자 정보 조회")
    void getUserDetail() {
        // Given
        String email = "test@email.com";
        User user = User.builder().email(email).build();
        
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn(email);
        SecurityContextHolder.setContext(securityContext);
        
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        
        // When
        User result = userService.getUserDetail();
        
        // Then
        assertThat(result.getEmail()).isEqualTo(email);
    }
    
    @Test
    @DisplayName("이메일 중복 시 예외 발생")
    void signUp_duplicateEmail_throwsException() {
        // given
        SignUpRequest request = new SignUpRequest("test@email.com", "password1234!", "john doe");
        given(userRepository.existsByEmail(request.email())).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> userService.signUp(request)).isInstanceOf(
            DuplicateEmailException.class).hasMessage("이미 사용 중인 이메일입니다.");
    }
    
    @Test
    @DisplayName("인증 정보 없으면 예외 발생")
    void getUserDetail_noAuthentication_throwsException() {
        // given
        SecurityContextHolder.clearContext();
        
        // when & then
        assertThatThrownBy(userService::getUserDetail).isInstanceOf(
            RuntimeException.class).hasMessage("인증된 사용자 정보가 없습니다.");
    }
    
    @Test
    @DisplayName("사용자 없으면 예외 발생")
    void getUserDetail_userNotFound_throwsException() {
        // given
        String email = "notfound@email.com";
        
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn(email);
        SecurityContextHolder.setContext(securityContext);
        
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(userService::getUserDetail).isInstanceOf(
            RuntimeException.class).hasMessage("사용자를 찾을 수 없습니다.");
    }
}
