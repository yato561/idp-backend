package com.idp.backend.util.async;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.idp.backend.dao.MetricsDao;
import com.idp.backend.dao.ServiceCatDao;
import com.idp.backend.dto.MetricRequest;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.mapper.MetricMapper;

@Component
@Profile("!kafka")
public class DefaultMetricIngestor implements AsyncIngestor<MetricRequest> {

    private final MetricsDao metricsDao;
    private final ServiceCatDao serviceDao;

    public DefaultMetricIngestor(MetricsDao metricsDao, ServiceCatDao serviceDao) {
        this.metricsDao = metricsDao;
        this.serviceDao = serviceDao;
    }

    @Override
    public void submit(MetricRequest payload) {
        ServiceCatInfo service = serviceDao.findById(payload.getServiceId());
        if (service != null) {
            metricsDao.save(MetricMapper.toEntity(payload, service));
        }
    }
}
