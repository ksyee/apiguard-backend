package com.apiguard.backend.domain.alert.service;

import com.apiguard.backend.domain.alert.entity.AlertConfig;
import com.apiguard.backend.domain.alert.entity.AlertType;
import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.endpoint.entity.Endpoint;

import java.util.List;

public interface NotificationService {

    boolean supports(AlertType alertType);

    void send(AlertConfig config, Endpoint endpoint, List<CheckResult> recentFailures);
}
