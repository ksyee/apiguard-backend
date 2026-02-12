package com.apiguard.backend.domain.endpoint.entity;

import com.apiguard.backend.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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

    @Column(columnDefinition = "jsonb")
    private String headers;

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

    public void update(String url, HttpMethod httpMethod, String headers, String body,
                       Integer expectedStatusCode, Integer checkInterval) {
        if (url != null) this.url = url;
        if (httpMethod != null) this.httpMethod = httpMethod;
        if (headers != null) this.headers = headers;
        if (body != null) this.body = body;
        if (expectedStatusCode != null) this.expectedStatusCode = expectedStatusCode;
        if (checkInterval != null) this.checkInterval = checkInterval;
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
