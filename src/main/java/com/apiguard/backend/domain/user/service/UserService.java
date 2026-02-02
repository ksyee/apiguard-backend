package com.apiguard.backend.domain.user.service;

import com.apiguard.backend.domain.user.dto.ChangePasswordRequest;
import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.dto.UpdateUserRequest;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.exception.DuplicateEmailException;
import com.apiguard.backend.global.exception.InvalidCredentialsException;
import com.apiguard.backend.global.exception.UnauthorizedException;
import com.apiguard.backend.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 1. null 체크
        if (authentication == null) {
            throw new RuntimeException("인증된 사용자 정보가 없습니다.");
        }
        
        // 2. 익명 사용자 체크
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        
        // 3. 인증 여부 체크
        if (!authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }
        
        // 인증된 사용자의 이름(Email) 꺼내기
        String email = authentication.getName();
        
        // DB 조회
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
    
    // 비밀번호 변경
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // 1. 현재 로그인한 사용자 가져오기
        User user = getUserDetail();
        
        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 3. 새 비밀번호 확인 일치 검증
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new InvalidCredentialsException("새 비밀번호가 일치하지 않습니다.");
        }
        
        // 4. 새 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.newPassword());
        
        user.changePassword(encodedPassword);
    }
}
