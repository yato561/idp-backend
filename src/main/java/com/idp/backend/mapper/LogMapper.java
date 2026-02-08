package com.idp.backend.mapper;


import com.idp.backend.dto.LogRequest;
import com.idp.backend.dto.LogResponse;
import com.idp.backend.entity.LogEntity;
import com.idp.backend.entity.ServiceCatInfo;

import java.time.Instant;

public class LogMapper {

    public static LogEntity toEntity(LogRequest request,ServiceCatInfo service){

        LogEntity entity= new LogEntity();

        entity.setServiceId(service.getServiceId());
        entity.setTimeStamp(Instant.now());
        entity.setLevel(request.getLevel());
        entity.setMessage(request.getMessage());
        entity.setTraceId(request.getTraceId());
        entity.setSource(request.getSource());
        return entity;
    }

    public static LogResponse toResponse(LogEntity logEntity){

        LogResponse response= new LogResponse();
        response.setLogId(logEntity.getLogId());
        response.setTimeStamp(logEntity.getTimeStamp());
        response.setLevel(logEntity.getLevel());
        response.setMessage(logEntity.getMessage());
        response.setTraceId(logEntity.getTraceId());
        response.setSource(logEntity.getSource());
        return response;
    }
}
