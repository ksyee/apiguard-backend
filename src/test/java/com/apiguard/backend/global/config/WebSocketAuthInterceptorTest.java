package com.apiguard.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.apiguard.backend.domain.check.service.CheckTopicAuthorizer;
import com.apiguard.backend.global.exception.ForbiddenException;
import com.apiguard.backend.global.exception.UnauthorizedException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    private static final String EMAIL = "test@email.com";
    private static final String TOKEN = "valid-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CheckTopicAuthorizer checkTopicAuthorizer;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private Message<byte[]> createMessage(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication createAuthentication() {
        return new UsernamePasswordAuthenticationToken(EMAIL, "", List.of());
    }

    @Test
    @DisplayName("CONNECT: 유효한 Bearer 토큰이면 인증 정보를 세션에 설정하고 통과시킨다")
    void connect_withValidToken_setsUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + TOKEN);
        Message<byte[]> message = createMessage(accessor);

        given(jwtTokenProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(TOKEN)).willReturn(createAuthentication());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("CONNECT: Authorization 헤더가 없으면 연결을 거부한다")
    void connect_withoutAuthorizationHeader_throwsUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = createMessage(accessor);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(UnauthorizedException.class);
        verify(jwtTokenProvider, never()).getAuthentication(TOKEN);
    }

    @Test
    @DisplayName("CONNECT: Bearer 형식이 아닌 헤더면 연결을 거부한다")
    void connect_withNonBearerHeader_throwsUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Basic abc123");
        Message<byte[]> message = createMessage(accessor);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("CONNECT: 유효하지 않은 토큰이면 연결을 거부한다")
    void connect_withInvalidToken_throwsUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid-token");
        Message<byte[]> message = createMessage(accessor);

        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(UnauthorizedException.class);
        verify(jwtTokenProvider, never()).getAuthentication("invalid-token");
    }

    @Test
    @DisplayName("SUBSCRIBE: 인증된 세션이면 토픽 인가를 위임하고 통과시킨다")
    void subscribe_withAuthenticatedSession_delegatesAuthorization() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(createAuthentication());
        accessor.setDestination("/topic/endpoints/1/checks");
        Message<byte[]> message = createMessage(accessor);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(checkTopicAuthorizer).authorizeSubscription(EMAIL, "/topic/endpoints/1/checks");
    }

    @Test
    @DisplayName("SUBSCRIBE: 인증되지 않은 세션이면 구독을 거부한다")
    void subscribe_withoutPrincipal_throwsUnauthorized() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/endpoints/1/checks");
        Message<byte[]> message = createMessage(accessor);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(checkTopicAuthorizer);
    }

    @Test
    @DisplayName("SUBSCRIBE: 인가 실패 예외가 그대로 전파된다")
    void subscribe_whenAuthorizerRejects_propagatesException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(createAuthentication());
        accessor.setDestination("/topic/projects/99/checks");
        Message<byte[]> message = createMessage(accessor);

        willThrow(new ForbiddenException("해당 토픽을 구독할 권한이 없습니다."))
            .given(checkTopicAuthorizer).authorizeSubscription(EMAIL, "/topic/projects/99/checks");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("CONNECT/SUBSCRIBE 외 커맨드는 검증 없이 통과시킨다")
    void otherCommands_passThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<byte[]> message = createMessage(accessor);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(jwtTokenProvider, checkTopicAuthorizer);
    }
}
