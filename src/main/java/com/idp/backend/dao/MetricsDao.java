package com.idp.backend.dao;

import com.idp.backend.dto.SummaryProjection;
import com.idp.backend.dto.SummaryResponse;
import com.idp.backend.entity.MetricEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface MetricsDao {

    MetricEntity save(MetricEntity metric);
    Page<MetricEntity> findByServiceId(UUID serviceId, Pageable page);
    MetricEntity findLatest(UUID serviceId);
    MetricEntity fetchHealth(UUID serviceId);
    SummaryProjection getSummary(UUID serviceId, Instant from,Instant to);
    Instant findLatestTimestamp(UUID serviceId);

}
