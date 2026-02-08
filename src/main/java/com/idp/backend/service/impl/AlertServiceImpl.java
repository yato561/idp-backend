package com.idp.backend.service.impl;

import com.idp.backend.dao.AlertDao;
import com.idp.backend.dto.AlertResponse;
import com.idp.backend.dto.SummaryResponse;
import com.idp.backend.entity.AlertEntity;
import com.idp.backend.mapper.AlertMapper;
import com.idp.backend.service.AlertService;
import com.idp.backend.service.MetricService;
import com.idp.backend.util.AlertRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertDao dao;
    private final MetricService metricService;
    private final AlertRuleEngine rules;


    @Override
    public void evaluate(UUID serviceId) {
        SummaryResponse s= metricService.getSummaryById(serviceId, 15, null,null);

        if (s== null || "NO_DATA".equals(s.getStatus())){
            return;
        }

        trigger(serviceId, "CPU_HIGH", "MEDIUM", rules.cpuHigh(s),
                "CPU usage exceeded threshold");

        trigger(serviceId, "MEMORY_HIGH", "MEDIUM", rules.memoryHigh(s),
                "Memory usage exceeded threshold");

        trigger(serviceId, "SERVICE_DOWN", "HIGH", rules.serviceDown(s),
                "Service is down");
    }

    private void trigger(UUID serviceId, String type, String severity, boolean condition, String message){
        if(!condition) return;

        if(dao.hasOpenAlert(serviceId, type)) return;

        AlertEntity entity= new AlertEntity();
        entity.setServiceId(serviceId);
        entity.setType(type);
        entity.setSeverity(severity);
        entity.setStatus("OPEN");
        entity.setMessage(message);

        dao.save(entity);
    }

    @Override
    public List<AlertResponse> getAll() {
        return dao.findOpen()
                .stream()
                .map(AlertMapper :: toResponse)
                .toList();
    }

    @Override
    public List<AlertResponse> getByService(UUID serviceId) {
        return dao.findByService(serviceId)
                .stream()
                .map(AlertMapper :: toResponse)
                .toList();
    }

    @Override
    public void resolve(UUID alertId) {
        AlertEntity alert = new AlertEntity();
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(Instant.now());
        dao.save(alert);
    }
}
