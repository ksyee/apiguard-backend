package com.apiguard.backend.domain.admin.notice.controller;

import com.apiguard.backend.domain.admin.notice.dto.CreateNoticeRequest;
import com.apiguard.backend.domain.admin.notice.dto.NoticeResponse;
import com.apiguard.backend.domain.admin.notice.dto.UpdateNoticeRequest;
import com.apiguard.backend.domain.admin.notice.service.NoticeService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeResponse> createNotice(@RequestBody @Valid CreateNoticeRequest request) {
        return ApiResponse.ok(noticeService.createNotice(request));
    }

    @PutMapping("/{noticeId}")
    public ApiResponse<NoticeResponse> updateNotice(
        @PathVariable Long noticeId,
        @RequestBody @Valid UpdateNoticeRequest request
    ) {
        return ApiResponse.ok(noticeService.updateNotice(noticeId, request));
    }

    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.ok();
    }
}
