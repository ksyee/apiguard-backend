package com.apiguard.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 보통 Entity에는 Setter를 쓰지 않음
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    // 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 비밀번호 변경
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // Soft Delete
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
