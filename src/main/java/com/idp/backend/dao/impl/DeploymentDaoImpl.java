package com.idp.backend.dao.impl;

import com.idp.backend.dao.DeploymentDao;
import com.idp.backend.entity.DeploymentEntity;
import com.idp.backend.repo.DeploymentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
@Component
public class DeploymentDaoImpl implements DeploymentDao {

    @Autowired
    private DeploymentRepo repo;
    @Override
    public DeploymentEntity save(DeploymentEntity d) {
        return repo.save(d);
    }

    @Override
    public Page<DeploymentEntity> findByService(UUID serviceId, Pageable page) {
        return repo.findByServiceId(serviceId, page);
    }


    @Override
    public DeploymentEntity latest(UUID serviceId) {
        return repo.findTopByServiceIdOrderByDeployedAtDesc(serviceId);
    }

    @Override
    public DeploymentEntity latestByEnv(UUID serviceId, String env) {
        return repo.findTopByServiceIdAndEnvOrderByDeployedAtDesc(serviceId, env);
    }

    @Override
    public List<DeploymentEntity> findAllByService(UUID serviceId) {
        return repo.findByServiceIdOrderByDeployedAtDesc(serviceId);
    }

}
