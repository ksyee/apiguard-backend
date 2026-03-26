package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(CheckResultResponse result, Long projectId) {
        messagingTemplate.convertAndSend(
            "/topic/endpoints/" + result.endpointId() + "/checks",
            result
        );

        messagingTemplate.convertAndSend(
            "/topic/projects/" + projectId + "/checks",
            result
        );

        log.debug("체크 결과 WebSocket 발행: endpointId={}, projectId={}", result.endpointId(), projectId);
    }
}
