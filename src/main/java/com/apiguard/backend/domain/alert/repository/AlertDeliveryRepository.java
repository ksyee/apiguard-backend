package com.apiguard.backend.domain.alert.repository;

import com.apiguard.backend.domain.alert.entity.AlertDelivery;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, Long> {

    List<AlertDelivery> findByAlertConfigIdOrderByTriggeredAtDesc(Long alertConfigId, Pageable pageable);
}
