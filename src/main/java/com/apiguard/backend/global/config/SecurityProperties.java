package com.apiguard.backend.global.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security") // application.yml의 security 하위 속성을 매핑
public class SecurityProperties {
    
    private List<String> whitelist; // security.whitelist 값을 여기에 자동으로 넣어줌
}