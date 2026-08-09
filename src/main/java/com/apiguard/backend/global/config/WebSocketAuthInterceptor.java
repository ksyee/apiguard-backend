package com.apiguard.backend.global.config;

import com.apiguard.backend.domain.check.service.CheckTopicAuthorizer;
import com.apiguard.backend.global.exception.UnauthorizedException;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CheckTopicAuthorizer checkTopicAuthorizer;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("WebSocket 연결 거부: Authorization 헤더 누락");
            throw new UnauthorizedException("WebSocket 연결에는 Bearer 토큰이 필요합니다.");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket 연결 거부: 유효하지 않은 토큰");
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }

        Authentication auth = jwtTokenProvider.getAuthentication(token);
        accessor.setUser(auth);
        log.debug("WebSocket 연결 인증 성공: {}", auth.getName());
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            log.warn("WebSocket 구독 거부: 인증되지 않은 세션 (destination={})", accessor.getDestination());
            throw new UnauthorizedException("인증되지 않은 WebSocket 세션입니다.");
        }

        checkTopicAuthorizer.authorizeSubscription(user.getName(), accessor.getDestination());
        log.debug("WebSocket 구독 인가 성공: user={}, destination={}", user.getName(), accessor.getDestination());
    }
}
