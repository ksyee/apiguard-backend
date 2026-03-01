package com.apiguard.backend.domain.admin.user.controller;

import com.apiguard.backend.domain.admin.user.dto.AdminUserResponse;
import com.apiguard.backend.domain.admin.user.dto.UpdateUserRoleRequest;
import com.apiguard.backend.domain.admin.user.service.AdminUserService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> getUsers() {
        return ApiResponse.ok(adminUserService.getUsers());
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.getUser(userId));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<Void> updateUserRole(
        @PathVariable Long userId,
        @RequestBody @Valid UpdateUserRoleRequest request
    ) {
        adminUserService.updateUserRole(userId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ApiResponse.ok();
    }
}
