package com.idp.backend.mapper;

import com.idp.backend.dto.AlertResponse;
import com.idp.backend.entity.AlertEntity;

public class AlertMapper {

    public static AlertResponse toResponse(AlertEntity entity){
        AlertResponse response = new AlertResponse();

        response.setId(entity.getId());
        response.setServiceId(entity.getServiceId());
        response.setType(entity.getType());
        response.setSeverity(entity.getSeverity());
        response.setStatus(entity.getStatus());
        response.setTriggeredAt(entity.getTriggeredAt());
        response.setResolvedAt(entity.getResolvedAt());
        response.setMessage(entity.getMessage());
        return response;
    }
}
