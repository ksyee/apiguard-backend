package com.apiguard.backend.global.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "apiguard.outbound")
public class OutboundUrlProperties {

    private boolean allowPrivateNetwork = false;
}
