package com.apiguard.backend.domain.user.controller;

import com.apiguard.backend.domain.user.dto.SignUpRequest;
import com.apiguard.backend.domain.user.service.UserService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users") // 공통 경로
@RequiredArgsConstructor // Lombok 사용 시 생성자 자동 주입
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/getuser")
    public String getUser() {
        return "User details";
    }
    
    @PostMapping("/signup")
    public ApiResponse<Long> signUp(@RequestBody @Valid SignUpRequest request) {
        
        userService.signUp(request);
        
        return ApiResponse.ok(1L);
    }
}
