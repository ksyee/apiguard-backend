package com.apiguard.backend.domain.admin.notice.service;

import com.apiguard.backend.domain.admin.notice.dto.CreateNoticeRequest;
import com.apiguard.backend.domain.admin.notice.dto.NoticeResponse;
import com.apiguard.backend.domain.admin.notice.dto.UpdateNoticeRequest;
import com.apiguard.backend.domain.admin.notice.entity.Notice;
import com.apiguard.backend.domain.admin.notice.repository.NoticeRepository;
import com.apiguard.backend.global.exception.NoticeNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<NoticeResponse> getNotices() {
        return noticeRepository.findAllByDeletedFalseOrderByPinnedDescCreatedAtDesc().stream()
            .map(NoticeResponse::from)
            .toList();
    }

    public NoticeResponse getNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndDeletedFalse(noticeId)
            .orElseThrow(() -> new NoticeNotFoundException("공지사항을 찾을 수 없습니다."));
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse createNotice(CreateNoticeRequest request) {
        Notice notice = Notice.builder()
            .title(request.title())
            .content(request.content())
            .pinned(request.pinned())
            .build();
        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, UpdateNoticeRequest request) {
        Notice notice = noticeRepository.findByIdAndDeletedFalse(noticeId)
            .orElseThrow(() -> new NoticeNotFoundException("공지사항을 찾을 수 없습니다."));
        notice.update(request.title(), request.content(), request.pinned());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdAndDeletedFalse(noticeId)
            .orElseThrow(() -> new NoticeNotFoundException("공지사항을 찾을 수 없습니다."));
        notice.softDelete();
    }
}
