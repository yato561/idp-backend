package com.idp.backend.dao;

import com.idp.backend.entity.DeploymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DeploymentDao {

    DeploymentEntity save(DeploymentEntity d);

    Page<DeploymentEntity> findByService(UUID serviceId, Pageable page);

    DeploymentEntity latestByEnv(UUID serviceId, String env);

    DeploymentEntity latest(UUID serviceId);

    List<DeploymentEntity> findAllByService(UUID serviceId);
}
