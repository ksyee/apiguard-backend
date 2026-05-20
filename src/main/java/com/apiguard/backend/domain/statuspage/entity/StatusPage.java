package com.apiguard.backend.domain.statuspage.entity;

import com.apiguard.backend.domain.workspace.entity.Workspace;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_pages")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StatusPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean allEndpoints = true;

    @ElementCollection
    @CollectionTable(
        name = "status_page_endpoints",
        joinColumns = @JoinColumn(name = "status_page_id")
    )
    @Column(name = "endpoint_id", nullable = false)
    @Builder.Default
    private Set<Long> selectedEndpointIds = new LinkedHashSet<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    public void update(String title, String description, Boolean isPublic) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (isPublic != null) this.isPublic = isPublic;
    }

    public void updateEndpointSelection(boolean allEndpoints, Set<Long> endpointIds) {
        this.allEndpoints = allEndpoints;
        this.selectedEndpointIds.clear();
        if (!allEndpoints) {
            this.selectedEndpointIds.addAll(endpointIds);
        }
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
