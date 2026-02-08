package com.idp.backend.mapper;

import com.idp.backend.dto.DeploymentRequest;
import com.idp.backend.dto.DeploymentResponse;
import com.idp.backend.entity.DeploymentEntity;

public class DeploymentMapper {

    public static DeploymentEntity toEntity(DeploymentRequest r) {
        DeploymentEntity e = new DeploymentEntity();
        e.setServiceId(r.getServiceId());
        e.setVersion(r.getVersion());
        e.setEnv(r.getEnv());
        e.setStatus(r.getStatus());
        e.setTriggeredBy(r.getTriggeredBy());
        return e;
    }

    public static DeploymentResponse toResponse(DeploymentEntity e) {
        return new DeploymentResponse(
                e.getDeployId(),
                e.getVersion(),
                e.getEnv(),
                e.getStatus(),
                e.getDeployedAt(),
                e.getTriggeredBy()
        );
    }
}

