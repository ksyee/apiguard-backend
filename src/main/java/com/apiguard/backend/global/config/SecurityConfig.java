package com.apiguard.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            // HTTP Basic 인증 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)
            // Form 로그인 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            // H2 Console 사용을 위한 Frame Options 설정 해제
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            // YAML에서 가져온 whitelist URL들은 모두 허용
            .authorizeHttpRequests(auth -> auth.requestMatchers(
                    securityProperties.getWhitelist().toArray(new String[0])).permitAll().anyRequest()
                .authenticated())
            // JWT 인증 필터 추가
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}