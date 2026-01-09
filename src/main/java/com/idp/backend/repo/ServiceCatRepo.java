package com.idp.backend.repo;

import com.idp.backend.entity.ServiceCatInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceCatRepo extends JpaRepository<ServiceCatInfo, UUID> {

    boolean existsByServiceName(String serviceName);

}
