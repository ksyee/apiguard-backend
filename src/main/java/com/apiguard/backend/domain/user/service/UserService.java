package com.apiguard.backend.domain.user.service;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.dto.UpdateUserRequest;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.exception.DuplicateEmailException;
import com.apiguard.backend.global.exception.UserNotFoundException;
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
        // 1. 보안 컨텍스트에서 인증 객체 꺼내기
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 2. 방어 로직 (실제로는 SecurityConfig에서 먼저 막히지만, 안전장치로 둠)
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증된 사용자 정보가 없습니다.");
        }
        
        // 3. 인증된 사용자의 이름(Email) 꺼내기
        String email = authentication.getName();
        
        // 4. DB 조회
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
    
    // 회원가입
    @Transactional
    public Long signUp(SignUpRequest signUpRequest) {
        
        if (userRepository.existsByEmail(signUpRequest.email())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }
        
        String encodedPassword = passwordEncoder.encode(signUpRequest.password());
        
        User user = User.builder()
            .email(signUpRequest.email())
            .nickname(signUpRequest.nickname())
            .password(encodedPassword)
            .role(Role.USER)
            .build();
        
        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }
    
    // 닉네임 변경
    @Transactional
    public void updateUser(UpdateUserRequest request) {
        User user = getUserDetail();
        if (request.nickname() != null) {
            user.updateNickname(request.nickname());
        }
    }
}
