package com.idp.backend.dao.impl;

import com.idp.backend.dao.MetricsDao;
import com.idp.backend.dto.SummaryProjection;
import com.idp.backend.dto.SummaryResponse;
import com.idp.backend.entity.MetricEntity;
import com.idp.backend.repo.MetricsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class MetricsDaoImpl implements MetricsDao {

    @Autowired
    public MetricsRepo metricsRepo;
    @Override
    public MetricEntity save(MetricEntity metric) {
        return metricsRepo.save(metric);
    }

    @Override
    public Page<MetricEntity> findByServiceId(UUID serviceId, Pageable page) {
        return metricsRepo.findByServiceId(serviceId,page);
    }

    @Override
    public MetricEntity findLatest(UUID serviceId) {
        return metricsRepo.findTopByServiceIdAndCpuUsageIsNotNullOrderByTimeStampDesc(serviceId);
    }

    @Override
    public MetricEntity fetchHealth(UUID serviceId) {
        return metricsRepo.findTopByServiceIdOrderByTimeStampDesc(serviceId);
    }

    @Override
    public SummaryProjection getSummary(UUID serviceId, Instant from, Instant to) {
        return metricsRepo.summarize(serviceId, from, to);
    }
    @Override
    public Instant findLatestTimestamp(UUID serviceId) {
        return metricsRepo.findLatestTimestamp(serviceId);
    }

}
