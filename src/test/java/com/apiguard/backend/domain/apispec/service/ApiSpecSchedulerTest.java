package com.apiguard.backend.domain.apispec.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.apiguard.backend.domain.apispec.entity.ApiSpecSource;
import com.apiguard.backend.domain.apispec.repository.ApiSpecSourceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApiSpecSchedulerTest {

    @Mock
    private ApiSpecSourceRepository specSourceRepository;

    @Mock
    private ApiSpecService apiSpecService;

    private ApiSpecScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ApiSpecScheduler(specSourceRepository, apiSpecService);
        ReflectionTestUtils.setField(scheduler, "autoCheckEnabled", true);
    }

    @Test
    @DisplayName("활성 OpenAPI 소스를 자동 검사한다")
    void scheduleSpecChecks_checksActiveSources() {
        ApiSpecSource source = ApiSpecSource.builder().id(1L).name("API").specUrl("https://example.com/openapi.json").build();
        given(specSourceRepository.findSchedulableActiveSources())
            .willReturn(List.of(source));

        scheduler.scheduleSpecChecks();

        verify(apiSpecService).checkActiveSource(1L);
    }

    @Test
    @DisplayName("개별 OpenAPI 소스 검사 실패는 다음 소스 검사에 영향 주지 않는다")
    void scheduleSpecChecks_individualFailure_continues() {
        ApiSpecSource source1 = ApiSpecSource.builder().id(1L).name("API 1").specUrl("https://example.com/1.json").build();
        ApiSpecSource source2 = ApiSpecSource.builder().id(2L).name("API 2").specUrl("https://example.com/2.json").build();
        given(specSourceRepository.findSchedulableActiveSources())
            .willReturn(List.of(source1, source2));
        willThrow(new IllegalArgumentException("fetch failed")).given(apiSpecService).checkActiveSource(1L);

        scheduler.scheduleSpecChecks();

        verify(apiSpecService).checkActiveSource(1L);
        verify(apiSpecService).checkActiveSource(2L);
    }

    @Test
    @DisplayName("자동 검사가 비활성화되면 소스를 조회하지 않는다")
    void scheduleSpecChecks_disabled_skips() {
        ReflectionTestUtils.setField(scheduler, "autoCheckEnabled", false);

        scheduler.scheduleSpecChecks();

        verify(specSourceRepository, never()).findSchedulableActiveSources();
    }
}
