package com.apiguard.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(timeoutFactory());
    }

    /**
     * 엔드포인트 점검 전용 RestTemplate.
     *
     * <p>기본 RestTemplate은 대상이 4xx/5xx를 반환하면 예외를 던지므로, 점검 입장에서는
     * 실제 상태 코드를 잃고 ERROR로만 기록된다. 점검은 대상의 상태 코드 자체가 관측 대상이므로
     * hasError를 항상 false로 두어 어떤 상태 코드든 ResponseEntity로 받아 그대로 기록한다.
     * (응답을 받지 못한 연결 실패/타임아웃은 여전히 ResourceAccessException으로 구분된다.)
     */
    @Bean
    public RestTemplate checkRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(timeoutFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return restTemplate;
    }

    private SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return factory;
    }
}
