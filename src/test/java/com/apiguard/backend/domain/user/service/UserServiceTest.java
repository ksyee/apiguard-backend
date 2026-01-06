package com.apiguard.backend.domain.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository; // 테스트용 Mock 레포지토리
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService; // 테스트용 Mock 서비스
    
    @Test
    void signUp() {
        // 1. 준비(Given)
        SignUpRequest request = new SignUpRequest("test@email.com", "test1234", "nickname");
        
        // 2. 실행(When)
        userService.signUp(request);
        
        // 3. 검증(Then)
        verify(userRepository).save(any()); // save가 호출됐는지 확인
    }
}