package com.apiguard.backend.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users") // 공통 경로
@RequiredArgsConstructor // Lombok 사용 시 생성자 자동 주입
public class UserController {

//    private final UserService userService;
    
    @GetMapping("/getuser")
    public String getUser() {
        return "User details";
    }
}
