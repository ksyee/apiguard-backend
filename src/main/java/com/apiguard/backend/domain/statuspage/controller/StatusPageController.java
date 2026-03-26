package com.apiguard.backend.domain.statuspage.controller;

import com.apiguard.backend.domain.statuspage.dto.CreateStatusPageRequest;
import com.apiguard.backend.domain.statuspage.dto.PublicStatusPageResponse;
import com.apiguard.backend.domain.statuspage.dto.StatusPageResponse;
import com.apiguard.backend.domain.statuspage.dto.UpdateStatusPageRequest;
import com.apiguard.backend.domain.statuspage.service.StatusPageService;
import com.apiguard.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatusPageController {

    private final StatusPageService statusPageService;

    /**
     * 공개 상태 페이지 조회 (인증 불필요)
     */
    @GetMapping("/status/{slug}")
    public ResponseEntity<ApiResponse<PublicStatusPageResponse>> getPublicStatusPage(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(statusPageService.getPublicPage(slug)));
    }

    /**
     * 워크스페이스 상태 페이지 생성
     */
    @PostMapping("/workspaces/{workspaceId}/status-page")
    public ResponseEntity<ApiResponse<StatusPageResponse>> create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateStatusPageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(statusPageService.create(workspaceId, request)));
    }

    /**
     * 워크스페이스 상태 페이지 조회
     */
    @GetMapping("/workspaces/{workspaceId}/status-page")
    public ResponseEntity<ApiResponse<StatusPageResponse>> get(
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok(ApiResponse.ok(statusPageService.getByWorkspaceId(workspaceId)));
    }

    /**
     * 워크스페이스 상태 페이지 수정
     */
    @PutMapping("/workspaces/{workspaceId}/status-page")
    public ResponseEntity<ApiResponse<StatusPageResponse>> update(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateStatusPageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(statusPageService.update(workspaceId, request)));
    }

    /**
     * 워크스페이스 상태 페이지 삭제
     */
    @DeleteMapping("/workspaces/{workspaceId}/status-page")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long workspaceId) {
        statusPageService.delete(workspaceId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
