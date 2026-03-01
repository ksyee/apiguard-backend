package com.apiguard.backend.domain.admin.user.service;

import com.apiguard.backend.domain.admin.user.dto.AdminUserResponse;
import com.apiguard.backend.domain.admin.user.dto.UpdateUserRoleRequest;
import com.apiguard.backend.domain.user.entity.Role;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.global.exception.UserNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public List<AdminUserResponse> getUsers() {
        return userRepository.findAllByDeletedFalseOrderByCreatedAtDesc().stream()
            .map(AdminUserResponse::from)
            .toList();
    }

    public AdminUserResponse getUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 역할입니다. (USER 또는 ADMIN)");
        }

        user.changeRole(role);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        user.softDelete();
    }
}
