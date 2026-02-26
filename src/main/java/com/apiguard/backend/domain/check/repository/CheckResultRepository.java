package com.apiguard.backend.domain.check.repository;

import com.apiguard.backend.domain.check.entity.CheckResult;
import com.apiguard.backend.domain.check.entity.CheckStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    long countByEndpointIdAndCheckedAtAfter(Long endpointId, LocalDateTime after);

    long countByEndpointIdAndStatusAndCheckedAtAfter(Long endpointId, CheckStatus status, LocalDateTime after);

    @Query("SELECT AVG(cr.responseTimeMs) FROM CheckResult cr WHERE cr.endpoint.id = :endpointId AND cr.checkedAt > :after")
    Double findAvgResponseTimeByEndpointIdAndCheckedAtAfter(@Param("endpointId") Long endpointId, @Param("after") LocalDateTime after);

    @Query(value = "SELECT date_trunc('hour', cr.checked_at) AS hour, " +
            "COUNT(*) AS check_count, " +
            "COUNT(*) FILTER (WHERE cr.status = 'SUCCESS') AS success_count, " +
            "AVG(cr.response_time_ms) AS avg_response_time_ms " +
            "FROM check_results cr " +
            "WHERE cr.endpoint_id = :endpointId AND cr.checked_at > :after " +
            "GROUP BY date_trunc('hour', cr.checked_at) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyStatsByEndpointId(@Param("endpointId") Long endpointId, @Param("after") LocalDateTime after);

    List<CheckResult> findByEndpointIdOrderByCheckedAtDesc(Long endpointId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM CheckResult cr WHERE cr.endpoint.id = :endpointId AND cr.checkedAt < :cutoff")
    int deleteByEndpointIdAndCheckedAtBefore(@Param("endpointId") Long endpointId, @Param("cutoff") LocalDateTime cutoff);
}
