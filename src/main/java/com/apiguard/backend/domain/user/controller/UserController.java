package com.apiguard.backend.domain.user.controller;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.dto.UpdateUserRequest;
import com.apiguard.backend.domain.user.dto.UserResponse;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users") // 공통 경로
@RequiredArgsConstructor // Lombok 사용 시 생성자 자동 주입
public class UserController {
    
    private final UserRepository userRepository;
    private final UserService userService;
    
    @GetMapping("/me")
    public ApiResponse<UserResponse> getUser(@Valid @Email String email) {
        User user = userService.getUserDetail();
        
        return ApiResponse.ok(UserResponse.from(user));
    }
    
    @PostMapping("/signup")
    public ApiResponse<Long> signUp(@RequestBody @Valid SignUpRequest request) {
        
        Long userId = userService.signUp(request);
        
        return ApiResponse.ok(userId);
    }
    
    @PatchMapping("/me")
    public ApiResponse<Void> updateMe(@RequestBody @Valid UpdateUserRequest request) {
        userService.updateUser(request);
        return ApiResponse.ok();
    }
}
