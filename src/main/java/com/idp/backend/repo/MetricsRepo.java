package com.idp.backend.repo;

import com.idp.backend.dto.SummaryProjection;
import com.idp.backend.entity.MetricEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.Instant;
import java.util.UUID;

public interface MetricsRepo extends JpaRepository<MetricEntity, UUID>, JpaSpecificationExecutor<MetricEntity> {

    Page<MetricEntity> findByServiceId(
            UUID serviceId,
            Pageable pageable
    );

    MetricEntity findTopByServiceIdAndCpuUsageIsNotNullOrderByTimeStampDesc(UUID serviceId);

    MetricEntity findTopByServiceIdOrderByTimeStampDesc(UUID serviceId);

    @Query("""
    SELECT
        AVG(m.cpuUsage) AS avgCpu,
        MAX(m.cpuUsage) AS maxCpu,
        AVG(m.memoryUsageMb) AS avgMemory,
        MAX(m.memoryUsageMb) AS maxMemory,
        MAX(m.timeStamp) AS lastSeen,
        MAX(m.version) AS version,
        MAX(m.env) AS env
    FROM MetricEntity m
    WHERE m.serviceId = :serviceId
      AND m.timeStamp BETWEEN :from AND :to
      AND m.cpuUsage IS NOT NULL
      AND m.memoryUsageMb IS NOT NULL
""")
    SummaryProjection summarize(
            @Param("serviceId") UUID serviceId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    SELECT MAX(m.timeStamp)
    FROM MetricEntity m
    WHERE m.serviceId = :serviceId
""")
    Instant findLatestTimestamp(@Param("serviceId") UUID serviceId);

}
