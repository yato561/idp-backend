package com.idp.backend.mapper;

import com.idp.backend.dto.MetricRequest;
import com.idp.backend.dto.MetricResponse;
import com.idp.backend.entity.MetricEntity;
import com.idp.backend.entity.ServiceCatInfo;

import java.time.Instant;
import java.util.UUID;

public class MetricMapper {

    public static MetricEntity toEntity(MetricRequest request, ServiceCatInfo service){
        MetricEntity entity= new MetricEntity();
        entity.setServiceId(service.getServiceId());
        entity.setTimeStamp(Instant.now());
        entity.setCpuUsage(request.getCpuUsage());
        entity.setMemoryUsageMb(request.getMemoryUsageMb());
        entity.setVersion(request.getVersion());
        entity.setDeployVersion(request.getDeployVersion());
        entity.setEnv(request.getEnv());
        return entity;
    }

    public static MetricEntity heartbeat(UUID ServiceId){
        MetricEntity metric = new MetricEntity();
        metric.setServiceId(ServiceId);
        metric.setTimeStamp(Instant.now());
        return metric;
    }

    public static MetricResponse toResponse(MetricEntity entity){
        MetricResponse response= new MetricResponse();
        response.setId(entity.getId());
        response.setTimestamp(entity.getTimeStamp());
        response.setCpuUsage(entity.getCpuUsage());
        response.setMemoryUsageMb(entity.getMemoryUsageMb());
        response.setVersion(entity.getVersion());
        response.setDeployVersion(entity.getDeployVersion());
        response.setEnv(entity.getEnv());
        return response;
    }
}
