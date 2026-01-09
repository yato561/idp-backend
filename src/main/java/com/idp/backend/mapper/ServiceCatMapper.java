package com.idp.backend.mapper;

import com.idp.backend.dto.ServiceCatRequest;
import com.idp.backend.dto.ServiceCatResponse;
import com.idp.backend.entity.ServiceCatInfo;

import java.time.LocalDateTime;

public class ServiceCatMapper {

    public static ServiceCatInfo toEntity(ServiceCatRequest request){
        ServiceCatInfo entity= new ServiceCatInfo();
        entity.setServiceName(request.getServiceName());
        entity.setRepoUrl(request.getRepoUrl());
        entity.setOwnerTeam(request.getOwnerTeam());
        entity.setRunTime(request.getRunTime());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    public static ServiceCatResponse toResponse(ServiceCatInfo entity){
        ServiceCatResponse response= new ServiceCatResponse();
        response.setServiceId(entity.getServiceId());
        response.setServiceName(entity.getServiceName());
        response.setRepoUrl(entity.getRepoUrl());
        response.setOwnerTeam(entity.getOwnerTeam());
        response.setRunTime(entity.getRunTime());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
