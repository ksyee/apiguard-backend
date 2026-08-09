package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.repository.EndpointRepository;
import com.apiguard.backend.domain.project.entity.Project;
import com.apiguard.backend.domain.project.repository.ProjectRepository;
import com.apiguard.backend.domain.user.entity.User;
import com.apiguard.backend.domain.user.repository.UserRepository;
import com.apiguard.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.UnauthorizedException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * WebSocket 체크 토픽(/topic/endpoints/**, /topic/projects/**) 구독 인가를 담당한다.
 * 워크스페이스 프로젝트는 워크스페이스 멤버십을, 개인 프로젝트는 소유자 여부를 검증하며
 * 허용 목록에 없는 destination은 기본적으로 거부한다.
 */
@Component
@RequiredArgsConstructor
public class CheckTopicAuthorizer {

    private static final Pattern ENDPOINT_TOPIC = Pattern.compile("^/topic/endpoints/(\\d+)(?:/.*)?$");
    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/projects/(\\d+)(?:/.*)?$");

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional(readOnly = true)
    public void authorizeSubscription(String email, String destination) {
        if (destination == null || destination.isBlank()) {
            throw new ForbiddenException("구독이 허용되지 않은 destination입니다.");
        }

        User user = userRepository.findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));

        Matcher endpointMatcher = ENDPOINT_TOPIC.matcher(destination);
        if (endpointMatcher.matches()) {
            authorizeEndpointTopic(Long.parseLong(endpointMatcher.group(1)), user);
            return;
        }

        Matcher projectMatcher = PROJECT_TOPIC.matcher(destination);
        if (projectMatcher.matches()) {
            authorizeProjectTopic(Long.parseLong(projectMatcher.group(1)), user);
            return;
        }

        // default deny: 허용 목록에 없는 destination은 모두 거부한다.
        throw new ForbiddenException("구독이 허용되지 않은 destination입니다.");
    }

    private void authorizeEndpointTopic(Long endpointId, User user) {
        // 존재 여부가 노출되지 않도록 조회 실패도 권한 오류로 처리한다.
        Endpoint endpoint = endpointRepository.findByIdAndDeletedFalse(endpointId)
            .orElseThrow(() -> new ForbiddenException("해당 토픽을 구독할 권한이 없습니다."));

        if (endpoint.getProject().isDeleted()) {
            throw new ForbiddenException("해당 토픽을 구독할 권한이 없습니다.");
        }

        authorizeProjectAccess(endpoint.getProject(), user);
    }

    private void authorizeProjectTopic(Long projectId, User user) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ForbiddenException("해당 토픽을 구독할 권한이 없습니다."));

        authorizeProjectAccess(project, user);
    }

    private void authorizeProjectAccess(Project project, User user) {
        if (project.getWorkspace() != null) {
            workspaceMemberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("해당 토픽을 구독할 권한이 없습니다."));
        } else if (project.getUser() == null || !project.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("해당 토픽을 구독할 권한이 없습니다.");
        }
    }
}
