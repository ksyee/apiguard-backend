package com.apiguard.backend.domain.admin.notice.repository;

import com.apiguard.backend.domain.admin.notice.entity.Notice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByDeletedFalseOrderByPinnedDescCreatedAtDesc();

    Optional<Notice> findByIdAndDeletedFalse(Long id);
}
