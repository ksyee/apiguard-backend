package com.apiguard.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
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
    void signUp() {
        // Given
        SignUpRequest request = new SignUpRequest("test@email.com", "test1234", "nickname");
        
        // When
        userService.signUp(request);
        
        // Then
        verify(userRepository).save(any());
    }

    @Test
    void getUserDetail() {
        // Given
        String email = "test@email.com";
        User user = User.builder().email(email).build();
        
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn(email);
        SecurityContextHolder.setContext(securityContext);
        
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        
        // When
        User result = userService.getUserDetail();
        
        // Then
        assertThat(result.getEmail()).isEqualTo(email);
    }
}
