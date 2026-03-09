package com.apiguard.backend.domain.user.repository;

import com.apiguard.backend.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);

    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<User> findByIdAndDeletedFalse(Long id);
}
