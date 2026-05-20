package com.apiguard.backend.domain.endpoint.entity;

import com.apiguard.backend.domain.project.entity.Project;
import java.time.LocalDateTime;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "endpoints")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> headers;

    @Column(columnDefinition = "text")
    private String body;

    @Builder.Default
    @Column(nullable = false)
    private int expectedStatusCode = 200;

    @Builder.Default
    @Column(nullable = false)
    private int checkInterval = 60;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime lastCheckedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    public void update(
        String url,
        HttpMethod httpMethod,
        Map<String, String> headers,
        String body,
        Integer expectedStatusCode,
        Integer checkInterval
    ) {
        if (url != null) {
            this.url = url;
        }
        if (httpMethod != null) {
            this.httpMethod = httpMethod;
        }
        this.headers = headers;
        this.body = body;
        if (expectedStatusCode != null) {
            this.expectedStatusCode = expectedStatusCode;
        }
        if (checkInterval != null) {
            this.checkInterval = checkInterval;
        }
    }

    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    public void updateLastCheckedAt() {
        this.lastCheckedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
