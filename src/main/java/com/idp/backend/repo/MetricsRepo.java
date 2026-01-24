package com.idp.backend.repo;

import com.idp.backend.entity.MetricEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import java.util.UUID;

public interface MetricsRepo extends JpaRepository<MetricEntity, UUID>, JpaSpecificationExecutor<MetricEntity> {

    Page<MetricEntity> findByServiceId(
            UUID serviceId,
            Pageable pageable
    );

    MetricEntity findTopByServiceIdAndCpuUsageIsNotNullOrderByTimeStampDesc(UUID serviceId);

    MetricEntity findTopByServiceIdOrderByTimeStampDesc(UUID serviceId);
}
