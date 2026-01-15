package com.apiguard.backend.domain.user.repository;

import com.apiguard.backend.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // 이메일로 회원을 찾는 기능
    Optional<User> findByEmail(String email);
    
    // 이메일 중복 확인
    boolean existsByEmail(String email);
}

