package com.apiguard.backend.domain.user.repository;

import com.apiguard.backend.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 활성 회원을 찾는 기능
    Optional<User> findByEmailAndDeletedFalse(String email);

    // 이메일 중복 확인 (탈퇴 회원 포함)
    boolean existsByEmail(String email);

    // 활성 회원 전체 조회 (어드민 전용)
    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();

    // ID로 활성 회원 조회 (어드민 전용)
    Optional<User> findByIdAndDeletedFalse(Long id);
}
