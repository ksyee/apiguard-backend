package com.apiguard.backend.domain.check.entity;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "check_results")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckStatus status;

    private Integer statusCode;

    private Long responseTimeMs;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
