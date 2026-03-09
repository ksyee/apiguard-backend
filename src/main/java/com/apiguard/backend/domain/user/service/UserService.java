package com.apiguard.backend.domain.user.service;

import com.apiguard.backend.domain.auth.service.AuthService;
import com.apiguard.backend.domain.user.dto.ChangePasswordRequest;
import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.dto.UpdateUserRequest;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.global.exception.DuplicateEmailException;
import com.apiguard.backend.global.exception.InvalidCredentialsException;
import com.apiguard.backend.global.exception.UnauthorizedException;
import com.apiguard.backend.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Autowired(required = false)
    private WorkspaceService workspaceService;

    public User getUserDetail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        if (!authentication.isAuthenticated()) {
            throw new UnauthorizedException("인증되지 않은 사용자입니다.");
        }

        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

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

        if (workspaceService != null) {
            workspaceService.createPersonalWorkspace(savedUser);
        }

        return savedUser.getId();
    }

    @Transactional
    public void updateUser(UpdateUserRequest request) {
        User user = getUserDetail();
        if (request.nickname() != null) {
            user.updateNickname(request.nickname());
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getUserDetail();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new InvalidCredentialsException("새 비밀번호가 일치하지 않습니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedPassword);
    }

    @Transactional
    public void deleteUser() {
        User user = getUserDetail();
        user.softDelete();
        authService.deleteRefreshToken(user.getEmail());
    }
}
