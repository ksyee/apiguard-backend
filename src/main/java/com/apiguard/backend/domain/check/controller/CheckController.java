package com.apiguard.backend.domain.check.controller;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import com.apiguard.backend.domain.check.service.CheckService;
import com.apiguard.backend.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    @PostMapping("/endpoints/{id}/test")
    public ApiResponse<CheckResultResponse> testEndpoint(@PathVariable Long id) {
        return ApiResponse.ok(checkService.testEndpoint(id));
    }
}
