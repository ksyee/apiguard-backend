package com.apiguard.backend.domain.statuspage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.entity.HttpMethod;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.statuspage.dto.CreateStatusPageRequest;
import com.apiguard.backend.domain.statuspage.entity.StatusPage;
import com.apiguard.backend.domain.statuspage.repository.StatusPageRepository;
import com.apiguard.backend.domain.workspace.entity.Workspace;
import com.apiguard.backend.domain.workspace.service.WorkspaceService;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusPageServiceTest {

    @Mock
    private StatusPageRepository statusPageRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private StatusPageService statusPageService;

    @Test
    @DisplayName("상태 페이지 생성 시 공개할 엔드포인트 선택 목록을 저장한다")
    void create_savesSelectedEndpointIds() {
        Workspace workspace = Workspace.builder()
            .id(1L)
            .name("Workspace")
            .slug("workspace")
            .build();
        Project project = Project.builder()
            .id(1L)
            .workspace(workspace)
            .name("Project")
            .build();
        Endpoint endpoint = Endpoint.builder()
            .id(10L)
            .project(project)
            .url("https://api.example.com/health")
            .httpMethod(HttpMethod.GET)
            .expectedStatusCode(200)
            .checkInterval(300)
            .isActive(true)
            .build();

        given(workspaceService.getWorkspaceWithMemberCheck(1L)).willReturn(workspace);
        given(statusPageRepository.findByWorkspaceIdAndDeletedFalse(1L)).willReturn(Optional.empty());
        given(statusPageRepository.existsBySlug("public-api")).willReturn(false);
        given(endpointRepository.findByProject_Workspace_IdAndDeletedFalse(1L)).willReturn(List.of(endpoint));
        given(statusPageRepository.save(org.mockito.ArgumentMatchers.any(StatusPage.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        statusPageService.create(
            1L,
            new CreateStatusPageRequest("Public API", "Status", "public-api", false, List.of(10L))
        );

        ArgumentCaptor<StatusPage> captor = ArgumentCaptor.forClass(StatusPage.class);
        verify(statusPageRepository).save(captor.capture());
        assertThat(captor.getValue().isAllEndpoints()).isFalse();
        assertThat(captor.getValue().getSelectedEndpointIds()).containsExactly(10L);
    }

    @Test
    @DisplayName("상태 페이지 생성 시 엔드포인트 선택값이 없으면 전체 공개로 저장한다")
    void create_withoutEndpointSelection_savesAllEndpoints() {
        Workspace workspace = Workspace.builder()
            .id(1L)
            .name("Workspace")
            .slug("workspace")
            .build();

        given(workspaceService.getWorkspaceWithMemberCheck(1L)).willReturn(workspace);
        given(statusPageRepository.findByWorkspaceIdAndDeletedFalse(1L)).willReturn(Optional.empty());
        given(statusPageRepository.existsBySlug("public-api")).willReturn(false);
        given(statusPageRepository.save(org.mockito.ArgumentMatchers.any(StatusPage.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        statusPageService.create(
            1L,
            new CreateStatusPageRequest("Public API", "Status", "public-api", null, null)
        );

        ArgumentCaptor<StatusPage> captor = ArgumentCaptor.forClass(StatusPage.class);
        verify(statusPageRepository).save(captor.capture());
        assertThat(captor.getValue().isAllEndpoints()).isTrue();
        assertThat(captor.getValue().getSelectedEndpointIds()).isEmpty();
    }

    @Test
    @DisplayName("상태 페이지 생성 시 빈 선택 목록과 전체 공개 false를 구분해 저장한다")
    void create_withEmptySelection_savesNoPublicEndpoints() {
        Workspace workspace = Workspace.builder()
            .id(1L)
            .name("Workspace")
            .slug("workspace")
            .build();

        given(workspaceService.getWorkspaceWithMemberCheck(1L)).willReturn(workspace);
        given(statusPageRepository.findByWorkspaceIdAndDeletedFalse(1L)).willReturn(Optional.empty());
        given(statusPageRepository.existsBySlug("public-api")).willReturn(false);
        given(statusPageRepository.save(org.mockito.ArgumentMatchers.any(StatusPage.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        statusPageService.create(
            1L,
            new CreateStatusPageRequest("Public API", "Status", "public-api", false, List.of())
        );

        ArgumentCaptor<StatusPage> captor = ArgumentCaptor.forClass(StatusPage.class);
        verify(statusPageRepository).save(captor.capture());
        assertThat(captor.getValue().isAllEndpoints()).isFalse();
        assertThat(captor.getValue().getSelectedEndpointIds()).isEmpty();
    }
}
