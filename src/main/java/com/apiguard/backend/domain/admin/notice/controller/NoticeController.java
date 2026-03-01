package com.apiguard.backend.domain.admin.notice.controller;

import com.apiguard.backend.domain.admin.notice.dto.NoticeResponse;
import com.apiguard.backend.domain.admin.notice.service.NoticeService;
import com.apiguard.backend.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ApiResponse<List<NoticeResponse>> getNotices() {
        return ApiResponse.ok(noticeService.getNotices());
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeResponse> getNotice(@PathVariable Long noticeId) {
        return ApiResponse.ok(noticeService.getNotice(noticeId));
    }
}
