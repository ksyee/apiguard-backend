package com.apiguard.backend.domain.apispec.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "api_spec_diffs")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApiSpecDiff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_source_id", nullable = false)
    private ApiSpecSource specSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_snapshot_id")
    private ApiSpecSnapshot baseSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_snapshot_id", nullable = false)
    private ApiSpecSnapshot headSnapshot;

    @Column(nullable = false)
    private boolean breaking;

    @Column(nullable = false)
    private int breakingChangeCount;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
