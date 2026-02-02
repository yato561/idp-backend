package com.idp.backend.service.impl;

import com.idp.backend.audit.Auditable;
import com.idp.backend.dao.DeploymentDao;
import com.idp.backend.dto.DeploySummaryResponse;
import com.idp.backend.dto.DeploymentRequest;
import com.idp.backend.dto.DeploymentResponse;
import com.idp.backend.dto.RollbackRequest;
import com.idp.backend.entity.DeploymentEntity;
import com.idp.backend.mapper.DeploymentMapper;
import com.idp.backend.service.DeploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class DeploymentServiceImpl implements DeploymentService {

    @Autowired
    private DeploymentDao dao;
    @Override
    @Auditable(action="DEPLOY", resource="SERVICE", resourceIdParam = "serviceId")

    public void register(DeploymentRequest request) {
        dao.save(DeploymentMapper.toEntity(request));
    }

    @Override
    public Page<DeploymentResponse> history(UUID serviceId, Pageable page) {
        return dao.findByService(serviceId, page).map(DeploymentMapper :: toResponse);
    }

    @Override
    public DeploymentResponse latest(UUID serviceId, String env) {
        DeploymentEntity entity= (env == null) ? dao.latest(serviceId):dao.latestByEnv(serviceId,env);
        return entity == null ? null : DeploymentMapper.toResponse(entity);
    }

    @Override
    public DeploySummaryResponse getSummary(UUID serviceId) {
        List<DeploymentEntity> deployments = dao.findAllByService(serviceId);

        Map<String, DeploySummaryResponse.DeploymentEnvSummary> map = new HashMap<>();

        for (DeploymentEntity d:deployments){
            map.putIfAbsent(
                    d.getEnv(),
                    new DeploySummaryResponse.DeploymentEnvSummary(
                            d.getVersion(),
                            d.getStatus(),
                            d.getDeployedAt(),
                            d.getTriggeredBy()
                    )
            );
        }
        return new DeploySummaryResponse(map);
    }

    @Override
    public void rollback(RollbackRequest request) {
        DeploymentEntity entity = new DeploymentEntity();

        entity.setServiceId(request.getServiceId());
        entity.setEnv(request.getEnv());
        entity.setVersion(request.getVersion());
        entity.setStatus("ROLLED_BACK");
        entity.setTriggeredBy(request.getTriggeredBy());

        dao.save(entity);
    }
}
