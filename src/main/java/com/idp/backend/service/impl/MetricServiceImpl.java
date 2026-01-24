package com.idp.backend.service.impl;

import com.idp.backend.dao.MetricsDao;
import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.dto.HealthResponse;
import com.idp.backend.dto.MetricRequest;
import com.idp.backend.dto.MetricResponse;
import com.idp.backend.entity.MetricEntity;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.mapper.MetricMapper;
import com.idp.backend.service.MetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class MetricServiceImpl implements MetricService {

    @Autowired
    public MetricsDao metricsDao;

    @Autowired
    public ServiceCatDao serviceDao;

    @Value("${health.threshold.seconds:120}")
    private long thresholdSeconds;

    @Override
    public void ingest(UUID serviceId, MetricRequest request) {
        ServiceCatInfo service= serviceDao.findById(serviceId);
        metricsDao.save(MetricMapper.toEntity(request,service));
    }

    @Override
    public void heartbeat(UUID serviceId) {
        metricsDao.save(MetricMapper.heartbeat(serviceId));
    }

    @Override
    public MetricResponse latest(UUID serviceId) {

        return MetricMapper.toResponse(metricsDao.findLatest(serviceId));
    }

    @Override
    public Page<MetricResponse> history(UUID serviceId, Pageable page) {

        return metricsDao.findByServiceId(serviceId,page).map(MetricMapper::toResponse);
    }

    @Override
    public HealthResponse getHealth(UUID serviceId) {
        MetricEntity latest= metricsDao.fetchHealth(serviceId);

        if(latest== null){
            return new HealthResponse(
                    serviceId, null, false, -1
            );
        }

        long diff = Duration.between(latest.getTimeStamp(), Instant.now()).getSeconds();

        return new HealthResponse(serviceId, latest.getTimeStamp(), diff<= thresholdSeconds,diff);
    }
}
