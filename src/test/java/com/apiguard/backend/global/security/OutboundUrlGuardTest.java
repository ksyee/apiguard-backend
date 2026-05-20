package com.apiguard.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboundUrlGuardTest {

    @Test
    @DisplayName("공개 HTTP URL은 허용한다")
    void validateHttpUrl_publicAddress_allowed() {
        OutboundUrlGuard guard = new OutboundUrlGuard(properties(false));

        URI uri = guard.validateHttpUrl("https://8.8.8.8/health", "엔드포인트 URL");

        assertThat(uri).isEqualTo(URI.create("https://8.8.8.8/health"));
    }

    @Test
    @DisplayName("localhost URL은 기본적으로 차단한다")
    void validateHttpUrl_localhost_blocked() {
        OutboundUrlGuard guard = new OutboundUrlGuard(properties(false));

        assertThatThrownBy(() -> guard.validateHttpUrl("http://localhost:8080/health", "엔드포인트 URL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("private network");
    }

    @Test
    @DisplayName("사설 IP URL은 기본적으로 차단한다")
    void validateHttpUrl_privateAddress_blocked() {
        OutboundUrlGuard guard = new OutboundUrlGuard(properties(false));

        assertThatThrownBy(() -> guard.validateHttpUrl("http://10.0.0.5/health", "엔드포인트 URL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("private network");
    }

    @Test
    @DisplayName("dev/test 설정에서는 사설 네트워크 URL을 허용할 수 있다")
    void validateHttpUrl_privateAddressAllowedByProperty() {
        OutboundUrlGuard guard = new OutboundUrlGuard(properties(true));

        URI uri = guard.validateHttpUrl("http://localhost:8080/health", "엔드포인트 URL");

        assertThat(uri).isEqualTo(URI.create("http://localhost:8080/health"));
    }

    @Test
    @DisplayName("http/https가 아닌 URL은 차단한다")
    void validateHttpUrl_invalidScheme_blocked() {
        OutboundUrlGuard guard = new OutboundUrlGuard(properties(true));

        assertThatThrownBy(() -> guard.validateHttpUrl("file:///etc/passwd", "엔드포인트 URL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("http/https");
    }

    private OutboundUrlProperties properties(boolean allowPrivateNetwork) {
        OutboundUrlProperties properties = new OutboundUrlProperties();
        properties.setAllowPrivateNetwork(allowPrivateNetwork);
        return properties;
    }
}
