package com.apiguard.backend.domain.check.service;

import com.apiguard.backend.domain.check.dto.CheckResultResponse;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.repository.CheckResultRepository;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;
import com.apiguard.backend.domain.endpoint.service.EndpointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckService {

    private final EndpointService endpointService;
    private final HttpCheckerService httpCheckerService;
    private final CheckResultRepository checkResultRepository;

    @Transactional
    public CheckResultResponse testEndpoint(Long endpointId) {
        Endpoint endpoint = endpointService.getEndpointWithOwnerCheck(endpointId);

        CheckResult result = httpCheckerService.check(endpoint);
        CheckResult saved = checkResultRepository.save(result);

        endpoint.updateLastCheckedAt();

        return CheckResultResponse.from(saved);
    }
}
