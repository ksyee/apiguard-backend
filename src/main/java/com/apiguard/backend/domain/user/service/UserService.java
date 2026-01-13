package com.apiguard.backend.domain.user.service;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션으로 설정(조회 성능 최적화)
public class UserService {
    
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    
    // 사용자 정보 조회(ID 기준)
    public User getUserDetail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
    
    @Transactional
    public void signUp(SignUpRequest signUpRequest) {
        
        if (userRepository.existsByEmail(signUpRequest.email())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        
        String encodedPassword = passwordEncoder.encode(signUpRequest.password());
        
        User user = User.builder()
            .email(signUpRequest.email())
            .nickname(signUpRequest.nickname())
            .password(encodedPassword)
            .role(Role.USER)
            .build();
        
        userRepository.save(user);
    }
}
