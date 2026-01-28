package com.idp.backend.repo;

import com.idp.backend.entity.DeploymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeploymentRepo extends JpaRepository<DeploymentEntity, UUID> {

    Page<DeploymentEntity> findByServiceId(UUID serviceId, Pageable page);

    DeploymentEntity findTopByServiceIdOrderByDeployedAtDesc(UUID serviceId);
}

